package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptGroupCases;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class AcceptGroupCaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptGroupCaseHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("prosecutioncasefile.command.handler.accept-group-cases")
    public void handleAcceptGroupCases(final Envelope<AcceptGroupCases> envelope) throws EventStreamException {
        final AcceptGroupCases acceptGroupCases = envelope.payload();
        final UUID groupId = acceptGroupCases.getGroupId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("prosecutioncasefile.command.handler.accept-group-cases for Group with id '{}'", groupId);
        }

        final EventStream eventStream = eventSource.getStreamById(groupId);
        final GroupProsecutionCaseFile groupProsecution = aggregateService.get(eventStream, GroupProsecutionCaseFile.class);

        final Stream<Object> events = groupProsecution.acceptGroupCases(groupId);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
