package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATED_PROSECUTION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectGroupCases;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class RejectGroupCaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectGroupCaseHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles("prosecutioncasefile.command.handler.reject-group-cases")
    public void handleRejectGroupCases(final Envelope<RejectGroupCases> envelope) throws EventStreamException {
        final RejectGroupCases rejectGroupCases = envelope.payload();
        final UUID groupId = rejectGroupCases.getGroupId();
        final UUID caseId = rejectGroupCases.getCaseId();
        final String caseUrn = rejectGroupCases.getCaseUrn();

        LOGGER.info("prosecutioncasefile.command.handler.reject-group-cases for groupId: {}, caseId: {}, caseUrn: {}", groupId, caseId, caseUrn);
        if (nonNull(groupId)) {
            final EventStream eventStream = eventSource.getStreamById(groupId);
            final GroupProsecutionCaseFile aggregate = aggregateService.get(eventStream, GroupProsecutionCaseFile.class);
            appendEventsToStream(envelope, eventStream,
                    aggregate.rejectGroupProsecution(asList(newProblem(DUPLICATED_PROSECUTION, "urn", caseUrn)))
            );
        } else if (nonNull(caseId)) {
            final EventStream eventStream = eventSource.getStreamById(caseId);
            final ProsecutionCaseFile aggregate = aggregateService.get(eventStream, ProsecutionCaseFile.class);
            appendEventsToStream(envelope, eventStream,
                    aggregate.rejectProsecution(asList(newProblem(DUPLICATED_PROSECUTION, "urn", caseUrn)))
            );
        }
    }

    private void appendEventsToStream(final Envelope<RejectGroupCases> envelope, final EventStream eventStream,
                                      final Stream<Object> events) throws EventStreamException {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}