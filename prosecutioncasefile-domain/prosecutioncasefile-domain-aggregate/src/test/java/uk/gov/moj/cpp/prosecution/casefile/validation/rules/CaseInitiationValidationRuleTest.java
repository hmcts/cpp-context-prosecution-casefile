package uk.gov.moj.cpp.prosecution.casefile.validation.rules;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_INITIATION_CODE_INVALID;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseInitiationValidationRuleTest {

    public static final String CHARGE_INITIATION_CODE = "C";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCaseInitiationCodeIsValid() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn(CHARGE_INITIATION_CODE);
        when(prosecutionWithReferenceData.getReferenceDataVO().getInitiationTypes()).thenReturn(Arrays.asList("C", "J"));

        final ValidationResult optionalProblem = new CaseInitiationValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService);

        assertThat(optionalProblem.isValid(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenCaseInitiationCodeIsInvalid() {
        final String invalidInitiationCode = "X";
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn(invalidInitiationCode);

        final Optional<Problem> optionalProblem = new CaseInitiationValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(CASE_INITIATION_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("initiationCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidInitiationCode));
    }

    @Test
    public void shouldReturnProblemWithEmptyStringValueWhenCaseInitiationCodeIsNull() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn(null);

        final Optional<Problem> optionalProblem = new CaseInitiationValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(CASE_INITIATION_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("initiationCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(""));
    }
}