package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Optional;

public class ParentGuardianObservedEthnicityValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getObservedEthnicity() == null) {
            return VALID;
        }

        final String observedEthnicity = defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getObservedEthnicity();
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        Optional<ObservedEthnicityReferenceData> observedEthnicityReferenceDataOptional = referenceDataVO.getObservedEthnicityReferenceData().stream().filter(observedEthnicityReferenceData -> observedEthnicityReferenceData.getEthnicityCode().equalsIgnoreCase(observedEthnicity)).findAny();
        if (observedEthnicityReferenceDataOptional.isPresent()) {
            return VALID;
        }

        if (referenceDataQueryService == null) {
            return newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID, new ProblemValue(null, PARENT_GUARDIAN_OBSERVED_ETHNICITY.getValue(), observedEthnicity))));
        }

        final List<ObservedEthnicityReferenceData> observedEthnicityReferenceData = referenceDataQueryService.retrieveObservedEthnicity();
        observedEthnicityReferenceDataOptional = observedEthnicityReferenceData.stream().filter(ethnicityReferenceData -> ethnicityReferenceData.getEthnicityCode().equalsIgnoreCase(observedEthnicity)).findAny();

        if (observedEthnicityReferenceDataOptional.isPresent()) {
            referenceDataVO.getObservedEthnicityReferenceData().add(observedEthnicityReferenceDataOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID, new ProblemValue(null, PARENT_GUARDIAN_OBSERVED_ETHNICITY.getValue(), observedEthnicity))));
        }
    }

}