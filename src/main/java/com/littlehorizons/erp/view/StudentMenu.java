package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Console menu for students, guardians and enrollment. User-facing text is Spanish; code is English. */
public class StudentMenu {

    private static final List<String> RELATIONSHIPS =
            List.of("Padre", "Madre", "Abuelo/a", "Tío/a", "Hermano/a mayor", "Tutor Legal");

    private final StudentController controller;

    public StudentMenu(StudentController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Gestión de estudiantes", List.of(
                new Menu.Item("Registrar estudiante (con su acudiente)", this::register),
                new Menu.Item("Listar estudiantes", this::listAll),
                new Menu.Item("Buscar estudiante por documento", this::search),
                new Menu.Item("Actualizar datos de contacto", this::updateContact),
                new Menu.Item("Cambiar estado (Activo/Graduado/Retirado/Suspendido)", this::changeStatus),
                new Menu.Item("Matricular estudiante en un grado", this::enroll),
                new Menu.Item("Asociar un acudiente a un estudiante existente", this::addGuardian),
                new Menu.Item("Ver estudiantes a cargo de un acudiente", this::studentsOfGuardian),
                new Menu.Item("Resumen de estudiantes por estado", this::showSummary)));
    }

    // ---------- registration ----------

    private void register() {
        System.out.println("\n== Nuevo estudiante ==");
        Student student = new Student(
                null,
                ConsoleInput.readOption("Tipo de documento", "TI", "CC", "CE", "Pasaporte"),
                ConsoleInput.readText("Número de documento: ", true),
                ConsoleInput.readText("Primer nombre: ", true),
                ConsoleInput.readText("Segundo nombre (opcional): ", false),
                ConsoleInput.readText("Primer apellido: ", true),
                ConsoleInput.readText("Segundo apellido (opcional): ", false),
                ConsoleInput.readDate("Fecha de nacimiento"),
                ConsoleInput.readOption("Género", "M", "F", "Otro"),
                ConsoleInput.readText("Dirección (opcional): ", false),
                ConsoleInput.readText("Teléfono (opcional): ", false),
                ConsoleInput.readText("Correo (opcional): ", false),
                ConsoleInput.readDateOrToday("Fecha de ingreso"),
                "Activo");
        int id = controller.register(student);
        System.out.println("  ✓ Estudiante registrado con ID " + id + ": " + student.getFullName());
        System.out.println("  Todo estudiante necesita al menos un acudiente.");
        try {
            linkGuardians(id);
        } catch (com.littlehorizons.erp.exception.OperationCancelledException e) {
            System.out.println("  ⚠ El estudiante quedó sin acudiente. Use la opción 'Asociar un acudiente'.");
        }
    }

    /** Links guardians to the student; at least one is required, so errors let the user retry. */
    private void linkGuardians(int studentId) {
        boolean linkedOne = false;
        do {
            try {
                String document = ConsoleInput.readText("Documento del acudiente: ", true);
                Optional<Guardian> existing = controller.findGuardian(document);
                int guardianId;
                if (existing.isPresent()) {
                    System.out.println("  Acudiente encontrado: " + existing.get().getFullName());
                    guardianId = existing.get().id();
                } else {
                    System.out.println("  Acudiente nuevo: complete sus datos.");
                    guardianId = registerGuardian(document);
                }
                controller.linkGuardian(studentId, guardianId);
                System.out.println("  ✓ Acudiente asociado.");
                linkedOne = true;
            } catch (BusinessRuleException e) {
                System.out.println("  ✗ " + e.getMessage());
            }
        } while (!linkedOne || ConsoleInput.confirm("¿Asociar otro acudiente?"));
    }

    private int registerGuardian(String document) {
        Guardian guardian = new Guardian(
                null,
                ConsoleInput.readOption("Tipo de documento", "CC", "CE", "Pasaporte"),
                document,
                ConsoleInput.readText("Primer nombre: ", true),
                ConsoleInput.readText("Segundo nombre (opcional): ", false),
                ConsoleInput.readText("Primer apellido: ", true),
                ConsoleInput.readText("Segundo apellido (opcional): ", false),
                ConsoleInput.readChoice("Parentesco:", RELATIONSHIPS.toArray(new String[0])),
                ConsoleInput.readText("Teléfono principal: ", true),
                ConsoleInput.readText("Correo (opcional): ", false),
                ConsoleInput.readText("Dirección (opcional): ", false),
                ConsoleInput.confirm("¿Es responsable de los pagos?"));
        return controller.registerGuardian(guardian);
    }

    private void addGuardian() {
        Student student = controller.requireByDocument(ConsoleInput.readText("Documento del estudiante: ", true));
        linkGuardians(student.id());
    }

    // ---------- queries ----------

    private void listAll() {
        List<Student> students = controller.listAll();
        if (students.isEmpty()) {
            System.out.println("  (No hay estudiantes registrados)");
            return;
        }
        System.out.printf("%n%-5s %-5s %-14s %-38s %-10s%n", "ID", "Tipo", "Documento", "Nombre", "Estado");
        System.out.println("-".repeat(76));
        students.forEach(s -> System.out.printf("%-5d %-5s %-14s %-38s %-10s%n",
                s.id(), s.documentType(), s.documentNumber(), s.getFullName(), s.status()));
        System.out.println("(" + students.size() + " estudiantes)");
    }

    private void search() {
        Student s = controller.requireByDocument(ConsoleInput.readText("Número de documento: ", true));
        System.out.println("\nID:         " + s.id());
        System.out.println("Nombre:     " + s.getFullName());
        System.out.println("Documento:  " + s.documentType() + " " + s.documentNumber());
        System.out.println("Nacimiento: " + s.birthDate());
        System.out.println("Estado:     " + s.status());
        System.out.println("Dirección:  " + orDash(s.address()));
        System.out.println("Teléfono:   " + orDash(s.phone()));
        System.out.println("Correo:     " + orDash(s.email()));
        System.out.println("Acudientes:");
        controller.guardiansOf(s.id()).forEach(g -> System.out.printf("  - %s (%s) tel. %s%n",
                g.getFullName(), g.relationship(), g.mainPhone()));
        TablePrinter.print(controller.enrollmentHistory(s.id()));
    }

    private void studentsOfGuardian() {
        Optional<Guardian> guardian = controller.findGuardian(ConsoleInput.readText("Documento del acudiente: ", true));
        if (guardian.isEmpty()) {
            System.out.println("  ✗ No existe un acudiente con ese documento.");
            return;
        }
        System.out.println("Acudiente: " + guardian.get().getFullName());
        TablePrinter.print(controller.studentsOfGuardian(guardian.get().id()));
    }

    private void showSummary() {
        System.out.println();
        controller.countByStatus().forEach((status, total) -> System.out.printf("%-12s %d%n", status, total));
    }

    // ---------- updates ----------

    private void updateContact() {
        Student student = controller.requireByDocument(ConsoleInput.readText("Documento del estudiante: ", true));
        String address = ConsoleInput.readText("Nueva dirección: ", false);
        String phone = ConsoleInput.readText("Nuevo teléfono: ", false);
        String email = ConsoleInput.readText("Nuevo correo: ", false);
        controller.updateContact(student.id(), address, phone, email);
        System.out.println("  ✓ Datos actualizados.");
    }

    private void changeStatus() {
        Student student = controller.requireByDocument(ConsoleInput.readText("Documento del estudiante: ", true));
        String status = ConsoleInput.readOption("Nuevo estado", "Activo", "Graduado", "Retirado", "Suspendido");
        if (ConsoleInput.confirm("¿Cambiar a " + status + " a " + student.getFullName() + "?")) {
            controller.changeStatus(student, status);
            System.out.println("  ✓ Estado actualizado.");
        }
    }

    private void enroll() {
        Student student = controller.requireByDocument(ConsoleInput.readText("Documento del estudiante: ", true));
        List<IdName> grades = controller.gradeLevels();
        grades.forEach(g -> System.out.printf("  %2d. %s%n", grades.indexOf(g), g.name()));
        int level = ConsoleInput.readInt("Grado (0 = Preescolar ... 11 = Once): ", 0, grades.size() - 1);
        int year = ConsoleInput.readInt("Año académico: ", 2000, 2100);
        controller.enroll(student, grades.get(level).id(), year);
        System.out.println("  ✓ " + student.getFullName() + " matriculado en " + grades.get(level).name() + " (" + year + ").");
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
