package uk.gov.moj.cpp.prosecution.casefile.command.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordGroupIdForSummonsApplication;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

@ServiceComponent(COMMAND_HANDLER)
public class RecordGroupIdForSummonsApplicationHandler extends BaseProsecutionCaseFileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordGroupIdForSummonsApplicationHandler.class);

    @Handles("prosecutioncasefile.command.record-group-id-for-summons-application")
    public void handleProsecutionCaseFiltered(final Envelope<RecordGroupIdForSummonsApplication> command) throws EventStreamException {

        final RecordGroupIdForSummonsApplication payload = command.payload();
        LOGGER.info("prosecutioncasefile.command.record-group-id-for-summons-application caseId: {}, groupId: {}",payload.getCaseId(), payload.getGroupId());

        appendEventsToStream(payload.getCaseId(), command, prosecutionCaseFile ->
                prosecutionCaseFile.recordGroupIdForSummonsApplication(payload.getCaseId(), payload.getGroupId()));

    }
}
