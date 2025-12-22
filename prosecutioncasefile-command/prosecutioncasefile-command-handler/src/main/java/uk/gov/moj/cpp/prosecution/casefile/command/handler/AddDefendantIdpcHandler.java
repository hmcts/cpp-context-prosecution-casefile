package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDefendantIdpcHandler {

    static final String STREAM_ID = "caseId";
    @Inject
    protected JsonObjectToObjectConverter converter;
    @Inject
    private Clock clock;
    @Inject
    private EventSource eventSource;
    @Inject
    private Enveloper enveloper;
    @Inject
    private AggregateService aggregateService;

    @Handles("prosecutioncasefile.command.add-defendant-idpc")
    public void addDefendantIdpc(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();

        final CaseDocument caseDocument = caseDocumentFrom(payload);
        final UUID defendantId = fromString(payload.getString("defendantId"));

        applyToCaseAggregate(command,
                aCase -> aCase.addDefendantIdpc(getCaseId(command.payloadAsJsonObject()),
                        caseDocument, defendantId)
        );
    }

    private CaseDocument caseDocumentFrom(final JsonObject payload) {
        return new CaseDocument(
                UUID.fromString(payload.getString("id")),
                UUID.fromString(payload.getString("materialId")),
                payload.getString("documentType", null),
                clock.now());
    }

    private void applyToCaseAggregate(final JsonEnvelope command, final Function<ProsecutionCaseFile, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(getCaseId(command.payloadAsJsonObject()));
        final ProsecutionCaseFile aCase = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        final Stream<Object> events = function.apply(aCase);

        eventStream.append(
                events
                        .map(toEnvelopeWithMetadataFrom(command)));
    }

    private UUID getCaseId(final JsonObject payload) {
        return fromString(payload.getString(STREAM_ID));
    }

}
