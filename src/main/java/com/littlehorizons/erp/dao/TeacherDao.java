package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Teacher;

import java.util.Optional;

public interface TeacherDao {

    int insert(Teacher teacher);

    Optional<Teacher> findByDocumentNumber(String documentNumber);

    boolean existsByEmail(String email);

    ReportTable listAll();
}
