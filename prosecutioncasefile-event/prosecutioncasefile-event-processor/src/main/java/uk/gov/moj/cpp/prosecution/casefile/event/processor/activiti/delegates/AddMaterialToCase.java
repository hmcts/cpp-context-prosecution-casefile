package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class AddMaterialToCase implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddMaterialToCase.class);

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {

        LOGGER.info("Task '{}' started for process {}", execution.getCurrentActivityName(), execution.getId());

        final String metadataAsString = execution.getVariable("metadata", String.class);
        final String caseId = execution.getVariable("caseId", String.class);
        final String documentReference = execution.getVariable("documentReference", String.class);
        final String documentType = execution.getVariable("documentType", String.class);
        final String materialId = execution.getVariable("materialId", String.class);
        final String defendantId = execution.getVariable("defendantId", String.class);

        final Metadata metadata = metadataFromString(metadataAsString);

        final JsonObject payload = Json.createObjectBuilder()
                .add("id", documentReference)
                .add("caseId", caseId)
                .add("materialId", materialId)
                .add("documentType", documentType)
                .add("defendantId", defendantId)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadata).withName("prosecutioncasefile.command.add-defendant-idpc"), payload);

        sender.send(envelope);
    }

}
