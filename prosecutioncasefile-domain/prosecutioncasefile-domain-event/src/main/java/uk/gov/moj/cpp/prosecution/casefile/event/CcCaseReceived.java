package uk.gov.moj.cpp.prosecution.casefile.event;

import java.util.UUID;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

@SuppressWarnings("Duplicates")
@Event("prosecutioncasefile.events.cc-case-received")
public class CcCaseReceived implements Serializable {
    private static final long serialVersionUID = 609898103244023485L;

    @SuppressWarnings("squid:S1948")
    private final ProsecutionWithReferenceData prosecutionWithReferenceData;
    @SuppressWarnings("squid:S1948")
    private final SummonsApprovedOutcome summonsApprovedOutcome;
    private final UUID id;

    @JsonCreator
    public CcCaseReceived(final ProsecutionWithReferenceData prosecutionWithReferenceData, final SummonsApprovedOutcome summonsApprovedOutcome, final UUID id) {
        this.prosecutionWithReferenceData = prosecutionWithReferenceData;
        this.summonsApprovedOutcome = summonsApprovedOutcome;
        this.id = id;
    }

    public static Builder ccCaseReceived() {
        return new CcCaseReceived.Builder();
    }

    public ProsecutionWithReferenceData getProsecutionWithReferenceData() {
        return prosecutionWithReferenceData;
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
        final CcCaseReceived that = (CcCaseReceived) o;
        return Objects.equals(getProsecutionWithReferenceData(), that.getProsecutionWithReferenceData()) && Objects.equals(getSummonsApprovedOutcome(), that.getSummonsApprovedOutcome());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProsecutionWithReferenceData(), getSummonsApprovedOutcome());
    }

    @Override
    public String toString() {
        return "CcCaseReceived{" +
                "prosecutionWithReferenceData=" + prosecutionWithReferenceData +
                ", summonsApprovedOutcome=" + summonsApprovedOutcome +
                '}';
    }

    public static class Builder {
        private ProsecutionWithReferenceData prosecutionWithReferenceData;
        private SummonsApprovedOutcome summonsApprovedOutcome;
        private UUID id;

        public Builder withProsecutionWithReferenceData(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
            this.prosecutionWithReferenceData = prosecutionWithReferenceData;
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

        public CcCaseReceived build() {
            return new CcCaseReceived(prosecutionWithReferenceData, summonsApprovedOutcome, id);
        }
    }
}
