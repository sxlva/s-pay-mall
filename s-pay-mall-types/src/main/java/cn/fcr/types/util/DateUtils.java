package cn.fcr.types.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 基于 LocalDateTime 的日期时间工具，提供格式化、解析等常用操作
 *
 * @author 傅崇睿
 */
public final class DateUtils {

    private DateUtils() {
    }

    /** 默认时区 */
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    /** 默认日期时间格式 */
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取当前日期时间（默认时区）
     *
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 获取当前日期时间（指定时区）
     *
     * @param zoneId 时区
     * @return 当前 LocalDateTime
     */
    public static LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(zoneId);
    }

    /**
     * 格式化为默认格式字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * 按指定格式转为字符串
     *
     * @param dateTime 日期时间
     * @param pattern  日期格式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将默认格式字符串解析为 LocalDateTime
     *
     * @param dateStr 日期字符串（yyyy-MM-dd HH:mm:ss）
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateStr) {
        return LocalDateTime.parse(dateStr, DEFAULT_FORMATTER);
    }

    /**
     * 按指定格式解析字符串为 LocalDateTime
     *
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateStr, String pattern) {
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }
}