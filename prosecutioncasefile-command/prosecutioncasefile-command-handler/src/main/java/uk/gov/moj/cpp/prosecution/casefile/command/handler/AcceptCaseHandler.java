package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.nameUUIDFromBytes;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static java.util.Objects.nonNull;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.CpsServeMaterialAggregate;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.service.DefenceService;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CpsFormValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CheckPendingEventsForNewDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

@SuppressWarnings({"squid:S134"})
@ServiceComponent(COMMAND_HANDLER)
public class AcceptCaseHandler {

    private static final String MATERIAL = "material";
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefenceService defenceService;

    @Inject
    private CpsFormValidator cpsFormValidator;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    protected ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Handles("prosecutioncasefile.command.accept-case")
    public void handleAcceptCase(final Envelope<AcceptCase> envelope) throws EventStreamException {
        final AcceptCase acceptCase = envelope.payload();
        final UUID streamId = acceptCase.getCaseId();

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);

        final EventStream eventStream = eventSource.getStreamById(streamId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        final Stream<Object> events = prosecutionCaseFile.acceptCase(acceptCase.getCaseId(), acceptCase.getDefendantIds(), referenceDataQueryService);

        final Stream<JsonEnvelope> mappedEvents = mapUsingOriginalMaterialEvents(events, jsonEnvelope, eventSource.getStreamById(streamId));

        eventStream.append(mappedEvents);

        final JsonObject prosecutionCase = fetchProsecutionCase(jsonEnvelope, streamId.toString());
        if (nonNull(prosecutionCase)) {
            final String caseUrn = prosecutionCase.getString("prosecutionCaseReference");
            final EventStream cpsEventStream = eventSource.getStreamById(nameUUIDFromBytes(caseUrn.getBytes(UTF_8)));
            if (nonNull(cpsEventStream)) {
                final CpsServeMaterialAggregate cpsServeMaterialAggregate = aggregateService.get(cpsEventStream, CpsServeMaterialAggregate.class);
                if (nonNull(cpsServeMaterialAggregate) ) {
                    final Stream<Object> petEvent = cpsServeMaterialAggregate.acceptCasePet(streamId, prosecutionCaseFile,
                            Optional.ofNullable(prosecutionCase), cpsFormValidator, referenceDataQueryService,
                            progressionService, jsonObjectToObjectConverter, objectToJsonObjectConverter, listToJsonArrayConverter, defenceService);
                    if(!petEvent.equals(Stream.empty())) {
                        cpsEventStream.append(petEvent.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
                    }

                    final Stream<Object> bcmEvent = cpsServeMaterialAggregate.acceptCaseBcm(streamId, Optional.ofNullable(prosecutionCase),
                            cpsFormValidator, referenceDataQueryService, progressionService,
                            objectToJsonObjectConverter, listToJsonArrayConverter);

                    if(!bcmEvent.equals(Stream.empty())) {
                        cpsEventStream.append(bcmEvent.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
                    }
                }
            }
        }
    }

    @Handles("prosecutioncasefile.command.check-pending-events-for-new-defendants")
    public void handleCheckPendingEvents(final Envelope<CheckPendingEventsForNewDefendants> envelope) throws EventStreamException {
        final UUID caseId = envelope.payload().getCaseId();
        final EventStream eventStream = eventSource.getStreamById(caseId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        final Stream<Object> events = prosecutionCaseFile.runPendingEvents(referenceDataQueryService);

        final JsonEnvelope jsonEnvelope = envelopeFrom(envelope.metadata(), JsonValue.NULL);

        final Stream<JsonEnvelope> mappedEvents = mapUsingOriginalMaterialEvents(events, jsonEnvelope, eventSource.getStreamById(caseId));

        eventStream.append(mappedEvents);
    }

    private Stream<JsonEnvelope> mapUsingOriginalMaterialEvents(final Stream<Object> events, final JsonEnvelope commandEnvelope, final EventStream eventStream) {
        try (final Stream<JsonEnvelope> eventsStream = eventStream.read()) {
            final Map<String, List<JsonEnvelope>> fileStoreIdToOriginalMaterialPendingEnvelope = getFileStoreIdToOriginalMaterialPendingEnvelope(eventsStream);

            return mapNewEventsToEnvelope(events, commandEnvelope, fileStoreIdToOriginalMaterialPendingEnvelope);
        }
    }

    private Stream<JsonEnvelope> mapNewEventsToEnvelope(final Stream<Object> events, final JsonEnvelope commandEnvelope, final Map<String, List<JsonEnvelope>> collectedEnvelopes) {
        return events.map(event -> mapNewEventsToEnvelope(commandEnvelope, collectedEnvelopes, event));
    }

    private JsonEnvelope mapNewEventsToEnvelope(final JsonEnvelope commandEnvelope, final Map<String, List<JsonEnvelope>> collectedEnvelopes, final Object event) {
        final Function<Object, JsonEnvelope> payloadToEnvelope;

        if (event instanceof MaterialAdded) {
            final MaterialAdded materialAdded = (MaterialAdded) event;

            final JsonEnvelope eventEnvelope = replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(commandEnvelope, collectedEnvelopes, materialAdded.getMaterial());
            payloadToEnvelope = toEnvelopeWithMetadataFrom(eventEnvelope);
        } else if (event instanceof MaterialRejected) {
            final MaterialRejected materialRejected = (MaterialRejected) event;

            final JsonEnvelope eventEnvelope = replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(commandEnvelope, collectedEnvelopes, materialRejected.getMaterial());
            payloadToEnvelope = toEnvelopeWithMetadataFrom(eventEnvelope);
        } else if (event instanceof MaterialAddedV2) {
            final MaterialAddedV2 materialAdded = (MaterialAddedV2) event;

            final JsonEnvelope eventEnvelope = replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(commandEnvelope, collectedEnvelopes, materialAdded);
            payloadToEnvelope = toEnvelopeWithMetadataFrom(eventEnvelope);
        } else if (event instanceof MaterialRejectedV2) {
            final MaterialRejectedV2 materialRejected = (MaterialRejectedV2) event;

            final JsonEnvelope eventEnvelope = replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(commandEnvelope, collectedEnvelopes, materialRejected);
            payloadToEnvelope = toEnvelopeWithMetadataFrom(eventEnvelope);
        } else {
            payloadToEnvelope = toEnvelopeWithMetadataFrom(commandEnvelope);
        }

        return payloadToEnvelope.apply(event);
    }

    private Map<String, List<JsonEnvelope>> getFileStoreIdToOriginalMaterialPendingEnvelope(final Stream<JsonEnvelope> eventsStream) {
        return eventsStream
                .filter(envelope -> "prosecutioncasefile.events.material-pending".equals(envelope.metadata().name()) ||
                        "prosecutioncasefile.events.material-pending-v2".equals(envelope.metadata().name()))
                .collect(Collectors.groupingBy(envelope ->
                        getFileStoreId(envelope.payloadAsJsonObject())));
    }

    private String getFileStoreId(JsonObject payload) {
        if (JsonValue.ValueType.STRING.equals(payload.get(MATERIAL).getValueType())) {
            return payload.getString(MATERIAL);
        } else {
            return payload.getJsonObject(MATERIAL).getString("fileStoreId");
        }
    }

    /**
     * For each new {@link MaterialAdded} event, we must overwrite the 'submissionId' field in the
     * metadata from the command with the 'submissionId' from the metadata of the original event
     * that created the related {@link MaterialPending} event.
     *
     * @param jsonEnvelope       the envelope containing metadata from the command raising the new
     *                           event
     * @param collectedEnvelopes the collection of envelopes representing the original Pending
     *                           Material events
     * @param material           the specific material
     * @return the updated envelope with the correct (material) 'submissionId' in the metadata
     */
    private JsonEnvelope replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(final JsonEnvelope jsonEnvelope, final Map<String, List<JsonEnvelope>> collectedEnvelopes, final Material material) {
        final JsonObject commandMetadata = jsonEnvelope.metadata().asJsonObject();
        final JsonEnvelope matchedEnvelope = collectedEnvelopes.get(material.getFileStoreId().toString()).get(0);
        return getJsonEnvelopeWithSubmissionId(commandMetadata, matchedEnvelope);
    }

    private JsonEnvelope replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(final JsonEnvelope jsonEnvelope, final Map<String, List<JsonEnvelope>> collectedEnvelopes, final MaterialAddedV2 materialAddedV2) {
        final JsonObject commandMetadata = jsonEnvelope.metadata().asJsonObject();
        final JsonEnvelope matchedEnvelope = collectedEnvelopes.get(materialAddedV2.getMaterial().toString()).get(0);
        return getJsonEnvelopeWithSubmissionId(commandMetadata, matchedEnvelope);
    }

    private JsonEnvelope replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(final JsonEnvelope jsonEnvelope, final Map<String, List<JsonEnvelope>> collectedEnvelopes, final MaterialRejectedV2 materialRejectedV2) {
        final JsonObject commandMetadata = jsonEnvelope.metadata().asJsonObject();
        final JsonEnvelope matchedEnvelope = collectedEnvelopes.get(materialRejectedV2.getMaterial().toString()).get(0);
        return getJsonEnvelopeWithSubmissionId(commandMetadata, matchedEnvelope);
    }

    private JsonEnvelope getJsonEnvelopeWithSubmissionId(final JsonObject commandMetadata, final JsonEnvelope matchedEnvelope) {
        final String submissionId = matchedEnvelope.metadata().asJsonObject().getString("submissionId");
        final JsonObject commandMetadataWithEventSubmission = createObjectBuilder(commandMetadata).add("submissionId", submissionId).build();
        final Metadata eventMetadata = metadataFrom(commandMetadataWithEventSubmission).build();
        return envelopeFrom(eventMetadata, matchedEnvelope.payloadAsJsonObject());
    }

    public JsonObject fetchProsecutionCase(final JsonEnvelope jsonEnvelope, final String caseId) {
        return prosecutionCaseQueryService.getProsecutionCaseByCaseId(jsonEnvelope, caseId);
    }
}