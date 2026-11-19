package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordMigratedCase;

@ServiceComponent(COMMAND_HANDLER)
public class RecordMigratedCaseHandler extends BaseProsecutionCaseFileHandler {

    @Handles("prosecutioncasefile.command.record-migrated-case")
    public void handleRecordMigratedCase(final Envelope<RecordMigratedCase> envelope) throws EventStreamException {
        final RecordMigratedCase payload = envelope.payload();

        appendEventsToStream(payload.getCaseId(), envelope,
                prosecutionCaseFile -> prosecutionCaseFile.recordMigratedCase(
                        payload.getCaseId(),
                        payload.getProsecutionCaseReference(),
                        payload.getMigrationSourceSystemName(),
                        payload.getMigrationSourceSystemCaseIdentifier(),
                        payload.getDefendants()));
    }
}
