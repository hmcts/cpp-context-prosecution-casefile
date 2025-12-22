package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.METADATA;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.function.Function;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

//TODO Remove (ATCM-3474)
//Custom metadata fields are lost when envelop is transferred via http, workaround is to pass them in payload.
@SuppressWarnings("squid:S4276")
public class EnvelopeHelper {

    private static final String FIELD_CAUSATION = "causation";
    private static final String ERROR_MESSAGE = "System user not found";

    @Inject
    private SystemUserProvider systemUserProvider;

    public JsonEnvelope withMetadataInPayload(final JsonEnvelope envelope) {
        return addMetadataToPayload(envelope);
    }

    public Envelope<JsonValue> withMetadataInPayloadForEnvelope(final JsonEnvelope envelope) {
        return addMetadataToPayload(envelope);
    }

    private JsonEnvelope addMetadataToPayload(final JsonEnvelope envelope) {
        final Function<String, Boolean> excludeCausation = key -> !FIELD_CAUSATION.equals(key);

        final Metadata metadataWithSystemUser = metadataFrom(envelope.metadata())
                .withUserId(systemUserProvider.getContextSystemUserId().orElseThrow(() -> new AccessControlViolationException(ERROR_MESSAGE)).toString())
                .build();

        final JsonObjectBuilder metadataWithoutCausation = createObjectBuilderWithFilter(metadataWithSystemUser.asJsonObject(), excludeCausation);

        final JsonObject payloadWithMetadata = createObjectBuilder(envelope.payloadAsJsonObject()).add(METADATA, metadataWithoutCausation).build();
        return envelopeFrom(envelope.metadata(), payloadWithMetadata);
    }
}
