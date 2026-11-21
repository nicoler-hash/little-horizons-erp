package test.modelo;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MODELO: representa un estudiante junto con el grado en el que está matriculado.
 * Es un DTO que combina datos de tres tablas: estudiantes, matriculas y grados.
 */
public class Estudiante {

    private int idEstudiante;
    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;      // puede ser NULL en la base de datos
    private String primerApellido;
    private String segundoApellido;    // puede ser NULL en la base de datos
    private int idGrado;
    private String nombreGrado;

    public Estudiante(int idEstudiante, String tipoDocumento, String numeroDocumento, String primerNombre,
                      String segundoNombre, String primerApellido, String segundoApellido,
                      int idGrado, String nombreGrado) {
        this.idEstudiante = idEstudiante;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.idGrado = idGrado;
        this.nombreGrado = nombreGrado;
    }

    /**
     * Nombre completo sin espacios dobles: solo se incluyen las partes que existen
     * (no nulas y no vacías). Ejemplo: segundoNombre NULL -> "Juan Pérez González".
     */
    public String getNombreCompleto() {
        return Stream.of(primerNombre, segundoNombre, primerApellido, segundoApellido)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /** Documento tal como se muestra en pantalla, por ejemplo "TI 1023456789". */
    public String getDocumento() {
        return Stream.of(tipoDocumento, numeroDocumento)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(parte -> !parte.isEmpty())
                .collect(Collectors.joining(" "));
    }

    public int getIdEstudiante() { return idEstudiante; }

    public void setIdEstudiante(int idEstudiante) { this.idEstudiante = idEstudiante; }

    public String getTipoDocumento() { return tipoDocumento; }

    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }

    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getPrimerNombre() { return primerNombre; }

    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getSegundoNombre() { return segundoNombre; }

    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getPrimerApellido() { return primerApellido; }

    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getSegundoApellido() { return segundoApellido; }

    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }

    public int getIdGrado() { return idGrado; }

    public void setIdGrado(int idGrado) { this.idGrado = idGrado; }

    public String getNombreGrado() { return nombreGrado; }

    public void setNombreGrado(String nombreGrado) { this.nombreGrado = nombreGrado; }

    @Override
    public String toString() {
        return "Estudiante{id=" + idEstudiante + ", nombre='" + getNombreCompleto() + "', documento='"
                + getDocumento() + "', grado='" + nombreGrado + "'}";
    }
}