package test.controlador;

import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import test.modelo.Estudiante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CONTROLADOR: consulta MySQL y aplica el filtro con Stream API.
 * Reutiliza DatabaseConnection (la conexión del proyecto principal) en lugar de duplicarla.
 */
public class BuscadorEstudiantesController {

    /** Año académico que exige el enunciado. */
    public static final int ANIO_ACADEMICO = 2025;

    /** Solo se consideran matrículas con este estado. */
    public static final String ESTADO_MATRICULA_ACTIVA = "Activa";

    /** Grados en el orden del menú: la opción 1 es Preescolar y la 12 es Once. */
    public static final List<String> GRADOS = List.of("Preescolar", "Primero", "Segundo", "Tercero", "Cuarto",
            "Quinto", "Sexto", "Séptimo", "Octavo", "Noveno", "Décimo", "Once");

    /**
     * Trae TODOS los estudiantes con matrícula activa del año indicado, con el nombre de su grado.
     * El filtro por grado NO se hace aquí: se hace después con Stream API.
     * (Nuestra base de datos usa la columna anio_academico, sin la letra ñ.)
     */
    private static final String SQL_ESTUDIANTES_ACTIVOS = """
            SELECT e.id_estudiante, e.tipo_documento, e.numero_documento,
                   e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido,
                   g.id_grado, g.nombre_grado
            FROM estudiantes e
            JOIN matriculas m ON m.id_estudiante = e.id_estudiante
            JOIN grados g ON g.id_grado = m.id_grado
            WHERE m.anio_academico = ? AND m.estado_matricula = ?
            ORDER BY g.nivel_numerico, e.id_estudiante""";

    /** Nombre del grado para una opción del menú (1 a 12). */
    public String obtenerNombreGrado(int opcion) {
        return GRADOS.get(opcion - 1);
    }

    /**
     * Ejecuta la consulta JDBC. Connection, PreparedStatement y ResultSet se cierran solos
     * porque están declarados en un try-with-resources.
     */
    public List<Estudiante> consultarEstudiantesActivos() throws SQLException {
        List<Estudiante> estudiantes = new ArrayList<>();
        try (Connection conexion = DatabaseConnection.getInstance().getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ESTUDIANTES_ACTIVOS)) {
            sentencia.setInt(1, ANIO_ACADEMICO);
            sentencia.setString(2, ESTADO_MATRICULA_ACTIVA);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    estudiantes.add(new Estudiante(
                            resultado.getInt("id_estudiante"),
                            resultado.getString("tipo_documento"),
                            resultado.getString("numero_documento"),
                            resultado.getString("primer_nombre"),
                            resultado.getString("segundo_nombre"),
                            resultado.getString("primer_apellido"),
                            resultado.getString("segundo_apellido"),
                            resultado.getInt("id_grado"),
                            resultado.getString("nombre_grado")));
                }
            }
        } catch (SQLException e) {
            AppLogger.error("No se pudo consultar los estudiantes activos del año " + ANIO_ACADEMICO, e);
            throw e;
        }
        return estudiantes;
    }

    /**
     * FILTRO CON STREAM API: se queda solo con los estudiantes del grado indicado.
     * stream() -> filter() con lambda -> collect(Collectors.toList()).
     */
    public List<Estudiante> filtrarPorGrado(List<Estudiante> estudiantes, String nombreGrado) {
        return estudiantes.stream()
                .filter(estudiante -> estudiante.getNombreGrado() != null
                        && estudiante.getNombreGrado().equalsIgnoreCase(nombreGrado))
                .collect(Collectors.toList());
    }

    /** Flujo completo: consulta a la base de datos y luego filtra por grado. */
    public List<Estudiante> buscarPorGrado(String nombreGrado) throws SQLException {
        return filtrarPorGrado(consultarEstudiantesActivos(), nombreGrado);
    }
}