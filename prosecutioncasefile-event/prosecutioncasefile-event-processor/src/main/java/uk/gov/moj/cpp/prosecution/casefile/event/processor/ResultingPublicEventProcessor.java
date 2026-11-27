package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncasefile.command.handler.RecordDecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.resulting.event.PublicDecisionToReferCaseForCourtHearingSaved;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ResultingPublicEventProcessor {

    @Inject
    private Sender sender;

    @Handles("public.resulting.decision-to-refer-case-for-court-hearing-saved")
    public void handleDecisionToReferCaseForCourtHearingSaved(final Envelope<PublicDecisionToReferCaseForCourtHearingSaved> envelope) {
        final PublicDecisionToReferCaseForCourtHearingSaved payload = envelope.payload();

        final Metadata metadata = JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.record-decision-to-refer-case-for-court-hearing-saved")
                .build();

        sender.send(Envelope.envelopeFrom(metadata, RecordDecisionToReferCaseForCourtHearingSaved
                .recordDecisionToReferCaseForCourtHearingSaved()
                .withCaseId(payload.getCaseId())
                .withReferralReasonId(payload.getReferralReasonId())
                .withDecisionSavedAt(payload.getDecisionSavedAt())
                .withEstimatedHearingDuration(payload.getEstimatedHearingDuration())
                .withHearingTypeId(payload.getHearingTypeId())
                .withListingNotes(payload.getListingNotes())
                .withSessionId(payload.getSessionId())
                .build()));
    }

}
