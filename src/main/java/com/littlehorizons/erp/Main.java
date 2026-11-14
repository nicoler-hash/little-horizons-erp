package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.*;
import com.littlehorizons.erp.dao.impl.*;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.*;

import java.sql.Connection;
import java.sql.SQLException;

/** Entry point and composition root: wires DAOs, controllers and views together. */
public class Main {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("The connection is not valid");
            }
            System.out.println("Conexión a la base de datos exitosa.");
        } catch (SQLException e) {
            AppLogger.error("Could not connect to the database", e);
            System.out.println("No se pudo conectar a MySQL: " + e.getMessage());
            System.out.println("Revise src/main/resources/db.properties y que docs/database.sql esté ejecutado.");
            return;
        }

        // Data access layer
        StudentDao studentDao = new JdbcStudentDao();
        GuardianDao guardianDao = new JdbcGuardianDao();
        EnrollmentDao enrollmentDao = new JdbcEnrollmentDao();
        TeacherDao teacherDao = new JdbcTeacherDao();
        SubjectDao subjectDao = new JdbcSubjectDao();
        TeachingAssignmentDao assignmentDao = new JdbcTeachingAssignmentDao();
        AcademicRecordDao recordDao = new JdbcAcademicRecordDao();
        AttendanceDao attendanceDao = new JdbcAttendanceDao();
        ClubDao clubDao = new JdbcClubDao();
        EventDao eventDao = new JdbcEventDao();
        PaymentConceptDao conceptDao = new JdbcPaymentConceptDao();
        ChargeDao chargeDao = new JdbcChargeDao();
        BillingDao billingDao = new JdbcBillingDao();
        ResourceDao resourceDao = new JdbcResourceDao();
        LoanDao loanDao = new JdbcLoanDao();
        ReportDao reportDao = new JdbcReportDao();

        // Controllers (business rules)
        StudentController studentController = new StudentController(studentDao, guardianDao, enrollmentDao);
        TeacherController teacherController = new TeacherController(teacherDao, subjectDao, assignmentDao);
        AcademicController academicController = new AcademicController(subjectDao, studentDao, teacherDao,
                enrollmentDao, assignmentDao, recordDao, attendanceDao);
        ExtracurricularController extracurricularController =
                new ExtracurricularController(clubDao, eventDao, studentDao, teacherDao);
        FinanceController financeController =
                new FinanceController(conceptDao, chargeDao, billingDao, studentDao, guardianDao);
        ResourceController resourceController = new ResourceController(resourceDao, loanDao, studentDao, teacherDao);
        ReportController reportController = new ReportController(reportDao, enrollmentDao, clubDao, eventDao);

        // Views
        new MainMenu(
                new StudentMenu(studentController),
                new AcademicMenu(academicController),
                new TeacherMenu(teacherController),
                new ExtracurricularMenu(extracurricularController),
                new FinanceMenu(financeController),
                new ResourceMenu(resourceController),
                new ReportMenu(reportController, financeController, academicController, resourceController)
        ).show();
    }
}
