package cn.fcr.types.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {
    }

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DEFAULT_FORMATTER);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parse(String dateStr) {
        return LocalDateTime.parse(dateStr, DEFAULT_FORMATTER);
    }

    public static LocalDateTime parse(String dateStr, String pattern) {
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }
}