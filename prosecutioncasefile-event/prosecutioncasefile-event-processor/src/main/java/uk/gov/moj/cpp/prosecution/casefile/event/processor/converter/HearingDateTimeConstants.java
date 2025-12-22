package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.format.DateTimeFormatter;

public class HearingDateTimeConstants {

    public static final DateTimeFormatter DATE_OF_HEARING_PATTERN = ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_OF_HEARING_PATTERN = ofPattern("HH:mm:ss.SSS");
    public static final DateTimeFormatter TIME_OF_HEARING_PATTERN_WITHOUT_MILLIS = ofPattern("HH:mm:ss");

    private HearingDateTimeConstants() {
    }
}
