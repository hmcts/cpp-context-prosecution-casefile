package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cps.prosecutioncasefile.command.api.ResolveErrors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResolveErrorsApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private ResolveErrors resolveErrors;

    @Captor
    private ArgumentCaptor<Envelope<ResolveErrors>> envelopeArgumentCaptor;

    @InjectMocks
    private ResolveErrorsApi resolveErrorsApi;

    @Test
    public void updateErrors() {
        final MetadataBuilder metadataBuilder = DefaultJsonMetadata.metadataBuilder();
        metadataBuilder.withName("prosecutioncasefile.command.resolve-errors").withId(randomUUID());
        when(envelope.metadata()).thenReturn(metadataBuilder.build());
        resolveErrorsApi.resolveErrors(envelope);
        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("prosecutioncasefile.command.handler.resolve-errors"));
    }
}