package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** School event ("eventos" table). Named SchoolEvent to avoid clashing with java.util.EventObject types. */
public record SchoolEvent(Integer id, String name, String description, String type, LocalDate date,
                          String place, boolean hasCompetition, Integer organizerId) { }
