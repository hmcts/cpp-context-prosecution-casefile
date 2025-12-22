package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.defendants-parked-for-summons-application-approval")
@SuppressWarnings("squid:S2384")
public class DefendantsParkedForSummonsApplicationApproval implements Serializable {

    private static final long serialVersionUID = -4951490385243539665L;

    private final UUID applicationId;
    @SuppressWarnings("squid:S1948")
    private final ProsecutionWithReferenceData prosecutionWithReferenceData;
    private final List<DefendantProblem> defendantWarnings;

    @JsonCreator
    public DefendantsParkedForSummonsApplicationApproval(final UUID applicationId, final ProsecutionWithReferenceData prosecutionWithReferenceData, final List<DefendantProblem> defendantWarnings) {
        this.applicationId = applicationId;
        this.prosecutionWithReferenceData = prosecutionWithReferenceData;
        this.defendantWarnings = defendantWarnings;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return prosecutionWithReferenceData;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    public static DefendantsParkedForSummonsApplicationApproval.Builder defendantsParkedForSummonsApplicationApproval() {
        return new DefendantsParkedForSummonsApplicationApproval.Builder();
    }

    public static class Builder {
        private UUID applicationId;
        private ProsecutionWithReferenceData prosecutionWithReferenceData;
        private List<DefendantProblem> defendantWarnings;

        public DefendantsParkedForSummonsApplicationApproval.Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public DefendantsParkedForSummonsApplicationApproval.Builder withProsecutionWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
            this.prosecutionWithReferenceData = prosecutionWithReferenceData;
            return this;
        }

        public DefendantsParkedForSummonsApplicationApproval.Builder withDefendantWarnings(final List<DefendantProblem> defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public DefendantsParkedForSummonsApplicationApproval build() {
            return new DefendantsParkedForSummonsApplicationApproval(applicationId, prosecutionWithReferenceData, defendantWarnings);
        }
    }
}
