package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;

/** Console menu for teachers, teaching load and weekly schedule. */
public class TeacherMenu {

    private static final String[] DAYS = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes"};

    private final TeacherController controller;

    public TeacherMenu(TeacherController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Gestión de profesores", List.of(
                new Menu.Item("Registrar profesor", this::register),
                new Menu.Item("Listar profesores", () -> TablePrinter.print(controller.list())),
                new Menu.Item("Asignar materia a un profesor", this::assignSubject),
                new Menu.Item("Registrar horario de clase", this::addScheduleSlot),
                new Menu.Item("Ver materias y horario de un profesor", this::showLoad)));
    }

    private void register() {
        System.out.println("\n== Nuevo profesor ==");
        Teacher teacher = new Teacher(
                null,
                ConsoleInput.readOption("Tipo de documento", "CC", "CE", "Pasaporte"),
                ConsoleInput.readText("Número de documento: ", true),
                ConsoleInput.readText("Primer nombre: ", true),
                ConsoleInput.readText("Segundo nombre (opcional): ", false),
                ConsoleInput.readText("Primer apellido: ", true),
                ConsoleInput.readText("Segundo apellido (opcional): ", false),
                ConsoleInput.readText("Correo electrónico: ", true),
                ConsoleInput.readText("Teléfono (opcional): ", false),
                ConsoleInput.readText("Especialización (ej: Matemáticas): ", false),
                ConsoleInput.readDateOrToday("Fecha de contratación"),
                "Activo");
        int id = controller.register(teacher);
        System.out.println("  ✓ Profesor registrado con ID " + id + ": " + teacher.getFullName());
    }

    private void assignSubject() {
        Teacher teacher = controller.requireByDocument(ConsoleInput.readText("Documento del profesor: ", true));
        Subject subject = controller.requireSubject(ConsoleInput.readText("Código de la materia (ej: MAT-5): ", true));
        int year = ConsoleInput.readInt("Año académico: ", 2000, 2100);
        String section = ConsoleInput.readOption("Sección", "A", "B", "C");
        controller.assignSubject(teacher, subject, year, section);
        System.out.println("  ✓ " + subject.name() + " asignada a " + teacher.getFullName() + ".");
    }

    private void addScheduleSlot() {
        Teacher teacher = controller.requireByDocument(ConsoleInput.readText("Documento del profesor: ", true));
        Subject subject = controller.requireSubject(ConsoleInput.readText("Código de la materia: ", true));
        int year = ConsoleInput.readInt("Año académico: ", 2000, 2100);
        String section = ConsoleInput.readOption("Sección", "A", "B", "C");
        String day = ConsoleInput.readChoice("Día de la semana:", DAYS);
        var start = ConsoleInput.readTime("Hora de inicio");
        var end = ConsoleInput.readTime("Hora de fin");
        controller.addScheduleSlot(teacher, subject, year, section, day, start, end);
        System.out.println("  ✓ Horario registrado.");
    }

    private void showLoad() {
        Teacher teacher = controller.requireByDocument(ConsoleInput.readText("Documento del profesor: ", true));
        int year = ConsoleInput.readInt("Año académico (" + LocalDate.now().getYear() + "): ", 2000, 2100);
        System.out.println("Profesor: " + teacher.getFullName());
        TablePrinter.print(controller.assignments(teacher, year));
        TablePrinter.print(controller.schedule(teacher, year));
    }
}
