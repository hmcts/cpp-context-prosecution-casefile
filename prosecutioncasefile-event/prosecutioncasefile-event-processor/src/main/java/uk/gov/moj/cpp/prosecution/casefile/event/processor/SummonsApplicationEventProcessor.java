package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived.manualCaseReceived;

import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.DefendantsParkedToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SummonsApplicationEventProcessor {

    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION = "progression.initiate-court-proceedings-for-application";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";

    @Inject
    private Sender sender;

    @Inject
    private DefendantsParkedToCourtApplicationProceedingsConverter converter;

    @Handles("prosecutioncasefile.events.defendants-parked-for-summons-application-approval")
    public void handleDefendantsParkedForSummonsApplicationApproval(final Envelope<DefendantsParkedForSummonsApplicationApproval> envelope) {
        final DefendantsParkedForSummonsApplicationApproval payload = envelope.payload();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION)
                .build();

        final Envelope<InitiateCourtApplicationProceedings> commandEnvelope = envelopeFrom(metadata, this.converter.convert(payload, envelope.metadata()));
        this.sender.send(commandEnvelope);

        final Prosecution prosecution = payload.getProsecutionWithReferenceData().getProsecution();
        if (MCC == prosecution.getChannel()) {
            final CaseDetails caseDetails = prosecution.getCaseDetails();
            emitPublicEventForMCC(envelope.metadata(), caseDetails.getCaseId(), payload.getApplicationId(), caseDetails.getProsecutorCaseReference());
        }
    }

    private void emitPublicEventForMCC(final Metadata metadata, final UUID caseId, final UUID applicationId, final String prosecutorCaseReference) {
        final Metadata publicEventMetadata = JsonEnvelope.metadataFrom(metadata)
                .withName(PUBLIC_EVENT_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED)
                .build();

        final ManualCaseReceived manualCaseReceived = manualCaseReceived()
                .withCaseId(caseId)
                .withApplicationId(applicationId)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .build();
        this.sender.send(envelopeFrom(publicEventMetadata, manualCaseReceived));
    }
}
