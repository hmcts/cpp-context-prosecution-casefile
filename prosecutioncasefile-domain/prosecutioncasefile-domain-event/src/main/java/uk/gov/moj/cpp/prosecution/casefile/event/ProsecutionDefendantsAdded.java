package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.prosecution-defendants-added")
@SuppressWarnings("squid:S2384")
public class ProsecutionDefendantsAdded {

    private final UUID caseId;

    private final UUID externalId;

    private final Channel channel;

    private final List<Defendant> defendants;

    private final ReferenceDataVO referenceDataVO;

    private final List<DefendantProblem> defendantWarnings;

    private final SummonsApprovedOutcome summonsApprovedOutcome;

    @JsonCreator
    public ProsecutionDefendantsAdded(final UUID caseId, final UUID externalId, final Channel channel,
                                      final List<Defendant> defendants, final ReferenceDataVO referenceDataVO,
                                      final List<DefendantProblem> defendantWarnings, final SummonsApprovedOutcome summonsApprovedOutcome) {
        this.caseId = caseId;
        this.externalId = externalId;
        this.channel = channel;
        this.defendants = defendants;
        this.referenceDataVO = referenceDataVO;
        this.defendantWarnings = defendantWarnings;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
    }

    public static Builder prosecutionDefendantsAdded() {
        return new ProsecutionDefendantsAdded.Builder();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public Channel getChannel() {
        return channel;
    }

    public List<DefendantProblem> getDefendantWarnings() {
        return this.defendantWarnings;
    }

    public SummonsApprovedOutcome getSummonsApprovedOutcome() {
        return summonsApprovedOutcome;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProsecutionDefendantsAdded that = (ProsecutionDefendantsAdded) o;

        if (getCaseId() != null ? !getCaseId().equals(that.getCaseId()) : that.getCaseId() != null) {
            return false;
        }
        if (getExternalId() != null ? !getExternalId().equals(that.getExternalId()) : that.getExternalId() != null) {
            return false;
        }
        if (getChannel() != that.getChannel()) {
            return false;
        }
        if (getDefendants() != null ? !getDefendants().equals(that.getDefendants()) : that.getDefendants() != null) {
            return false;
        }
        if (getDefendantWarnings() != null ? !getDefendantWarnings().equals(that.getDefendantWarnings()) : that.getDefendantWarnings() != null) {
            return false;
        }
        return getSummonsApprovedOutcome() != null ? getSummonsApprovedOutcome().equals(that.getSummonsApprovedOutcome()) : that.getSummonsApprovedOutcome() == null;
    }

    @Override
    public int hashCode() {
        int result = getCaseId() != null ? getCaseId().hashCode() : 0;
        result = 31 * result + (getExternalId() != null ? getExternalId().hashCode() : 0);
        result = 31 * result + (getChannel() != null ? getChannel().hashCode() : 0);
        result = 31 * result + (getDefendants() != null ? getDefendants().hashCode() : 0);
        result = 31 * result + (getDefendantWarnings() != null ? getDefendantWarnings().hashCode() : 0);
        result = 31 * result + (getSummonsApprovedOutcome() != null ? getSummonsApprovedOutcome().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProsecutionDefendantsAdded{" +
                "caseId=" + caseId +
                ", externalId=" + externalId +
                ", channel=" + channel +
                ", defendants=" + defendants +
                ", defendantWarnings=" + defendantWarnings +
                ", summonsApprovedOutcome=" + summonsApprovedOutcome +
                '}';
    }

    public static class Builder {
        private UUID caseId;
        private UUID externalId;
        private Channel channel;
        private List<Defendant> defendants;
        private ReferenceDataVO referenceDataVO;
        private List<DefendantProblem> defendantWarnings;
        private SummonsApprovedOutcome summonsApprovedOutcome;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withExternalId(final UUID externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder withChannel(final Channel channel) {
            this.channel = channel;
            return this;
        }

        public Builder withDefendants(final List<Defendant> defendants) {
            this.defendants = defendants;
            return this;
        }

        public Builder withReferenceDataVO(final ReferenceDataVO referenceDataVO) {
            this.referenceDataVO = referenceDataVO;
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

        public ProsecutionDefendantsAdded build() {
            return new ProsecutionDefendantsAdded(caseId, externalId, channel, defendants, referenceDataVO, defendantWarnings, summonsApprovedOutcome);
        }
    }
}
