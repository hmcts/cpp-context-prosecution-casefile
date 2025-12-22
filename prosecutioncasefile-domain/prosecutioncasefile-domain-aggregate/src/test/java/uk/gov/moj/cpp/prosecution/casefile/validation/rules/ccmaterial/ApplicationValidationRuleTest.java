package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.APPLICATION_ID_NOT_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.COURT_APPLICATION_ID;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationValidationRuleTest {

    private static final String DOCUMENT_CATEGORY_DEFENDANT_LEVEL = "DEFENDANT_LEVEL";
    private static final String DOCUMENT_CATEGORY_APPLICATIONS_LEVEL = "APPLICATIONS";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    private ApplicationValidationRule applicationValidationRule = new ApplicationValidationRule();

    private UUID courtApplicationId = randomUUID();

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsNotApplications() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(null);
        caseDocumentWithReferenceData.setDocumentCategory(DOCUMENT_CATEGORY_DEFENDANT_LEVEL);

        final Optional<Problem> optionalProblem = applicationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnValidWhenDocumentCategoryIsApplicationsAndCpsHasApplication() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(randomUUID());
        caseDocumentWithReferenceData.setHasApplication(true);
        caseDocumentWithReferenceData.setDocumentCategory(DOCUMENT_CATEGORY_APPLICATIONS_LEVEL);

        final Optional<Problem> optionalProblem = applicationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnProblemWhenDocumentCategoryIsApplicationsAndCpsHasNoApplication() {

        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = getCaseDocumentWithReferenceData(courtApplicationId);
        caseDocumentWithReferenceData.setHasApplication(false);
        caseDocumentWithReferenceData.setDocumentCategory(DOCUMENT_CATEGORY_APPLICATIONS_LEVEL);

        final Optional<Problem> optionalProblem = applicationValidationRule.validate(caseDocumentWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(APPLICATION_ID_NOT_FOUND, COURT_APPLICATION_ID.getValue(), courtApplicationId.toString());

        assertThat(optionalProblem.get(), is(expectedProblem));
    }

    private static CaseDocumentWithReferenceData getCaseDocumentWithReferenceData(final UUID courtApplicationId) {
        return new CaseDocumentWithReferenceData(randomUUID(), false, null, "documentType", false, false, CourtApplicationSubject.courtApplicationSubject().withCourtApplicationId(courtApplicationId).build(), ProsecutionCaseSubject.prosecutionCaseSubject().build(), null, null, null);
    }
}
