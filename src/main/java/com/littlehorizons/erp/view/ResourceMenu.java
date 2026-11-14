package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;

/** Console menu for the lendable resources and their loans. */
public class ResourceMenu {

    private final ResourceController controller;

    public ResourceMenu(ResourceController controller) {
        this.controller = controller;
    }

    public void show() {
        Menu.run("Gestión de recursos", List.of(
                new Menu.Item("Registrar recurso", this::register),
                new Menu.Item("Ver inventario", this::list),
                new Menu.Item("Cambiar estado (mantenimiento / baja / disponible)", this::changeStatus),
                new Menu.Item("Registrar préstamo", this::lend),
                new Menu.Item("Registrar devolución", this::giveBack),
                new Menu.Item("Ver préstamos abiertos", this::activeLoans),
                new Menu.Item("Reporte de préstamos vencidos", () -> TablePrinter.print(controller.overdueReport()))));
    }

    private void register() {
        String code = ConsoleInput.readText("Código único del recurso: ", true).toUpperCase();
        String name = ConsoleInput.readText("Nombre: ", true);
        List<IdName> categories = controller.categories();
        String[] names = categories.stream().map(IdName::name).toArray(String[]::new);
        String category = ConsoleInput.readChoice("Categoría:", names);
        int categoryId = categories.get(List.of(names).indexOf(category)).id();
        String description = ConsoleInput.readText("Descripción (opcional): ", false);
        String location = ConsoleInput.readText("Ubicación física (ej: Biblioteca): ", false);
        int id = controller.register(new Resource(null, code, name, categoryId, description, location, "Disponible"));
        System.out.println("  ✓ Recurso registrado con ID " + id + ".");
    }

    private void list() {
        String filter = ConsoleInput.readChoice("Filtrar por estado:", "Todos", "Disponible", "Prestado",
                "En Mantenimiento", "Dado de Baja");
        TablePrinter.print(controller.list(filter.equals("Todos") ? null : filter));
    }

    private void changeStatus() {
        Resource resource = controller.requireByCode(ConsoleInput.readText("Código del recurso: ", true));
        System.out.println("Recurso: " + resource.name() + " (estado actual: " + resource.status() + ")");
        String status = ConsoleInput.readChoice("Nuevo estado:", ResourceController.MANUAL_STATUSES.toArray(new String[0]));
        controller.changeStatus(resource, status);
        System.out.println("  ✓ Estado actualizado.");
    }

    private BorrowerType askBorrowerType() {
        String type = ConsoleInput.readChoice("Tipo de prestatario:", "Estudiante", "Profesor");
        return type.equals("Estudiante") ? BorrowerType.STUDENT : BorrowerType.TEACHER;
    }

    private void lend() {
        BorrowerType type = askBorrowerType();
        String document = ConsoleInput.readText("Documento del " + type.label().toLowerCase() + ": ", true);
        Resource resource = controller.requireByCode(ConsoleInput.readText("Código del recurso: ", true));
        LocalDate loanDate = ConsoleInput.readDateOrToday("Fecha del préstamo");
        LocalDate dueDate = ConsoleInput.readDate("Fecha de devolución programada");
        int id = controller.lend(type, document, resource, loanDate, dueDate);
        System.out.println("  ✓ Préstamo " + id + " registrado: " + resource.name() + ", vence " + dueDate + ".");
    }

    private void giveBack() {
        BorrowerType type = askBorrowerType();
        TablePrinter.print(controller.activeLoans(type));
        int loanId = ConsoleInput.readInt("ID del préstamo a devolver: ", 1, Integer.MAX_VALUE);
        LocalDate date = ConsoleInput.readDateOrToday("Fecha de devolución");
        controller.giveBack(type, loanId, date);
        System.out.println("  ✓ Devolución registrada; el recurso quedó disponible.");
    }

    private void activeLoans() {
        TablePrinter.print(controller.activeLoans(askBorrowerType()));
    }
}
