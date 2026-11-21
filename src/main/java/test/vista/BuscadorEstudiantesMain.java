package test.vista;

import test.controlador.BuscadorEstudiantesController;
import test.modelo.Estudiante;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * VISTA: menú de consola del buscador. Es el punto de entrada del examen: se ejecuta directamente,
 * sin pasar por el login ni por el menú principal del ERP.
 */
public class BuscadorEstudiantesMain {

    private static final String LINEA_DOBLE = "=".repeat(79);
    private static final String LINEA_SIMPLE = "-".repeat(79);

    private final BuscadorEstudiantesController controlador;
    private final Scanner scanner;

    public BuscadorEstudiantesMain(BuscadorEstudiantesController controlador, Scanner scanner) {
        this.controlador = controlador;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        new BuscadorEstudiantesMain(new BuscadorEstudiantesController(), new Scanner(System.in)).ejecutar();
    }

    /** Bucle del menú principal: se repite hasta que el usuario elige 0 (o se acaba la entrada). */
    public void ejecutar() {
        boolean salir = false;
        while (!salir) {
            mostrarMenuBuscador();
            String opcion = leerLinea();
            if (opcion == null) {
                break;
            }
            switch (opcion.trim()) {
                case "1" -> filtrarPorGradoAcademico();
                case "2", "3", "4", "5" -> mostrarNoImplementado();
                case "0" -> salir = true;
                default -> {
                    System.out.println("\nError: Opción no válida. Seleccione un número entre 0 y 5.");
                    esperarEnter("Presione Enter para continuar...");
                }
            }
        }
        System.out.println("\nSaliendo del buscador...");
        System.out.println("Gracias por usar el sistema.");
    }

    // ---------- menú principal ----------

    private void mostrarMenuBuscador() {
        System.out.println();
        System.out.println(LINEA_DOBLE);
        System.out.println("            BUSCADOR AVANZADO DE ESTUDIANTES - PROTOTIPO");
        System.out.println(LINEA_DOBLE);
        System.out.println();
        System.out.println("Seleccione el filtro que desea aplicar:");
        System.out.println();
        System.out.println("1. Filtrar por grado académico [FUNCIONAL]");
        System.out.println("2. Filtrar por rango de edad [NO IMPLEMENTADO]");
        System.out.println("3. Filtrar por género [NO IMPLEMENTADO]");
        System.out.println("4. Filtrar por estado de matrícula [NO IMPLEMENTADO]");
        System.out.println("5. Filtrar por rango de promedio académico [NO IMPLEMENTADO]");
        System.out.println("0. Salir");
        System.out.println();
        System.out.print("Seleccione una opción: ");
    }

    private void mostrarNoImplementado() {
        System.out.println();
        System.out.println("Esta funcionalidad aún no ha sido implementada.");
        System.out.println("Será agregada en futuras versiones del sistema.");
        System.out.println();
        esperarEnter("Presione Enter para continuar...");
    }

    // ---------- filtro por grado ----------

    /** Pide el grado, valida la entrada y, si es válida, ejecuta la búsqueda. */
    private void filtrarPorGradoAcademico() {
        while (true) {
            mostrarGrados();
            String entrada = leerLinea();
            if (entrada == null) {
                return;
            }
            int numero;
            try {
                numero = Integer.parseInt(entrada.trim());
            } catch (NumberFormatException e) {
                System.out.println();
                System.out.println("Error: Debe ingresar un número válido.");
                esperarEnter("Presione Enter para intentar nuevamente...");
                continue;
            }
            if (numero == 0) {
                System.out.println();
                System.out.println("Operación cancelada.");
                esperarEnter("Presione Enter para volver al menú principal...");
                return;
            }
            if (numero < 1 || numero > BuscadorEstudiantesController.GRADOS.size()) {
                System.out.println();
                System.out.println("Error: Debe seleccionar un número entre 1 y 12.");
                esperarEnter("Presione Enter para intentar nuevamente...");
                continue;
            }
            ejecutarBusqueda(controlador.obtenerNombreGrado(numero));
            return;
        }
    }

    private void mostrarGrados() {
        System.out.println();
        System.out.println("--- FILTRAR POR GRADO ACADÉMICO ---");
        System.out.println();
        System.out.println("Grados disponibles en el sistema:");
        List<String> grados = BuscadorEstudiantesController.GRADOS;
        for (int i = 0; i < grados.size(); i++) {
            System.out.println((i + 1) + ". " + grados.get(i));
        }
        System.out.println();
        System.out.print("Ingrese el número del grado que desea filtrar (o 0 para cancelar): ");
    }

    private void ejecutarBusqueda(String nombreGrado) {
        System.out.println();
        System.out.println("Procesando búsqueda...");
        System.out.println("Consultando estudiantes matriculados en " + nombreGrado + "...");
        try {
            List<Estudiante> resultados = controlador.buscarPorGrado(nombreGrado);
            mostrarResultados(nombreGrado, resultados);
        } catch (SQLException e) {
            mostrarErrorConexion(e.getMessage());
        } catch (RuntimeException | ExceptionInInitializerError e) {
            // por ejemplo: falta el archivo db.properties o no tiene la URL
            mostrarErrorConexion(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    // ---------- resultados ----------

    private void mostrarResultados(String nombreGrado, List<Estudiante> resultados) {
        int anio = BuscadorEstudiantesController.ANIO_ACADEMICO;
        System.out.println();
        System.out.println(LINEA_DOBLE);
        System.out.println("            RESULTADOS DE LA BÚSQUEDA");
        System.out.println(LINEA_DOBLE);
        System.out.println();
        System.out.println("Filtro aplicado: Grado académico = " + nombreGrado);
        System.out.println();

        if (resultados.isEmpty()) {
            System.out.println("No se encontraron estudiantes matriculados en " + nombreGrado
                    + " para el año " + anio + ".");
            System.out.println();
            System.out.println("Posibles razones:");
            System.out.println("  - No hay estudiantes matriculados en ese grado para el año " + anio);
            System.out.println("  - Verifique que la base de datos tenga información del año actual");
        } else {
            int total = resultados.size();
            System.out.println((total == 1 ? "Se encontró 1 estudiante matriculado en " : "Se encontraron " + total
                    + " estudiantes matriculados en ") + nombreGrado + " (año " + anio + "):");
            System.out.println();
            System.out.println(LINEA_SIMPLE);
            for (Estudiante estudiante : resultados) {
                System.out.println("ID: " + estudiante.getIdEstudiante());
                System.out.println("Nombre: " + estudiante.getNombreCompleto());
                System.out.println("Documento: " + estudiante.getDocumento());
                System.out.println(LINEA_SIMPLE);
            }
            System.out.println();
            System.out.println("Total de resultados: " + total + (total == 1 ? " estudiante" : " estudiantes"));
        }
        System.out.println();
        System.out.println(LINEA_DOBLE);
        System.out.println("Presione Enter para volver al menú principal...");
        System.out.println(LINEA_DOBLE);
        leerLinea();
    }

    private void mostrarErrorConexion(String detalle) {
        System.out.println();
        System.out.println("Error: No fue posible consultar la base de datos.");
        System.out.println("Verifique que MySQL esté en ejecución y que db.properties tenga el usuario y la");
        System.out.println("contraseña correctos, y que el script docs/database.sql esté ejecutado.");
        System.out.println("Detalle: " + detalle);
        esperarEnter("Presione Enter para volver al menú principal...");
    }

    // ---------- lectura de consola ----------

    /** Lee una línea; devuelve null si ya no hay entrada (así el programa nunca se cae por falta de datos). */
    private String leerLinea() {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private void esperarEnter(String mensaje) {
        System.out.println(mensaje);
        leerLinea();
    }
}