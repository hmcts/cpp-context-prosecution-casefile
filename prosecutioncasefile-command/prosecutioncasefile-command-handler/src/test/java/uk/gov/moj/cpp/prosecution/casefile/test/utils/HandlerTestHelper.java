package uk.gov.moj.cpp.prosecution.casefile.test.utils;

import static com.google.common.io.Resources.getResource;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HandlerTestHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private HandlerTestHelper() {
    }

    //TODO: refactor into matcher (needs techpod support)
    public static void matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName,
                                  final Supplier<JsonValue> expectedResultPayload) {

        final JsonObject payload = jsonEnvelopeStream.filter(jsonEnvelope -> jsonEnvelope.metadata().name().equals(eventName))
                .findFirst()
                .map(JsonEnvelope::payloadAsJsonObject)
                .orElse(null);

        assertThat(generatedEventAsJsonNode(payload), equalTo(generatedEventAsJsonNode(expectedResultPayload.get())));
    }

    public static void matchEvent(final Stream<JsonEnvelope> jsonEnvelopeStream,
                                  final String eventName,
                                  final List<JsonValue> expectedResultPayloads) {

        final List<JsonEnvelope> collect = jsonEnvelopeStream.collect(Collectors.toList());
        int idx = 0;
        for (final JsonEnvelope jsonEnvelope : collect) {
            if (jsonEnvelope.metadata().name().equals(eventName)) {
                final JsonNode actualEvent = generatedEventAsJsonNode(jsonEnvelope.payloadAsJsonObject());
                assertThat(actualEvent, equalTo(generatedEventAsJsonNode(expectedResultPayloads.get(idx))));
                idx++;
            }
        }

        assertThat(collect.size(), equalTo(idx));
    }

    public static Metadata metadataFor(final String commandName, final UUID commandId) {
        return metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }

    public static Metadata metadataFor(final String commandName) {
        return metadataFrom(metadataFor(commandName, randomUUID()))
                .build();
    }

    public static JsonNode generatedEventAsJsonNode(final Object generatedEvent) {
        return OBJECT_MAPPER.valueToTree(generatedEvent);
    }

    public static <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }

}
