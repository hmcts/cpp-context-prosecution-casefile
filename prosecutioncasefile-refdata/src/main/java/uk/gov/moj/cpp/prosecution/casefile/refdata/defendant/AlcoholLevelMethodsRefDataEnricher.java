package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class AlcoholLevelMethodsRefDataEnricher implements DefendantRefDataEnricher {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        List<AlcoholLevelMethodReferenceData> alcoholLevelMethodRefData = null;


        for(final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList){
            final List<String> defendantAlcoholLevelMethods = getDefendantAlcoholLevelMethods(defendantsWithReferenceData);

            if (!defendantAlcoholLevelMethods.isEmpty()) {
                if (isNull(alcoholLevelMethodRefData)){
                    alcoholLevelMethodRefData = referenceDataQueryService.retrieveAlcoholLevelMethods();
                }

                if (!alcoholLevelMethodRefData.isEmpty()) {
                    addRefData(defendantsWithReferenceData, defendantAlcoholLevelMethods, alcoholLevelMethodRefData);
                }
            }
        }

    }

    private List<String> getDefendantAlcoholLevelMethods(final DefendantsWithReferenceData defendantsWithReferenceData) {
        return defendantsWithReferenceData.getDefendants().stream()
                .filter(defendant -> nonNull(defendant.getOffences()))
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getAlcoholRelatedOffence()) && isNotEmpty(offence.getAlcoholRelatedOffence().getAlcoholLevelMethod()))
                .map(offence -> offence.getAlcoholRelatedOffence().getAlcoholLevelMethod())
                .collect(Collectors.toList());
    }

    private void addRefData(final DefendantsWithReferenceData defendantsWithReferenceData, final List<String> alcoholLevelMethods, final List<AlcoholLevelMethodReferenceData> alcoholLevelMethodRefData) {

        if (nonNull(alcoholLevelMethodRefData) && !alcoholLevelMethodRefData.isEmpty()) {
            final List<AlcoholLevelMethodReferenceData> filteredAlcoholLevelMethodRefData = alcoholLevelMethodRefData.stream()
                    .filter(alcoholLevelMethodReferenceData -> alcoholLevelMethods.contains(alcoholLevelMethodReferenceData.getMethodCode()))
                    .collect(Collectors.toList());
            defendantsWithReferenceData.getReferenceDataVO().setAlcoholLevelMethodReferenceData(filteredAlcoholLevelMethodRefData);
        }
    }

}