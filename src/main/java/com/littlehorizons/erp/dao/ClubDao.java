package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ReportTable;

import java.time.LocalDate;
import java.util.Optional;

public interface ClubDao {

    enum JoinResult { JOINED, CLUB_NOT_FOUND, ALREADY_MEMBER, CLUB_FULL }

    int insert(Club club);

    Optional<Club> findByName(String name);

    ReportTable listActive();

    /** Atomic: locks the club, checks the capacity and registers the membership. */
    JoinResult join(int studentId, int clubId, LocalDate date);

    boolean leave(int studentId, int clubId, LocalDate date, String reason);

    ReportTable members(int clubId);

    ReportTable clubsOfStudent(int studentId);

    ReportTable participationReport();
}
