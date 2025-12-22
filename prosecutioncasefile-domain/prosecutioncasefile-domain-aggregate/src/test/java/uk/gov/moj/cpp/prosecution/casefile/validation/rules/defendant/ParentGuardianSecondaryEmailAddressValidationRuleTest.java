package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS;

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
public class ParentGuardianSecondaryEmailAddressValidationRuleTest {

    private static final String INVALID_EMAIL = "invalid@email";
    private static final String VALID_EMAIL = "valid@email.com";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenSecondaryEmailAddressNotPresent() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithNoSecondaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenValidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithValidSecondaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenInvalidSecondaryEmailAddress() {
        when(defendantWithReferenceData.getDefendant()).thenReturn(getIndividualWithParentGuardianWithInvalidSecondaryEmail());

        final Optional<Problem> optionalProblem = new ParentGuardianSecondaryEmailAddressValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(INVALID_EMAIL));
    }

    private Defendant getIndividualWithParentGuardianWithNoSecondaryEmail() {
        return defendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private Defendant getIndividualWithParentGuardianWithValidSecondaryEmail() {
        return defendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().withSecondaryEmail(VALID_EMAIL).build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private Defendant getIndividualWithParentGuardianWithInvalidSecondaryEmail() {
        return defendant()
                .withIndividual(individual()
                        .withParentGuardianInformation(parentGuardianInformation()
                                .withPersonalInformation(personalInformation().withContactDetails(contactDetails().withSecondaryEmail(INVALID_EMAIL).build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

}
