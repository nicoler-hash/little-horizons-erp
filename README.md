# 1. Título del Proyecto

# ERP Colegio Internacional "Little Horizons"

Sistema de Planificación de Recursos Empresariales (ERP) educativo, de consola, escrito en Java con arquitectura
**MVC** y base de datos **MySQL 8.0+**.

---

## 2. Descripción

### Propósito
Centralizar y automatizar los procesos académicos, extracurriculares, financieros y de recursos del Colegio
Internacional "Little Horizons" (preescolar a 11°, más de 500 estudiantes y 40 profesores).

### Problemas que resuelve
| Problema del colegio | Cómo lo resuelve el sistema |
|---|---|
| Notas en cuadernos que se pierden | Registro de notas (0.0 – 5.0) por materia y período, con historial completo |
| Actividades extracurriculares sin registro | Clubes con control de cupo y eventos con participantes y posiciones |
| Facturación manual y con errores | Pagos con **factura electrónica en archivo .txt** (CUFE y QR simulados, numeración consecutiva) |
| Libros y equipos que "se pierden" | Inventario con préstamos, devoluciones y reporte de vencidos |
| Profesores sin visibilidad del rendimiento | Promedios por estudiante/curso y reporte de estudiantes que necesitan apoyo |

### Funcionalidades principales
Gestión de estudiantes y acudientes · matrícula por grado · materias y carga docente con horarios sin cruces ·
calificaciones con promedios automáticos · asistencia con alertas de inasistencia · clubes y eventos ·
conceptos de pago, cargos, becas/descuentos y morosos · facturas .txt · inventario y préstamos · reportes.

---

## 3. Autores

| Nombre completo | Correo electrónico | Contacto |
|---|---|---|
| [Tu nombre completo] | [tu.correo@ejemplo.com] | [GitHub: @tu-usuario] |

---

## 4. Tecnologías utilizadas

| Componente | Detalle |
|---|---|
| Lenguaje | Java 17 (records, text blocks, `switch` con flechas, Stream API) |
| IDE | IntelliJ IDEA |
| Construcción | Maven 3.9+ |
| Base de datos | MySQL 8.0 o superior (se requieren los `CHECK` constraints) |
| Librerías externas | `mysql-connector-j 8.4.0` (driver JDBC). **Ninguna más**: la factura se genera como texto plano con una clase propia (`InvoiceTextGenerator`) |
| Logging | `java.util.logging` (JDK) |

---

## 5. Requerimientos previos

- **JDK 17 o superior**
- **MySQL Server 8.0 o superior** (⚠️ no funciona con MySQL 5.7: los `CHECK` no se aplican y algunas consultas fallan)
- Maven 3.9+ (o usar el Maven incluido en IntelliJ IDEA)
- Git

---

## 6. Configuración de la base de datos

### 6.1. Creación de la base de datos
El script `docs/database.sql` contiene el **DDL** (estructura) y el **DML** (datos de prueba). Crea la base de datos
`erp_little_horizons` desde cero:

```bash
mysql -u root -p < docs/database.sql
```

> ⚠️ El script ejecuta `DROP DATABASE IF EXISTS erp_little_horizons`: borra y recrea la base de datos.

Con DBeaver o MySQL Workbench: abra `docs/database.sql` y ejecútelo completo (como *script*, no sentencia por sentencia).

### 6.2. Configuración de credenciales
Edite `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8
db.user=root
db.password=SU_CONTRASEÑA
```

> No suba su contraseña real a GitHub: deje `db.password=` vacío antes de hacer commit.

### 6.3. Instalación del driver JDBC
Maven lo descarga automáticamente (dependencia `com.mysql:mysql-connector-j:8.4.0` en el `pom.xml`).
Sin Maven: descargue el JAR desde <https://dev.mysql.com/downloads/connector/j/> y agréguelo a las librerías del proyecto
en IntelliJ (*File → Project Structure → Libraries*).

---

## 7. Instalación y ejecución

### 7.1. Clonar el repositorio
```bash
git clone https://github.com/<usuario>/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en el IDE
IntelliJ IDEA → *File → Open* → seleccione la carpeta (o el `pom.xml`) → *Open as Project*. Espere a que Maven
importe las dependencias.

### 7.3. Compilar
```bash
mvn compile
```

### 7.4. Ejecutar
- Desde IntelliJ: ejecute la clase `com.littlehorizons.erp.Main`.
- Desde la terminal: `mvn compile exec:java`

> Si en la consola ve símbolos raros en lugar de tildes o recuadros, agregue la opción de VM `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` (IntelliJ: *Run → Edit Configurations → Modify options → Add VM options*).

Al iniciar, el programa comprueba la conexión (`Conexión a la base de datos exitosa.`) y muestra el menú principal.
Las facturas (`.txt`) se guardan en la carpeta `invoices/` (en el directorio desde donde se ejecuta el programa) y los
errores en `error_log.txt`.

**Consejo de uso:** en cualquier pregunta puede escribir `/q` para cancelar la operación y volver al menú.

### Datos de prueba útiles
Con los datos del script puede probar rápidamente (consulte los documentos reales en el menú de listados):

| Prueba | Dónde |
|---|---|
| Morosos (12 estudiantes con pensión de octubre vencida) | Gestión Financiera → Reporte de morosos |
| Alertas de inasistencia (2 estudiantes) | Gestión Académica → Reporte de inasistencias (use 52 semanas) |
| Facturas (60 emitidas, FE-000001 a FE-000060; varias con 2 ítems) | Gestión Financiera → Listar facturas emitidas / Ver o reimprimir una factura |
| Estudiantes con promedio < 3.0 (3 estudiantes) | Reportes → Estudiantes que necesitan apoyo (año 2026, período III) |
| Préstamos vencidos (4) | Gestión de Recursos → Reporte de préstamos vencidos |

---

## 8. Estructura del proyecto

```
little-horizons-erp/
├── docs/
│   └── database.sql                 DDL + DML (datos de prueba)
├── invoices/                        Facturas .txt generadas
├── pom.xml
├── LICENSE
└── src/main/
    ├── resources/
    │   ├── db.properties            Conexión a MySQL
    │   └── school.properties        Datos del colegio para la factura
    └── java/com/littlehorizons/erp/
        ├── Main.java                Punto de entrada y composición de dependencias
        ├── model/                   MODELO: entidades del dominio (records)
        ├── view/                    VISTA: menús de consola
        ├── controller/              CONTROLADOR: reglas de negocio
        ├── dao/                     Interfaces de acceso a datos
        │   └── impl/                Implementaciones JDBC
        ├── exception/               Excepciones propias
        └── util/                    Utilidades (JDBC, consola, logs, factura .txt, CUFE)
```

### Arquitectura MVC
```
 Usuario ──► VIEW ──► CONTROLLER ──► DAO (interfaz) ──► JDBC ──► MySQL
              ▲            │               │
              └── MODEL ◄──┴───────────────┘
```

| Paquete | Responsabilidad | Qué NO hace |
|---|---|---|
| `model` | Entidades inmutables (`Student`, `Teacher`, `Payment…`) | No contiene SQL ni `System.out` |
| `view` | Muestra menús, lee y valida la entrada, imprime resultados | No contiene SQL ni reglas de negocio |
| `controller` | Reglas de negocio (cupos, rangos de notas, préstamos, facturación) | No imprime ni conoce JDBC |
| `dao` / `dao.impl` | Único lugar donde hay SQL | No contiene reglas de negocio |
| `util` | Infraestructura compartida (conexión, logs, factura .txt, entrada de consola) | — |

---

## 9. Principios y patrones de diseño aplicados

### 9.1. Principios SOLID

| Principio | Aplicación en el código |
|---|---|
| **S** – Responsabilidad única | `JdbcStudentDao` solo persiste; `StudentController` solo aplica reglas; `StudentMenu` solo interactúa con el usuario; `Validators` solo valida; `InvoiceTextGenerator` solo arma el texto de la factura. |
| **O** – Abierto/Cerrado | Se puede añadir otra implementación de `StudentDao` (p. ej. en memoria o JPA) sin modificar los controladores. `Menu.run(...)` recibe una lista de opciones: añadir una opción no cambia el motor del menú. `BorrowerType` permite nuevos tipos de prestatario con una constante más. |
| **L** – Sustitución de Liskov | Cualquier `StudentDao`/`BillingDao`… puede reemplazar a su implementación JDBC: los controladores solo dependen del contrato de la interfaz. |
| **I** – Segregación de interfaces | Interfaces pequeñas por agregado (`StudentDao`, `ClubDao`, `LoanDao`…) en lugar de un único DAO gigante; `BillingDao.CufeCalculator` es una interfaz funcional de un solo método. |
| **D** – Inversión de dependencias | Los controladores reciben las interfaces DAO por constructor; `Main` es la raíz de composición que decide las implementaciones concretas. |

### 9.2. Patrones de diseño

| Patrón | Dónde | Justificación |
|---|---|---|
| **MVC** | Todo el proyecto | Separa presentación, reglas y datos. |
| **DAO** | `dao/` + `dao/impl/` | Aísla el SQL; las capas superiores no dependen de JDBC. |
| **Singleton** | `DatabaseConnection` | Una única configuración de conexión compartida; entrega conexiones nuevas bajo demanda. |
| **Plantilla con callbacks** (estilo `JdbcTemplate`) | `Jdbc` (`query`, `update`, `insert`, `inTransaction`) | Elimina el código repetitivo: siempre `PreparedStatement`, `try-with-resources`, `commit`/`rollback` y registro de errores. |
| **Inyección de dependencias** | Constructores de controladores y vistas, armados en `Main` | Facilita pruebas y sustitución de implementaciones. |
| **Strategy (por lambdas)** | `Jdbc.RowMapper`, `Jdbc.Binder`, `BillingDao.CufeCalculator` | El comportamiento variable se pasa como función. |
| **Value Object / DTO** | `record`s del paquete `model` | Objetos inmutables, con `equals`/`hashCode`/`toString` automáticos. |
| **Transacción atómica** | `JdbcBillingDao.registerPayment`, `JdbcLoanDao.lend/giveBack`, `JdbcClubDao.join` | Pago + consecutivo + factura, o préstamo + estado del recurso, o cupo + membresía: todo o nada. |

### 9.3. Lambdas y Stream API
- `StudentController.countByStatus()` → `stream().collect(groupingBy(..., counting()))`.
- `AcademicController.enrollInAllSubjects()` → `stream().filter(...).toList()` y `forEach` con lambda.
- `Names.join(...)` → `Stream.of(...).filter(Objects::nonNull).collect(joining(" "))`.
- Menús: cada opción es un `Menu.Item(label, () -> ...)` con lambdas o referencias a métodos.
- `Optional.map` y `Optional.orElseThrow` en la búsqueda de entidades.

---

## 10. Funcionalidades principales

### Módulo 1 – Gestión de estudiantes y académica
- Registro de estudiantes con documento, nacimiento, dirección y contactos; **todo estudiante exige al menos un acudiente**.
- Un acudiente puede tener varios estudiantes (hermanos).
- Matrícula por grado y año; estado Activo / Graduado / Retirado / Suspendido (baja lógica, nunca se borra).
- Inscripción en materias por período (una materia o todas las de su grado).
- Registro de notas 0.0–5.0 con un decimal: solo el profesor asignado puede calificar a estudiantes inscritos.
- Promedio por materia, promedio general por período y promedio del curso por materia; historial completo.
- Asistencia diaria (Presente, Ausente, Tardanza, Excusa Médica) y reporte de más de 3 ausencias en una semana.

### Módulo 2 – Profesores
- Directorio, asignación de materias, y **horarios de clase con detección de cruces** del mismo profesor.

### Módulo 3 – Actividades extracurriculares
- Catálogo de clubes con cupo máximo validado **dentro de una transacción** (sin sobrecupo por concurrencia).
- Inscripción/retiro con fechas y estado; eventos con tipo y fecha; participantes y posiciones (1.º, 2.º, 3.º, Participante).

### Módulo 4 – Financiero
- Catálogo de conceptos, valores personalizados (becas, descuento por hermanos) y cargos por estudiante.
- Registro de pagos con método y recibo; historial por estudiante o por acudiente; **reporte de morosos** (> 30 días).
- **Factura electrónica (.txt)** con varias líneas por factura: razón social, NIT, datos completos del cliente (acudiente), estudiante y grado, detalle (código, descripción, cantidad, valor unitario, IVA, subtotal), desglose de IVA por tarifa, subtotal / IVA / total, **total en letras**, CUFE (SHA-384), QR simulado y resolución de habilitación simulada. Archivo `invoices/factura_000123.txt`. Desde el menú se pueden **listar** las facturas emitidas y **ver / reimprimir** cualquiera (se reconstruye desde la base de datos). Al registrar un pago se pueden agregar varios ítems (cargos pendientes o conceptos con cantidad) y se emite **una sola factura**; todo ocurre en una transacción.

### Módulo 5 – Recursos
- Inventario (libros, equipos de laboratorio y deportivos, dispositivos) con estado y ubicación.
- Préstamos a estudiantes y profesores, devoluciones, y reporte de préstamos vencidos.

### Reportes
Estudiantes por grado · promedios del curso · estudiantes que necesitan apoyo · inasistencias · morosos ·
recaudo por concepto · participación en clubes y eventos · préstamos vencidos.

---

## 11. Manejo de errores y logs

### Sistema de logging
`AppLogger` usa `java.util.logging` con un `FileHandler` en modo *append*.

- **Ubicación:** `error_log.txt` en el directorio de trabajo (la raíz del proyecto al ejecutar desde IntelliJ o Maven).
- El archivo se crea la primera vez que ocurre un error.
- Cada entrada incluye fecha, nivel, mensaje y la traza completa de la excepción.

### Estrategia de excepciones
| Capa | Qué hace |
|---|---|
| **DAO / `Jdbc`** | Captura `SQLException`, la **registra en `error_log.txt`** y lanza `DataAccessException` (no comprobada). Dentro de transacciones hace `rollback`. |
| **Controlador** | Valida reglas y lanza `BusinessRuleException` con un mensaje seguro para el usuario. |
| **Vista (`Menu`)** | Un único punto captura `BusinessRuleException` (muestra el mensaje), `DataAccessException` (mensaje genérico + referencia al log) y `OperationCancelledException` (`/q`). |
| **Recursos** | Todos los `Connection`, `PreparedStatement` y `ResultSet` se cierran con `try-with-resources`. |

Todas las consultas SQL usan `PreparedStatement` con parámetros (sin concatenar datos del usuario).

---

## 12. Capturas de pantalla (opcional)

> Agregue aquí capturas del menú principal, un reporte y una factura (`docs/img/...`).

---

## 13. Problemas conocidos y limitaciones (opcional)

- Cada factura es de un solo estudiante y un solo acudiente (si un acudiente paga por dos hermanos, son dos facturas).
- La factura se genera como texto plano (`.txt`), no como PDF.
- Los pagos son completos: no se registran abonos parciales.
- El CUFE, el QR y la resolución son **simulados**: no hay conexión con la DIAN.
- No hay autenticación ni roles de usuario.
- Los acudientes se asocian a estudiantes desde la consola; no hay edición de datos de acudientes.
- No incluye pruebas automatizadas.

## 14. Futuras mejoras (opcional)

Autenticación por roles · abonos parciales · facturas con varios estudiantes · interfaz web o JavaFX · pruebas unitarias con
mocks de los DAO · envío de la factura por correo · reportes exportables a Excel, factura también en PDF.

## 15. Licencia

Distribuido bajo la licencia MIT. Consulte el archivo [LICENSE](LICENSE).

## 16. Contacto

[Tu nombre completo] — [tu.correo@ejemplo.com]
