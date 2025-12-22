package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Optional;

public class SelfDefinedEthnicityValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null
                || defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation() == null
                || defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation().getEthnicity() == null) {
            return VALID;
        }

        final String selfDefinedIEthnicity = defendantWithReferenceData.getDefendant().getIndividual().getSelfDefinedInformation().getEthnicity();


        Optional<SelfdefinedEthnicityReferenceData> referenceSelfDefinedEthnicityOptional = defendantWithReferenceData.getReferenceDataVO()
                .getSelfdefinedEthnicityReferenceData().stream()
                .filter(referenceSelfdefinedEthnicity -> referenceSelfdefinedEthnicity.getCode().equals(selfDefinedIEthnicity)).findAny();

        if (referenceSelfDefinedEthnicityOptional.isPresent()) {
            return VALID;
        }

        if (referenceDataQueryService == null) {
            return newValidationResult(of(newProblem(DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID, new ProblemValue(null, DEFENDANT_SELF_DEFINED_ETHNICITY.getValue(), selfDefinedIEthnicity))));
        }

        referenceSelfDefinedEthnicityOptional = defendantWithReferenceData.getReferenceDataVO()
                .getSelfdefinedEthnicityReferenceData().isEmpty() ? referenceDataQueryService.retrieveSelfDefinedEthnicity().stream().filter(ethnicity -> ethnicity.getCode().equals(selfDefinedIEthnicity)).findAny() : empty();
        if (referenceSelfDefinedEthnicityOptional.isPresent()) {
            defendantWithReferenceData.getReferenceDataVO().getSelfdefinedEthnicityReferenceData().add(referenceSelfDefinedEthnicityOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID, new ProblemValue(null, DEFENDANT_SELF_DEFINED_ETHNICITY.getValue(), selfDefinedIEthnicity))));
        }

    }
}
