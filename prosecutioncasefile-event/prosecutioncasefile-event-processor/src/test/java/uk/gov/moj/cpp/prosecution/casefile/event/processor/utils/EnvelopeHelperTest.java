package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.METADATA;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.JsonObjects;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EnvelopeHelperTest {

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private EnvelopeHelper envelopeHelper;

    private UUID systemUserId = randomUUID();

    @BeforeEach
    public void init() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
    }

    @Test
    public void shouldAddMetadataToPayload() {
        final JsonObject payload = createObjectBuilder().add("random", "random").build();
        final Metadata metadata = metadataWithDefaults().build();
        final JsonEnvelope envelope = envelopeFrom(metadata, payload);

        final Metadata expectedMetadata = withSystemUserAndWithoutCausation(envelope.metadata(), systemUserId);
        final JsonObject expectedPayload = JsonObjects.createObjectBuilder(payload).add(METADATA, expectedMetadata.asJsonObject()).build();

        final JsonEnvelope actualEnvelope = envelopeHelper.withMetadataInPayload(envelope);

        assertThat(actualEnvelope.payloadAsJsonObject(), is(expectedPayload));
    }

    private Metadata withSystemUserAndWithoutCausation(final Metadata metadata, final UUID userId) {
        return metadataFrom(JsonObjects.createObjectBuilder(metadata.asJsonObject()).build()).withUserId(userId.toString()).build();
    }

}
