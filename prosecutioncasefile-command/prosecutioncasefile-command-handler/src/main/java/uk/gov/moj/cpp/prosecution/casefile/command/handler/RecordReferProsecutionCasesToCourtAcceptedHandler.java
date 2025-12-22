package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncasefile.command.handler.RecordReferProsecutionCasesToCourtAccepted;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RecordReferProsecutionCasesToCourtAcceptedHandler extends BaseProsecutionCaseFileHandler {
    @Handles("prosecutioncasefile.command.record-refer-prosecution-cases-to-court-accepted")
    public void handleRecordReferProsecutionCasesToCourt(final Envelope<RecordReferProsecutionCasesToCourtAccepted> envelope) throws EventStreamException {
        final RecordReferProsecutionCasesToCourtAccepted payload = envelope.payload();

        appendEventsToStream(payload.getCaseId(), envelope,
                prosecutionCaseFile -> prosecutionCaseFile.recordDecisionToReferCaseForCaseHearing(payload.getCaseId(), payload.getReferralReasonId()));
    }
}
