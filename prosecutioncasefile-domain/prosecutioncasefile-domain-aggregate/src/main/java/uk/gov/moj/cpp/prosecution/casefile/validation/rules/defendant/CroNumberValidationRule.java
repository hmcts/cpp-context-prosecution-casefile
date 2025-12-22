package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.regex.Pattern;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Constants.CRO_NUMBER_REGEX_ONE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Constants.CRO_NUMBER_REGEX_TWO;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_CRO_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.CRO_NUMBER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class CroNumberValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern CRO_NUMBER_FORMAT_ONE = Pattern.compile(CRO_NUMBER_REGEX_ONE.getValue());
    private static final Pattern CRO_NUMBER_FORMAT_TWO = Pattern.compile(CRO_NUMBER_REGEX_TWO.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getCroNumber() == null) {
            return VALID;
        }

        final String croNumber = defendantWithReferenceData.getDefendant().getCroNumber();

        return (CRO_NUMBER_FORMAT_ONE.matcher(croNumber).matches() || CRO_NUMBER_FORMAT_TWO.matcher(croNumber).matches()) ?
                VALID :
                newValidationResult(of(newProblem(INVALID_CRO_NUMBER, new ProblemValue(null, CRO_NUMBER.getValue(), croNumber))));
    }
}
