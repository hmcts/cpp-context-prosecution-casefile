package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class DefendantIdpcEventProcessor {

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.defendant-idpc-added")
    public void handleDefendantIdpcAdded(final Envelope<DefendantIdpcAdded> defendantIdpcAddedEnvelope) {
        final DefendantIdpcAdded defendantIdpcAdded = defendantIdpcAddedEnvelope.payload();
        final UUID caseId = defendantIdpcAdded.getCaseId();
        final UUID defendantId = defendantIdpcAdded.getDefendantId();
        final UUID materialId = defendantIdpcAdded.getCaseDocument().getMaterialId();
        final ZonedDateTime addedAt = defendantIdpcAdded.getCaseDocument().getAddedAt();

        final Metadata metadata = metadataFrom(defendantIdpcAddedEnvelope.metadata())
                .withName("public.prosecutioncasefile.defendant-idpc-added")
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("materialId", materialId.toString())
                .add("defendantId", defendantId.toString())
                .add("publishedDate", addedAt.toLocalDate().toString())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }
}
