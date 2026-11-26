package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class CcProsecutionHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Handles("prosecutioncasefile.command.initiate-cc-prosecution-with-reference-data")
    public void initiateCCProsecutionWithReferenceData(final Envelope<ProsecutionWithReferenceData> envelope) throws EventStreamException {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = envelope.payload();
        appendEventsToStream(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId(), envelope,
                prosecutionCaseFile -> prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceData, newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), referenceDataQueryService));
    }
}
