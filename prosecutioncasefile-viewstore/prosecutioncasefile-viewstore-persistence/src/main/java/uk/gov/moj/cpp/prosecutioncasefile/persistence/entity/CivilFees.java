package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "civil_fee")
public class CivilFees implements Serializable {

    @Id
    @Column(name = "fee_id", unique = true, nullable = false)
    private UUID feeId;

    @Column(name = "case_Id")
    private UUID caseId;

    @Column(name = "fee_type")
    private String feeType;

    @Column(name = "fee_status")
    private String feeStatus;

    @Column(name = "payment_reference")
    private String paymentReference;

    public CivilFees() {
    }

    public CivilFees(final UUID feeId,
                     final UUID caseId,
                     final String feeType,
                     final String feeStatus,
                     final String paymentReference) {
        this.feeId = feeId;
        this.caseId = caseId;
        this.feeType = feeType;
        this.feeStatus = feeStatus;
        this.paymentReference = paymentReference;
    }

    public UUID getFeeId() {
        return feeId;
    }

    public void setFeeId(final UUID feeId) {
        this.feeId = feeId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(final String feeType) {
        this.feeType = feeType;
    }

    public String getFeeStatus() {
        return feeStatus;
    }

    public void setFeeStatus(final String feeStatus) {
        this.feeStatus = feeStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(final String paymentReference) {
        this.paymentReference = paymentReference;
    }
}
