package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;

import javax.inject.Inject;

public class SelfDefineEthnictyRefDataEnricher implements DefendantRefDataEnricher {
    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final List<SelfdefinedEthnicityReferenceData> refEthnicities = referenceDataQueryService.retrieveSelfDefinedEthnicity();

        for (final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList) {
            final List<String> ethnicityList = defendantsWithReferenceData.getDefendants().stream()
                    .filter(x -> x.getIndividual() != null
                            && x.getIndividual().getSelfDefinedInformation() != null
                            && x.getIndividual().getSelfDefinedInformation().getEthnicity() != null
                    ).map(x -> x.getIndividual().getSelfDefinedInformation().getEthnicity())
                    .collect(toList());

            final List<String> ethnicityParentGuardianList = defendantsWithReferenceData.getDefendants().stream()
                    .filter(x -> x.getIndividual() != null
                            && x.getIndividual().getParentGuardianInformation() != null
                            && x.getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity() != null
                    ).map(x -> x.getIndividual().getParentGuardianInformation().getSelfDefinedEthnicity())
                    .collect(toList());

            if (!ethnicityList.isEmpty() || !ethnicityParentGuardianList.isEmpty()) {
                final List<SelfdefinedEthnicityReferenceData> ethnicities = refEthnicities.stream()
                        .filter(observedEthnicityReferenceData -> (ethnicityList.contains(observedEthnicityReferenceData.getCode())) ||
                                ethnicityParentGuardianList.stream().anyMatch(observedEthnicityReferenceData.getCode()::equalsIgnoreCase))
                        .collect(toList());
                defendantsWithReferenceData.getReferenceDataVO().setSelfdefinedEthnicityReferenceData(ethnicities);
            }

        }

    }
}
