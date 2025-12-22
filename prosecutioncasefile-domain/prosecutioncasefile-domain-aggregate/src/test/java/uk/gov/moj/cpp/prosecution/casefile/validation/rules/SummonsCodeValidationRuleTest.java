package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.SUMMONS_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_SUMMONS_CODE;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsCodeValidationRuleTest {

    public static final String SUMMONS_CODE = "A";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenInitiationCodeNotS() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn("C");
        final Optional<Problem> optionalProblem = new SummonsCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenSummonsCodeIsValid() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode()).thenReturn(SUMMONS_CODE);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn("S");
        when(referenceDataQueryService.retrieveSummonsCodes()).thenReturn((Arrays.asList(new SummonsCodeReferenceData(null, null, SUMMONS_CODE, null, null, null))));

        final Optional<Problem> optionalProblem = new SummonsCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenSummonsCodeIsInvalid() {
        final String invalidSummonsCode = "X";
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode()).thenReturn(invalidSummonsCode);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn("S");
        when(referenceDataQueryService.retrieveSummonsCodes()).thenReturn((Arrays.asList(new SummonsCodeReferenceData(null, null, SUMMONS_CODE, null, null, null))));

        final Optional<Problem> optionalProblem = new SummonsCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(SUMMONS_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CASE_SUMMONS_CODE.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidSummonsCode));
    }

    @Test
    public void shouldReturnProblemWithEmptyStringValueWhenSummonsCodeIsNull() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode()).thenReturn(null);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn("S");

        final Optional<Problem> optionalProblem = new SummonsCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(SUMMONS_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CASE_SUMMONS_CODE.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(""));
    }

    @Test
    public void shouldReturnProblemWhenSummonsCodeIsEmpty() {
        final String invalidSummonsCode = "";
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode()).thenReturn(invalidSummonsCode);
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()).thenReturn("S");
        when(referenceDataQueryService.retrieveSummonsCodes()).thenReturn((Arrays.asList(new SummonsCodeReferenceData(null, null, SUMMONS_CODE, null, null, null))));

        final Optional<Problem> optionalProblem = new SummonsCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(SUMMONS_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CASE_SUMMONS_CODE.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidSummonsCode));
    }
}