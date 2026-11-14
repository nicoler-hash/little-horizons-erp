package com.littlehorizons.erp.model;

/** Extracurricular club ("clubes" table). */
public record Club(Integer id, String name, String description, Integer supervisorId, int maxCapacity,
                   String meetingSchedule) { }
