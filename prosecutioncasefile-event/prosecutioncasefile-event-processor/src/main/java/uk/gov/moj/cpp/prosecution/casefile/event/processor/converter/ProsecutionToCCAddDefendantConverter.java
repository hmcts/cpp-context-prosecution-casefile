package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;

import javax.inject.Inject;

public class ProsecutionToCCAddDefendantConverter implements Converter<ProsecutionDefendantsAdded, AddDefendantsToCourtProceedings> {

    @Inject
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Inject
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Override
    public AddDefendantsToCourtProceedings convert(final ProsecutionDefendantsAdded source) {
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(source.getReferenceDataVO());
        paramsVO.setCaseId(source.getCaseId());
        paramsVO.setSummonsApprovedOutcome(source.getSummonsApprovedOutcome());

        return AddDefendantsToCourtProceedings.addDefendantsToCourtProceedings()
                .withDefendants(prosecutionCaseFileDefendantToCCDefendantConverter.convert(source.getDefendants(), paramsVO))
                .withListHearingRequests(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.
                        convert(source.getDefendants(), paramsVO))
                .build();
    }
}
