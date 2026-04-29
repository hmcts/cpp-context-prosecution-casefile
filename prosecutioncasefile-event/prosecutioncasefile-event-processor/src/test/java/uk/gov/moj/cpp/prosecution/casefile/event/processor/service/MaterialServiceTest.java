package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialServiceTest {

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Envelope envelope;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private MaterialService materialService;

    @Test
    public void shouldUploadMaterial() {
        final UUID fileServiceId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID userId = randomUUID();

        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(of(userId.toString()));

        materialService.uploadMaterial(fileServiceId, materialId, envelope);

        verify(sender).send(any(JsonEnvelope.class));
    }
}