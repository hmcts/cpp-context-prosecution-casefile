package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicSjpCaseCreated;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpPublicEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private SjpPublicEventProcessor sjpPublicEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<AcceptCase>> captor;

    @Test
    public void shouldHandleSjpCaseCreated() {

        final Envelope<PublicSjpCaseCreated> sjpCaseCreatedEnvelope = sjpCaseCreatedEnvelope();

        sjpPublicEventProcessor.sjpCaseCreated(sjpCaseCreatedEnvelope);

        verify(sender, times(2)).send(captor.capture());

        final List<Envelope<AcceptCase>> envelopeToSender = captor.getAllValues();
        final Metadata metadata = envelopeToSender.get(0).metadata();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.accept-case"));
        assertThat(metadata.clientCorrelationId(), is(sjpCaseCreatedEnvelope.metadata().clientCorrelationId()));

        final AcceptCase payload = envelopeToSender.get(0).payload();
        assertThat(payload, is(notNullValue()));
        assertThat(payload.getCaseId(), is(sjpCaseCreatedEnvelope.payload().getId()));
    }

    private static Envelope<PublicSjpCaseCreated> sjpCaseCreatedEnvelope() {
        final PublicSjpCaseCreated sjpCaseCreated = PublicSjpCaseCreated.publicSjpCaseCreated()
                .withPostingDate(LocalDate.of(2018, 8, 2))
                .withId(randomUUID())
                .build();

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName("public.sjp.sjp-case-created")
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(sjpCaseCreated.getId())
                .withClientCorrelationId(randomUUID().toString());

        return envelopeFrom(metadataBuilder, sjpCaseCreated);
    }

}