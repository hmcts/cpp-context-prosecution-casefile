package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ApplicationAcceptedToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.ApplicationParameters;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;
import java.util.UUID;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationEventProcessor.class);
    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION = "progression.initiate-court-proceedings-for-application";
    public static final String PUBLIC_PROSECUTIONCASEFILE_SUBMIT_APPLICATION_VALIDATION_FAILED = "public.prosecutioncasefile.submit-application-validation-failed";
    private static final String NOTIFICATION_NOTIFY_SEND_EMAIL_NOTIFICATION = "notificationnotify.send-email-notification";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SENDER_ADDRESS = "sendToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String SUBJECT = "subject";

    @Inject
    private ApplicationAcceptedToCourtApplicationProceedingsConverter converter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private Sender sender;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Handles("prosecutioncasefile.events.submit-application-accepted")
    public void handleSubmitApplicationAccepted(final Envelope<SubmitApplicationAccepted> envelope) {
        final SubmitApplicationAccepted sourcePayload = envelope.payload();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION)
                .build();

        final Envelope<InitiateCourtApplicationProceedings> commandEnvelope = envelopeFrom(metadata, converter.convert(sourcePayload));
        sender.send(commandEnvelope);
    }

    @Handles("prosecutioncasefile.events.submit-application-validation-failed")
    public void handleSubmitApplicationValidationFailed(final Envelope<SubmitApplicationValidationFailed> envelope) {
        final SubmitApplicationValidationFailed payload = envelope.payload();

        if (nonNull(payload.getSenderEmail())) {
            sendValidationFailedEmailNotification(envelope, payload);
        } else {

            final Metadata metadata = metadataFrom(envelope.metadata())
                    .withName(PUBLIC_PROSECUTIONCASEFILE_SUBMIT_APPLICATION_VALIDATION_FAILED)
                    .build();

            sender.send(Envelope.envelopeFrom(metadata, envelope.payload()));
        }
    }

    private void sendValidationFailedEmailNotification(final Envelope envelope, final SubmitApplicationValidationFailed payload) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Sending validation failed email notification for ApplicationId: {} ", payload.getApplicationSubmitted().getCourtApplication().getId());
        }

        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, UUID.randomUUID().toString());
        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, applicationParameters.getEmailTemplateId());
        notifyObjectBuilder.add(SENDER_ADDRESS, payload.getSenderEmail());

        final JsonObjectBuilder personalisationObjectBuilder = createObjectBuilder();
        personalisationObjectBuilder.add(SUBJECT, payload.getEmailSubject());
        notifyObjectBuilder.add(PERSONALISATION, personalisationObjectBuilder.build());

        final Metadata metadata = Envelope.metadataFrom(envelope.metadata()).withName(NOTIFICATION_NOTIFY_SEND_EMAIL_NOTIFICATION).build();

        sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelopeFrom(metadata, notifyObjectBuilder.build())));

    }
}
