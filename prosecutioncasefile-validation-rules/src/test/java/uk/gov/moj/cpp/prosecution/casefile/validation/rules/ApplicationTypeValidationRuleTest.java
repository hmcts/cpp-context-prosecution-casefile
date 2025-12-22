package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.validCourtApplicationType;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
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
public class ApplicationTypeValidationRuleTest {

    private SubmitApplication submitApplication;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private final ApplicationTypeValidationRule applicationTypeValidationRule = new ApplicationTypeValidationRule();

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        List<SubmitApplicationValidationRule> validationRules = singletonList(applicationTypeValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }


    @Test
    public void shouldNotReturnErrorWhenCourtApplicationTypesIsValid() {

        final String applicationType = "CP96504";
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationType(validCourtApplicationType(applicationType))
                .build();

        final AdditionalInformation additionalInformation = new AdditionalInformation(null, asList(buildCourtApplicationType(applicationType), buildCourtApplicationType("invalid-code")), null);

        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();
        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    private CourtApplicationType buildCourtApplicationType(final String applicationType) {
        return CourtApplicationType.courtApplicationType()
                .withCode(applicationType).build();
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationTypesNotFoundInRefData() {

        final String applicationType = "CP96504";
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationType(validCourtApplicationType(applicationType))
                .build();

        final AdditionalInformation additionalInformation = new AdditionalInformation(null, asList(buildCourtApplicationType("invalid-code2"), buildCourtApplicationType("invalid-code1")), null);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();
        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.APPLICATION_TYPE_NOT_FOUND));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationTypesIsNull() {

        final String applicationType = "CP96504";
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .build();

        final AdditionalInformation additionalInformation = new AdditionalInformation(null, asList(buildCourtApplicationType(applicationType), buildCourtApplicationType("invalid-code1")), null);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();
        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.APPLICATION_TYPE_NOT_FOUND));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationTypesCodeIsNull() {

        final String applicationType = "CP96504";
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationType(validCourtApplicationType(null))
                .build();

        final AdditionalInformation additionalInformation = new AdditionalInformation(null, asList(buildCourtApplicationType(applicationType), buildCourtApplicationType("invalid-code1")), null);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();
        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.APPLICATION_TYPE_NOT_FOUND));
    }
}