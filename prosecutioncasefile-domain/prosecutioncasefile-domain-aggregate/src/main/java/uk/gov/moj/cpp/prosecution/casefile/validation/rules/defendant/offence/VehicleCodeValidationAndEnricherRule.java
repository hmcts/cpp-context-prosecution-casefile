package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_OFFENCE_VEHICLE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_VEHICLE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("squid:S3776")
public class VehicleCodeValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final List<Offence> vehicleOffenceList = getVehicleOffenceList(defendantWithReferenceData);

        if(vehicleOffenceList.isEmpty()) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        for(final Offence offence : vehicleOffenceList) {
            if (referenceDataVO.getVehicleCodesReferenceData().stream().noneMatch(vehicleCodeReferenceData -> offence.getVehicleRelatedOffence()!=null && vehicleCodeReferenceData.getCode().equalsIgnoreCase(offence.getVehicleRelatedOffence().getVehicleCode()))) {

                if (referenceDataQueryService == null) {
                    problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_VEHICLE_CODE.getValue(), offence.getVehicleRelatedOffence().getVehicleCode()));
                }

                final List<VehicleCodeReferenceData> vehicleCodeReferenceData = referenceDataQueryService!=null?referenceDataQueryService.retrieveVehicleCodes():new ArrayList<>();
                final Optional<VehicleCodeReferenceData> vehicleCodeReferenceDataOptional = vehicleCodeReferenceData.stream().filter(vehicleCode ->
                        (offence.getVehicleRelatedOffence()!=null && vehicleCode.getCode().equalsIgnoreCase(offence.getVehicleRelatedOffence().getVehicleCode()))).findAny();

                if (vehicleCodeReferenceDataOptional.isPresent()) {
                    referenceDataVO.getVehicleCodesReferenceData().add(vehicleCodeReferenceDataOptional.get());
                } else {
                    problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_VEHICLE_CODE.getValue(), offence.getVehicleRelatedOffence().getVehicleCode()));
                }
            }
        }

        if(problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(DEFENDANT_OFFENCE_VEHICLE_CODE, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }

    private List<Offence> getVehicleOffenceList(DefendantWithReferenceData defendantWithReferenceData) {
        return defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(offence -> offence.getVehicleRelatedOffence() != null && offence.getVehicleRelatedOffence().getVehicleCode() != null)
                .collect(toList());
    }
}
