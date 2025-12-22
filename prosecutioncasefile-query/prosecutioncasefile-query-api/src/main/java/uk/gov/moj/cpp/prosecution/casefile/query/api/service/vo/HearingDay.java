package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingDay {

    private String courtCentreId;
    private String roomId;
    private ZonedDateTime sittingDay;

    public HearingDay(String courtCentreId, String roomId, ZonedDateTime sittingDay) {
        this.courtCentreId = courtCentreId;
        this.roomId = roomId;
        this.sittingDay = sittingDay;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public String getRoomId() {
        return roomId;
    }

    public ZonedDateTime getSittingDay() {
        return sittingDay;
    }

    public static HearingDay.Builder hearingDay() {
        return new HearingDay.Builder();
    }

    public static class Builder {
        private String courtCentreId;
        private String roomId;
        private ZonedDateTime sittingDay;

        public HearingDay.Builder withCourtCentreId(final String courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public HearingDay.Builder withRoomId(final String roomId) {
            this.roomId = roomId;
            return this;
        }

        public HearingDay.Builder withSittingDay(final ZonedDateTime sittingDay) {
            this.sittingDay = sittingDay;
            return this;
        }

        public HearingDay build() {
            return new HearingDay(courtCentreId, roomId, sittingDay);
        }
    }
}