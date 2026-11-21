-- =====================================================================
-- Datos de prueba para el BUSCADOR AVANZADO DE ESTUDIANTES (examen)
-- Base de datos: erp_little_horizons (MySQL 8.0+)
--
-- El buscador consulta matrículas ACTIVAS del año 2025. Los datos de prueba
-- principales (docs/database.sql) trabajan con el año 2026, por eso este
-- script agrega estudiantes con matrícula 2025.
--
-- Uso: ejecutar COMPLETO (Alt+X en DBeaver) DESPUÉS de docs/database.sql.
-- El script se puede ejecutar varias veces: primero borra sus propios datos,
-- identificándolos por número de documento (no usa ids fijos).
--
-- Casos que cubre:
--   * Décimo: 3 estudiantes activos (Juan Carlos, María Elena y Carlos).
--   * Carlos Gómez Silva NO tiene segundo nombre (nombre completo sin espacios dobles).
--   * Andrés Felipe Rojas Mejía tiene matrícula 2025 en Décimo pero "Retirada":
--     NO debe aparecer.
--   * Valentina Cárdenas (sin segundo nombre ni segundo apellido) está en Noveno:
--     aparece al filtrar por Noveno, pero NO por Décimo.
--   * Once: ningún estudiante activo en 2025 -> mensaje "No se encontraron...".
--   * Los estudiantes de Once del 2026 tienen una matrícula 2025 "Completada" en
--     Décimo: tampoco deben aparecer en Décimo.
-- =====================================================================

USE erp_little_horizons;

-- ---------- 1. Limpieza (solo los datos de este script, por documento) ----------
DELETE FROM matriculas
WHERE id_estudiante IN (SELECT id_estudiante FROM estudiantes
                        WHERE numero_documento IN ('1023456789','1023456790','1023456791','1023456792','1023456793'));
DELETE FROM estudiante_acudiente
WHERE id_estudiante IN (SELECT id_estudiante FROM estudiantes
                        WHERE numero_documento IN ('1023456789','1023456790','1023456791','1023456792','1023456793'));
DELETE FROM estudiantes
WHERE numero_documento IN ('1023456789','1023456790','1023456791','1023456792','1023456793');
DELETE FROM acudientes
WHERE numero_documento IN ('52345601','52345602','52345603','52345604','52345605');

-- ---------- 2. Estudiantes ----------
INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido,
                         segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico,
                         fecha_ingreso, estado) VALUES
('TI', '1023456789', 'Juan',      'Carlos', 'Pérez',    'González', '2010-03-14', 'M', 'Carrera 30 # 45-12, Cabecera del Llano, Bucaramanga', '310 245 8891', 'juan.perez@estudiantes.littlehorizons.edu.co',   '2025-01-20', 'Activo'),
('TI', '1023456790', 'María',     'Elena',  'López',    'Ramírez',  '2010-07-22', 'F', 'Calle 52 # 28-40, Álamos, Bucaramanga',               '315 778 3320', 'maria.lopez@estudiantes.littlehorizons.edu.co',   '2025-01-20', 'Activo'),
('TI', '1023456791', 'Carlos',    NULL,     'Gómez',    'Silva',    '2009-11-05', 'M', 'Carrera 27 # 60-18, Provenza, Bucaramanga',             '300 512 9047', 'carlos.gomez@estudiantes.littlehorizons.edu.co',  '2025-01-20', 'Activo'),
('TI', '1023456792', 'Andrés',    'Felipe', 'Rojas',    'Mejía',    '2010-01-30', 'M', 'Calle 41 # 33-25, Real de Minas, Bucaramanga',          '320 664 1105', NULL,                                              '2025-01-20', 'Retirado'),
('TI', '1023456793', 'Valentina', NULL,     'Cárdenas', NULL,       '2011-05-09', 'F', 'Carrera 19 # 37-50, Centro, Bucaramanga',               '311 903 4476', NULL,                                              '2025-01-20', 'Activo');

-- ---------- 3. Acudientes (uno por estudiante) y su relación ----------
INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, primer_apellido, segundo_apellido,
                        parentesco, telefono_principal, correo_electronico, direccion, es_responsable_financiero) VALUES
('CC', '52345601', 'Marta',   'González', 'Rueda',   'Madre', '310 245 0001', 'marta.gonzalez@gmail.com',   'Carrera 30 # 45-12, Cabecera del Llano, Bucaramanga', TRUE),
('CC', '52345602', 'Claudia', 'Ramírez',  'Pinzón',  'Madre', '315 778 0002', 'claudia.ramirez@gmail.com',  'Calle 52 # 28-40, Álamos, Bucaramanga',               TRUE),
('CC', '52345603', 'Jorge',   'Gómez',    'Duarte',  'Padre', '300 512 0003', 'jorge.gomez@hotmail.com',    'Carrera 27 # 60-18, Provenza, Bucaramanga',           TRUE),
('CC', '52345604', 'Patricia','Mejía',    'Ardila',  'Madre', '320 664 0004', 'patricia.mejia@outlook.com', 'Calle 41 # 33-25, Real de Minas, Bucaramanga',        TRUE),
('CC', '52345605', 'Luis',    'Cárdenas', 'Salazar', 'Padre', '311 903 0005', 'luis.cardenas@gmail.com',    'Carrera 19 # 37-50, Centro, Bucaramanga',             TRUE);

INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia)
SELECT e.id_estudiante, a.id_acudiente, TRUE, TRUE
FROM estudiantes e
JOIN acudientes a ON (e.numero_documento, a.numero_documento) IN
     (('1023456789','52345601'), ('1023456790','52345602'), ('1023456791','52345603'),
      ('1023456792','52345604'), ('1023456793','52345605'));

-- ---------- 4. Matrículas del año 2025 ----------
-- Décimo, activas
INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, fecha_matricula)
SELECT e.id_estudiante, g.id_grado, 2025, 'Activa', '2025-01-13'
FROM estudiantes e JOIN grados g ON g.nombre_grado = 'Décimo'
WHERE e.numero_documento IN ('1023456789', '1023456790', '1023456791');

-- Décimo, pero RETIRADA (no debe aparecer en el buscador)
INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, fecha_matricula, fecha_retiro,
                        observaciones)
SELECT e.id_estudiante, g.id_grado, 2025, 'Retirada', '2025-01-13', '2025-06-15', 'Retiro voluntario'
FROM estudiantes e JOIN grados g ON g.nombre_grado = 'Décimo'
WHERE e.numero_documento = '1023456792';

-- Noveno, activa
INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, fecha_matricula)
SELECT e.id_estudiante, g.id_grado, 2025, 'Activa', '2025-01-13'
FROM estudiantes e JOIN grados g ON g.nombre_grado = 'Noveno'
WHERE e.numero_documento = '1023456793';

-- ---------- 5. Verificación: lo mismo que consulta el buscador ----------
SELECT g.nombre_grado AS grado, e.id_estudiante, e.tipo_documento, e.numero_documento,
       CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre
FROM estudiantes e
JOIN matriculas m ON m.id_estudiante = e.id_estudiante
JOIN grados g ON g.id_grado = m.id_grado
WHERE m.anio_academico = 2025 AND m.estado_matricula = 'Activa'
ORDER BY g.nivel_numerico, e.primer_apellido;
