package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class InitiationTypesRefDataEnricher implements CaseRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList) {
        final List<String> initiationTypesRef = referenceDataQueryService.getInitiationCodes();

        prosecutionWithReferenceDataList.forEach(each -> {
            final String initiationCode = each.getProsecution().getCaseDetails().getInitiationCode();

            final List<String> initiationTypes = initiationTypesRef.stream()
                    .filter(code -> code.equals(initiationCode))
                    .collect(Collectors.toList());

            each.getReferenceDataVO().setInitiationTypes(initiationTypes);
        });
    }
}
