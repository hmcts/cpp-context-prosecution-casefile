package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.UUID;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;

public class MaterialService {
    public static final String CONTEXT = "context";
    public static final String SOURCE = "originator";
    public static final String ORIGINATOR_VALUE = "court";


    protected static final String UPLOAD_MATERIAL = "material.command.upload-file";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialService.class.getCanonicalName());
    private static final String FIELD_MATERIAL_ID = "materialId";
    private static final String FIELD_FILE_SERVICE_ID = "fileServiceId";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final Envelope envelope) {
        LOGGER.info("material being uploaded '{}' file service id '{}'", materialId, fileServiceId);
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));
        final JsonObject uploadMaterialPayload = Json.createObjectBuilder()
                .add(FIELD_MATERIAL_ID, materialId.toString())
                .add(FIELD_FILE_SERVICE_ID, fileServiceId.toString())
                .build();
        LOGGER.info("requesting material service to upload file id {} for material {}", fileServiceId, materialId);
        sender.send(assembleEnvelopeWithPayloadAndMetaDetails(uploadMaterialPayload, UPLOAD_MATERIAL, userId.toString()));
    }

    private static JsonEnvelope assembleEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String userId) {
        final Metadata metadata = createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, userId);
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    private static Metadata createMetadataWithProcessIdAndUserId(final String id, final String name, final String userId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .add(SOURCE, ORIGINATOR_VALUE)
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(USER_ID, userId))
                .build()).build();
    }

    private static JsonObject addMetadataToPayload(final JsonObject load, final Metadata metadata) {
        final JsonObjectBuilder job = Json.createObjectBuilder();
        load.entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(JsonEnvelope.METADATA, metadata.asJsonObject());
        return job.build();
    }


}
