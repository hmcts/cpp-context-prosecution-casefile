package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.BAIL_CONDITIONS_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_BAIL_CONDITIONS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class BailConditionsValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {


    public static final String CONDITIONAL_BAIL_STATUS = "B";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final String custodyStatus = defendantWithReferenceData.getDefendant().getCustodyStatus();

        if (custodyStatus != null && CONDITIONAL_BAIL_STATUS.equals(custodyStatus) && isBailConditionsEmpty(defendantWithReferenceData)) {
            return newValidationResult(of(newProblem(BAIL_CONDITIONS_REQUIRED, new ProblemValue(null, DEFENDANT_BAIL_CONDITIONS.getValue(), ""))));

        }

        return VALID;
    }

    private boolean isBailConditionsEmpty(final DefendantWithReferenceData defendantWithReferenceData) {
        return (defendantWithReferenceData.getDefendant().getIndividual() != null) && (defendantWithReferenceData.getDefendant().getIndividual().getBailConditions() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getBailConditions().trim().isEmpty());
    }
}
