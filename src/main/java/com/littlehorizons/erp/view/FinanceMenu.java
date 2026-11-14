package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentOutcome;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Console menu for payment concepts, charges, payments and invoices. */
public class FinanceMenu {

    private final FinanceController controller;

    public FinanceMenu(FinanceController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Gestión financiera", List.of(
                new Menu.Item("Ver catálogo de conceptos de pago", () -> TablePrinter.print(controller.catalog())),
                new Menu.Item("Definir valor personalizado (beca / descuento)", this::setCustomAmount),
                new Menu.Item("Generar cargo a un estudiante", this::createCharge),
                new Menu.Item("Ver cargos pendientes de un estudiante", this::pendingCharges),
                new Menu.Item("Registrar pago y generar factura (.txt)", this::registerPayment),
                new Menu.Item("Listar facturas emitidas", () -> TablePrinter.print(controller.invoices())),
                new Menu.Item("Ver / reimprimir una factura", this::showInvoice),
                new Menu.Item("Historial de pagos de un estudiante", this::studentHistory),
                new Menu.Item("Historial de pagos de un acudiente", this::guardianHistory),
                new Menu.Item("Reporte de morosos (más de 30 días)", () -> TablePrinter.print(controller.delinquentStudents()))));
    }

    private PaymentConcept askConcept() {
        TablePrinter.print(controller.catalog());
        return controller.requireConcept(ConsoleInput.readInt("ID del concepto: ", 1, Integer.MAX_VALUE));
    }

    private void setCustomAmount() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        PaymentConcept concept = askConcept();
        System.out.println("Valor actual: " + money(controller.resolveAmount(student, concept)));
        BigDecimal amount = ConsoleInput.readDecimal("Nuevo valor: ", BigDecimal.ZERO, new BigDecimal("999999999"), 2);
        String reason = ConsoleInput.readText("Motivo (ej: Descuento por hermanos): ", true);
        controller.setCustomAmount(student, concept, amount, reason);
        System.out.println("  ✓ Valor personalizado guardado.");
    }

    private void createCharge() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        PaymentConcept concept = askConcept();
        String description = ConsoleInput.readText("Descripción (ej: Pensión Noviembre 2026): ", true);
        LocalDate dueDate = ConsoleInput.readDate("Fecha de vencimiento");
        int id = controller.createCharge(student, concept, description, dueDate);
        System.out.println("  ✓ Cargo " + id + " creado por " + money(controller.resolveAmount(student, concept)) + ".");
    }

    private void pendingCharges() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        System.out.println("Estudiante: " + student.getFullName());
        TablePrinter.print(controller.pendingCharges(student));
    }

    private void registerPayment() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        Guardian guardian = chooseGuardian(student);

        List<FinanceController.PaymentDraft> drafts = new ArrayList<>();
        do {
            drafts.add(askItem(student, drafts));
            printDrafts(drafts);
        } while (ConsoleInput.confirm("¿Agregar otro ítem a la misma factura?"));

        System.out.println("\nEstudiante: " + student.getFullName());
        System.out.println("Cliente:    " + guardian.getFullName());
        if (!ConsoleInput.confirm("¿Confirmar el pago y emitir la factura?")) {
            System.out.println("  Pago cancelado.");
            return;
        }
        String method = ConsoleInput.readChoice("Método de pago:",
                FinanceController.PAYMENT_METHODS.toArray(new String[0]));
        LocalDate date = ConsoleInput.readDateOrToday("Fecha de pago");

        PaymentOutcome outcome = controller.registerPayment(student, guardian, drafts, method, date);
        System.out.println("  ✓ Pago registrado. Factura " + outcome.receipt().invoiceNumber()
                + " | Recibo " + outcome.receipt().receiptNumber() + " | Total " + money(outcome.receipt().total()));
        if (outcome.file() != null) {
            System.out.println("  ✓ Factura guardada en: " + outcome.file().toAbsolutePath());
            if (ConsoleInput.confirm("¿Desea ver la factura ahora?")) {
                System.out.println(controller.reprintInvoice(outcome.receipt().invoiceNumber()).text());
            }
        } else {
            System.out.println("  ⚠ " + outcome.warning());
        }
    }

    /** Asks for one invoice item: a pending charge or a concept paid directly (with quantity). */
    private FinanceController.PaymentDraft askItem(Student student, List<FinanceController.PaymentDraft> current) {
        String mode = ConsoleInput.readChoice("¿Qué desea pagar?", "Un cargo pendiente", "Un concepto directo");
        if (mode.equals("Un cargo pendiente")) {
            TablePrinter.print(controller.pendingCharges(student));
            Charge charge = controller.requirePendingCharge(student,
                    ConsoleInput.readInt("ID del cargo a pagar: ", 1, Integer.MAX_VALUE));
            boolean repeated = current.stream().anyMatch(d -> d.charge() != null && d.charge().id().equals(charge.id()));
            if (repeated) {
                throw new com.littlehorizons.erp.exception.BusinessRuleException(
                        "Ese cargo ya está en la factura.");
            }
            return controller.createDraft(student, controller.requireConcept(charge.conceptId()), charge, 1);
        }
        PaymentConcept concept = askConcept();
        int quantity = ConsoleInput.readInt("Cantidad: ", 1, 50);
        return controller.createDraft(student, concept, null, quantity);
    }

    private void printDrafts(List<FinanceController.PaymentDraft> drafts) {
        System.out.println("\n  Ítems de la factura:");
        BigDecimal total = BigDecimal.ZERO;
        for (FinanceController.PaymentDraft d : drafts) {
            System.out.printf("   - %-34s x%-3d %16s%n", d.description(), d.quantity(), money(d.total()));
            total = total.add(d.total());
        }
        System.out.println("   " + "-".repeat(58));
        System.out.printf("   %-39s %16s%n", "TOTAL (IVA incluido)", money(total));
    }

    private void showInvoice() {
        TablePrinter.print(controller.invoices());
        String number = ConsoleInput.readText("Número de factura (ej: FE-000073 o 73): ", true);
        FinanceController.InvoiceCopy copy = controller.reprintInvoice(number);
        System.out.println();
        System.out.println(copy.text());
        if (copy.file() != null) {
            System.out.println("Archivo: " + copy.file().toAbsolutePath());
        }
    }

    private Guardian chooseGuardian(Student student) {
        List<Guardian> guardians = controller.guardiansOf(student);
        if (guardians.isEmpty()) {
            throw new com.littlehorizons.erp.exception.BusinessRuleException(
                    "El estudiante no tiene acudientes registrados.");
        }
        if (guardians.size() == 1) {
            return guardians.get(0);
        }
        String[] names = guardians.stream()
                .map(g -> g.getFullName() + " (" + g.relationship() + ")").toArray(String[]::new);
        String chosen = ConsoleInput.readChoice("¿Quién realiza el pago?", names);
        return guardians.get(List.of(names).indexOf(chosen));
    }

    private void studentHistory() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        System.out.println("Estudiante: " + student.getFullName());
        TablePrinter.print(controller.paymentHistory(student));
    }

    private void guardianHistory() {
        Guardian guardian = controller.requireGuardian(ConsoleInput.readText("Documento del acudiente: ", true));
        System.out.println("Acudiente: " + guardian.getFullName());
        TablePrinter.print(controller.paymentHistory(guardian));
    }

    private static String money(BigDecimal value) {
        return "$ " + NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO")).format(value);
    }
}
