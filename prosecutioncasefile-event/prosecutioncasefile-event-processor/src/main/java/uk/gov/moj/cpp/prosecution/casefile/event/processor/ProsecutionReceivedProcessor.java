package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceCaseDetails.defenceCaseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase.acceptCase;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived.manualCaseReceived;

import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileDefendantToDefenceDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.CCCaseToProsecutionCaseConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceCaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ValidationCompleted;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionReceivedProcessor {

    private static final String PUBLIC_PROSECUTIONCASEFILE_EVENTS_VALIDATION_COMPLETE = "public.prosecutioncasefile.events.validation-completed";
    private static final String PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";
    private static final String PROGRESSION_INITIATE_COURT_PROCEEDINGS = "progression.initiate-court-proceedings";
    private static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED = "public.prosecutioncasefile.cc-case-received";

    @Inject
    private Sender sender;

    @Inject
    private CCCaseToProsecutionCaseConverter ccCaseToProsecutionCaseConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private ProsecutionCaseFileDefendantToDefenceDefendantConverter prosecutionCaseFileDefendantToDefenceDefendantConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionReceivedProcessor.class);

    @Handles("prosecutioncasefile.events.cc-case-received")
    public void handleCcCaseReceived(final Envelope<CcCaseReceived> envelope) {

        LOGGER.info("Received prosecutioncasefile.events.cc-case-received- {} ", envelope.payload());
        final CcCaseReceived ccCaseReceived = envelope.payload();
        final Prosecution prosecution = ccCaseReceived.getProsecutionWithReferenceData().getProsecution();
        final InitiateCourtProceedings initiateCourtProceedings = ccCaseToProsecutionCaseConverter.convert(ccCaseReceived);

        final JsonEnvelope envelope1 = envelopeHelper.withMetadataInPayload(
                envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_INITIATE_COURT_PROCEEDINGS), objectToJsonObjectConverter.convert(initiateCourtProceedings)));

        LOGGER.info("Sending progression.initiate-court-proceedings sending envelope- {} ", envelope1.payload());

        sender.sendAsAdmin(envelope1);

        final AcceptCase acceptCase = acceptCase()
                .withCaseId(prosecution.getCaseDetails().getCaseId())
                .build();

        final Channel channel = prosecution.getChannel();

        if (channel == SPI) {
            final Metadata metadata = envelope.metadata();
            final Metadata sjpCaseUpdateMetadata = getMetadata(metadata, PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH);
            sender.send(envelopeFrom(sjpCaseUpdateMetadata, acceptCase));
        }

        final UUID caseId = prosecution.getCaseDetails().getCaseId();
        final String prosecutorCaseReference = prosecution.getCaseDetails().getProsecutorCaseReference();

        final uk.gov.moj.cps.prosecutioncasefile.domain.event.CcCaseReceived publicEvent = getCcCaseReceived(prosecution, channel, caseId, prosecutorCaseReference, ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getProsecutorsReferenceData());

        final Metadata metadata = envelope.metadata();

        emitPublicEvent(publicEvent, envelope);

        if (channel == MCC) {
            emitPublicEventForMCC(getMetadata(metadata, PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED), caseId, prosecutorCaseReference);
        }
    }

    @Handles("prosecutioncasefile.events.cc-case-received-with-warnings")
    public void handleCcCaseReceivedWithWarnings(final Envelope<CcCaseReceivedWithWarnings> envelope) {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = envelope.payload().getProsecutionWithReferenceData();
        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();

        final InitiateCourtProceedings initiateCourtProceedings = ccCaseToProsecutionCaseConverter.convert(
                CcCaseReceived.ccCaseReceived().withProsecutionWithReferenceData(prosecutionWithReferenceData)
                        .withId(envelope.payload().getId()).build());

        final JsonEnvelope envelope1 = envelopeHelper.withMetadataInPayload(
                envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_INITIATE_COURT_PROCEEDINGS), objectToJsonObjectConverter.convert(initiateCourtProceedings)));

        sender.sendAsAdmin(envelope1);

        final AcceptCase acceptCase = acceptCase()
                .withCaseId(prosecution.getCaseDetails().getCaseId())
                .build();

        final Channel channel = prosecution.getChannel();
        final UUID caseId = prosecution.getCaseDetails().getCaseId();
        final String prosecutorCaseReference = prosecution.getCaseDetails().getProsecutorCaseReference();

        if (channel == SPI) {
            final Metadata metadata = envelope.metadata();
            final Metadata sjpCaseUpdateMetadata = getMetadata(metadata, PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH);
            sender.send(envelopeFrom(sjpCaseUpdateMetadata, acceptCase));
        } else if (channel == MCC) {
            final Metadata metadata = envelope.metadata();

            final Metadata mccEventMetadata = getMetadata(metadata, PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED);
            emitPublicEventForMCC(mccEventMetadata, caseId, prosecutorCaseReference);
        }

        final uk.gov.moj.cps.prosecutioncasefile.domain.event.CcCaseReceived publicEvent = getCcCaseReceived(prosecution, channel, caseId, prosecutorCaseReference, prosecutionWithReferenceData.getReferenceDataVO().getProsecutorsReferenceData());
        emitPublicEvent(publicEvent, envelope);
    }

    private uk.gov.moj.cps.prosecutioncasefile.domain.event.CcCaseReceived getCcCaseReceived(final Prosecution prosecution, final Channel channel, final UUID caseId, final String prosecutorCaseReference, final ProsecutorsReferenceData prosecutorsReferenceData) {
        final DefenceCaseDetails defenceCaseDetails = defenceCaseDetails()
                .withCaseId(caseId)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .withProsecutor(prosecutor()
                        .withProsecutingAuthority(prosecutorsReferenceData.getShortName())
                        .withProsecutionAuthorityId(prosecutorsReferenceData.getId())
                        .build())
                .build();

        final List<Defendant> defendants = prosecution.getDefendants();

        return ccCaseReceived()
                .withCaseDetails(defenceCaseDetails)
                .withDefendants(prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(defendants))
                .withChannel(channel)
                .build();
    }

    private Metadata getMetadata(final Metadata metadata, final String eventName) {
        return metadataFrom(metadata)
                .withName(eventName)
                .build();
    }

    @Handles("prosecutioncasefile.events.validation-completed")
    public void handleValidationCompleted(final Envelope<ValidationCompleted> envelope) {
        final Metadata publicEventMetadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_PROSECUTIONCASEFILE_EVENTS_VALIDATION_COMPLETE)
                .build();
        sender.send(envelopeFrom(publicEventMetadata, envelope.payload()));
    }

    private void emitPublicEventForMCC(final Metadata metadata, final UUID caseId, final String prosecutorCaseReference) {
        final ManualCaseReceived manualCaseReceived = manualCaseReceived()
                .withCaseId(caseId)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .build();
        sender.send(envelopeFrom(metadata, objectToJsonObjectConverter.convert(manualCaseReceived)));
    }

    private void emitPublicEvent(final uk.gov.moj.cps.prosecutioncasefile.domain.event.CcCaseReceived publicEvent, final Envelope<?> envelope) {
        sender.send(envelop(publicEvent)
                .withName(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED)
                .withMetadataFrom(envelope));
    }
}
