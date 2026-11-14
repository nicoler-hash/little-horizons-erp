package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.model.ClassRoster;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Console menu for subjects, grades and attendance. */
public class AcademicMenu {

    private final AcademicController controller;

    public AcademicMenu(AcademicController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Gestión académica", List.of(
                new Menu.Item("Listar materias", this::listSubjects),
                new Menu.Item("Inscribir estudiante en una materia", this::enrollInSubject),
                new Menu.Item("Inscribir estudiante en todas las materias de su grado", this::enrollInAll),
                new Menu.Item("Ver materias inscritas de un estudiante", this::showEnrollments),
                new Menu.Item("Registrar nota", this::recordGrade),
                new Menu.Item("Historial académico de un estudiante", this::showHistory),
                new Menu.Item("Promedios de un estudiante", this::showAverages),
                new Menu.Item("Tomar asistencia de una clase", this::takeAttendance),
                new Menu.Item("Reporte de inasistencias preocupantes", this::absenceReport),
                new Menu.Item("Asistencia de un estudiante", this::studentAttendance)));
    }

    private int askYear() {
        return ConsoleInput.readInt("Año académico (ej: " + LocalDate.now().getYear() + "): ", 2000, 2100);
    }

    private void listSubjects() {
        List<IdName> grades = controller.gradeLevels();
        grades.forEach(g -> System.out.printf("  %2d. %s%n", grades.indexOf(g), g.name()));
        int level = ConsoleInput.readInt("Grado a consultar (0-" + (grades.size() - 1) + ", 99 = todos): ", 0, 99);
        Integer gradeId = level >= grades.size() ? null : grades.get(level).id();
        TablePrinter.print(controller.listSubjects(gradeId));
    }

    private void enrollInSubject() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        Subject subject = controller.requireSubject(ConsoleInput.readText("Código de la materia: ", true));
        int year = askYear();
        String period = ConsoleInput.readChoice("Período:", AcademicController.PERIODS.toArray(new String[0]));
        String section = ConsoleInput.readOption("Sección", "A", "B", "C");
        controller.enrollInSubject(student, subject, year, period, section);
        System.out.println("  ✓ Estudiante inscrito en " + subject.name() + " (período " + period + ").");
    }

    private void enrollInAll() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        int year = askYear();
        String period = ConsoleInput.readChoice("Período:", AcademicController.PERIODS.toArray(new String[0]));
        String section = ConsoleInput.readOption("Sección", "A", "B", "C");
        int added = controller.enrollInAllSubjects(student, year, period, section);
        System.out.println("  ✓ Inscripciones nuevas: " + added);
    }

    private void showEnrollments() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        TablePrinter.print(controller.enrollmentsOf(student, askYear()));
    }

    private void recordGrade() {
        Teacher teacher = controller.requireTeacher(ConsoleInput.readText("Documento del profesor: ", true));
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        Subject subject = controller.requireSubject(ConsoleInput.readText("Código de la materia: ", true));
        int year = askYear();
        String period = ConsoleInput.readChoice("Período:", AcademicController.GRADE_PERIODS.toArray(new String[0]));
        BigDecimal score = ConsoleInput.readDecimal("Nota (0.0 a 5.0): ", AcademicController.MIN_SCORE,
                AcademicController.MAX_SCORE, 1);
        String notes = ConsoleInput.readText("Observaciones (opcional): ", false);
        controller.recordGrade(teacher, student, subject, year, period, score, notes);
        System.out.println("  ✓ Nota " + score.setScale(1) + " registrada para " + student.getFullName()
                + " en " + subject.name() + " (período " + period + ").");
    }

    private void showHistory() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        System.out.println("Estudiante: " + student.getFullName());
        TablePrinter.print(controller.gradeHistory(student));
    }

    private void showAverages() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        int year = askYear();
        System.out.println("Estudiante: " + student.getFullName());
        TablePrinter.print(controller.subjectAverages(student, year));
        System.out.println("\nPromedio general por período:");
        AcademicController.PERIODS.forEach(period -> {
            Optional<BigDecimal> average = controller.generalAverage(student, year, period);
            System.out.printf("  Período %-3s %s%n", period, average.map(BigDecimal::toPlainString).orElse("sin notas"));
        });
    }

    private void takeAttendance() {
        Teacher teacher = controller.requireTeacher(ConsoleInput.readText("Documento del profesor: ", true));
        Subject subject = controller.requireSubject(ConsoleInput.readText("Código de la materia: ", true));
        int year = askYear();
        ClassRoster roster = controller.loadRoster(teacher, subject, year);
        LocalDate date = ConsoleInput.readDateOrToday("Fecha de la clase");

        System.out.println("\nEstudiantes de " + subject.name() + " (todos quedan 'Presente' salvo las novedades):");
        roster.students().forEach(s -> System.out.printf("  %-14s %s%n", s.documentNumber(), s.getFullName()));

        Map<Integer, String> exceptions = new LinkedHashMap<>();
        while (true) {
            String document = ConsoleInput.readText("Documento con novedad (ENTER para terminar): ", false);
            if (document == null) {
                break;
            }
            Optional<Student> found = roster.students().stream()
                    .filter(s -> s.documentNumber().equals(document)).findFirst();
            if (found.isEmpty()) {
                System.out.println("  ✗ Ese estudiante no está en esta clase.");
                continue;
            }
            String status = ConsoleInput.readChoice("Estado:", "Ausente", "Tardanza", "Excusa Médica", "Presente");
            exceptions.put(found.get().id(), status);
        }
        int saved = controller.saveAttendance(roster, date, exceptions);
        System.out.println("  ✓ Asistencia guardada: " + saved + " estudiantes, " + exceptions.size() + " novedades.");
    }

    private void absenceReport() {
        int weeks = ConsoleInput.readInt("¿Cuántas semanas hacia atrás? (1-52): ", 1, 52);
        TablePrinter.print(controller.absenceAlerts(weeks));
    }

    private void studentAttendance() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        int days = ConsoleInput.readInt("¿Cuántos días hacia atrás? (1-365): ", 1, 365);
        TablePrinter.print(controller.attendanceOf(student, days));
    }
}
