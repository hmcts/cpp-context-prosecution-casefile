package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static uk.gov.moj.cpp.prosecution.casefile.refdata.HearingTypeLookUp.findHearingType;

public class HearingTypesRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final HearingTypes hearingTypes = referenceDataQueryService.retrieveHearingTypes();

        defendantsWithReferenceDataList.forEach(defendantsWithReferenceData -> {
                    final List<Defendant> defendants = defendantsWithReferenceData.getDefendants();
                    final Optional<HearingType> hearingTypeReferenceData = findHearingType(defendants, hearingTypes);
                    hearingTypeReferenceData.ifPresent(defendantsWithReferenceData.getReferenceDataVO()::setHearingType);
                }

        );


    }
}
