package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CASE_INITIATION_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_INITIATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;


public class CaseInitiationValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {


    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String initiationCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode();

        if(initiationCode == null) {
            return newValidationResult(of(newProblem(CASE_INITIATION_CODE_INVALID, new ProblemValue(null,CASE_INITIATION_CODE.getValue(), ""))));
        }

        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        if (referenceDataVO.getInitiationTypes().stream().anyMatch(x -> x.equals(initiationCode))) {
            return VALID;
        } else {
            return newValidationResult(of(newProblem(CASE_INITIATION_CODE_INVALID, new ProblemValue(null,CASE_INITIATION_CODE.getValue(), initiationCode))));
        }
    }
}
