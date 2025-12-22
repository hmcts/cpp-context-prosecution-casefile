package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

@SuppressWarnings("squid:S2384")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDefendantHearings {

    private String caseId;
    private String defendantId;
    private List<DefendantHearings> hearings;

    public CaseDefendantHearings(String caseId, String defendantId, List<DefendantHearings> hearings) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.hearings = hearings;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public List<DefendantHearings> getHearings() {
        return hearings;
    }

    @JsonIgnore
    public ZonedDateTime getEarliestHearingDay() {
        if (nonNull(hearings)) {
            return hearings.stream()
                    .flatMap(dh -> dh.getHearingDays().stream())
                    .min(comparing(HearingDay::getSittingDay))
                    .map(HearingDay::getSittingDay).orElse(null);
        }
        return null;
    }

    @JsonIgnore
    public boolean isEarliestHearingDayInThePast() {
        final ZonedDateTime earliestHearingDay = getEarliestHearingDay();
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        return nonNull(earliestHearingDay)
                && (earliestHearingDay.isBefore(now) || earliestHearingDay.isEqual(now));
    }

    @JsonIgnore
    public boolean isEarliestHearingDayInFuture() {
        final ZonedDateTime earliestHearingDay = getEarliestHearingDay();
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        return nonNull(earliestHearingDay) && earliestHearingDay.isAfter(now);
    }
}
