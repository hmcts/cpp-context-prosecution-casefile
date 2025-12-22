package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToInitiateCourtProceedingsConverter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupCasesReceivedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCasesReceivedEventProcessor.class);
    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES = "progression.initiate-court-proceedings-for-group-cases";

    @Inject
    private Sender sender;

    @Inject
    private GroupCasesReceivedToInitiateCourtProceedingsConverter groupCasesReceivedToInitiateCourtProceedingsConverter;

    @Handles("prosecutioncasefile.events.group-cases-received")
    public void handleGroupCasesReceived(final Envelope<GroupCasesReceived> envelope) {

        final GroupCasesReceived payload = envelope.payload();
        LOGGER.info("Posting {} for submission id {}  and calling {} ", PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES, payload.getGroupProsecutionList().getExternalId(), PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES)
                .build();

        final Envelope<InitiateCourtProceedingsForGroupCases> commandEnvelope = envelopeFrom(metadata, this.groupCasesReceivedToInitiateCourtProceedingsConverter.convert(payload));
        this.sender.send(commandEnvelope);
    }


}