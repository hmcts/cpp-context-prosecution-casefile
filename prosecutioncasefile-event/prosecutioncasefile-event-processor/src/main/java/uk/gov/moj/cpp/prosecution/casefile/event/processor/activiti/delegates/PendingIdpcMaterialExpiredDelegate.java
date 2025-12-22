package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

@SuppressWarnings("WeakerAccess")
@Named
public class PendingIdpcMaterialExpiredDelegate implements JavaDelegate {

    @Inject
    private Enveloper enveloper;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {
        final UUID fileStoreId = fromString(execution.getProcessBusinessKey());
        final UUID caseId = execution.getVariable("caseId", UUID.class);
        final Metadata metadata = metadataFromString(execution.getVariable("metadata", String.class));
        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("fileServiceId", fileStoreId.toString())
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadata).withName("prosecutioncasefile.command.expire-pending-idpc-material"), payload);

        sender.sendAsAdmin(envelope);
    }
}
