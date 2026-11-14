package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.ConsoleInput;

import java.util.List;

/** Generic menu loop. Handles the errors of every action in one place so that views stay small. */
public final class Menu {

    /** One menu entry: a label and the action it runs. */
    public record Item(String label, Runnable action) { }

    private Menu() { }

    public static void run(String title, List<Item> items) {
        int option;
        do {
            System.out.println("\n--- " + title.toUpperCase() + " ---");
            for (int i = 0; i < items.size(); i++) {
                System.out.printf("%2d. %s%n", i + 1, items.get(i).label());
            }
            System.out.println(" 0. Volver al menú anterior");
            System.out.println("(Escriba " + ConsoleInput.CANCEL_COMMAND + " en cualquier pregunta para cancelar)");
            try {
                option = ConsoleInput.readInt("Seleccione una opción: ", 0, items.size());
            } catch (OperationCancelledException e) {
                return;
            }
            if (option > 0) {
                execute(items.get(option - 1));
            }
        } while (option != 0);
    }

    private static void execute(Item item) {
        try {
            item.action().run();
        } catch (OperationCancelledException e) {
            System.out.println("  ↩ Operación cancelada.");
        } catch (BusinessRuleException e) {
            System.out.println("  ✗ " + e.getMessage());
        } catch (DataAccessException e) {
            System.out.println("  ✗ Ocurrió un error con la base de datos. Revise error_log.txt para más detalles.");
        }
    }
}
