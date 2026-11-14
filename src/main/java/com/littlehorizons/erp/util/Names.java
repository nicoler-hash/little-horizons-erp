package com.littlehorizons.erp.util;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Small helpers for building display names. */
public final class Names {

    private Names() { }

    /** Joins the non-null parts with a single space. */
    public static String join(String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).collect(Collectors.joining(" "));
    }
}
