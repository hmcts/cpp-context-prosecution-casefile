package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "business_validation_errors")
public class BusinessValidationErrorDetails implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "error_value")
    private String errorValue;

    @Column(name = "field_id")
    private String fieldId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "defendant_id")
    private UUID defendantId;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "court_name")
    private String courtName;

    @Column(name = "court_location")
    private String courtLocation;

    @Column(name = "case_type")
    private String caseType;

    @Column(name = "urn")
    private String urn;

    @Column(name = "defendant_bail_status")
    private String defendantBailStatus;

    @Column(name = "defendant_charge_date")
    private LocalDate defendantChargeDate;

    @Column(name = "defendant_hearing_date")
    private LocalDate defendantHearingDate;

    @Column(name = "version")
    private Long version;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;


    public BusinessValidationErrorDetails() {

    }

    @SuppressWarnings("squid:S00107")
    public BusinessValidationErrorDetails(final UUID id,
                                          final String errorValue,
                                          final String fieldId,
                                          final String displayName,
                                          final UUID caseId,
                                          final UUID defendantId,
                                          final String fieldName,
                                          final String courtName,
                                          final String courtLocation,
                                          final String caseType,
                                          final String urn,
                                          final String defendantBailStatus,
                                          final String firstName,
                                          final String lastName,
                                          final String organisationName,
                                          final LocalDate defendantChargeDate,
                                          final LocalDate defendantHearingDate,
                                          final LocalDate dateOfBirth) {
        this.id = id;
        this.errorValue = errorValue;
        this.fieldId = fieldId;
        this.displayName = displayName;
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.fieldName = fieldName;
        this.courtName = courtName;
        this.courtLocation = courtLocation;
        this.caseType = caseType;
        this.urn = urn;
        this.defendantBailStatus = defendantBailStatus;
        this.defendantChargeDate = defendantChargeDate;
        this.defendantHearingDate = defendantHearingDate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.organisationName = organisationName;
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getErrorValue() {
        return errorValue;
    }

    public void setErrorValue(final String errorValue) {
        this.errorValue = errorValue;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(final String fieldId) {
        this.fieldId = fieldId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public String getCourtName() {
        return courtName;
    }

    public void setCourtName(final String courtName) {
        this.courtName = courtName;
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

    public String getDefendantBailStatus() {
        return defendantBailStatus;
    }

    public void setDefendantBailStatus(final String defendantBailStatus) {
        this.defendantBailStatus = defendantBailStatus;
    }

    public LocalDate getDefendantChargeDate() {
        return defendantChargeDate;
    }

    public void setDefendantChargeDate(final LocalDate defendantChargeDate) {
        this.defendantChargeDate = defendantChargeDate;
    }

    public LocalDate getDefendantHearingDate() {
        return defendantHearingDate;
    }

    public void setDefendantHearingDate(final LocalDate defendantHearingDate) {
        this.defendantHearingDate = defendantHearingDate;
    }

    public String getCourtLocation() {
        return courtLocation;
    }

    public void setCourtLocation(final String courtLocation) {
        this.courtLocation = courtLocation;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(final Long versionNumber) {
        this.version = versionNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
