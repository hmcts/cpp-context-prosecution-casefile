package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.regex.Pattern;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Constants.PNC_ID_REGEX;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_PNC_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PNC_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class PncIdValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern PNC_ID_FORMAT = Pattern.compile(PNC_ID_REGEX.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getPncIdentifier() == null) {
            return VALID;
        }

        final String pncId = defendantWithReferenceData.getDefendant().getPncIdentifier();

        return PNC_ID_FORMAT.matcher(pncId).matches() ?
                VALID :
                newValidationResult(of(newProblem(INVALID_PNC_ID, new ProblemValue(null, PNC_ID.getValue(), pncId))));
    }
}
