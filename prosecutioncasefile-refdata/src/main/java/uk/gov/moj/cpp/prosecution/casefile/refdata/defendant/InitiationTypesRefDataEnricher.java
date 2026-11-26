package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.RefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class InitiationTypesRefDataEnricher implements RefDataEnricher<DefendantWithReferenceData> {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final DefendantWithReferenceData defendantWithReferenceData) {

        final String initiationCode = defendantWithReferenceData.getDefendant().getInitiationCode();

        final List<String> initiationTypes = referenceDataQueryService.getInitiationCodes().stream()
                .filter(code -> code.equals(initiationCode))
                .collect(Collectors.toList());
        defendantWithReferenceData.getReferenceDataVO().setInitiationTypes(initiationTypes);
    }

    @Override
    public void enrich(final List<DefendantWithReferenceData> prosecutionWithReferenceDataList) {
        throw new UnsupportedOperationException();
    }
}
