package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_CRO_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CRO_NUMBER;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CroNumberSpiValidationRuleTest {

    private static final String TEST_INVALID_CRO_NUMBER = "1234111336556";
    private static final String TEST_VALID_CRO_NUMBER_ONE = "12345.123455";
    private static final String TEST_VALID_CRO_NUMBER_TWO = "1234AB.11";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCRONumberNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(null));

        final Optional<Problem> optionalProblem = new CroNumberSpiValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyErrorListWhenValidCRONumberOne() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_VALID_CRO_NUMBER_ONE));

        final Optional<Problem> optionalProblemOne = new CroNumberSpiValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblemOne.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyErrorListWhenValidCRONumberTwo() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_VALID_CRO_NUMBER_TWO));

        final Optional<Problem> optionalProblemTwo = new CroNumberSpiValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblemTwo.isPresent(), is(false));
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void shouldReturnProblemWhenInvalidCRONumber() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getDefendant(TEST_INVALID_CRO_NUMBER));

        final Optional<Problem> optionalProblem = new CroNumberSpiValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(INVALID_CRO_NUMBER.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CRO_NUMBER.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(TEST_INVALID_CRO_NUMBER));
    }

    private Defendant getDefendant(String croNumber) {
        if(Objects.nonNull(croNumber)) {
            return defendant().withCroNumber(croNumber).build();
        }else{
            return defendant().build();
        }
    }
}
