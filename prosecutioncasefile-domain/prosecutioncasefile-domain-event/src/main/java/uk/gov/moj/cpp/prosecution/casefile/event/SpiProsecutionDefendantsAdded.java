package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * @deprecated use {@link ProsecutionDefendantsAdded}
 */
@Deprecated(since = "7.0.32")
@Event("prosecutioncasefile.events.spi-prosecution-defendants-added")
@SuppressWarnings({"squid:S2384","squid:S1133"})
public class SpiProsecutionDefendantsAdded {

    private final UUID caseId;

    private final UUID externalId;

    private final List<Defendant> defendants;

    private final ReferenceDataVO referenceDataVO;

    private final List<DefendantProblem> defendantWarnings;

    @JsonCreator
    public SpiProsecutionDefendantsAdded(final UUID caseId, final UUID externalId, final List<Defendant> defendants,
                                         final ReferenceDataVO referenceDataVO, final List<DefendantProblem> defendantWarnings) {
        this.caseId = caseId;
        this.externalId = externalId;
        this.defendants = defendants;
        this.referenceDataVO = referenceDataVO;
        this.defendantWarnings = defendantWarnings;
    }

    public static Builder spiProsecutionDefendantsAdded() {
        return new SpiProsecutionDefendantsAdded.Builder();
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

    public List<DefendantProblem> getDefendantWarnings() {
        return defendantWarnings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SpiProsecutionDefendantsAdded that = (SpiProsecutionDefendantsAdded) o;

        if (getCaseId() != null ? !getCaseId().equals(that.getCaseId()) : that.getCaseId() != null) {
            return false;
        }
        if (getExternalId() != null ? !getExternalId().equals(that.getExternalId()) : that.getExternalId() != null) {
            return false;
        }
        if (getDefendants() != null ? !getDefendants().equals(that.getDefendants()) : that.getDefendants() != null) {
            return false;
        }
        return getDefendantWarnings() != null ? getDefendantWarnings().equals(that.getDefendantWarnings()) : that.getDefendantWarnings() == null;
    }

    @Override
    public int hashCode() {
        int result = getCaseId() != null ? getCaseId().hashCode() : 0;
        result = 31 * result + (getExternalId() != null ? getExternalId().hashCode() : 0);
        result = 31 * result + (getDefendants() != null ? getDefendants().hashCode() : 0);
        result = 31 * result + (getDefendantWarnings() != null ? getDefendantWarnings().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SpiProsecutionDefendantsAdded{" +
                "caseId=" + caseId +
                ", externalId=" + externalId +
                ", defendants=" + defendants +
                ", defendantWarnings=" + defendantWarnings +
                '}';
    }

    public static class Builder {
        private UUID caseId;

        private UUID externalId;

        private ReferenceDataVO referenceDataVO;

        private List<Defendant> defendants;

        private List<DefendantProblem> defendantWarnings;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withExternalId(final UUID externalId) {
            this.externalId = externalId;
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

        public SpiProsecutionDefendantsAdded build() {
            return new SpiProsecutionDefendantsAdded(caseId, externalId, defendants, referenceDataVO, defendantWarnings);
        }
    }
}
