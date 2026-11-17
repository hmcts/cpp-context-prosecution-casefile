package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.HearingDateTimeConstants.DATE_OF_HEARING_PATTERN;

import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * The single reading of when a case-level found hearing starts.
 * <p>
 * A magistrates' "find a hearing" case carries its hearing on the prosecution as a
 * {@code listNewHearing} rather than per-defendant as an {@code initialHearing}, and how you read
 * its start depends on the {@link HearingDateTimeType}: a {@code FIXED} hearing is pinned by its
 * earliest start (falling back to the listed start), a {@code WEEK_COMMENCING} one only by the week
 * it falls in. Anything else — {@code DATE_TO_BE_FIXED}, or a hearing missing the field its type
 * implies — has no knowable start and yields an empty optional; callers decide what that means.
 * <p>
 * Kept in one place deliberately: the box hearing's application due date
 * ({@link ProsecutionToBoxHearingRequestConverter}) and the youth determination on the court
 * application party ({@link ProsecutionCaseFileDefendantToCourtApplicationPartyConverter}) must
 * agree on the hearing date, and previously held a copy each.
 */
public class HearingRequestStart {

    private HearingRequestStart() {
    }

    /**
     * When the found hearing starts, or empty when the request is absent or its type carries no
     * usable date.
     */
    public static Optional<ZonedDateTime> startDateTime(final HearingRequest listNewHearing) {
        if (isNull(listNewHearing)) {
            return empty();
        }

        final HearingDateTimeType hearingDateTimeType = listNewHearing.getHearingDateTimeType();

        if (HearingDateTimeType.FIXED == hearingDateTimeType) {
            return ofNullable(ofNullable(listNewHearing.getEarliestStartDateTime())
                    .orElse(listNewHearing.getListedStartDateTime()));
        }

        if (HearingDateTimeType.WEEK_COMMENCING == hearingDateTimeType) {
            return ofNullable(listNewHearing.getWeekCommencingDate())
                    .map(WeekCommencingDate::getStartDate)
                    .map(startDate -> LocalDate.parse(startDate, DATE_OF_HEARING_PATTERN).atStartOfDay(UTC));
        }

        return empty();
    }

    /**
     * The date the found hearing starts, for callers that only need the day.
     */
    public static Optional<LocalDate> startDate(final HearingRequest listNewHearing) {
        return startDateTime(listNewHearing).map(ZonedDateTime::toLocalDate);
    }
}
