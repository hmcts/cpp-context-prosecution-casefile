package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class AbstractCommandHandler {

    public void appendEventsToStream(final uk.gov.justice.services.messaging.Envelope<?> envelope, final uk.gov.justice.services.eventsourcing.source.core.EventStream eventStream, final java.util.stream.Stream<Object> events) throws uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException {
        final uk.gov.justice.services.messaging.JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
