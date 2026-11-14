package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.ReportTable;

import java.util.List;
import java.util.Optional;

public interface GuardianDao {

    int insert(Guardian guardian);

    Optional<Guardian> findByDocumentNumber(String documentNumber);

    void linkToStudent(int studentId, int guardianId, boolean main, boolean emergencyContact);

    boolean isLinked(int studentId, int guardianId);

    /** Guardians of a student, main guardian first. */
    List<Guardian> findByStudent(int studentId);

    ReportTable studentsOfGuardian(int guardianId);
}
