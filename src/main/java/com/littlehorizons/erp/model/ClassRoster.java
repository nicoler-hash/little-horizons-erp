package com.littlehorizons.erp.model;

import java.util.List;

/** The students enrolled in a subject taught by a teacher, used to take attendance. */
public record ClassRoster(Teacher teacher, Subject subject, int academicYear, List<Student> students) { }
