package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.NON_CIVIL_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MojOffences;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;
import java.util.UUID;

import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class CivilCaseOffencesValidationRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData prosecutionWithReferenceData;

    @InjectMocks
    private CivilCaseOffencesValidationRule underTest;

    @Test
    public void shouldReturnValidWhenNotCivilCase() {
        when(prosecutionWithReferenceData.getCaseDetails().getFeeStatus()).thenReturn(null);

        ValidationResult validationResult = underTest.validate(prosecutionWithReferenceData, referenceDataQueryService);

        assertThat(validationResult, is(ValidationResult.VALID));
    }

    @Test
    public void shouldReturnValidWhenOffencesAreOfCivilType() {
        UUID offenceId = UUID.randomUUID();
        String offenceCode = "someOffenceCode";

        when(prosecutionWithReferenceData.getCaseDetails().getFeeStatus()).thenReturn("someFeeStatus");
        when(prosecutionWithReferenceData.getDefendant().getOffences())
                .thenReturn(singletonList(Offence.offence().withOffenceId(offenceId).withOffenceCode(offenceCode).build()));
        when(referenceDataQueryService.retrieveOffencesByType("VP"))
                .thenReturn(singletonList(MojOffences.mojOffences().withCjsOffenceCode(offenceCode).build()));

        ValidationResult validationResult = underTest.validate(prosecutionWithReferenceData, referenceDataQueryService);

        assertThat(validationResult, is(ValidationResult.VALID));
    }

    @Test
    public void shouldReturnProblemWhenOffencesAreNotCivilType() {
        UUID offenceId = UUID.randomUUID();
        String offenceCode = "someNonCivilOffenceCode";

        when(prosecutionWithReferenceData.getCaseDetails().getFeeStatus()).thenReturn("someFeeStatus");
        when(prosecutionWithReferenceData.getDefendant().getOffences())
                .thenReturn(singletonList(Offence.offence().withOffenceId(offenceId).withOffenceCode(offenceCode).build()));
        when(referenceDataQueryService.retrieveOffencesByType("VP"))
                .thenReturn(singletonList(MojOffences.mojOffences().withCjsOffenceCode("someCivilOffenceCode").build()));

        Optional<Problem> problem = underTest.validate(prosecutionWithReferenceData, referenceDataQueryService).problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(NON_CIVIL_OFFENCES.name()));
        assertThat(problem.get().getValues().get(0).getValue(), is(offenceCode));
        assertThat(problem.get().getValues().get(0).getId(), is(offenceId.toString()));
        assertThat(problem.get().getValues().get(0).getKey(), is(OFFENCE_CODE.getValue()));
    }

}