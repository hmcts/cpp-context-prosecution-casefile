package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_INITIATION_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_INITIATION_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class DefendantInitiationCodeValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    public static final String INITIATION_CODE_J = "J";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String defendantInitiationCode = defendantWithReferenceData.getDefendant().getInitiationCode();

        if (nonNull(defendantInitiationCode)) {

            final String caseInitiationCode = defendantWithReferenceData.getCaseDetails().getInitiationCode();
            if (!caseInitiationCode.equals(INITIATION_CODE_J) && defendantInitiationCode.equals(INITIATION_CODE_J)) {
                return newValidationResult(of(newProblem(DEFENDANT_INITIATION_CODE_INVALID, new ProblemValue(null, DEFENDANT_INITIATION_CODE.getValue(), defendantInitiationCode))));
            }
            if (caseInitiationCode.equals(INITIATION_CODE_J) && !defendantInitiationCode.equals(INITIATION_CODE_J)) {
                return newValidationResult(of(newProblem(DEFENDANT_INITIATION_CODE_INVALID, new ProblemValue(null, DEFENDANT_INITIATION_CODE.getValue(), defendantInitiationCode))));
            }
        }
        return VALID;
    }
}