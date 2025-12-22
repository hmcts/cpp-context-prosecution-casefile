package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory.APPLICATIONS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.APPLICATION_ID_NOT_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.COURT_APPLICATION_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class ApplicationValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService context) {
        if (!APPLICATIONS.toString().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentCategory())) {
            return VALID;
        }
        return validateApplicationId(caseDocumentWithReferenceData);
    }

    private ValidationResult validateApplicationId(final CaseDocumentWithReferenceData caseDocumentWithReferenceData) {
        return (caseDocumentWithReferenceData.getCourtApplicationSubject() != null && !caseDocumentWithReferenceData.isHasApplication()) ? newValidationResult(of(newProblem(APPLICATION_ID_NOT_FOUND, COURT_APPLICATION_ID.getValue(), caseDocumentWithReferenceData.getCourtApplicationSubject().getCourtApplicationId()))) : VALID;
    }
}
