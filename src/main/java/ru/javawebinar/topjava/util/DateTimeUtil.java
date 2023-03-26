package ru.javawebinar.topjava.util;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateTimeUtil {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1,1,1,0,0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(3000, 1,1,0,0);

    public static String toString(LocalDateTime ldt) {
        return ldt == null ? "" : ldt.format(DATE_TIME_FORMATTER);
    }
    public static LocalDateTime getStartInclusive(LocalDate localDate){
        return localDate != null ? localDate.atStartOfDay() : MIN_DATE;
    }
    public static LocalDateTime getEndExclusive(LocalDate localDate){
        return localDate != null ? localDate.plus(1, ChronoUnit.DAYS).atStartOfDay() : MAX_DATE;
    }
    public static @Nullable LocalDate parseLocalDate(@Nullable String string){
        return StringUtils.hasLength(string) ? LocalDate.parse(string) : null;
    }
    public static @Nullable LocalTime parseLocaleTime(@Nullable String string){
        return StringUtils.hasLength(string) ? LocalTime.parse(string) : null;
    }

}

