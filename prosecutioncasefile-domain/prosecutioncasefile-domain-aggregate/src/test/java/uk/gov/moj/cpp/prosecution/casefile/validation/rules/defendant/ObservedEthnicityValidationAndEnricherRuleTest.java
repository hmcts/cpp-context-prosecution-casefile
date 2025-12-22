package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_OBSERVED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_OBSERVED_ETHNICITY;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ObservedEthnicityValidationAndEnricherRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenNullObservedEthnicity() {
        when(defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getObservedEthnicity()).thenReturn(null);
        final Optional<Problem> optionalProblem = new ObservedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenObservedEthnicityMatches() {
        final Integer ethnicityCode = 1;
        final ObservedEthnicityReferenceData observedEthnicityReferenceData = new ObservedEthnicityReferenceData(ethnicityCode.toString(),
                "White - North European",
                randomUUID(), 1,
                now().minusMonths(2).toString(),
                now().plusMonths(1).toString());

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setObservedEthnicityReferenceData(asList(observedEthnicityReferenceData));

        when(defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getObservedEthnicity()).thenReturn(ethnicityCode);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(referenceDataVO);

        final Optional<Problem> optionalProblem = new ObservedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenObservedEthnicityDoesNotMatch() {
        final Integer ethnicityCode = 1;
        final ObservedEthnicityReferenceData observedEthnicityReferenceData = new ObservedEthnicityReferenceData("2",
                "White - South European",
                randomUUID(), 1,
                now().minusMonths(2).toString(),
                now().plusMonths(1).toString());

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setObservedEthnicityReferenceData(asList(observedEthnicityReferenceData));

        when(defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getObservedEthnicity()).thenReturn(ethnicityCode);
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(referenceDataQueryService.retrieveObservedEthnicity()).thenReturn(asList(observedEthnicityReferenceData));

        final Optional<Problem> optionalProblem = new ObservedEthnicityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_OBSERVED_ETHNICITY_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_OBSERVED_ETHNICITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(ethnicityCode.toString()));
    }

}
