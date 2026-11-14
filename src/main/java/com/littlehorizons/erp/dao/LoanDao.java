package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;

public interface LoanDao {

    /** Atomic: locks the resource, requires it to be available, registers the loan and marks it as lent. */
    int lend(BorrowerType type, int resourceId, int borrowerId, LocalDate loanDate, LocalDate dueDate);

    /** Atomic: closes the loan and makes the resource available again. */
    void giveBack(BorrowerType type, int loanId, LocalDate returnDate);

    /** Flags active loans past their due date as overdue. @return number of loans flagged. */
    int markOverdue(LocalDate today);

    ReportTable activeLoans(BorrowerType type);

    ReportTable overdueLoans(LocalDate today);
}
