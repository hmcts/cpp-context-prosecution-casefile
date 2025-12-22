package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.case-validation-failed")
@SuppressWarnings("squid:S2384")
public class CaseValidationFailed {

    private final Prosecution prosecution;

    private final List<Problem> problems;

    private final UUID externalId;

    private final InitialHearing initialHearing;

    @JsonCreator
    public CaseValidationFailed(final Prosecution prosecution, final List<Problem> problems, final UUID externalId, InitialHearing initialHearing) {
        this.prosecution = prosecution;
        this.problems = problems;
        this.externalId = externalId;
        this.initialHearing = initialHearing;
    }

    public Prosecution getProsecution() {
        return prosecution;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public InitialHearing getInitialHearing() {
        return initialHearing;
    }

}
