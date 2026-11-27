package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.EjectCase;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class EjectCaseHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("prosecutioncasefile.command.eject-case")
    public void handleEjectCase(final Envelope<EjectCase> envelope) throws EventStreamException {
        final EjectCase ejectCase = envelope.payload();
        appendEventsToStream(ejectCase.getCaseId(), envelope, prosecutionCaseFile ->
                prosecutionCaseFile.ejectCase(ejectCase.getCaseId()));
    }
}
