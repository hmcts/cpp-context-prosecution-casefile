package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Optional;

public class ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity() == null) {
            return VALID;
        }

        final String selfDefinedEthnicity = defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity();
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();
        Optional<SelfdefinedEthnicityReferenceData> selfDefinedEthnicityReferenceDataOptional = referenceDataVO.getSelfdefinedEthnicityReferenceData().stream().filter(selfdefinedEthnicityReferenceData -> selfdefinedEthnicityReferenceData.getCode().equalsIgnoreCase(selfDefinedEthnicity)).findAny();
        if (selfDefinedEthnicityReferenceDataOptional.isPresent()) {
            return VALID;
        }

        if (referenceDataQueryService == null) {
            return newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID, new ProblemValue(null, PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY.getValue(), selfDefinedEthnicity))));
        }

        final List<SelfdefinedEthnicityReferenceData> selfdefinedEthnicityReferenceData = referenceDataQueryService.retrieveSelfDefinedEthnicity();
        selfDefinedEthnicityReferenceDataOptional = selfdefinedEthnicityReferenceData.stream().filter(ethnicityReferenceData -> ethnicityReferenceData.getCode().equalsIgnoreCase(selfDefinedEthnicity)).findAny();

        if (selfDefinedEthnicityReferenceDataOptional.isPresent()) {
            referenceDataVO.getSelfdefinedEthnicityReferenceData().add(selfDefinedEthnicityReferenceDataOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID, new ProblemValue(null, PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY.getValue(), selfDefinedEthnicity))));
        }
    }
}
