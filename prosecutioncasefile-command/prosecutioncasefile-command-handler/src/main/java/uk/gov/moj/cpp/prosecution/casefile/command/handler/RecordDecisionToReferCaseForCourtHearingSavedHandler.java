package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncasefile.command.handler.RecordDecisionToReferCaseForCourtHearingSaved;


@ServiceComponent(Component.COMMAND_HANDLER)
public class RecordDecisionToReferCaseForCourtHearingSavedHandler extends BaseProsecutionCaseFileHandler {

    @Handles("prosecutioncasefile.command.record-decision-to-refer-case-for-court-hearing-saved")
    public void handleRecordDecisionToReferCaseForCaseHearing(final Envelope<RecordDecisionToReferCaseForCourtHearingSaved> envelope) throws EventStreamException {
        final RecordDecisionToReferCaseForCourtHearingSaved payload = envelope.payload();

        appendEventsToStream(payload.getCaseId(), envelope,
                prosecutionCaseFile -> prosecutionCaseFile.recordDecisionToReferCaseForCaseHearing(payload.getCaseId(), payload.getReferralReasonId()));
    }

}
