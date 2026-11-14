package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Console menu for clubs and events. */
public class ExtracurricularMenu {

    private final ExtracurricularController controller;

    public ExtracurricularMenu(ExtracurricularController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Actividades extracurriculares", List.of(
                new Menu.Item("Crear club", this::createClub),
                new Menu.Item("Listar clubes (cupos)", () -> TablePrinter.print(controller.listClubs())),
                new Menu.Item("Inscribir estudiante en un club", this::joinClub),
                new Menu.Item("Retirar estudiante de un club", this::leaveClub),
                new Menu.Item("Ver miembros de un club", this::clubMembers),
                new Menu.Item("Ver clubes de un estudiante", this::clubsOfStudent),
                new Menu.Item("Crear evento", this::createEvent),
                new Menu.Item("Calendario de eventos", () -> TablePrinter.print(controller.listEvents())),
                new Menu.Item("Registrar participante en un evento", this::addParticipant),
                new Menu.Item("Ver participantes de un evento", this::eventParticipants),
                new Menu.Item("Ver eventos de un estudiante", this::eventsOfStudent)));
    }

    private void createClub() {
        String name = ConsoleInput.readText("Nombre del club: ", true);
        String description = ConsoleInput.readText("Descripción (opcional): ", false);
        Integer supervisorId = null;
        String teacherDocument = ConsoleInput.readText("Documento del profesor encargado (ENTER = ninguno): ", false);
        if (teacherDocument != null) {
            Optional<Teacher> teacher = controller.findTeacher(teacherDocument);
            if (teacher.isEmpty()) {
                System.out.println("  ✗ No existe un profesor con ese documento.");
                return;
            }
            supervisorId = teacher.get().id();
        }
        int capacity = ConsoleInput.readInt("Cupo máximo: ", 1, 200);
        String schedule = ConsoleInput.readText("Horario de reunión (ej: Martes 3-5 PM): ", false);
        int id = controller.createClub(new Club(null, name, description, supervisorId, capacity, schedule));
        System.out.println("  ✓ Club creado con ID " + id + ".");
    }

    private void joinClub() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        TablePrinter.print(controller.listClubs());
        int clubId = ConsoleInput.readInt("ID del club: ", 1, Integer.MAX_VALUE);
        controller.joinClub(student, clubId);
        System.out.println("  ✓ " + student.getFullName() + " inscrito en el club.");
    }

    private void leaveClub() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        TablePrinter.print(controller.clubsOf(student));
        int clubId = ConsoleInput.readInt("ID del club: ", 1, Integer.MAX_VALUE);
        String reason = ConsoleInput.readText("Motivo del retiro (opcional): ", false);
        controller.leaveClub(student, clubId, reason);
        System.out.println("  ✓ Estudiante retirado del club.");
    }

    private void clubMembers() {
        TablePrinter.print(controller.listClubs());
        TablePrinter.print(controller.clubMembers(ConsoleInput.readInt("ID del club: ", 1, Integer.MAX_VALUE)));
    }

    private void clubsOfStudent() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        TablePrinter.print(controller.clubsOf(student));
    }

    private void createEvent() {
        String name = ConsoleInput.readText("Nombre del evento: ", true);
        String description = ConsoleInput.readText("Descripción (opcional): ", false);
        String type = ConsoleInput.readChoice("Tipo de evento:", ExtracurricularController.EVENT_TYPES.toArray(new String[0]));
        var date = ConsoleInput.readDate("Fecha del evento");
        String place = ConsoleInput.readText("Lugar (opcional): ", false);
        boolean competition = ConsoleInput.confirm("¿El evento tiene competencia con posiciones?");
        int id = controller.createEvent(new SchoolEvent(null, name, description, type, date, place, competition, null));
        System.out.println("  ✓ Evento creado con ID " + id + ".");
    }

    private void addParticipant() {
        TablePrinter.print(controller.listEvents());
        SchoolEvent event = controller.requireEvent(ConsoleInput.readInt("ID del evento: ", 1, Integer.MAX_VALUE));
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        String role = ConsoleInput.readChoice("Rol:", "Participante", "Competidor", "Organizador", "Asistente");
        String position = null;
        if (event.hasCompetition()) {
            position = ConsoleInput.readChoice("Posición obtenida:",
                    ExtracurricularController.POSITIONS.toArray(new String[0]));
        }
        controller.addParticipant(event, student, role, position);
        System.out.println("  ✓ " + student.getFullName() + " registrado en " + event.name() + ".");
    }

    private void eventParticipants() {
        TablePrinter.print(controller.listEvents());
        TablePrinter.print(controller.eventParticipants(ConsoleInput.readInt("ID del evento: ", 1, Integer.MAX_VALUE)));
    }

    private void eventsOfStudent() {
        Student student = controller.requireStudent(ConsoleInput.readText("Documento del estudiante: ", true));
        TablePrinter.print(controller.eventsOf(student));
    }
}
