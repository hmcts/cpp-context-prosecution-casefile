package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.RESPONDENT_DETAILS_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentOrganisation;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentOrganisationWithoutNameAndAddress;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentPerson;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentPersonWithoutAddress;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentPersonWithoutName;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.buildRespondentPersonWithoutNameAndAddress;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
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
public class RespondentDetailsValidationRuleTest {

    @InjectMocks
    private final RespondentDetailsValidationRule respondentDetailsValidationRule = new RespondentDetailsValidationRule();
    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;
    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() {
        List<SubmitApplicationValidationRule> validationRules = singletonList(respondentDetailsValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenRespondentPersonValidAddressAndFullName() {
        final Respondent respondent = buildRespondentPerson();
        List<Optional<ValidationError>> validationError = validateRespondents(singletonList(respondent));

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnErrorWhenRespondentOrganisationValidAddressAndName() {
        final Respondent respondent = buildRespondentOrganisation();
        List<Optional<ValidationError>> validationError = validateRespondents(singletonList(respondent));

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidationErrorWhenRespondentPersonValidAddressAndInvalidFullName() {
        final Respondent respondent = buildRespondentPersonWithoutName();
        List<Optional<ValidationError>> validationError = validateRespondents(singletonList(respondent));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(RESPONDENT_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenRespondentPersonInvalidAddressAndValidFullName() {
        final Respondent respondent = buildRespondentPerson();
        final Respondent respondent2 = buildRespondentPersonWithoutAddress();

        List<Optional<ValidationError>> validationError = validateRespondents(asList(respondent, respondent2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(RESPONDENT_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenRespondentPersonInvalidAddressAndFullName() {
        final Respondent respondent = buildRespondentPerson();
        final Respondent respondent2 = buildRespondentPersonWithoutNameAndAddress();

        List<Optional<ValidationError>> validationError = validateRespondents(asList(respondent, respondent2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(RESPONDENT_DETAILS_REQUIRED));
    }

    @Test
    public void shouldReturnValidationErrorWhenRespondentOrganisationInvalidAddressAndName() {
        final Respondent respondent = buildRespondentOrganisation();
        final Respondent respondent2 = buildRespondentOrganisationWithoutNameAndAddress();

        List<Optional<ValidationError>> validationError = validateRespondents(asList(respondent, respondent2));

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(RESPONDENT_DETAILS_REQUIRED));
    }

    private List<Optional<ValidationError>> validateRespondents(final List<Respondent> respondents) {
        SubmitApplication submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(CourtApplication.courtApplication()
                        .withRespondents(respondents).build())
                .withBoxHearingRequest(BoxHearingRequest.boxHearingRequest().build())
                .build();

        return submitApplicationValidator.validate(submitApplication,null);
    }
}
