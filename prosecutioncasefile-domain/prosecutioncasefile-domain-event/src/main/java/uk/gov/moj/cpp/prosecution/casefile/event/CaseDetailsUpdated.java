package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;

@Event("prosecutioncasefile.events.case-details-updated")
public class CaseDetailsUpdated {
    private final String caseId;

    private final String contestedFeeStatus;

    private final String contestedPaymentReference;

    private final String feeStatus;

    private final String paymentReference;

    public CaseDetailsUpdated(final String caseId, final String contestedFeeStatus, final String contestedPaymentReference, final String feeStatus, final String paymentReference) {
        this.caseId = caseId;
        this.contestedFeeStatus = contestedFeeStatus;
        this.contestedPaymentReference = contestedPaymentReference;
        this.feeStatus = feeStatus;
        this.paymentReference = paymentReference;
    }

    public static Builder caseDetailsUpdated() {
        return new CaseDetailsUpdated.Builder();
    }

    public String getCaseId() {
        return caseId;
    }

    public String getContestedFeeStatus() {
        return contestedFeeStatus;
    }

    public String getContestedPaymentReference() {
        return contestedPaymentReference;
    }

    public String getFeeStatus() {
        return feeStatus;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public static class Builder {
        private String caseId;

        private String contestedFeeStatus;

        private String contestedPaymentReference;

        private String feeStatus;

        private String paymentReference;

        public CaseDetailsUpdated.Builder withCaseId(final String caseId) {
            this.caseId = caseId;
            return this;
        }

        public CaseDetailsUpdated.Builder withContestedFeeStatus(final String contestedFeeStatus) {
            this.contestedFeeStatus = contestedFeeStatus;
            return this;
        }

        public CaseDetailsUpdated.Builder withContestedPaymentReference(final String contestedPaymentReference) {
            this.contestedPaymentReference = contestedPaymentReference;
            return this;
        }

        public CaseDetailsUpdated.Builder withFeeStatus(final String feeStatus) {
            this.feeStatus = feeStatus;
            return this;
        }

        public CaseDetailsUpdated.Builder withPaymentReference(final String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }

        public CaseDetailsUpdated build() {
            return new CaseDetailsUpdated(caseId, contestedFeeStatus, contestedPaymentReference, feeStatus, paymentReference);
        }
    }
}
