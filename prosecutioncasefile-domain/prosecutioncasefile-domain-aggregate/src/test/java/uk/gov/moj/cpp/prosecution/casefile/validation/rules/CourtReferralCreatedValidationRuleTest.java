package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.lang.Boolean.FALSE;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_REFERRED_TO_OPEN_COURT;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class CourtReferralCreatedValidationRuleTest {

    private final CourtReferralCreatedValidationRule sut = new CourtReferralCreatedValidationRule();

    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldValidateToHaveAProblemWhenTheCaseIsReferredToCourt() {
        UUID referralReasonId = randomUUID();

        Optional<Problem> problemOptional = sut.validate(getCaseDocumentWithReferenceData(referralReasonId, true), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problemOptional, equalTo(of(
                new Problem(
                        CASE_REFERRED_TO_OPEN_COURT.toString(),
                        singletonList(new ProblemValue(null, "referralReasonId", referralReasonId.toString()))))));

    }

    @Test
    public void shouldValidateToNotHaveAProblemWhenTheCaseIsNotReferredToCourt() {
        Optional<Problem> problemOptional = sut.validate(getCaseDocumentWithReferenceData(randomUUID(), false), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problemOptional.isPresent(), is(FALSE));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final UUID referralReasonId, final boolean caseReferredToCourt) {
        return new CaseDocumentWithReferenceData(referralReasonId, caseReferredToCourt, null, null, null, null, false, false);
    }

}