package com.dtech.auth.util;

import java.time.ZoneId;
import java.time.Instant;
import java.util.Date;

/**
 * Converts database date-only values without allowing a JVM timezone to move
 * the calendar date backward or forward.
 */
public final class DateOnlyUtil {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Colombo");

    private DateOnlyUtil() {
    }

    public static String format(Date value) {
        if (value == null) {
            return null;
        }
        return Instant.ofEpochMilli(value.getTime())
                .atZone(APPLICATION_ZONE)
                .toLocalDate()
                .toString();
    }
}
