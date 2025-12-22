package uk.gov.moj.cpp.prosecution.casefile.event;

import java.util.UUID;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.cc-case-received-with-warnings")
@SuppressWarnings({"squid:S2384", "Duplicates"})
public class CcCaseReceivedWithWarnings {

    @SuppressWarnings("squid:S1948")
    private final ProsecutionWithReferenceData prosecutionWithReferenceData;
    private final List<Problem> caseWarnings;
    private final List<DefendantProblem> defendantWarnings;
    private final SummonsApprovedOutcome summonsApprovedOutcome;
    private final UUID id;

    @JsonCreator
    public CcCaseReceivedWithWarnings(final ProsecutionWithReferenceData prosecutionWithReferenceData, final List<Problem> caseWarnings, final List<DefendantProblem> defendantWarnings, final SummonsApprovedOutcome summonsApprovedOutcome,  final UUID id) {
        this.prosecutionWithReferenceData = prosecutionWithReferenceData;
        this.caseWarnings = caseWarnings;
        this.defendantWarnings = defendantWarnings;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
        this.id = id;
    }

    public static Builder ccCaseReceivedWithWarnings() {
        return new CcCaseReceivedWithWarnings.Builder();
    }

    public ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return prosecutionWithReferenceData;
    }

    public List<Problem> getCaseWarnings() {
        return caseWarnings;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CcCaseReceivedWithWarnings that = (CcCaseReceivedWithWarnings) o;
        return Objects.equals(getProsecutionWithReferenceData(), that.getProsecutionWithReferenceData()) && Objects.equals(getCaseWarnings(), that.getCaseWarnings()) && Objects.equals(getDefendantWarnings(), that.getDefendantWarnings()) && Objects.equals(getSummonsApprovedOutcome(), that.getSummonsApprovedOutcome());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProsecutionWithReferenceData(), getCaseWarnings(), getDefendantWarnings(), getSummonsApprovedOutcome());
    }

    @Override
    public String toString() {
        return "CcCaseReceivedWithWarnings{" +
                "prosecutionWithReferenceData=" + prosecutionWithReferenceData +
                ", caseWarnings=" + caseWarnings +
                ", defendantWarnings=" + defendantWarnings +
                ", summonsApprovedOutcome=" + summonsApprovedOutcome +
                '}';
    }

    public static class Builder {
        private ProsecutionWithReferenceData prosecutionWithReferenceData;
        private List<Problem> caseWarnings;
        private List<DefendantProblem> defendantWarnings;
        private SummonsApprovedOutcome summonsApprovedOutcome;
        private UUID id;

        public Builder withProsecutionWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
            this.prosecutionWithReferenceData = prosecutionWithReferenceData;
            return this;
        }

        public Builder withCaseWarnings(final List<Problem> caseWarnings) {
            this.caseWarnings = caseWarnings;
            return this;
        }

        public Builder withDefendantWarnings(final List<DefendantProblem> defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public Builder withSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
            this.summonsApprovedOutcome = summonsApprovedOutcome;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public CcCaseReceivedWithWarnings build() {
            return new CcCaseReceivedWithWarnings(prosecutionWithReferenceData, caseWarnings, defendantWarnings, summonsApprovedOutcome, id);
        }
    }
}
