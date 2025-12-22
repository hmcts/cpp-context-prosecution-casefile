package uk.gov.moj.cpp.prosecution.casefile;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;

import java.util.List;
import java.util.UUID;

public class ValidationHelper {
    private ValidationHelper() {
    }

    public static CaseValidationFailed buildCaseValidationFailedEvent(final Prosecution prosecution, final UUID externalId, final List<Problem> caseProblems, final DefendantsWithReferenceData defendantsWithReferenceData) {
        final InitialHearing initialHearing = defendantsWithReferenceData.getDefendants().isEmpty() ? null : defendantsWithReferenceData.getDefendants().get(0).getInitialHearing();
        return new CaseValidationFailed(prosecution, caseProblems, externalId, initialHearing);
    }
}
