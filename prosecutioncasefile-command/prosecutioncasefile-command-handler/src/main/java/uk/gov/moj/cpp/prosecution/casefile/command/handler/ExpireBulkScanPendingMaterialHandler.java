package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpireBulkScanPendingMaterial;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class ExpireBulkScanPendingMaterialHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private Clock clock;

    @Handles("prosecutioncasefile.command.expire-bulk-scan-pending-material")
    public void expirePendingMaterial(final Envelope<ExpireBulkScanPendingMaterial> expirePendingMaterialCommand) throws EventStreamException {
        final ExpireBulkScanPendingMaterial expirePendingMaterial = expirePendingMaterialCommand.payload();

        appendEventsToStream(expirePendingMaterial.getCaseId(), expirePendingMaterialCommand, prosecutionCaseFile ->
                prosecutionCaseFile.expireBulkScanPendingMaterial(expirePendingMaterial.getFileStoreId(), clock.now()));
    }
}
