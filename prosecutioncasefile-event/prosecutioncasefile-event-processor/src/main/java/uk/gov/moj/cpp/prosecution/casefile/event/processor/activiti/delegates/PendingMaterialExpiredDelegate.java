package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;

@Named
public class PendingMaterialExpiredDelegate implements JavaDelegate {

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {
        final UUID fileStoreId = UUID.fromString(execution.getProcessBusinessKey());
        final UUID caseId = execution.getVariable("caseId", UUID.class);
        final Metadata metadata = metadataFromString(execution.getVariable("metadata", String.class));

        final JsonObject expirePendingMaterialJsonObject = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("fileStoreId", fileStoreId.toString())
                .build();


        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadata).withName("prosecutioncasefile.command.expire-pending-material"), expirePendingMaterialJsonObject);
        sender.sendAsAdmin(envelope);
    }
}
