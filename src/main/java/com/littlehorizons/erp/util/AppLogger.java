package com.littlehorizons.erp.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Central logger. Writes errors to error_log.txt in the working directory. */
public final class AppLogger {

    private static final String LOG_FILE = "error_log.txt";
    private static final Logger LOGGER = Logger.getLogger("littlehorizons.erp");

    static {
        try {
            FileHandler handler = new FileHandler(LOG_FILE, true);
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Could not open log file " + LOG_FILE + ": " + e.getMessage());
        }
    }

    private AppLogger() { }

    public static void error(String message, Throwable cause) {
        LOGGER.log(Level.SEVERE, message, cause);
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }
}
