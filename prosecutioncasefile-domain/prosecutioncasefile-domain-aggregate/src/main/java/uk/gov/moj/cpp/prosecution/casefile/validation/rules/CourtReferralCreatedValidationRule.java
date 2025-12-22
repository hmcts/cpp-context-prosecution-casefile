package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.Problems;

import java.util.Optional;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.*;

public class CourtReferralCreatedValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService > {
    @Override
    public ValidationResult validate(CaseDocumentWithReferenceData caseDocumentWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {

       return newValidationResult(Optional.of(caseDocumentWithReferenceData)
                .filter(CaseDocumentWithReferenceData::isCaseReferredToCourt)
                .map(prosecutionAlreadyAccepted -> Problems.newProblem(CASE_REFERRED_TO_OPEN_COURT,
                        new ProblemValue(null,"referralReasonId", caseDocumentWithReferenceData.getReferralReasonId().toString()))));
    }
}