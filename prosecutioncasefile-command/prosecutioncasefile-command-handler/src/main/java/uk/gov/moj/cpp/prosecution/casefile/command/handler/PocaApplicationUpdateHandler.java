package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationAggregate;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.PocaCourtApplication;

import javax.inject.Inject;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@SuppressWarnings("squid:S1168")
@ServiceComponent(COMMAND_HANDLER)
public class PocaApplicationUpdateHandler extends AbstractCommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PocaApplicationUpdateHandler.class.getName());

    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("prosecutioncasefile.command.update-application-status")
    public void pocaApplicationUpdateStatus(final Envelope<PocaCourtApplication> pocaCourtApplicationEnvelope) throws EventStreamException {

        final PocaCourtApplication sourcePocaCourtApplication = pocaCourtApplicationEnvelope.payload();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("prosecutioncasefile.command.update-application-status for Court Application Id: {}", nonNull(sourcePocaCourtApplication.getCourtApplication()) ? sourcePocaCourtApplication.getCourtApplication().getId() : null);
        }

        final EventStream eventStream = eventSource.getStreamById(sourcePocaCourtApplication.getCourtApplication().getId());
        final ApplicationAggregate applicationAggregate = aggregateService.get(eventStream, ApplicationAggregate.class);

        appendEventsToStream(pocaCourtApplicationEnvelope, eventStream, applicationAggregate.acceptPocaApplication(sourcePocaCourtApplication.getCourtApplication()));
    }
}