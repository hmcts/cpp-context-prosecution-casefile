package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived.manualCaseReceived;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionCaseUnsupported.publicProsecutionCaseUnsupported;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated.sjpProsecutionInitiated;

import uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.enterpriseid.mapper.EnterpriseIdService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.SjpProsecutionToSjpCaseConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssociateEnterpriseId;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiatedWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SjpProsecutionProcessor {

    private static final String SJP_COMMAND_CREATE_CASE = "sjp.create-sjp-case";
    private static final String PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private EnterpriseIdService enterpriseIdService;

    @Inject
    private SjpProsecutionToSjpCaseConverter sjpProsecutionToSjpCaseConverter;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Handles("prosecutioncasefile.events.sjp-prosecution-received")
    public void handleSjpProsecutionReceived(final Envelope<SjpProsecutionReceived> envelope) {
        final Metadata metadata = envelope.metadata();
        final Prosecution prosecution = envelope.payload().getProsecution();
        associateEnterpriseIdAndEmitEvent(metadata, prosecution);
    }

    @Handles("prosecutioncasefile.events.sjp-prosecution-received-with-warnings")
    public void handleSjpProsecutionReceivedWithWarnings(final Envelope<SjpProsecutionReceivedWithWarnings> envelope) {
        final Metadata metadata = envelope.metadata();
        final Prosecution prosecution = envelope.payload().getProsecution();
        associateEnterpriseIdAndEmitEvent(metadata, prosecution);
    }

    @Handles("prosecutioncasefile.events.sjp-prosecution-initiated")
    public void handleSjpProsecutionInitiated(final Envelope<SjpProsecutionInitiated> envelope) {
        createSjpCase(envelope.payload(), envelope.metadata());
    }

    @Handles("prosecutioncasefile.events.sjp-prosecution-initiated-with-warnings")
    public void handleSjpProsecutionInitiatedWithWarnings(final Envelope<SjpProsecutionInitiatedWithWarnings> envelope) {
        final SjpProsecutionInitiatedWithWarnings sjpProsecutionInitiatedWithWarnings = envelope.payload();

        final SjpProsecutionInitiated sjpProsecutionInitiated = sjpProsecutionInitiated()
                .withEnterpriseId(sjpProsecutionInitiatedWithWarnings.getEnterpriseId())
                .withProsecution(sjpProsecutionInitiatedWithWarnings.getProsecution()).build();

        createSjpCase(sjpProsecutionInitiated, envelope.metadata());
    }

    @Handles("prosecutioncasefile.events.prosecution-case-unsupported")
    public void handleProsecutionCaseUnsupported(final Envelope<ProsecutionCaseUnsupported> envelope) {
        final ProsecutionCaseUnsupported payload = envelope.payload();
        final Metadata metadata =  metadataFrom(envelope.metadata())
                .withName("public.prosecutioncasefile.prosecution-case-unsupported")
                .build();

        final PublicProsecutionCaseUnsupported publicPayload = publicProsecutionCaseUnsupported()
                .withChannel(payload.getChannel())
                .withErrorMessage(payload.getErrorMessage())
                .withExternalId(payload.getExternalId())
                .withPoliceSystemId(payload.getPoliceSystemId())
                .withUrn(payload.getUrn())
                .build();
        sender.send(Envelope.envelopeFrom(metadata, publicPayload));
    }

    private void associateEnterpriseIdAndEmitEvent(Metadata metadata, Prosecution prosecution) {
        final UUID caseId = prosecution.getCaseDetails().getCaseId();
        final String prosecutorCaseReference = prosecution.getCaseDetails().getProsecutorCaseReference();

        associateEnterpriseId(caseId, metadata);

        if (prosecution.getChannel() == MCC) {
            emitMCCPublicEvent(metadata, caseId, prosecutorCaseReference);
        }
    }

    private void associateEnterpriseId(final UUID caseId, final Metadata metadata) {
        final String enterpriseId = enterpriseIdService.enterpriseIdForCase(caseId);

        sender.send(envelopeFrom(
                metadataFrom(metadata).withName("prosecutioncasefile.command.associate-enterprise-id"),
                new AssociateEnterpriseId(caseId, enterpriseId)));
    }

    private void createSjpCase(final SjpProsecutionInitiated sjpProsecutionInitiated, final Metadata metadata) {
        final CreateSjpCase sjpCase = sjpProsecutionToSjpCaseConverter.convert(sjpProsecutionInitiated);
        sender.sendAsAdmin(envelopeHelper.withMetadataInPayload(
                envelopeFrom(
                        metadataFrom(metadata).withName(SJP_COMMAND_CREATE_CASE),
                        objectToJsonObjectConverter.convert(sjpCase))));
    }

    private void emitMCCPublicEvent(Metadata metadata, UUID caseId, String prosecutorCaseReference) {
        final ManualCaseReceived manualCaseReceived = manualCaseReceived()
                .withCaseId(caseId)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .build();

        final Metadata sjpCaseMetadata = metadataFrom(metadata)
                .withName(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED)
                .build();
        sender.send(Envelope.envelopeFrom(sjpCaseMetadata, objectToJsonObjectConverter.convert(manualCaseReceived)));
    }


}
