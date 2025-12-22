package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaSubmitted;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class OnlinePleaEventProcessorTest {
    @InjectMocks
    private OnlinePleaEventProcessor onlinePleaEventProcessor;
    @Mock
    private EnvelopeHelper envelopeHelper;
    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;
    @Mock
    private Sender sender;
    @Mock
    private MetadataHelper metadataHelper;
    @Captor
    private ArgumentCaptor<Envelope<PleadOnline>> pleadOnlineEnvelopeCaptor;

    @Test
    public void shouldHandleOnlinePleaSubmitted() {
        final Envelope<OnlinePleaSubmitted> onlinePleaSubmittedEnvelope = createOnlinePleaSubmittedEvent(InitiationCode.C);
        onlinePleaEventProcessor.handleOnlinePleaSubmitted(onlinePleaSubmittedEnvelope);

        verify(sender).send(pleadOnlineEnvelopeCaptor.capture());
        final Envelope<PleadOnline> envelopeToSender = pleadOnlineEnvelopeCaptor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("progression.plead-online"));
    }

    @Test
    public void shouldHandleOnlinePleaSubmittedForSJP() {
        final Envelope<OnlinePleaSubmitted> onlinePleaSubmittedEnvelope = createOnlinePleaSubmittedEvent(InitiationCode.J);
        onlinePleaEventProcessor.handleOnlinePleaSubmitted(onlinePleaSubmittedEnvelope);

        verify(sender).send(pleadOnlineEnvelopeCaptor.capture());
        final Envelope<PleadOnline> envelopeToSender = pleadOnlineEnvelopeCaptor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("public.prosecutioncasefile.sjp-plead-online"));
    }

    private static Envelope<OnlinePleaSubmitted> createOnlinePleaSubmittedEvent(final InitiationCode initiationCode) {
        final UUID caseId = randomUUID();
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.online-plea-submitted"),
                OnlinePleaSubmitted.onlinePleaSubmitted()
                        .withCaseId(caseId)
                        .withCreatedBy(randomUUID())
                        .withReceivedDateTime(ZonedDateTime.now())
                        .withPleadOnline(PleadOnline.pleadOnline()
                                .withCaseId(caseId)
                                .withDefendantId(randomUUID())
                                .withComeToCourt(true)
                                .withInitiationCode(initiationCode)
                                .build())
                        .build());
    }
}
