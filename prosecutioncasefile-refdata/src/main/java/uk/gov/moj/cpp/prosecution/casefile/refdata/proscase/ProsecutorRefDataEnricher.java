package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProsecutorRefDataEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId = new HashMap<>();
        final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority = new HashMap<>();

        prosecutionWithReferenceDataList.forEach(
                each -> {
                    final Prosecutor prosecutor = each.getProsecution().getCaseDetails().getProsecutor();
                    if (nonNull(prosecutor)) {
                        ProsecutorsReferenceData prosecutorsReferenceData = getRefProsecutorFromMap(prosecutor, refProsecutorByAuthorityId, refProsecutorByAuthority);

                        if (isNull(prosecutorsReferenceData)) {
                            prosecutorsReferenceData = this.getRefProsecutor(prosecutor);
                            putRefProsecutorToMap(prosecutor, prosecutorsReferenceData, refProsecutorByAuthorityId, refProsecutorByAuthority);
                        }

                        each.getReferenceDataVO().setProsecutorsReferenceData(prosecutorsReferenceData);
                    }

                }

        );
    }

    private ProsecutorsReferenceData getRefProsecutorFromMap(final Prosecutor prosecutor, final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId, final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority){
        if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            return refProsecutorByAuthorityId.get(prosecutor.getProsecutionAuthorityId());
        } else {
            return refProsecutorByAuthority.get(prosecutor.getProsecutingAuthority());
        }
    }

    private void putRefProsecutorToMap(final Prosecutor prosecutor, final ProsecutorsReferenceData prosecutorsReferenceData, final Map<UUID, ProsecutorsReferenceData> refProsecutorByAuthorityId, final Map<String, ProsecutorsReferenceData> refProsecutorByAuthority){
        if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            refProsecutorByAuthorityId.put(prosecutor.getProsecutionAuthorityId(), prosecutorsReferenceData);
        }

        if (nonNull(prosecutor.getProsecutingAuthority())) {
            refProsecutorByAuthority.put(prosecutor.getProsecutingAuthority(), prosecutorsReferenceData);
        }
    }

    private ProsecutorsReferenceData getRefProsecutor(final Prosecutor prosecutor) {
        if (nonNull(prosecutor.getProsecutionAuthorityId())) {
            return referenceDataQueryService.getProsecutorById(prosecutor.getProsecutionAuthorityId());
        }
        return referenceDataQueryService.retrieveProsecutors(prosecutor.getProsecutingAuthority());
    }
}
