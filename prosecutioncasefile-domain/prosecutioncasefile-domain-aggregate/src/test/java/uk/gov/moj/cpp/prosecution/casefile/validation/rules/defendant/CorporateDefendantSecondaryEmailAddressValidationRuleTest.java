package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CORPORATE_DEFENDANT_SECONDARY_EMAIL_ADDRESS;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CorporateDefendantSecondaryEmailAddressValidationRuleTest {

    private static final String INVALID_EMAIL = "invalid@email";
    private static final String VALID_EMAIL = "valid@email.com";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenSecondaryEmailAddressNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithNoSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenValidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithValidSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenInvalidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getCorporateDefendantWithInvalidSecondaryEmail());

        final Optional<Problem> optionalProblem = new CorporateDefendantSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(CORPORATE_DEFENDANT_SECONDARY_EMAIL_ADDRESS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_EMAIL));
    }

    private Defendant getCorporateDefendantWithNoSecondaryEmail() {
        return defendant().build();
    }

    private Defendant getCorporateDefendantWithValidSecondaryEmail() {
        return defendant().withEmailAddress2(VALID_EMAIL).build();
    }

    private Defendant getCorporateDefendantWithInvalidSecondaryEmail() {
        return defendant().withEmailAddress2(INVALID_EMAIL).build();
    }

}
