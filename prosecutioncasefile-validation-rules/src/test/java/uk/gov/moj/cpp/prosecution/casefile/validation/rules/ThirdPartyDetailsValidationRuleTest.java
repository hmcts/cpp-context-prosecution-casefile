package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyOrganisation;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyOrganisationWithoutNameAndAddress;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyPerson;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyPersonWithoutAddress;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyPersonWithoutName;
import static uk.gov.moj.cpp.prosecution.casefile.validation.TestUtils.buildThirdPartyPersonWithoutNameAndAddress;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.THIRD_PARTY_DETAILS_REQUIRED;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ThirdParty;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ThirdPartyDetailsValidationRuleTest {

    @InjectMocks
    private final ThirdPartyDetailsValidationRule thirdPartyDetailsValidationRule = new ThirdPartyDetailsValidationRule();
    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;
    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() {
        List<SubmitApplicationValidationRule> validationRules = singletonList(thirdPartyDetailsValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenThirdPartiesPersonValidAddressAndFullName() {
        final ThirdParty thirdParty = buildThirdPartyPerson();
        List<Optional<ValidationError>> validationError = validateThirdParty(singletonList(thirdParty));

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnErrorWhenThirdPartiesOrganisationValidAddressAndName() {
        final ThirdParty thirdParty = buildThirdPartyOrganisation();
        List<Optional<ValidationError>> validationError = validateThirdParty(singletonList(thirdParty));

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidationErrorWhenThirdPartiesPersonValidAddressAndInvalidFullName() {
        final ThirdParty thirdParty = buildThirdPartyPersonWithoutName();
        List<Optional<ValidationError>> validationError = validateThirdParty(singletonList(thirdParty));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(THIRD_PARTY_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenThirdPartiesPersonInvalidAddressAndValidFullName() {
        final ThirdParty thirdParty = buildThirdPartyPerson();
        final ThirdParty thirdParty2 = buildThirdPartyPersonWithoutAddress();

        List<Optional<ValidationError>> validationError = validateThirdParty(asList(thirdParty, thirdParty2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(THIRD_PARTY_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenThirdPartiesPersonInvalidAddressAndFullName() {
        final ThirdParty thirdParty = buildThirdPartyPerson();
        final ThirdParty thirdParty2 = buildThirdPartyPersonWithoutNameAndAddress();

        List<Optional<ValidationError>> validationError = validateThirdParty(asList(thirdParty, thirdParty2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(THIRD_PARTY_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenThirdPartiesOrganisationInvalidAddressAndName() {
        final ThirdParty thirdParty = buildThirdPartyOrganisation();
        final ThirdParty thirdParty2 = buildThirdPartyOrganisationWithoutNameAndAddress();

        List<Optional<ValidationError>> validationError = validateThirdParty(asList(thirdParty, thirdParty2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(THIRD_PARTY_DETAILS_REQUIRED));
    }

    private List<Optional<ValidationError>> validateThirdParty(final List<ThirdParty> thirdParties) {
        SubmitApplication submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withThirdParties(thirdParties).build())
                .withBoxHearingRequest(BoxHearingRequest.boxHearingRequest().build())
                .build();

        return submitApplicationValidator.validate(submitApplication,null);
    }
}
