package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "business_validation_errors_case_details")
public class BusinessValidationErrorCaseDetails implements Serializable {

    @Id
    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "case_details")
    private String caseDetails;


    public BusinessValidationErrorCaseDetails() {

    }

    @SuppressWarnings("squid:S00107")
    public BusinessValidationErrorCaseDetails(final UUID caseId,
                                              final String caseDetails) {
        this.caseId = caseId;
        this.caseDetails = caseDetails;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final String caseDetails) {
        this.caseDetails = caseDetails;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
