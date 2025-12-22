package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.PROSECUTOR_OUCODE_NOT_RECOGNISED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorValidationRuleTest {

    private static final String OU_CODE = "OU_CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private ProsecutorValidationRule prosecutorValidationRule = new ProsecutorValidationRule();

    @Test
    public void shouldReturnValidWhenProsecutionCaseIsNull() {

        final Optional<Problem> optionalProblem = prosecutorValidationRule.validate(getCaseDocumentWithReferenceData(null), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenOUCodeIsValid() {

        when(referenceDataQueryService.retrieveProsecutors(OU_CODE)).thenReturn(getProsecutionReferenceData());

        final Optional<Problem> optionalProblem = prosecutorValidationRule.validate(getCaseDocumentWithReferenceData(ProsecutionCaseSubject.prosecutionCaseSubject().withProsecutingAuthority(OU_CODE).build()), referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenOUCodeIsInvalid() {
        final String INVALID_OU_CODE = "invalid_ou_code";

        final Optional<Problem> actualProblem = prosecutorValidationRule.validate(getCaseDocumentWithReferenceData(ProsecutionCaseSubject.prosecutionCaseSubject().withProsecutingAuthority(INVALID_OU_CODE).build()), referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(PROSECUTOR_OUCODE_NOT_RECOGNISED, FieldName.OU_CODE.getValue(), INVALID_OU_CODE);

        assertThat(actualProblem.get(), is(expectedProblem));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final ProsecutionCaseSubject prosecutionCaseSubject) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, CourtApplicationSubject.courtApplicationSubject().build(), prosecutionCaseSubject, null, null, null);
    }

    private ProsecutorsReferenceData getProsecutionReferenceData() {
        return ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(UUID.randomUUID())
                .build();
    }
}
