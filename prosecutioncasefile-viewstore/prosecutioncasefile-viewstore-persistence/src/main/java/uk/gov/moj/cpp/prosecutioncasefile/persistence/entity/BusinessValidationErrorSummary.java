package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "business_validation_errors_summary_view")
public class BusinessValidationErrorSummary implements Serializable {

    @Id
    @Column(name = "case_id", nullable = false, unique = true)
    private UUID caseId;

    @Column(name = "court_location")
    private String courtLocation;

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "urn")
    private String urn;

    @Column(name = "defendant_hearing_date")
    private LocalDate defendantHearingDate;

    @Column(name = "defendant_bail_status")
    private String defendantBailStatus;

    public BusinessValidationErrorSummary() {
        // Required by JPA
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCourtLocation() {
        return courtLocation;
    }

    public void setCourtLocation(final String courtLocation) {
        this.courtLocation = courtLocation;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(final String caseType) {
        this.caseType = caseType;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }

    public LocalDate getDefendantHearingDate() {
        return defendantHearingDate;
    }

    public void setDefendantHearingDate(final LocalDate defendantHearingDate) {
        this.defendantHearingDate = defendantHearingDate;
    }

    public String getDefendantBailStatus() {
        return defendantBailStatus;
    }

    public void setDefendantBailStatus(final String defendantBailStatus) {
        this.defendantBailStatus = defendantBailStatus;
    }

}
