package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@SuppressWarnings("squid:S2384")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefendantHearings {

    private String hearingId;
    private String hearingListingStatus;
    private List<HearingDay> hearingDays;

    public DefendantHearings(String hearingId, String hearingListingStatus, List<HearingDay> hearingDays) {
        this.hearingId = hearingId;
        this.hearingListingStatus = hearingListingStatus;
        this.hearingDays = hearingDays;
    }

    public String getHearingId() {
        return hearingId;
    }

    public String getHearingListingStatus() {
        return hearingListingStatus;
    }

    public List<HearingDay> getHearingDays() {
        return hearingDays;
    }

    public static DefendantHearings.Builder defendantHearings() {
        return new DefendantHearings.Builder();
    }

    public static class Builder {
        private String hearingId;
        private String hearingListingStatus;
        private List<HearingDay> hearingDays;

        public DefendantHearings.Builder withHearingId(final String hearingId) {
            this.hearingId = hearingId;
            return this;
        }

        public DefendantHearings.Builder withHearingListingStatus(final String hearingListingStatus) {
            this.hearingListingStatus = hearingListingStatus;
            return this;
        }

        public DefendantHearings.Builder withHearingDays(final List<HearingDay> hearingDays) {
            this.hearingDays = hearingDays;
            return this;
        }

        public DefendantHearings build() {
            return new DefendantHearings(hearingId, hearingListingStatus, hearingDays);
        }
    }
}