package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.justice.core.courts.SummonsApprovedOutcome;

import java.util.UUID;

public class SummonsApplicationApprovedDetails {
    private final UUID applicationId;

    private final SummonsApprovedOutcome summonsApprovedOutcome;

    private final UUID caseId;

    public SummonsApplicationApprovedDetails(final UUID caseId, final UUID applicationId, final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.applicationId = applicationId;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    public static Builder summonsApplicationApprovedDetails() {
        return new SummonsApplicationApprovedDetails.Builder();
    }

    public static class Builder {
        private UUID applicationId;

        private SummonsApprovedOutcome summonsApprovedOutcome;

        private UUID caseId;

        public Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder withSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
            this.summonsApprovedOutcome = summonsApprovedOutcome;
            return this;
        }

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public SummonsApplicationApprovedDetails build() {
            return new SummonsApplicationApprovedDetails(caseId, applicationId, summonsApprovedOutcome);
        }
    }
}
