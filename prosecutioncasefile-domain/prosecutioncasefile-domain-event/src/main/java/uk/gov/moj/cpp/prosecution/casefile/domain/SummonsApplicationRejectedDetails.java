package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.justice.core.courts.SummonsRejectedOutcome;

import java.util.UUID;

public class SummonsApplicationRejectedDetails {
    private final UUID caseId;

    private final UUID applicationId;

    private final SummonsRejectedOutcome summonsRejectedOutcome;

    public SummonsApplicationRejectedDetails(final UUID caseId, final UUID applicationId, final SummonsRejectedOutcome summonsRejectedOutcome) {
        this.applicationId = applicationId;
        this.summonsRejectedOutcome = summonsRejectedOutcome;
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public SummonsRejectedOutcome getSummonsRejectedOutcome() {
        return summonsRejectedOutcome;
    }

    public static Builder summonsApplicationRejectedDetails() {
        return new SummonsApplicationRejectedDetails.Builder();
    }

    public static class Builder {
        private UUID caseId;

        private UUID applicationId;

        private SummonsRejectedOutcome summonsRejectedOutcome;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder withSummonsRejectedOutcome(final SummonsRejectedOutcome summonsRejectedOutcome) {
            this.summonsRejectedOutcome = summonsRejectedOutcome;
            return this;
        }

        public SummonsApplicationRejectedDetails build() {
            return new SummonsApplicationRejectedDetails(caseId, applicationId, summonsRejectedOutcome);
        }
    }
}
