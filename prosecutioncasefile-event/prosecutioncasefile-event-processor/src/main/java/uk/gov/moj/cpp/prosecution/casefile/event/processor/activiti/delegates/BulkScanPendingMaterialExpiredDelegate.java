package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingMaterial.expirePendingMaterial;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingMaterial;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

@Named
public class BulkScanPendingMaterialExpiredDelegate implements JavaDelegate {

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Override
    public void execute(final DelegateExecution execution) {
        final UUID fileStoreId = UUID.fromString(execution.getProcessBusinessKey());
        final UUID caseId = execution.getVariable("caseId", UUID.class);
        final Metadata metadata = metadataFromString(execution.getVariable("metadata", String.class));

        final ExpirePendingMaterial expirePendingMaterial = expirePendingMaterial()
                .withCaseId(caseId)
                .withFileStoreId(fileStoreId)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadata).withName("prosecutioncasefile.command.expire-bulk-scan-pending-material"), objectToJsonObjectConverter.convert(expirePendingMaterial));

        sender.sendAsAdmin(envelope);

    }
}
