package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CaseDefendantChangedCommand;

import uk.gov.moj.cps.prosecutioncasefile.command.handler.UpdateCaseWithDefendant;

@ServiceComponent(COMMAND_HANDLER)
public class DefendantChangedHandler extends BaseProsecutionCaseFileHandler {

    @Handles("prosecutioncasefile.command.case-defendant-changed")
    public void handleCaseDefendantChanged(final Envelope<CaseDefendantChangedCommand> envelope) throws EventStreamException {
        final CaseDefendantChangedCommand caseDefendantChanged = envelope.payload();
        appendEventsToStream(caseDefendantChanged.getDefendantId(), envelope, prosecutionCaseFile ->
                prosecutionCaseFile.updateDefendant1( caseDefendantChanged.getDefendantId(), caseDefendantChanged.getDateOfBirth(), caseDefendantChanged.getPersonDetails()));
    }

    @Handles("prosecutioncasefile.command.update-case-with-defendant")
    public void handleUpdateCaseWithDefendant(final Envelope<UpdateCaseWithDefendant> envelope) throws EventStreamException {
        final UpdateCaseWithDefendant updateCaseWithDefendant = envelope.payload();
        appendEventsToStream(updateCaseWithDefendant.getCaseId(), envelope, prosecutionCaseFile ->
                prosecutionCaseFile.updateDefendant(updateCaseWithDefendant.getCaseId(), updateCaseWithDefendant.getDefendantId(), updateCaseWithDefendant.getDateOfBirth(), updateCaseWithDefendant.getPersonDetails(), updateCaseWithDefendant.getOrganisationName()));
    }
}