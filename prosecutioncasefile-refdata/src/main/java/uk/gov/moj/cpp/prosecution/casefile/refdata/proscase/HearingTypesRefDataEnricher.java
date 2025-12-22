package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static uk.gov.moj.cpp.prosecution.casefile.refdata.HearingTypeLookUp.findHearingType;

public class HearingTypesRefDataEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final HearingTypes hearingTypes = referenceDataQueryService.retrieveHearingTypes();

        prosecutionWithReferenceDataList.forEach(each -> {
                    final List<Defendant> defendants = each.getProsecution().getDefendants();
                    final Optional<HearingType> hearingTypeReferenceData = findHearingType(defendants, hearingTypes);
                    hearingTypeReferenceData.ifPresent(each.getReferenceDataVO()::setHearingType);
                }
        );

    }

}

