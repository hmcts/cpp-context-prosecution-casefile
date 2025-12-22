package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

public class BaseProsecutionCaseFileHandler {

    @Inject
    protected EventSource eventSource;

    @Inject
    protected AggregateService aggregateService;


    protected void appendEventsToStream(final UUID streamId,
                                        final Envelope<?> envelope,
                                        final Function<ProsecutionCaseFile, Stream<Object>> function) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(streamId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        final Stream<Object> events = function.apply(prosecutionCaseFile);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);


        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    protected void appendApplicationEventsToStream(final UUID streamId,
                                                   final Envelope<?> envelope,
                                                   final Function<ApplicationFile, Stream<Object>> function) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(streamId);
        final ApplicationFile applicationFile = aggregateService.get(eventStream, ApplicationFile.class);

        final Stream<Object> events = function.apply(applicationFile);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);


        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
