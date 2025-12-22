package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.time.Clock.systemDefaultZone;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantIdpcEventProcessorTest {

    @Captor
    private ArgumentCaptor<JsonEnvelope> captor;

    @Mock
    private Sender sender;

    @InjectMocks
    private DefendantIdpcEventProcessor defendantIdpcEventProcessor;

    @Test
    public void testHandleDefendantIdpcAdded() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseDocumentId = randomUUID();
        final UUID caseDocumentMaterialId = randomUUID();
        final ZonedDateTime publishedDate = now(systemDefaultZone());

        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.events.defendant-idpc-added")
                .withId(randomUUID())
                .build();

        final DefendantIdpcAdded defendantIdpcAdded = new DefendantIdpcAdded(caseId,
                new CaseDocument(caseDocumentId, caseDocumentMaterialId, "", publishedDate),
                defendantId);

        defendantIdpcEventProcessor.handleDefendantIdpcAdded(envelopeFrom(metadata, defendantIdpcAdded));

        verify(sender).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("public.prosecutioncasefile.defendant-idpc-added"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("defendantId"), is(defendantId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("materialId"), is(caseDocumentMaterialId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("publishedDate"), is(publishedDate.toLocalDate().toString()));
    }
}