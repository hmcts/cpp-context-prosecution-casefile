package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Hearing implements Serializable {
    private static final long serialVersionUID = -8292925776055532727L;

    @Column(name = "court_id")
    private String courtId;

    @Column(name = "court_Name")
    private String courtName;

    @Column(name = "court_room")
    private Integer courtRoom;

    @Column(name = "type_of_court")
    private String typeOfCourt;

    @Column(name = "hearing_date")
    private String hearingDate;

    public Hearing() {
    }

    public Hearing(final String courtId, final String courtName, final Integer courtRoom, final String typeOfCourt, final String hearingDate) {
        setCourtId(courtId);
        setCourtName(courtName);
        setCourtRoom(courtRoom);
        setTypeOfCourt(typeOfCourt);
        setHearingDate(hearingDate);
    }

    public String getCourtId() {
        return courtId;
    }

    public Integer getCourtRoom() {
        return courtRoom;
    }

    public String getTypeOfCourt() {
        return typeOfCourt;
    }

    public String getHearingDate() {
        return hearingDate;
    }

    public String getCourtName() {
        return courtName;
    }

    private void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    private void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    private void setCourtRoom(Integer courtRoom) {
        this.courtRoom = courtRoom;
    }

    private void setTypeOfCourt(String typeOfCourt) {
        this.typeOfCourt = typeOfCourt;
    }

    private void setHearingDate(String hearingDate) {
        this.hearingDate = hearingDate;
    }
}
