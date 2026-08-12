package com.learnpulse.backend.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public static String currentIsoTimestamp() {
        return ISO_FORMATTER.format(Instant.now());
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) return null;
        return ISO_FORMATTER.format(instant);
    }
}
