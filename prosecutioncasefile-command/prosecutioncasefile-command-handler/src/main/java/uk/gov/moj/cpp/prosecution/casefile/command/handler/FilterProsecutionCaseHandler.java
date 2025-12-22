package uk.gov.moj.cpp.prosecution.casefile.command.handler;


import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.FilterProsecutionCase;

import java.util.UUID;

@ServiceComponent(COMMAND_HANDLER)
public class FilterProsecutionCaseHandler extends BaseProsecutionCaseFileHandler {

    @Handles("prosecutioncasefile.command.filter-prosecution-case")
    public void handleProsecutionCaseFiltered(final Envelope<FilterProsecutionCase> command) throws EventStreamException {

        final FilterProsecutionCase filterProsecutionCase = command.payload();

        final UUID caseId = filterProsecutionCase.getCaseId();

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
                prosecutionCaseFile.filterCase(caseId));

    }
}

