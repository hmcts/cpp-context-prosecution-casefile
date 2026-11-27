package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.time.LocalDateTime;
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
public class ApplicationDueDateValidatorTest {

    private SubmitApplication submitApplication;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private final ApplicationDueDateValidationRule applicationDueDateValidationRule = new ApplicationDueDateValidationRule();

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() {
        List<SubmitApplicationValidationRule> validationRules = singletonList(applicationDueDateValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenApplicationDueDateIsValid() {
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(CourtApplication.courtApplication()
                .build()).withBoxHearingRequest(BoxHearingRequest.boxHearingRequest()
                .withApplicationDueDate(LocalDateTime.now().plusDays(1L).toLocalDate()).build()).build();

        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnErrorWhenApplicationDueDateNotValid() {
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(CourtApplication.courtApplication()
                .build()).withBoxHearingRequest(BoxHearingRequest.boxHearingRequest()
                .withApplicationDueDate(LocalDateTime.now().minusDays(1L).toLocalDate()).build()).build();

        List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,null);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.APPLICATION_DUE_DATE_INVALID));
    }

}
