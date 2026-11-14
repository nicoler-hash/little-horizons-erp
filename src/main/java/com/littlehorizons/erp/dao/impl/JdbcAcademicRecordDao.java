package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.util.Jdbc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcAcademicRecordDao implements AcademicRecordDao {

    @Override
    public void save(AcademicRecord r) {
        String sql = """
                INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, anio_academico,
                  periodo_academico, valor_nota, observaciones, fecha_registro)
                VALUES (?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE valor_nota = VALUES(valor_nota), id_profesor = VALUES(id_profesor),
                  observaciones = VALUES(observaciones), fecha_registro = VALUES(fecha_registro)""";
        Jdbc.update(sql, args(r.studentId(), r.subjectId(), r.teacherId(), r.academicYear(), r.period(),
                r.score(), r.notes(), r.recordDate()));
    }

    @Override
    public ReportTable history(int studentId) {
        return Jdbc.table("Historial académico", List.of("Año", "Período", "Materia", "Nota"), """
                SELECT ra.anio_academico, ra.periodo_academico, m.nombre_materia, ra.valor_nota
                FROM registros_academicos ra JOIN materias m ON m.id_materia = ra.id_materia
                WHERE ra.id_estudiante = ?
                ORDER BY ra.anio_academico, FIELD(ra.periodo_academico, 'I','II','III','IV','Final'), m.nombre_materia""",
                args(studentId));
    }

    @Override
    public ReportTable subjectAverages(int studentId, int academicYear) {
        return Jdbc.table("Promedio por materia (" + academicYear + ")", List.of("Materia", "Notas", "Promedio"), """
                SELECT m.nombre_materia, COUNT(*), ROUND(AVG(ra.valor_nota), 2)
                FROM registros_academicos ra JOIN materias m ON m.id_materia = ra.id_materia
                WHERE ra.id_estudiante = ? AND ra.anio_academico = ? AND ra.periodo_academico <> 'Final'
                GROUP BY m.id_materia, m.nombre_materia ORDER BY m.nombre_materia""", args(studentId, academicYear));
    }

    @Override
    public Optional<BigDecimal> generalAverage(int studentId, int academicYear, String period) {
        return Jdbc.queryOne("SELECT ROUND(AVG(valor_nota), 2) FROM registros_academicos "
                        + "WHERE id_estudiante = ? AND anio_academico = ? AND periodo_academico = ?",
                args(studentId, academicYear, period), rs -> rs.getBigDecimal(1));
    }
}
