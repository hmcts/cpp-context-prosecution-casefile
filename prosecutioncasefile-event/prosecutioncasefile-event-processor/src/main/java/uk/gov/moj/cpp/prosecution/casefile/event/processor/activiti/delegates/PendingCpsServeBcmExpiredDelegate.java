package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

@Named
public class PendingCpsServeBcmExpiredDelegate implements JavaDelegate {

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {
        final UUID timerUUID = fromString(execution.getProcessBusinessKey());

        final Metadata metadata = JsonEnvelope
                .metadataFrom(metadataFromString(execution.getVariable("metadata", String.class)))
                .withName("prosecutioncasefile.command.cps-reject-bcm-for-timer-expire")
                .build();

        sender.send(Envelope.envelopeFrom(metadata, createObjectBuilder()
                .add("timerUUID", timerUUID.toString())
                .build()));
    }
}
