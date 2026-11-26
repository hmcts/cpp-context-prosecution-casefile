package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
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
public class DefendantMatchValidationRuleTest {

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @InjectMocks
    private final DefendantMatchValidationRule defendantMatchValidationRule = new DefendantMatchValidationRule();


    @BeforeEach
    public void setup() throws IllegalAccessException {
        List<SubmitApplicationValidationRule> validationRules = singletonList(defendantMatchValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotRaisedErrorCode_whenAllRespondentsHasOneOrNoneMatching() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(asList(
                                respondent()
                                        .withCpsDefendantId(randomUUID().toString())
                                        .withIsDefendantMatched(true)
                                        .withIsMultipleDefendantMatched(false)
                                        .build(),
                                respondent()
                                        .withAsn(randomUUID().toString())
                                        .withIsDefendantMatched(false)
                                        .withIsMultipleDefendantMatched(false)
                                        .build()
                        ))
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldRaisedErrorCode_WhenRespondentHasMoreThenOneMatching() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(asList(
                                respondent()
                                        .withCpsDefendantId(randomUUID().toString())
                                        .withIsDefendantMatched(true)
                                        .withIsMultipleDefendantMatched(false)
                                        .build(),
                                respondent()
                                        .withAsn(randomUUID().toString())
                                        .withIsDefendantMatched(true)
                                        .withIsMultipleDefendantMatched(true)
                                        .build()
                        ))
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.DEFENDANT_DETAILS_NOT_FOUND));
    }

    @Test
    public void shouldNotRaisedErrorCode_whenNoRespondents_isEmpty() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(emptyList())
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotRaisedErrorCode_whenNoRespondents_isNull() {
        final SubmitApplication submitApplication = submitApplication()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(null)
                        .build())
                .build();

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,null);

        assertThat(validationError.get(0).isPresent(), is(false));
    }
}