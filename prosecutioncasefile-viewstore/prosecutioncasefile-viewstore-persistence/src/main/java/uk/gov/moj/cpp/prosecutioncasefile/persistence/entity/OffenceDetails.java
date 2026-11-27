package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "offence")
public class OffenceDetails implements Serializable {

    private static final long serialVersionUID = -6173322170572695945L;

    @Id
    @Column(name = "offence_id")
    private UUID offenceId;

    @Column(name = "applied_compensation")
    private BigDecimal appliedCompensation;

    @Column(name = "back_duty")
    private BigDecimal backDuty;

    @Column(name = "back_duty_date_from")
    private LocalDate backDutyDateFrom;

    @Column(name = "back_duty_date_to")
    private LocalDate backDutyDateTo;

    @Column(name = "charge_date")
    private LocalDate chargeDate;

    @Column(name = "offence_code")
    private String offenceCode;

    @Column(name = "offence_committed_date")
    private LocalDate offenceCommittedDate;

    @Column(name = "offence_committed_end_date")
    private LocalDate offenceCommittedEndDate;

    @Column(name = "offence_date_code")
    private Integer offenceDateCode;

    @Column(name = "offence_location")
    private String offenceLocation;

    @Column(name = "offence_sequence_number")
    private Integer offenceSequenceNumber;

    @Column(name = "offence_wording")
    private String offenceWording;

    @Column(name = "offence_wording_welsh")
    private String offenceWordingWelsh;

    @Column(name = "statement_of_facts")
    private String statementOfFacts;

    @Column(name = "statement_of_facts_welsh")
    private String statementOfFactsWelsh;

    @Column(name = "vehicle_make")
    private String vehicleMake;

    @Column(name = "vehicle_registration_mark")
    private String vehicleRegistrationMark;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private DefendantDetails defendant;

    @Embedded
    private AlcoholOffenceDetail alcoholOffenceDetail;

    @SuppressWarnings("squid:S00107")
    public OffenceDetails(final UUID offenceId,
                          final BigDecimal appliedCompensation,
                          final BigDecimal backDuty,
                          final LocalDate backDutyDateFrom,
                          final LocalDate backDutyDateTo,
                          final LocalDate chargeDate,
                          final String offenceCode,
                          final LocalDate offenceCommittedDate,
                          final LocalDate offenceCommittedEndDate,
                          final Integer offenceDateCode,
                          final String offenceLocation,
                          final Integer offenceSequenceNumber,
                          final String offenceWording,
                          final String offenceWordingWelsh,
                          final String statementOfFacts,
                          final String statementOfFactsWelsh,
                          final String vehicleMake,
                          final String vehicleRegistrationMark,
                          final AlcoholOffenceDetail alcoholOffenceDetail) {
        this.offenceId = offenceId;
        this.appliedCompensation = appliedCompensation;
        this.backDuty = backDuty;
        this.backDutyDateFrom = backDutyDateFrom;
        this.backDutyDateTo = backDutyDateTo;
        this.chargeDate = chargeDate;
        this.offenceCode = offenceCode;
        this.offenceCommittedDate = offenceCommittedDate;
        this.offenceCommittedEndDate = offenceCommittedEndDate;
        this.offenceDateCode = offenceDateCode;
        this.offenceLocation = offenceLocation;
        this.offenceSequenceNumber = offenceSequenceNumber;
        this.offenceWording = offenceWording;
        this.offenceWordingWelsh = offenceWordingWelsh;
        this.statementOfFacts = statementOfFacts;
        this.statementOfFactsWelsh = statementOfFactsWelsh;
        this.vehicleMake = vehicleMake;
        this.vehicleRegistrationMark = vehicleRegistrationMark;
        this.alcoholOffenceDetail = alcoholOffenceDetail;
    }

    public OffenceDetails() {

    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public BigDecimal getAppliedCompensation() {
        return appliedCompensation;
    }

    public void setAppliedCompensation(final BigDecimal appliedCompensation) {
        this.appliedCompensation = appliedCompensation;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public void setBackDuty(final BigDecimal backDuty) {
        this.backDuty = backDuty;
    }

    public LocalDate getBackDutyDateFrom() {
        return backDutyDateFrom;
    }

    public void setBackDutyDateFrom(final LocalDate backDutyDateFrom) {
        this.backDutyDateFrom = backDutyDateFrom;
    }

    public LocalDate getBackDutyDateTo() {
        return backDutyDateTo;
    }

    public void setBackDutyDateTo(final LocalDate backDutyDateTo) {
        this.backDutyDateTo = backDutyDateTo;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(final LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public void setOffenceCode(final String offenceCode) {
        this.offenceCode = offenceCode;
    }

    public LocalDate getOffenceCommittedDate() {
        return offenceCommittedDate;
    }

    public void setOffenceCommittedDate(final LocalDate offenceCommittedDate) {
        this.offenceCommittedDate = offenceCommittedDate;
    }

    public LocalDate getOffenceCommittedEndDate() {
        return offenceCommittedEndDate;
    }

    public void setOffenceCommittedEndDate(final LocalDate offenceCommittedEndDate) {
        this.offenceCommittedEndDate = offenceCommittedEndDate;
    }

    public Integer getOffenceDateCode() {
        return offenceDateCode;
    }

    public void setOffenceDateCode(final Integer offenceDateCode) {
        this.offenceDateCode = offenceDateCode;
    }

    public String getOffenceLocation() {
        return offenceLocation;
    }

    public void setOffenceLocation(final String offenceLocation) {
        this.offenceLocation = offenceLocation;
    }

    public Integer getOffenceSequenceNumber() {
        return offenceSequenceNumber;
    }

    public void setOffenceSequenceNumber(final Integer offenceSequenceNumber) {
        this.offenceSequenceNumber = offenceSequenceNumber;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public void setOffenceWording(final String offenceWording) {
        this.offenceWording = offenceWording;
    }

    public String getOffenceWordingWelsh() {
        return offenceWordingWelsh;
    }

    public void setOffenceWordingWelsh(final String offenceWordingWelsh) {
        this.offenceWordingWelsh = offenceWordingWelsh;
    }

    public String getStatementOfFacts() {
        return statementOfFacts;
    }

    public void setStatementOfFacts(final String statementOfFacts) {
        this.statementOfFacts = statementOfFacts;
    }

    public String getStatementOfFactsWelsh() {
        return statementOfFactsWelsh;
    }

    public void setStatementOfFactsWelsh(final String statementOfFactsWelsh) {
        this.statementOfFactsWelsh = statementOfFactsWelsh;
    }

    public DefendantDetails getDefendant() {
        return defendant;
    }

    public void setDefendant(final DefendantDetails defendant) {
        this.defendant = defendant;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleRegistrationMark() {
        return vehicleRegistrationMark;
    }

    public void setVehicleRegistrationMark(String vehicleRegistrationMark) {
        this.vehicleRegistrationMark = vehicleRegistrationMark;
    }

    public AlcoholOffenceDetail getAlcoholOffenceDetail() {
        return alcoholOffenceDetail;
    }

    public void setAlcoholOffenceDetail(final AlcoholOffenceDetail alcoholOffenceDetail) {
        this.alcoholOffenceDetail = alcoholOffenceDetail;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}

