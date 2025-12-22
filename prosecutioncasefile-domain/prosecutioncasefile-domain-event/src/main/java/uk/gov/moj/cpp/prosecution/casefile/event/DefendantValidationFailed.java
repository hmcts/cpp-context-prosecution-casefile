package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.defendant-validation-failed")
@SuppressWarnings("squid:S2384")
public class DefendantValidationFailed {

    private final Defendant defendant;

    private final List<Problem> problems;

    private final UUID caseId;

    private final String urn;

    private final String caseType;

    private final String policeSystemId;


    @JsonCreator
    public DefendantValidationFailed(final Defendant defendant, final List<Problem> problems, final UUID caseId, final String urn, final String caseType, final String policeSystemId) {
        this.defendant = defendant;
        this.problems = problems;
        this.caseId = caseId;
        this.urn = urn;
        this.caseType = caseType;
        this.policeSystemId =  policeSystemId;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public String getCaseType() {
        return caseType;
    }

    public String getPoliceSystemId() {
        return policeSystemId;
    }

    public static DefendantValidationFailed.Builder defendantValidationFailed() {
        return new DefendantValidationFailed.Builder();
    }
    public static class Builder {
        private Defendant defendant;
        private List<Problem> problems;
        private UUID caseId;
        private String urn;
        private String caseType;
        private String policeSystemId;

        public DefendantValidationFailed.Builder withDefendant(final Defendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public DefendantValidationFailed.Builder withProblems(final List<Problem> problems) {
            this.problems = new ArrayList<>(problems);
            return this;
        }

        public DefendantValidationFailed.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantValidationFailed.Builder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public DefendantValidationFailed.Builder withCaseType(final String caseType) {
            this.caseType = caseType;
            return this;
        }

        public DefendantValidationFailed.Builder withPoliceSystemId(final String policeSystemId) {
            this.policeSystemId = policeSystemId;
            return this;
        }

        public DefendantValidationFailed build() {
            return new DefendantValidationFailed(defendant, problems, caseId, urn, caseType, policeSystemId);
        }
    }
}
