package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.BillingDao;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentOutcome;
import com.littlehorizons.erp.model.InvoiceRequest;
import com.littlehorizons.erp.model.PaymentItem;
import com.littlehorizons.erp.model.PaymentReceipt;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.InvoiceTextGenerator;
import com.littlehorizons.erp.util.Names;
import com.littlehorizons.erp.util.SchoolConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Payment concepts, charges, payments, delinquency and electronic invoices. */
public class FinanceController {

    public static final List<String> PAYMENT_METHODS =
            List.of("Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cheque");
    public static final int DELINQUENCY_DAYS = 30;
    private static final Path INVOICE_DIRECTORY = Path.of("invoices");
    private static final String ENROLLMENT_CONCEPT_CODE = "MAT";
    private static final int MAX_INVOICE_LINES = 15;
    private static final int MAX_QUANTITY = 50;

    private final PaymentConceptDao conceptDao;
    private final ChargeDao chargeDao;
    private final BillingDao billingDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;

    public FinanceController(PaymentConceptDao conceptDao, ChargeDao chargeDao, BillingDao billingDao,
                             StudentDao studentDao, GuardianDao guardianDao) {
        this.conceptDao = conceptDao;
        this.chargeDao = chargeDao;
        this.billingDao = billingDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
    }

    // ---------- lookups ----------

    public Student requireStudent(String document) {
        return studentDao.findByDocumentNumber(document).orElseThrow(
                () -> new BusinessRuleException("No existe un estudiante con el documento " + document + "."));
    }

    public Guardian requireGuardian(String document) {
        return guardianDao.findByDocumentNumber(document).orElseThrow(
                () -> new BusinessRuleException("No existe un acudiente con el documento " + document + "."));
    }

    public List<Guardian> guardiansOf(Student student) {
        return guardianDao.findByStudent(student.id());
    }

    public PaymentConcept requireConcept(int conceptId) {
        return conceptDao.findById(conceptId).orElseThrow(
                () -> new BusinessRuleException("El concepto de pago no existe."));
    }

    public Charge requirePendingCharge(Student student, int chargeId) {
        Charge charge = chargeDao.findById(chargeId).orElseThrow(
                () -> new BusinessRuleException("El cargo no existe."));
        if (charge.studentId() != student.id() || !charge.status().equals("Pendiente")) {
            throw new BusinessRuleException("El cargo no está pendiente para este estudiante.");
        }
        return charge;
    }

    // ---------- catalog and amounts ----------

    public ReportTable catalog() {
        return conceptDao.catalogReport();
    }

    /** The amount a student pays for a concept: his or her custom amount (discount/scholarship) or the base. */
    public BigDecimal resolveAmount(Student student, PaymentConcept concept) {
        return conceptDao.findCustomAmount(student.id(), concept.id()).orElse(concept.baseAmount());
    }

    public void setCustomAmount(Student student, PaymentConcept concept, BigDecimal amount, String reason) {
        if (amount.signum() < 0) {
            throw new BusinessRuleException("El valor no puede ser negativo.");
        }
        conceptDao.saveCustomAmount(student.id(), concept.id(), amount, reason);
    }

    // ---------- charges ----------

    public int createCharge(Student student, PaymentConcept concept, String description, LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today.minusYears(1))) {
            throw new BusinessRuleException("La fecha de vencimiento es demasiado antigua.");
        }
        return chargeDao.insert(new Charge(null, student.id(), concept.id(), description,
                resolveAmount(student, concept), today, dueDate, "Pendiente"));
    }

    public ReportTable pendingCharges(Student student) {
        return chargeDao.pendingReport(student.id());
    }

    public List<Charge> pendingChargeList(Student student) {
        return chargeDao.findPendingByStudent(student.id());
    }

    public ReportTable delinquentStudents() {
        LocalDate today = LocalDate.now();
        return chargeDao.delinquentReport(today.minusDays(DELINQUENCY_DAYS), today);
    }

    // ---------- payments and invoices ----------

    /** One item of an invoice being prepared: a concept (optionally paying a charge), quantity and unit price. */
    public record PaymentDraft(PaymentConcept concept, Charge charge, int quantity, BigDecimal unitAmount,
                               String description) {

        public BigDecimal total() {
            return unitAmount.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Prepares an invoice item. Charges are paid in full (quantity 1) at the amount that was charged. */
    public PaymentDraft createDraft(Student student, PaymentConcept concept, Charge charge, int quantity) {
        if (charge != null) {
            if (charge.conceptId() != concept.id()) {
                throw new BusinessRuleException("El cargo no corresponde al concepto seleccionado.");
            }
            return new PaymentDraft(concept, charge, 1, charge.amount(), charge.description());
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessRuleException("La cantidad debe estar entre 1 y " + MAX_QUANTITY + ".");
        }
        return new PaymentDraft(concept, null, quantity, resolveAmount(student, concept), concept.name());
    }

    /**
     * Registers the payment of all the items (atomically with the invoice number and the invoice) and then writes
     * the .txt invoice. If the file cannot be written the payment stays registered and the outcome has a warning.
     */
    public PaymentOutcome registerPayment(Student student, Guardian guardian, List<PaymentDraft> drafts,
                                          String method, LocalDate paymentDate) {
        if (drafts.isEmpty()) {
            throw new BusinessRuleException("Agregue al menos un ítem a la factura.");
        }
        if (drafts.size() > MAX_INVOICE_LINES) {
            throw new BusinessRuleException("Una factura admite hasta " + MAX_INVOICE_LINES + " ítems.");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de pago no puede ser futura.");
        }
        if (!guardianDao.isLinked(student.id(), guardian.id())) {
            throw new BusinessRuleException("El acudiente no está asociado a este estudiante.");
        }
        long distinctCharges = drafts.stream().filter(d -> d.charge() != null).map(d -> d.charge().id()).distinct().count();
        if (distinctCharges != drafts.stream().filter(d -> d.charge() != null).count()) {
            throw new BusinessRuleException("No puede pagar el mismo cargo dos veces en una factura.");
        }

        List<PaymentItem> items = drafts.stream()
                .map(d -> new PaymentItem(d.concept().id(), d.charge() == null ? null : d.charge().id(),
                        d.quantity(), d.unitAmount(), d.description(),
                        d.concept().code().equals(ENROLLMENT_CONCEPT_CODE) ? "Anual" : quarterOf(paymentDate)))
                .toList();
        PaymentReceipt receipt = billingDao.registerPayment(
                new InvoiceRequest(student.id(), guardian.id(), method, paymentDate, paymentDate.getYear(), items),
                (number, date, total) -> CufeGenerator.generate(number, SchoolConfig.nit(), date, total));

        InvoiceData invoice = new InvoiceData(
                receipt.invoiceNumber(), receipt.issueDate(), receipt.cufe(),
                InvoiceData.resolutionText(receipt.resolutionNumber(), receipt.resolutionDate(), receipt.prefix(),
                        receipt.rangeFrom(), receipt.rangeTo()),
                guardian.getFullName(), guardian.documentType() + " " + guardian.documentNumber(),
                orDash(guardian.address()), orDash(guardian.mainPhone()), orDash(guardian.email()),
                Names.join(student.firstName(), student.lastName(), student.secondLastName()),
                receipt.gradeName(), paymentDate.getYear(), receipt.lines(), receipt.subtotal(), receipt.vat(),
                receipt.total(), method, receipt.receiptNumber());
        try {
            Path file = InvoiceTextGenerator.generate(invoice, INVOICE_DIRECTORY);
            billingDao.updateInvoiceFile(receipt.invoiceId(), file.getFileName().toString(),
                    file.toAbsolutePath().toString());
            return new PaymentOutcome(receipt, file, null);
        } catch (IOException e) {
            AppLogger.error("Could not write invoice file " + receipt.invoiceNumber(), e);
            return new PaymentOutcome(receipt, null,
                    "El pago quedó registrado, pero no se pudo crear el archivo .txt de la factura.");
        }
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String quarterOf(LocalDate date) {
        int month = date.getMonthValue();
        if (month <= 3) {
            return "I";
        }
        if (month <= 6) {
            return "II";
        }
        return month <= 9 ? "III" : "IV";
    }

    public ReportTable paymentHistory(Student student) {
        return billingDao.historyOfStudent(student.id());
    }

    public ReportTable paymentHistory(Guardian guardian) {
        return billingDao.historyOfGuardian(guardian.id());
    }

    /** An invoice rebuilt from the database, plus the file it was written to (null if writing failed). */
    public record InvoiceCopy(String text, Path file) { }

    public ReportTable invoices() {
        return billingDao.invoiceList();
    }

    /** Accepts "FE-000073" or just "73". */
    private static String normalizeInvoiceNumber(String input) {
        String value = input.trim().toUpperCase();
        return value.matches("\\d+") ? String.format("FE-%06d", Long.parseLong(value)) : value;
    }

    /** Rebuilds an issued invoice from the database, (re)writes its .txt file and returns the text. */
    public InvoiceCopy reprintInvoice(String invoiceNumber) {
        String number = normalizeInvoiceNumber(invoiceNumber);
        InvoiceData data = billingDao.findInvoiceData(number).orElseThrow(
                () -> new BusinessRuleException("No existe la factura " + number + "."));
        Path file = null;
        try {
            file = InvoiceTextGenerator.generate(data, INVOICE_DIRECTORY);
            billingDao.updateInvoiceFileByNumber(number, file.getFileName().toString(),
                    file.toAbsolutePath().toString());
        } catch (IOException e) {
            AppLogger.error("Could not write invoice file " + number, e);
        }
        return new InvoiceCopy(InvoiceTextGenerator.render(data), file);
    }

    public ReportTable revenueByConcept(int year) {
        return billingDao.revenueByConcept(year);
    }
}
