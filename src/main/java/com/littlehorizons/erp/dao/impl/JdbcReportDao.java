package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.math.BigDecimal;
import java.util.List;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcReportDao implements ReportDao {

    @Override
    public ReportTable courseAverages(int gradeId, int academicYear, String period) {
        return Jdbc.table("Promedio del curso por materia (" + academicYear + ", período " + period + ")",
                List.of("Materia", "Estudiantes", "Promedio", "Mínima", "Máxima"), """
                SELECT m.nombre_materia, COUNT(*), ROUND(AVG(ra.valor_nota), 2), MIN(ra.valor_nota), MAX(ra.valor_nota)
                FROM registros_academicos ra JOIN materias m ON m.id_materia = ra.id_materia
                WHERE m.id_grado = ? AND ra.anio_academico = ? AND ra.periodo_academico = ?
                GROUP BY m.id_materia, m.nombre_materia ORDER BY m.nombre_materia""",
                args(gradeId, academicYear, period));
    }

    @Override
    public ReportTable studentsNeedingSupport(int academicYear, String period, BigDecimal belowAverage) {
        return Jdbc.table("Estudiantes que necesitan apoyo (promedio < " + belowAverage + ")",
                List.of("Documento", "Estudiante", "Grado", "Promedio"), """
                SELECT e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, e.primer_apellido, e.segundo_apellido),
                       g.nombre_grado, ROUND(AVG(ra.valor_nota), 2)
                FROM registros_academicos ra
                JOIN estudiantes e ON e.id_estudiante = ra.id_estudiante
                JOIN matriculas mt ON mt.id_estudiante = e.id_estudiante AND mt.anio_academico = ra.anio_academico
                JOIN grados g ON g.id_grado = mt.id_grado
                WHERE ra.anio_academico = ? AND ra.periodo_academico = ?
                GROUP BY e.id_estudiante, g.id_grado
                HAVING AVG(ra.valor_nota) < ?
                ORDER BY AVG(ra.valor_nota)""", args(academicYear, period, belowAverage));
    }

    @Override
    public ReportTable studentsPerStatus() {
        return Jdbc.table("Estudiantes por estado", List.of("Estado", "Total"),
                "SELECT estado, COUNT(*) FROM estudiantes GROUP BY estado ORDER BY estado", Jdbc.NO_ARGS);
    }
}
