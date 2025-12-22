package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.envelopeWithIdpcProcessId;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class UploadFile implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadFile.class);

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {

        LOGGER.info("Task '{}' started for process {}", execution.getCurrentActivityName(), execution.getId());

        final String documentReference = execution.getVariable("documentReference", String.class);
        final String metadataAsString = execution.getVariable("metadata", String.class);
        final Metadata originalMetadata = metadataFromString(metadataAsString);

        final JsonObject uploadFileCommandPayload = Json.createObjectBuilder()
                .add("materialId", UUID.randomUUID().toString())
                .add("fileServiceId", documentReference)
                .build();

        final JsonEnvelope uploadFileCommand = envelopeWithIdpcProcessId(
                metadataFrom(originalMetadata).withName("material.command.upload-file").build(),
                uploadFileCommandPayload,
                execution.getId());

        sender.send(uploadFileCommand);
    }

}
