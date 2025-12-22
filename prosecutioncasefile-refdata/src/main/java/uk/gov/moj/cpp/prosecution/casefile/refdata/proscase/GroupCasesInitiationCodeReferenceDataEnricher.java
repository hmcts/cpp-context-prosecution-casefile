package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import javax.inject.Inject;
import java.util.List;

public class GroupCasesInitiationCodeReferenceDataEnricher implements GroupCasesReferenceDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final List<String> initiationTypes = referenceDataQueryService.getInitiationCodes();
        prosecutionWithReferenceDataList.forEach(each -> each.getReferenceDataVO().setInitiationTypes(initiationTypes));
    }
}
