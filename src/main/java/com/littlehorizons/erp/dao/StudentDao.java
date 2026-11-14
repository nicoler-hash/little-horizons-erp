package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Optional;

/** Persistence contract for students. Controllers depend on this interface, not on JDBC. */
public interface StudentDao {

    int insert(Student student);

    Optional<Student> findByDocumentNumber(String documentNumber);

    List<Student> findAll();

    boolean updateContact(int id, String address, String phone, String email);

    boolean updateStatus(int id, String status);
}
