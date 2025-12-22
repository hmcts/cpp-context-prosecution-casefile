package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase.acceptCase;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded.prosecutionSubmissionSucceeded;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileDefendantToDefenceDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionToCCAddDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CheckPendingEventsForNewDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseUpdatedWithDefendant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionDefendantsAddedEventProcessor {

    private static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";
    private static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED = "public.prosecutioncasefile.prosecution-defendants-added";
    private static final String PROGRESSION_ADD_DEFENDANT_TO_COURTS = "progression.add-defendants-to-court-proceedings";
    private static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    public static final String PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS = "prosecutioncasefile.command.check-pending-events-for-new-defendants";

    @Inject
    private Sender sender;

    @Inject
    private ProsecutionToCCAddDefendantConverter prosecutionToCCAddDefendantConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private ProsecutionCaseFileDefendantToDefenceDefendantConverter prosecutionCaseFileDefendantToDefenceDefendantConverter;

    @Handles("prosecutioncasefile.events.prosecution-defendants-added")
    public void handleProsecutionDefendantsAdded(final Envelope<ProsecutionDefendantsAdded> envelope) {

        final ProsecutionDefendantsAdded payload = envelope.payload();

        addDefendantsToCourtProceedings(envelope);

        if (SPI.equals(payload.getChannel())) {
            raiseIdpcCommand(envelope);
        }

        raiseProsecutionDefendantsAddedPublicEvent(envelope);

        if (isEmpty(payload.getDefendantWarnings())) {
            raiseProsecutionSubmissionSucceeded(envelope);
        } else {
            raiseProsecutionSubmissionSucceededWithWarnings(envelope);
        }

        final Metadata prosecutionDefendantsAddedMetadata = metadataFrom(envelope.metadata())
                .withName(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS)
                .build();

        sender.send(envelopeFrom(prosecutionDefendantsAddedMetadata, CheckPendingEventsForNewDefendants.checkPendingEventsForNewDefendants().withCaseId(payload.getCaseId()).build()));
    }

    @Handles("prosecutioncasefile.events.case-updated-with-defendant")
    public void handleProsecutionDefendantsChanged(final Envelope<CaseUpdatedWithDefendant> envelope) {

        final CaseUpdatedWithDefendant payload = envelope.payload();

        final Metadata prosecutionDefendantsAddedMetadata = metadataFrom(envelope.metadata())
                .withName(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS)
                .build();

        sender.send(envelopeFrom(prosecutionDefendantsAddedMetadata, CheckPendingEventsForNewDefendants.checkPendingEventsForNewDefendants().withCaseId(payload.getCaseId()).build()));
    }

    private void addDefendantsToCourtProceedings(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = prosecutionToCCAddDefendantConverter.convert(envelope.payload());

        final JsonEnvelope jsonEnvelope =
                envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PROGRESSION_ADD_DEFENDANT_TO_COURTS), objectToJsonObjectConverter.convert(addDefendantsToCourtProceedings));

        sender.send(jsonEnvelope);
    }

    private void raiseIdpcCommand(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final AcceptCase acceptCase = acceptCase()
                .withCaseId(envelope.payload().getCaseId())
                .build();

        final Metadata prosecutionDefendantsAddedMetadata = metadataFrom(envelope.metadata())
                .withName(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH)
                .build();

        sender.send(envelopeFrom(prosecutionDefendantsAddedMetadata, acceptCase));
    }

    private void raiseProsecutionDefendantsAddedPublicEvent(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final ProsecutionDefendantsAdded payload = envelope.payload();
        final Metadata publicEventMetadata = metadataFrom(envelope.metadata())
                .withName(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED)
                .build();

        final uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.ProsecutionDefendantsAdded prosecutionDefendantsAdded = uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.ProsecutionDefendantsAdded
                .prosecutionDefendantsAdded()
                .withCaseId(payload.getCaseId())
                .withDefendants(prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(payload.getDefendants()))
                .withChannel(payload.getChannel())
                .build();

        sender.send(envelopeFrom(publicEventMetadata, prosecutionDefendantsAdded));
    }

    private void raiseProsecutionSubmissionSucceeded(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final ProsecutionDefendantsAdded payload = envelope.payload();
        final MetadataBuilder builder = metadataFrom(envelope.metadata());
        final Metadata metadata = builder.withName(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED).build();

        final ProsecutionSubmissionSucceeded prosecutionSubmissionSucceeded = prosecutionSubmissionSucceeded()
                .withCaseId(payload.getCaseId())
                .withExternalId(payload.getExternalId())
                .withChannel(payload.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, prosecutionSubmissionSucceeded));
    }

    private void raiseProsecutionSubmissionSucceededWithWarnings(final Envelope<ProsecutionDefendantsAdded> envelope) {
        final ProsecutionDefendantsAdded payload = envelope.payload();
        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        final Metadata metadata = builder.withName(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED).build();

        final ProsecutionSubmissionSucceededWithWarnings prosecutionSubmissionSucceededWithWarnings = prosecutionSubmissionSucceededWithWarnings()
                .withCaseId(payload.getCaseId())
                .withDefendantWarnings(payload.getDefendantWarnings())
                .withExternalId(payload.getExternalId())
                .withChannel(payload.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, prosecutionSubmissionSucceededWithWarnings));
    }
}
