package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.SUMMONS_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CASE_SUMMONS_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

public class SummonsCodeValidationRule implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    public static final String SUMMONS_CASE_TYPE = "S";

    @Override
    public ValidationResult validate(final ProsecutionWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String summonsCode = prosecutionWithReferenceData.getProsecution().getCaseDetails().getSummonsCode();

        if (!SUMMONS_CASE_TYPE.equals(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode())) {
            return VALID;
        }

        if(summonsCode == null) {
            return newValidationResult(of(newProblem(SUMMONS_CODE_INVALID, new ProblemValue(null,CASE_SUMMONS_CODE.getValue(), ""))));
        }

        if ((referenceDataQueryService.retrieveSummonsCodes().stream().anyMatch(s -> s.getSummonsCode().equals(summonsCode)))) {
            return VALID;
        } else {
            return newValidationResult(of(newProblem(SUMMONS_CODE_INVALID, new ProblemValue(null,CASE_SUMMONS_CODE.getValue(), summonsCode))));
        }

    }
}
