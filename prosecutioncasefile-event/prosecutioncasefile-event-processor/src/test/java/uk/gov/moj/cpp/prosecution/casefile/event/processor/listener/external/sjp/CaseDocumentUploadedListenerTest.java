package uk.gov.moj.cpp.prosecution.casefile.event.processor.listener.external.sjp;

import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentUploadedListenerTest {
    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeArgumentCaptor;

    @InjectMocks
    private CaseDocumentUploadedListener testObj;

    @Test
    public void shouldProcessRecordUploadCaseDocument() {

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.record-upload-case-document"),
                Json.createObjectBuilder().build());

        testObj.handleSjpCaseDocumentUploaded(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

    }
}