package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingIdpcMaterial;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class ExpirePendingIdpcMaterialHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private Clock clock;

    @Handles("prosecutioncasefile.command.expire-pending-idpc-material")
    public void expirePendingIdpcMaterial(final Envelope<ExpirePendingIdpcMaterial> expirePendingIdpcMaterialCommand) throws EventStreamException {
        final ExpirePendingIdpcMaterial expirePendingIdpcMaterial = expirePendingIdpcMaterialCommand.payload();

        appendEventsToStream(expirePendingIdpcMaterial.getCaseId(), expirePendingIdpcMaterialCommand, prosecutionCaseFile ->
                prosecutionCaseFile.expirePendingIdpcMaterial(expirePendingIdpcMaterial.getFileServiceId(), clock.now()));
    }
}
