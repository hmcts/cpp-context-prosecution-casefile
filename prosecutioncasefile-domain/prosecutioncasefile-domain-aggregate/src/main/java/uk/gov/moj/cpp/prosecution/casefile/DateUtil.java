package uk.gov.moj.cpp.prosecution.casefile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final String DATE_TEN_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";

    private DateUtil() {
    }

    public static LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_TEN_FORMAT_YYYY_MM_DD));
    }

    public static String convertToStringFromLocalDate(final LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(DATE_TEN_FORMAT_YYYY_MM_DD));
    }
}
