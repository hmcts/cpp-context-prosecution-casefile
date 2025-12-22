package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENDER_CODE_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_OFFENDER_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.Optional;

public class OffenderCodeValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null || defendantWithReferenceData.getDefendant().getIndividual().getOffenderCode() == null) {
            return VALID;
        }

        final String offenderCode = defendantWithReferenceData.getDefendant().getIndividual().getOffenderCode();

        Optional<OffenderCodeReferenceData> referenceDataOffenderCodeOptional = defendantWithReferenceData.getReferenceDataVO()
                                     .getOffenderCodeReferenceData().stream()
                                     .filter(referenceDataOffenceCode -> referenceDataOffenceCode.getOffenderCode().equals(offenderCode)).findAny();

        if (referenceDataOffenderCodeOptional.isPresent()) {
            return VALID;
        }

        referenceDataOffenderCodeOptional = referenceDataQueryService.retrieveOffenderCodes().stream().filter(offenderCodes -> offenderCodes.getOffenderCode().equals(offenderCode)).findAny();
        if (referenceDataOffenderCodeOptional.isPresent()) {
            defendantWithReferenceData.getReferenceDataVO().getOffenderCodeReferenceData().add(referenceDataOffenderCodeOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(OFFENDER_CODE_IS_INVALID, new ProblemValue(null,DEFENDANT_OFFENDER_CODE.getValue(), offenderCode))));
        }

    }
}
