package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_ALREADY_EJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Optional;

public class CaseEjectValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData input, ReferenceDataQueryService context) {
        return newValidationResult(Optional.of(input).filter(CaseDocumentWithReferenceData::isCaseEjected)
                .map(e -> newProblem(CASE_ALREADY_EJECTED, "documentType", Optional.ofNullable(input.getDocumentType()).orElse(""))));
    }
}
