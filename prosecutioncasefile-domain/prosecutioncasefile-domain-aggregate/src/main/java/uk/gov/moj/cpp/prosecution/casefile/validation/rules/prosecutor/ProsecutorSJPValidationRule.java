package uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor;


import static java.util.Optional.of;

import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTING_AUTHORITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;


/**
 * \subsection Validation Rules
 * <p>
 * ProsecutorSJPValidationRule  check SJP is active via prosecution reference data
 */

public class ProsecutorSJPValidationRule
        implements ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(ProsecutionWithReferenceData prosecution, ReferenceDataQueryService query) {

        final Prosecutor prosecutor = prosecution.getProsecution().getCaseDetails().getProsecutor();
        if (prosecutor.getReferenceData() != null && prosecutor.getReferenceData().getSjpFlag()) {
            return VALID;
        } else {
            return newValidationResult(of(newProblem(ProblemCode.PROSECUTOR_NOT_RECOGNISED_AS_AN_AUTHORISED_SJP_PROSECUTOR,
                    new ProblemValue(null,
                            PROSECUTING_AUTHORITY.getValue(),
                            prosecutor.getProsecutingAuthority()))));
        }
    }
}



