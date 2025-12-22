package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ON_CP;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2Test {

    private static final String DEFENDANT_LEVEL = "Defendant level";
    private static final String CASE_LEVEL = "Case level";
    private static final String APPLICATIONS = "Applications";

    private static final String DEFENDANT_ID = "6e70f5da-e513-498c-9d97-8eecb0883088";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2 ccDocumentDefendantLevelValidationRuleForPendingV2 = new CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2();

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsCaseLevel() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(CASE_LEVEL);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantIsNotOrganisation() {

        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsApplications() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(APPLICATIONS);

        final Optional<Problem> optionalProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantIsDuplicated() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID).build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().addAll(getDuplicatedDefendants());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIsInvalid() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID)
                        .withOrganisationName("org1").build())
                .build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withOrganisationName("org").build());

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(1));
        assertThat(actualProblem.get(0).getCode(), is(DEFENDANT_ON_CP.toString()));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIsValid() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails().withCpsDefendantId(DEFENDANT_ID)
                        .withOrganisationName("org").build())
                .build();
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().add(defendant().withOrganisationName("org").build());

        final List<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems();

        assertThat(actualProblem.size(), is(0));
    }

    @Test
    public void ShouldReturnValidWhenOrganisationMatchesForMultipleDefendants(){
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject().
                withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails()
                        .withOrganisationName("org")
                        .withCpsDefendantId(DEFENDANT_ID).build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, getDefendantsWithIndividual());

        final Optional<Problem> actualProblem = ccDocumentDefendantLevelValidationRuleForPendingV2.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem.isPresent(), is(false));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(String documentCategory) {
        return getCaseDocumentWithReferenceData(DefendantSubject.defendantSubject().build(), documentCategory);
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject, String documentCategory) {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = new  CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, new HashMap<>());
        caseDocumentWithReferenceData.setDocumentCategory(documentCategory);
        return caseDocumentWithReferenceData;
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject, final List<Defendant> defendants) {
        final Map<String, UUID> validIds = new HashMap<>();
        validIds.put(DEFENDANT_ID, fromString(defendants.get(0).getId()));
        return new CaseDocumentWithReferenceData(randomUUID(), false, defendants, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, validIds);
    }

    private List<Defendant> getDuplicatedDefendants() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .withOrganisationName("Org")
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .withOrganisationName("Org")
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
    }

    private List<Defendant> getDefendantsWithIndividual() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mr")
                                        .withFirstName("John")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withPersonalInformation(PersonalInformation.personalInformation()
                                        .withTitle("Mis")
                                        .withFirstName("Suzan")
                                        .withGivenName2("Doe")
                                        .withGivenName2("Doe 2")
                                        .withLastName("Doe last")
                                        .build())
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
                                        .build())
                                .build())
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
    }
}
