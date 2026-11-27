package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class VehicleCodeRefDataEnricher implements DefendantRefDataEnricher {
    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final List<VehicleCodeReferenceData> vehicleCodeReferenceDataList = referenceDataQueryService.retrieveVehicleCodes();

        defendantsWithReferenceDataList.forEach(defendantsWithReferenceData -> {
            final List<String> vehicleCodes = defendantsWithReferenceData.getDefendants().stream()
                    .flatMap(x -> x.getOffences().stream())
                    .filter(offence -> nonNull(offence) && nonNull(offence.getVehicleRelatedOffence()) && nonNull(offence.getVehicleRelatedOffence().getVehicleCode()))
                    .map(offence -> offence.getVehicleRelatedOffence().getVehicleCode())
                    .collect(Collectors.toList());

            if (isNotEmpty(vehicleCodes)) {
                final List<VehicleCodeReferenceData> vehicleCodeReferenceData = vehicleCodeReferenceDataList.stream()
                        .filter(vehicleCodeRefData -> vehicleCodes.stream().anyMatch(vehicleCodeRefData.getCode()::equalsIgnoreCase))
                        .collect(Collectors.toList());

                defendantsWithReferenceData.getReferenceDataVO().setVehicleCodesReferenceData(vehicleCodeReferenceData);
            }

        });

    }
}
