package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_IS_IN_SESSION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.Problems;

public class SjpCaseInSessionValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData input, ReferenceDataQueryService context) {
        return newValidationResult(of(input).filter(CaseDocumentWithReferenceData::isCaseAssigned).map(prosecutionCase -> Problems.newProblem(CASE_IS_IN_SESSION,
                new ProblemValue(null, "caseInSession", "true"))));
    }
}
