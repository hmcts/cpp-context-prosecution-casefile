package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.POLICE_FORCE_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.POLICE_FORCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


public class PoliceForceCodeValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String policeForceCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceForceCode();

        if(policeForceCode == null) {
            return VALID;
        }

        if(referenceDataQueryService.retrievePoliceForceCode().stream().anyMatch( x -> x.getPoliceForceCode().equals(policeForceCode))) {
            return VALID;
        }else {
            return newValidationResult(of(newProblem(POLICE_FORCE_CODE_INVALID, new ProblemValue(null,POLICE_FORCE_CODE.getValue(), policeForceCode))));
        }
    }
}
