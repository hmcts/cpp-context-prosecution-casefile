package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_NATIONALITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_NATIONALITY;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NationalityValidationAndEnricherRuleTest {

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCorpDefendant() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(null);
        final Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenNationalityNotProvided() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                        .withNationality(null)
                        .build())
                .build());
        final Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenNationalityIsEmpty() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                        .withNationality("")
                        .build())
                .build());
        final Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnViolationWhenNationalityDoesNotMatch() {
        final String countryCode = "GBR";
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                        .withNationality(countryCode)
                        .build())
                .build());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(referenceDataQueryService.retrieveCountryNationality()).thenReturn(emptyList());

        final Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_NATIONALITY_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(DEFENDANT_NATIONALITY.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(countryCode));
    }

    @Test
    public void shouldReturnEmptyListWhenNationalityIsValid() {
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                        .withNationality("GBR")
                        .build())
                .build());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(referenceDataQueryService.retrieveCountryNationality()).thenReturn(asList(
                new ReferenceDataCountryNationality(null, null, null, null, null, "GBR", "British", null, null, null)));

        Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

        //should use cached value when invoked second time
        optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
        verify(referenceDataQueryService, times(1)).retrieveCountryNationality();
    }

    @Test
    public void shouldReturnEmptyListWhenNationalityIsSuppliedAsCJSCode() {
        final String CJS_CODE = "126589";
        when(defendantWithReferenceData.getDefendant().getIndividual()).thenReturn(Individual.individual()
                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                        .withNationality(CJS_CODE.toString())
                        .build())
                .build());
        when(defendantWithReferenceData.getReferenceDataVO()).thenReturn(new ReferenceDataVO());
        when(referenceDataQueryService.retrieveCountryNationality()).thenReturn(asList(
                new ReferenceDataCountryNationality(CJS_CODE, null, null, null, null, "GBR", "British", null, null, null)));

        Optional<Problem> optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));

        //should use cached value when invoked second time
        optionalProblem = new NationalityValidationAndEnricherRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
        verify(referenceDataQueryService, times(1)).retrieveCountryNationality();
    }

}