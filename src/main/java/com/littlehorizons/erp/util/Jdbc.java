package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.ReportTable;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thin JDBC helper (template pattern). Every statement is a PreparedStatement, every resource is closed
 * with try-with-resources, SQL errors are logged and wrapped in an unchecked DataAccessException, and
 * {@link #inTransaction} commits or rolls back automatically.
 */
public final class Jdbc {

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface Work<T> {
        T execute(Connection conn) throws SQLException;
    }

    public static final Binder NO_ARGS = ps -> { };

    private Jdbc() { }

    /** Binds positional parameters (LocalDate, LocalTime, BigDecimal, String, Integer, null...). */
    public static Binder args(Object... values) {
        return ps -> {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
        };
    }

    // ---------- connection management ----------

    public static <T> T withConnection(String description, Work<T> work) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return work.execute(conn);
        } catch (SQLException e) {
            throw fail(description, e);
        }
    }

    public static <T> T inTransaction(String description, Work<T> work) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail(description, e);
        }
    }

    // ---------- statements on an existing connection (used inside transactions) ----------

    public static <T> List<T> query(Connection conn, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }

    public static <T> Optional<T> queryOne(Connection conn, String sql, Binder binder, RowMapper<T> mapper)
            throws SQLException {
        List<T> rows = query(conn, sql, binder, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    public static int update(Connection conn, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated key. */
    public static int insert(Connection conn, String sql, Binder binder) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    // ---------- statements on their own connection ----------

    public static <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        return withConnection(sql, conn -> query(conn, sql, binder, mapper));
    }

    public static <T> Optional<T> queryOne(String sql, Binder binder, RowMapper<T> mapper) {
        return withConnection(sql, conn -> queryOne(conn, sql, binder, mapper));
    }

    public static int update(String sql, Binder binder) {
        return withConnection(sql, conn -> update(conn, sql, binder));
    }

    public static int insert(String sql, Binder binder) {
        return withConnection(sql, conn -> insert(conn, sql, binder));
    }

    public static boolean exists(String sql, Binder binder) {
        return queryOne(sql, binder, rs -> rs.getInt(1)).orElse(0) > 0;
    }

    /** Runs a query and returns every column as text, ready to be printed. */
    public static ReportTable table(String title, List<String> headers, String sql, Binder binder) {
        return withConnection(title, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    int columns = rs.getMetaData().getColumnCount();
                    List<List<String>> rows = new ArrayList<>();
                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= columns; i++) {
                            String value = rs.getString(i);
                            row.add(value == null ? "-" : value);
                        }
                        rows.add(row);
                    }
                    return new ReportTable(title, headers, rows);
                }
            }
        });
    }

    // ---------- null-safe column readers ----------

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    public static LocalTime time(ResultSet rs, String column) throws SQLException {
        Time value = rs.getTime(column);
        return value == null ? null : value.toLocalTime();
    }

    public static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static DataAccessException fail(String description, SQLException cause) {
        AppLogger.error("Database error: " + description, cause);
        return new DataAccessException(description, cause);
    }
}
