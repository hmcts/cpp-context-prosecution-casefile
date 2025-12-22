package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

public class NationalityRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantWithReferenceDataList) {
        final List<ReferenceDataCountryNationality> referenceDataCountryNationalitiesRef = referenceDataQueryService.retrieveCountryNationality();

        for (final DefendantsWithReferenceData defendantsWithReferenceData: defendantWithReferenceDataList) {
            final List<String> nationalities = defendantsWithReferenceData.getDefendants().stream()
                    .filter(defendant -> nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getSelfDefinedInformation())
                            && nonNull(defendant.getIndividual().getSelfDefinedInformation().getNationality()))
                    .map(defendant -> defendant.getIndividual().getSelfDefinedInformation().getNationality())
                    .collect(Collectors.toList());

            final List<String> additionalNationalities = defendantsWithReferenceData.getDefendants().stream()
                    .filter(defendant -> nonNull(defendant.getIndividual()) && nonNull(defendant.getIndividual().getSelfDefinedInformation()) &&
                            nonNull(defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality()))
                    .map(defendant -> defendant.getIndividual().getSelfDefinedInformation().getAdditionalNationality())
                    .collect(Collectors.toList());

            final List<String> defendantNationalities = Stream.concat(nationalities.stream(), additionalNationalities.stream()).collect(Collectors.toList());

            final ReferenceDataVO referenceDataVO = defendantsWithReferenceData.getReferenceDataVO();
            if (referenceDataVO.getCountryNationalityReferenceData().isEmpty()) {
                final List<ReferenceDataCountryNationality> referenceDataCountryNationalities = referenceDataCountryNationalitiesRef.stream()
                        .filter(referenceDataCountryNationality -> defendantNationalities.contains(referenceDataCountryNationality.getIsoCode())
                                || (nonNull(referenceDataCountryNationality.getCjsCode()) && defendantNationalities.contains(referenceDataCountryNationality.getCjsCode())))
                        .collect(Collectors.toList());

                referenceDataVO.getCountryNationalityReferenceData().addAll(referenceDataCountryNationalities);
            }

        }
    }
}
