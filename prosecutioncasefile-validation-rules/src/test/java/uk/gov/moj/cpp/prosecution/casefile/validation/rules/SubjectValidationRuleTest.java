package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.OffenceActiveOrder.COURT_ORDER;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.SUBJECT_INVALID;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Applicant;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
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
public class SubjectValidationRuleTest {

    public static final String CODE = "CD390";
    private SubmitApplication submitApplication;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    private final SubjectValidationRule subjectValidationRule = new SubjectValidationRule();

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        List<SubmitApplicationValidationRule> validationRules = singletonList(subjectValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenCourtApplicationHasMoreThenOneSubjectFlagTruePartyAndValidCourtApplicationType() {
        final CourtApplication courtApplication = getCourtApplication(false,
                false,true,true);
        final AdditionalInformation additionalInformation = getAdditionalInformation(COURT_ORDER);
        submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldNotReturnErrorWhenCourtApplicationHasMoreThenOneSubjectFlagTrueParty() {
        final CourtApplication courtApplication = getCourtApplication(false,
                false,true,true);
        final AdditionalInformation additionalInformation = getAdditionalInformation(COURT_ORDER);
        submitApplication = SubmitApplication.submitApplication()
                .withCourtApplication(courtApplication)
                .build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationApplicantIsRespondentAndCourtApplicationTypeContainsOffenceAndRespondentDefendantMatchedIsFalse() {
        final CourtApplication courtApplication = getCourtApplication(false,
                false,true,false);
        final AdditionalInformation additionalInformation = getAdditionalInformation(OFFENCE);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(SUBJECT_INVALID));
    }


    @Test
    public void shouldReturnErrorWhenCourtApplicationApplicantIsSubjectAndApplicationTypeContainsOffence() {
        final CourtApplication courtApplication = getCourtApplication(true,
                false,false,false);
        final AdditionalInformation additionalInformation = getAdditionalInformation(OFFENCE);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(SUBJECT_INVALID));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationHasMoreThenOneSubjectFlagTrueParty() {

        final CourtApplication courtApplication = getCourtApplication(true,
                false,true,false);

        final AdditionalInformation additionalInformation = getAdditionalInformation(OFFENCE);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(SUBJECT_INVALID));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationHasNoSubjectFlagTrueParty() {

        final CourtApplication courtApplication = getCourtApplication(false,
                false,false,true);

        final AdditionalInformation additionalInformation = getAdditionalInformation(OFFENCE);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.SUBJECT_REQUIRED));
    }

    @Test
    public void shouldReturnErrorWhenCourtApplicationHasMoreThanOneIsSubjectTrueFlag() {

        final CourtApplication courtApplication = getCourtApplication(false,
                true,true,true);
        final AdditionalInformation additionalInformation = getAdditionalInformation(OFFENCE);
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();

        List<Optional<ValidationError>> validationError  = submitApplicationValidator
                .validate(submitApplication, additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(SUBJECT_INVALID));
    }

    private CourtApplication getCourtApplication(final boolean applicantIsSubject,
                                                 final boolean respondentIsSubject1,
                                                 final boolean respondentIsSubject2,
                                                 final boolean respondentIsDefendantMatched) {
        return courtApplication()
                .withApplicant(Applicant.applicant()
                        .withIsSubject(applicantIsSubject)
                        .build())
                .withRespondents(asList(Respondent.respondent()
                                .withIsSubject(respondentIsSubject1)
                                .build(),
                        Respondent.respondent()
                                .withIsSubject(respondentIsSubject2)
                                .withIsDefendantMatched(respondentIsDefendantMatched)
                                .build()))
                .withCourtApplicationType(uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationType.courtApplicationType()
                        .withCode(CODE).build())
                .build();
    }

    private AdditionalInformation getAdditionalInformation(final OffenceActiveOrder offenceActiveOrder) {
        return new AdditionalInformation(emptyList(), asList(CourtApplicationType
                .courtApplicationType()
                .withOffenceActiveOrder(offenceActiveOrder)
                .withCode(CODE)
                .build()),null);
    }
}
