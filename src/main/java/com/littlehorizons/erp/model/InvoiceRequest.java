package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/** Everything needed to register the payment of one or more items and issue a single invoice. */
public record InvoiceRequest(int studentId, int guardianId, String method, LocalDate paymentDate, int academicYear,
                             List<PaymentItem> items) { }
