package uk.gov.moj.cpp.prosecution.casefile.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase.acceptCase;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicCaseAssigned;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicCaseUnassigned;
import uk.gov.moj.cpp.sjp.json.schema.event.PublicSjpCaseCreated;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssignCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.UnassignCase;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SjpPublicEventProcessor {

    private static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";

    @Inject
    private Sender sender;

    @Handles("public.sjp.sjp-case-created")
    public void sjpCaseCreated(final Envelope<PublicSjpCaseCreated> sjpCaseCreatedEnvelope) {

        final AcceptCase acceptCase = acceptCase()
                .withCaseId(sjpCaseCreatedEnvelope.payload().getId())
                .build();

        final Metadata metadata = metadataFrom(sjpCaseCreatedEnvelope.metadata())
                .withName("prosecutioncasefile.command.accept-case")
                .build();

        sender.send(envelopeFrom(metadata, acceptCase));

        final Metadata sjpCaseUpdateMetadata = metadataFrom(sjpCaseCreatedEnvelope.metadata())
                .withName(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH)
                .build();

        sender.send(envelopeFrom(sjpCaseUpdateMetadata, acceptCase));
    }

    @Handles("public.sjp.case-assigned")
    public void sjpCaseAssigned(final Envelope<PublicCaseAssigned> caseAssignedEnvelope) {
        final AssignCase assignCase = AssignCase.assignCase().withCaseId(caseAssignedEnvelope.payload().getCaseId()).build();

        final Metadata metadata = metadataFrom(caseAssignedEnvelope.metadata())
                .withName("prosecutioncasefile.command.assign-case")
                .build();

        sender.send(envelopeFrom(metadata, assignCase));
    }

    @Handles("public.sjp.case-unassigned")
    public void sjpCaseUnAssigned(final Envelope<PublicCaseUnassigned> caseUnassignedEnvelope) {
        final UnassignCase unassignCase = UnassignCase.unassignCase().withCaseId(caseUnassignedEnvelope.payload().getCaseId()).build();

        final Metadata metadata = metadataFrom(caseUnassignedEnvelope.metadata())
                .withName("prosecutioncasefile.command.unassign-case")
                .build();

        sender.send(envelopeFrom(metadata, unassignCase));
    }
}
