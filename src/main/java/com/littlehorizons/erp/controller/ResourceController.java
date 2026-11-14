package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Resource;

import java.time.LocalDate;
import java.util.List;

/** Lendable resources and the loan cycle (lend, return, overdue). */
public class ResourceController {

    public static final List<String> MANUAL_STATUSES = List.of("Disponible", "En Mantenimiento", "Dado de Baja");
    private static final int MAX_LOAN_DAYS = 60;

    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ResourceController(ResourceDao resourceDao, LoanDao loanDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    public List<IdName> categories() {
        return resourceDao.findCategories();
    }

    public int register(Resource resource) {
        if (resourceDao.findByCode(resource.code()).isPresent()) {
            throw new BusinessRuleException("Ya existe un recurso con el código " + resource.code() + ".");
        }
        return resourceDao.insert(resource);
    }

    public ReportTable list(String statusFilter) {
        return resourceDao.listReport(statusFilter);
    }

    public Resource requireByCode(String code) {
        return resourceDao.findByCode(code.toUpperCase()).orElseThrow(
                () -> new BusinessRuleException("No existe un recurso con el código " + code + "."));
    }

    /** Manual status change (maintenance, write-off, back to available). Lent resources cannot be changed here. */
    public void changeStatus(Resource resource, String status) {
        if (resource.status().equals("Prestado")) {
            throw new BusinessRuleException("El recurso está prestado: registre primero la devolución.");
        }
        if (!MANUAL_STATUSES.contains(status)) {
            throw new BusinessRuleException("Estado no válido.");
        }
        resourceDao.updateStatus(resource.id(), status);
    }

    public int lend(BorrowerType type, String borrowerDocument, Resource resource, LocalDate loanDate,
                    LocalDate dueDate) {
        if (!dueDate.isAfter(loanDate)) {
            throw new BusinessRuleException("La fecha de devolución debe ser posterior a la del préstamo.");
        }
        if (dueDate.isAfter(loanDate.plusDays(MAX_LOAN_DAYS))) {
            throw new BusinessRuleException("El plazo máximo de préstamo es de " + MAX_LOAN_DAYS + " días.");
        }
        int borrowerId;
        if (type == BorrowerType.STUDENT) {
            var student = studentDao.findByDocumentNumber(borrowerDocument).orElseThrow(
                    () -> new BusinessRuleException("No existe un estudiante con ese documento."));
            if (!student.status().equals("Activo")) {
                throw new BusinessRuleException("Solo los estudiantes activos pueden solicitar préstamos.");
            }
            borrowerId = student.id();
        } else {
            var teacher = teacherDao.findByDocumentNumber(borrowerDocument).orElseThrow(
                    () -> new BusinessRuleException("No existe un profesor con ese documento."));
            if (!teacher.status().equals("Activo")) {
                throw new BusinessRuleException("Solo los profesores activos pueden solicitar préstamos.");
            }
            borrowerId = teacher.id();
        }
        return loanDao.lend(type, resource.id(), borrowerId, loanDate, dueDate);
    }

    public void giveBack(BorrowerType type, int loanId, LocalDate returnDate) {
        if (returnDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de devolución no puede ser futura.");
        }
        loanDao.giveBack(type, loanId, returnDate);
    }

    public ReportTable activeLoans(BorrowerType type) {
        return loanDao.activeLoans(type);
    }

    /** Flags overdue loans and returns the report of everything not returned on time. */
    public ReportTable overdueReport() {
        LocalDate today = LocalDate.now();
        loanDao.markOverdue(today);
        return loanDao.overdueLoans(today);
    }
}
