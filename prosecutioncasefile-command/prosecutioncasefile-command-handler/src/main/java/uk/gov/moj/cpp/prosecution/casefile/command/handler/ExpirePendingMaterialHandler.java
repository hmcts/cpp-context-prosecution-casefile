package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingMaterial;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class ExpirePendingMaterialHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private Clock clock;

    @Handles("prosecutioncasefile.command.expire-pending-material")
    public void expirePendingMaterial(final Envelope<ExpirePendingMaterial> expirePendingMaterialCommand) throws EventStreamException {
        final ExpirePendingMaterial expirePendingMaterial = expirePendingMaterialCommand.payload();

        appendEventsToStream(expirePendingMaterial.getCaseId(), expirePendingMaterialCommand, prosecutionCaseFile ->
                prosecutionCaseFile.expirePendingMaterial(expirePendingMaterial.getFileStoreId(), clock.now()));
    }
}
