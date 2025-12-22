package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import javax.inject.Inject;
import java.util.List;

public class GroupCasesProsecutorReferenceDataEnricher implements GroupCasesReferenceDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {

        final Prosecutor prosecutor = prosecutionWithReferenceDataList.stream()
                .map(ProsecutionWithReferenceData::getProsecution)
                .map(Prosecution::getCaseDetails)
                .map(CaseDetails::getProsecutor)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Prosecutor not present"));

        final ProsecutorsReferenceData prosecutorsReferenceData = this.getRefProsecutor(prosecutor);
        if (nonNull(prosecutorsReferenceData)) {
            prosecutionWithReferenceDataList.forEach(each -> each.getReferenceDataVO().setProsecutorsReferenceData(prosecutorsReferenceData));
        }

    }

    private ProsecutorsReferenceData getRefProsecutor(final Prosecutor prosecutor) {
        if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            return referenceDataQueryService.getProsecutorById(prosecutor.getProsecutionAuthorityId());
        }
        return referenceDataQueryService.retrieveProsecutors(prosecutor.getProsecutingAuthority());
    }
}
