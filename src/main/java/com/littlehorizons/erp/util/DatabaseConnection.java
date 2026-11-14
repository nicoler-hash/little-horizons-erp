package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Singleton that holds the database configuration and hands out new connections. */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "/db.properties";
    private static final DatabaseConnection INSTANCE;

    static {
        INSTANCE = new DatabaseConnection();
    }

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() throws IllegalStateException {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + CONFIG_FILE + " in resources.");
            }
            props.load(in);
        } catch (IOException e) {
            AppLogger.error("Could not read " + CONFIG_FILE, e);
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
    }

    public static DatabaseConnection getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
