package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATE_DEFENDANT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsOrganisationDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CCDocumentDefendantLevelWithOrganisationValidationRuleV2Test {

    private static final String DEFENDANT_LEVEL = "Defendant level";
    private static final String CASE_LEVEL = "Case level";
    private static final String APPLICATIONS = "Applications";

    private static final String DEFENDANT_ID = "6e70f5da-e513-498c-9d97-8eecb0883088";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private CCDocumentDefendantLevelWithOrganisationValidationRuleV2 defendantLevelWithOrganisationValidationRule = new CCDocumentDefendantLevelWithOrganisationValidationRuleV2();

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsCaseLevel() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(CASE_LEVEL);

        final Optional<Problem> optionalProblem = defendantLevelWithOrganisationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantIsNotOrganisation() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);

        final Optional<Problem> optionalProblem = defendantLevelWithOrganisationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsApplications() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(APPLICATIONS);

        final Optional<Problem> optionalProblem = defendantLevelWithOrganisationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDefendantLevelDoesNotExist() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithoutDefendantLevel(DEFENDANT_LEVEL);

        final Optional<Problem> optionalProblem = defendantLevelWithOrganisationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDefendantIsDuplicated() {
        DefendantSubject defendantSubject = DefendantSubject.defendantSubject()
                .withCpsOrganisationDefendantDetails(CpsOrganisationDefendantDetails.cpsOrganisationDefendantDetails()
                        .withOrganisationName("org")
                        .withCpsDefendantId(DEFENDANT_ID)
                        .build())
                .build();

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(defendantSubject, DEFENDANT_LEVEL);
        caseDocumentWithReferenceData.getDefendants().addAll(getDuplicatedDefendants());

        final Optional<Problem> actualProblem = defendantLevelWithOrganisationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(DUPLICATE_DEFENDANT, "prosecutorDefendantId", DEFENDANT_ID);

        assertThat(actualProblem.get(), is(expectedProblem));
    }


    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(String documentCategory) {
        return getCaseDocumentWithReferenceData(DefendantSubject.defendantSubject().build(), documentCategory);
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithoutDefendantLevel(String documentCategory) {
        return getCaseDocumentWithReferenceData(null, documentCategory);
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final DefendantSubject defendantSubject, String documentCategory) {
        CaseDocumentWithReferenceData caseDocumentWithReferenceData =  new CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, null, ProsecutionCaseSubject.prosecutionCaseSubject().withDefendantSubject(defendantSubject).build(), null, null, new HashMap<>());
        caseDocumentWithReferenceData.setDocumentCategory(documentCategory);
        return caseDocumentWithReferenceData;
    }

    private List<Defendant> getDuplicatedDefendants() {
        return Arrays.asList(defendant()
                        .withId(randomUUID().toString())
                        .withOrganisationName("org")
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .withOrganisationName("org")
                        .build(),
                defendant()
                        .withId(randomUUID().toString())
                        .build());
    }
}
