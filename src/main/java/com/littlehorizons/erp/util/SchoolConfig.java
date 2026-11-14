package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** School identity printed on invoices. Values come from school.properties. */
public final class SchoolConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = SchoolConfig.class.getResourceAsStream("/school.properties")) {
            if (in != null) {
                PROPS.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            AppLogger.error("Could not read school.properties", e);
        }
    }

    private SchoolConfig() { }

    private static String get(String key, String fallback) {
        return PROPS.getProperty(key, fallback);
    }

    public static String legalName() { return get("school.legalName", "Colegio Internacional Little Horizons S.A.S."); }

    public static String nit() { return get("school.nit", "901234567-8"); }

    public static String address() { return get("school.address", "-"); }

    public static String phone() { return get("school.phone", "-"); }

    public static String email() { return get("school.email", "-"); }
}
