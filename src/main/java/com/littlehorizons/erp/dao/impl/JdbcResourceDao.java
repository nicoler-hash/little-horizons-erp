package com.littlehorizons.erp.dao.impl;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.IdName;
import com.littlehorizons.erp.model.ReportTable;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.util.Jdbc;

import java.util.List;
import java.util.Optional;

import static com.littlehorizons.erp.util.Jdbc.args;

public class JdbcResourceDao implements ResourceDao {

    @Override
    public int insert(Resource r) {
        return Jdbc.insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                + "ubicacion_fisica) VALUES (?,?,?,?,?)",
                args(r.code(), r.name(), r.categoryId(), r.description(), r.location()));
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return Jdbc.queryOne("SELECT * FROM recursos WHERE codigo_recurso = ?", args(code),
                rs -> new Resource(rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                        rs.getString("nombre_recurso"), rs.getInt("id_categoria"), rs.getString("descripcion"),
                        rs.getString("ubicacion_fisica"), rs.getString("estado_recurso")));
    }

    @Override
    public List<IdName> findCategories() {
        return Jdbc.query("SELECT id_categoria, nombre_categoria FROM categorias_recurso ORDER BY id_categoria",
                Jdbc.NO_ARGS, rs -> new IdName(rs.getInt(1), rs.getString(2)));
    }

    @Override
    public ReportTable listReport(String statusFilter) {
        return Jdbc.table("Inventario de recursos", List.of("Código", "Recurso", "Categoría", "Ubicación", "Estado"), """
                SELECT r.codigo_recurso, r.nombre_recurso, c.nombre_categoria, COALESCE(r.ubicacion_fisica, '-'),
                       r.estado_recurso
                FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
                WHERE (? IS NULL OR r.estado_recurso = ?) ORDER BY c.nombre_categoria, r.nombre_recurso""",
                args(statusFilter, statusFilter));
    }

    @Override
    public void updateStatus(int resourceId, String status) {
        Jdbc.update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", args(status, resourceId));
    }
}
