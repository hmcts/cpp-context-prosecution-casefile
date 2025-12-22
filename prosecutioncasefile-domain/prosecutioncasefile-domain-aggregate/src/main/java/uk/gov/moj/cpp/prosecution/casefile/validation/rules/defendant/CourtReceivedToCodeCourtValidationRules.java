package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

public class CourtReceivedToCodeCourtValidationRules extends CourtValidationRules {

    @Override
    public ValidationResult validate(DefendantWithReferenceData defendantWithReferenceData, ReferenceDataQueryService referenceDataQueryService) {
        return validateResult(referenceDataQueryService, FieldName.COURT_RECEIVED_TO, defendantWithReferenceData.getCaseDetails().getCourtReceivedToCode());
    }
}
