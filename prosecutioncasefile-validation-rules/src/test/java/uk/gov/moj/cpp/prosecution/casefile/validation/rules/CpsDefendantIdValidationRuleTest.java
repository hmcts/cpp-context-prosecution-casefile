package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent.respondent;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication.submitApplication;

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
public class CpsDefendantIdValidationRuleTest {

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @InjectMocks
    private final CpsDefendantIdValidationRule cpsDefendantIdValidationRule = new CpsDefendantIdValidationRule();


    @BeforeEach
    public void setup() throws IllegalAccessException {
        List<SubmitApplicationValidationRule> validationRules = singletonList(cpsDefendantIdValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotRaisedErrorCode_whenCpsDefendantIdIsGiven_andIsDefendantMatchIsTrue() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(singletonList(respondent()
                                .withCpsDefendantId(randomUUID().toString())
                                .withIsDefendantMatched(true)
                                .build()))
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotRaisedErrorCode_whenCpsDefendantIsIsEmpty_andIsDefendantMatchIsTrue() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(singletonList(respondent()
                                .withIsDefendantMatched(true)
                                .build()))
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldRaisedErrorCode_whencpsDefendantIdIsGiven_andIsDefendantMatchIsFalse() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(singletonList(respondent()
                                .withCpsDefendantId(randomUUID().toString())
                                .withIsDefendantMatched(false)
                                .build()))
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.DEFENDANT_DETAILS_NOT_FOUND));
    }

}