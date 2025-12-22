package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.POLICE_FORCE_CODE_INVALID;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PoliceForceCodeValidationRuleTest {

    public static final String POLICE_FORCE_CODE = "33";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ProsecutionWithReferenceData prosecutionWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCasePoliceForceCodeIsValid() {

        PoliceForceReferenceData policeForceReferenceData = new PoliceForceReferenceData(null,null, POLICE_FORCE_CODE, null, null, null, null, null, null);
        List<PoliceForceReferenceData>  policeForceReferenceDataList = new ArrayList<>();
        policeForceReferenceDataList.add(policeForceReferenceData);

        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceForceCode()).thenReturn(POLICE_FORCE_CODE);
        when(prosecutionWithReferenceData.getReferenceDataVO().getPoliceForceReferenceData()).thenReturn(policeForceReferenceDataList);
        when(referenceDataQueryService.retrievePoliceForceCode()).thenReturn(policeForceReferenceDataList);

        final ValidationResult optionalProblem = new PoliceForceCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService);

        assertThat(optionalProblem.isValid(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenCasePoliceForceCodeIsInvalid() {
        final String invalidPoliceForceCode = "X";
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceForceCode()).thenReturn(invalidPoliceForceCode);

        final Optional<Problem> optionalProblem = new PoliceForceCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(POLICE_FORCE_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("policeForceCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidPoliceForceCode));
    }

    @Test
    public void shouldReturnProblemWithEmptyStringValueWhenCasePoliceForceCodeIsNull() {
        when(prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceForceCode()).thenReturn(null);

        final ValidationResult optionalProblem = new PoliceForceCodeValidationRule().validate(prosecutionWithReferenceData, referenceDataQueryService);

        assertThat(optionalProblem.isValid(), is(true));
    }
}