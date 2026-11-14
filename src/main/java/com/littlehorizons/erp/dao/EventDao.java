package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.SchoolEvent;

import java.util.Optional;

public interface EventDao {

    int insert(SchoolEvent event);

    Optional<SchoolEvent> findById(int id);

    ReportTable listAll();

    boolean existsParticipant(int eventId, int studentId);

    void addParticipant(int eventId, int studentId, String role, String position);

    ReportTable participants(int eventId);

    ReportTable participationOfStudent(int studentId);

    ReportTable participationByType();
}
