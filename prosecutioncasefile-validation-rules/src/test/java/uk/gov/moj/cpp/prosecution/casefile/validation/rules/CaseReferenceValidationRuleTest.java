package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication.courtApplication;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.CASE_URN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.utils.TestUtils.validSingleCourtApplicationCase;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseReferenceValidationRuleTest {

    private SubmitApplication submitApplication;

    @Mock
    private CaseDetailsService caseDetailsService;

    @Mock
    private Instance<SubmitApplicationValidationRule> validatorRules;

    @InjectMocks
    private final CaseReferenceValidationRule caseReferenceValidationRule = new CaseReferenceValidationRule();

    @InjectMocks
    private SubmitApplicationValidator submitApplicationValidator;

    @BeforeEach
    public void setup() throws IllegalAccessException {

        CourtApplication courtApplication = courtApplication()
                .withCourtApplicationCases(validSingleCourtApplicationCase())
                .build();
        submitApplication = SubmitApplication.submitApplication().withCourtApplication(courtApplication).build();
        List<SubmitApplicationValidationRule> validationRules = singletonList(caseReferenceValidationRule);
        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    @Test
    public void shouldNotReturnErrorWhenCourtCasesIsValid() {
        ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(CASE_ID)
                .withDefendants(asList(
                        Defendant.defendant().withMasterDefendantId(UUID.randomUUID()).build(),
                        Defendant.defendant().withMasterDefendantId(UUID.randomUUID()).build()
                ))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(CASE_URN)
                        .build())
                .build();

        AdditionalInformation additionalInformation = new AdditionalInformation(singletonList(prosecutionCase),null, null);
        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication,additionalInformation);

        assertThat(validationError.get(0).isPresent(), is(false));
    }

    @Test
    public void shouldReturnErrorWhenProsecutionCasesNotFound() {
        AdditionalInformation additionalInformation = new AdditionalInformation(null,null, null);

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, additionalInformation);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.CASE_NOT_FOUND));
    }

    @Test
    public void shouldReturnErrorWhenCourtCasesNotFound() {

        final List<Optional<ValidationError>> validationError = submitApplicationValidator.validate(submitApplication, null);

        assertThat(validationError.isEmpty(), is(false));
        assertThat(validationError.get(0).isPresent(), is(true));
        assertThat(validationError.get(0).get(), is(ValidationError.CASE_NOT_FOUND));
    }
}