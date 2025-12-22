package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;

import javax.inject.Inject;

public class ModeOfTrialRefDataEnricher implements DefendantRefDataEnricher {

    private static final String SUMMARY_ONLY_OFFENCE = "Summary-only offence";
    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public void enrich(final List<DefendantsWithReferenceData> defendantsWithReferenceDataList) {
        final List<ModeOfTrialReasonsReferenceData> modeOfTrialReasonsReferenceDataList = referenceDataQueryService.retrieveModeOfTrialReasons();

        for (final DefendantsWithReferenceData defendantsWithReferenceData: defendantsWithReferenceDataList) {
            final ReferenceDataVO referenceDataVO = defendantsWithReferenceData.getReferenceDataVO();

            final List<String> offenceMotReasonIds = defendantsWithReferenceData.getDefendants().stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> nonNull(offence.getMotReasonId()))
                    .map(offence -> offence.getMotReasonId().toString())
                    .collect(toList());

            final List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceData = modeOfTrialReasonsReferenceDataList.stream()
                    .filter(modeOfTrialReasonsReferenceData -> (offenceMotReasonIds.contains(modeOfTrialReasonsReferenceData.getId())))
                    .collect(toList());
            referenceDataVO.getModeOfTrialReasonsReferenceData().addAll(modeOfTrialReferenceData);

            final List<Offence> offencesWithNullMotReasonIds = defendantsWithReferenceData.getDefendants().stream()
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(offence -> isNull(offence.getMotReasonId()))
                    .collect(toList());
            if (!offencesWithNullMotReasonIds.isEmpty()) {
                final List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceDataWithSummaryOnlyOffence = modeOfTrialReasonsReferenceDataList.stream()
                        .filter(modeOfTrialReasonsReferenceData -> modeOfTrialReasonsReferenceData.getDescription().equals(SUMMARY_ONLY_OFFENCE))
                        .collect(toList());

                referenceDataVO.getModeOfTrialReasonsReferenceData().addAll(modeOfTrialReferenceDataWithSummaryOnlyOffence);
            }

        }
    }
}
