package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;


import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonMetadata;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

public class MetadataHelper {

    private static final String IDPC_ID = "idpcId";
    private static final String CC_METADATA = "ccMetadata";
    private static final String CC_FILE_STORE_ID = "fileStoreId";


    private MetadataHelper() {
    }

    public static Metadata metadataFromString(final String metadataString) {
        return metadataFrom(readJson(metadataString)).build();
    }

    public static String metadataToString(final Metadata metadata) {
        return metadata.asJsonObject().toString();
    }

    private static JsonObject readJson(final String json) {
        try (final JsonReader jsonReader = JsonObjects.createReader(new StringReader(json))) {
            return jsonReader.readObject();
        }
    }
    public static JsonEnvelope envelopeWithIdpcProcessId(final Metadata originalMetadata, final JsonObject payload, final String processId) {
        final Metadata newMetadata = metadataWithIdpcProcessId(originalMetadata, processId);
        final JsonObject payloadWithMetadata = payloadWithMetadata(payload, newMetadata);

        return envelopeFrom(newMetadata, payloadWithMetadata);
    }

    public static Optional<String> getIdpcProcessId(final JsonEnvelope envelope) {
        return ofNullable(envelope.metadata().asJsonObject().getString(IDPC_ID, null));
    }

    public static Metadata metadataWithIdpcProcessId(final Metadata metadata, final String processId) {
        return JsonEnvelope.metadataFrom(
                createObjectBuilder(metadata.asJsonObject())
                        .add(IDPC_ID, processId)
                        .build())
                .build();
    }

    public JsonEnvelope envelopeWithCustomMetadata(final Metadata originalMetadata, final JsonObject ccMetadata, final JsonObject payload, final String fileServiceId) {
        final Metadata enrichedMetadata = enrichMetadata(originalMetadata, ccMetadata, fileServiceId);
        final JsonObject enrichedPayload = payloadWithMetadata(payload, enrichedMetadata);
        return envelopeFrom(enrichedMetadata, enrichedPayload);
    }

    private Metadata enrichMetadata(final Metadata metadata, final JsonObject ccMetadata, final String fileServiceId) {
        final JsonObjectBuilder builder = createObjectBuilder(metadata.asJsonObject())
                .add(CC_METADATA, ccMetadata);
        ofNullable(fileServiceId).ifPresent(s -> builder.add(CC_FILE_STORE_ID, fileServiceId));
        return JsonEnvelope.metadataFrom(builder.build())
                .build();
    }

    public Optional<JsonObject> getCCMetadata(final JsonEnvelope envelope) {
        return ofNullable(envelope.metadata().asJsonObject().getJsonObject(CC_METADATA));
    }

    public String getFileStoreId(final JsonEnvelope envelope) {
        return envelope.metadata().asJsonObject().getString(CC_FILE_STORE_ID, null);
    }

    private static JsonObject payloadWithMetadata(final JsonObject payload, final Metadata metadata) {

        final JsonObjectBuilder updatedMetadataBuilder = JsonObjects.createObjectBuilderWithFilter(metadata.asJsonObject(), key -> !JsonMetadata.CAUSATION.equals(key));

        return createObjectBuilder(payload)
                .add(JsonEnvelope.METADATA, updatedMetadataBuilder)
                .build();
    }
}
