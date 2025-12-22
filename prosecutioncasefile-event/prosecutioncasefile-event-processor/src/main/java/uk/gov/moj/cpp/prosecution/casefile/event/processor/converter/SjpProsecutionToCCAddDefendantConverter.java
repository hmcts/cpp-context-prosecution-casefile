package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.SpiProsecutionDefendantsAdded;

import javax.inject.Inject;
@SuppressWarnings("squid:S1133")
public class SjpProsecutionToCCAddDefendantConverter implements Converter<SpiProsecutionDefendantsAdded, AddDefendantsToCourtProceedings> {

    @Inject
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Inject
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Override
    public AddDefendantsToCourtProceedings convert(final SpiProsecutionDefendantsAdded source) {
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(source.getReferenceDataVO());
        paramsVO.setCaseId(source.getCaseId());

        return AddDefendantsToCourtProceedings.addDefendantsToCourtProceedings()
                .withDefendants(prosecutionCaseFileDefendantToCCDefendantConverter.convert(source.getDefendants(), paramsVO))
                .withListHearingRequests(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.
                        convert(source.getDefendants(), paramsVO))
                .build();
    }
}
