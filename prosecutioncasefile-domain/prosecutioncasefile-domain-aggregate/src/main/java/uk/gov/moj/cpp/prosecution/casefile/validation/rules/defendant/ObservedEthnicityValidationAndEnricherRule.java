package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_OBSERVED_ETHNICITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_OBSERVED_ETHNICITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Optional;

public class ObservedEthnicityValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null
                || defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation() == null
                || defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getObservedEthnicity() == null) {
            return VALID;
        }

        final String observedEthnicity = defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getObservedEthnicity().toString();

        Optional<ObservedEthnicityReferenceData> referenceObservedEthnicityOptional = defendantWithReferenceData.getReferenceDataVO()
                .getObservedEthnicityReferenceData().stream()
                .filter(referenceObservedEthnicity -> referenceObservedEthnicity.getEthnicityCode().equals(observedEthnicity)).findAny();

        if (referenceObservedEthnicityOptional.isPresent()) {
            return VALID;
        }

        if (referenceDataQueryService == null) {
            return newValidationResult(of(newProblem(DEFENDANT_OBSERVED_ETHNICITY_INVALID, new ProblemValue(null, DEFENDANT_OBSERVED_ETHNICITY.getValue(), observedEthnicity))));
        }

        referenceObservedEthnicityOptional = referenceDataQueryService.retrieveObservedEthnicity().stream().filter(ethnicity -> ethnicity.getEthnicityCode().equals(observedEthnicity)).findAny();
        if (referenceObservedEthnicityOptional.isPresent()) {
            defendantWithReferenceData.getReferenceDataVO().getObservedEthnicityReferenceData().add(referenceObservedEthnicityOptional.get());
            return VALID;
        } else {
            return newValidationResult(of(newProblem(DEFENDANT_OBSERVED_ETHNICITY_INVALID, new ProblemValue(null, DEFENDANT_OBSERVED_ETHNICITY.getValue(), observedEthnicity))));
        }

    }
}
