package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CourtApplicationCreatedFromProgression;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2583"})
@ServiceComponent(EVENT_PROCESSOR)
public class PocaEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PocaEventProcessor.class);
    private static final String NOTIFICATION_NOTIFY_SEND_EMAIL_NOTIFICATION = "notificationnotify.send-email-notification";
    private static final String PROSECUTION_CASEFILE_COMMAND_ADD_APPLICATION_MATERIAL = "prosecutioncasefile.command.add-application-material-v2";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String FIELD_NOTIFICATION_ID = "notificationId";

    @Inject
    private Sender sender;

    @Inject
    private EnvelopeHelper envelopeHelper;

    @Inject
    private FileRetriever fileRetriever;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @SuppressWarnings("squid:S2221")
    @Handles("prosecutioncasefile.events.court-application-created-from-progression")
    public void handleSubmitApplicationAccepted(final JsonEnvelope envelope) throws FileServiceException {
        LOGGER.info("'prosecutioncasefile.events.court-application-created-from-progression' received {}", envelope.toObfuscatedDebugString());

        final JsonObject eventPayload = envelope.payloadAsJsonObject();

        final CourtApplicationCreatedFromProgression courtApplicationCreatedFromProgression = jsonObjectToObjectConverter.convert(eventPayload, CourtApplicationCreatedFromProgression.class);
        final CourtApplication courtApplication = courtApplicationCreatedFromProgression.getCourtApplication();

        String fileName = StringUtils.EMPTY;
        String fileId = StringUtils.EMPTY;

        final Optional<FileReference> fileReferenceOptional = fileRetriever.retrieve(courtApplicationCreatedFromProgression.getPocaFileId());
        if (nonNull(fileReferenceOptional) && fileReferenceOptional.isPresent()) {
            try (final FileReference fileReference = fileReferenceOptional.get()) {
                fileName = fileReference.getMetadata().getString("fileName");
                fileId = fileReference.getFileId().toString();
            } catch (Exception e) {
                LOGGER.error("Exception while retrieving file name", e);
            }
        }

        final CourtApplicationSubject courtApplicationSubject = CourtApplicationSubject.courtApplicationSubject().withCourtApplicationId(courtApplication.getId()).build();

        final JsonObject applicationMaterialCommandPayload = Json.createObjectBuilder()
                .add("applicationId", courtApplication.getId().toString())
                .add("submissionId", UUID.randomUUID().toString())
                .add("material", fileId)
                .add("materialType", "Applications")
                .add("materialContentType", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .add("materialName", "POCA document")
                .add("fileName", fileName)
                .add("courtApplicationSubject", objectToJsonObjectConverter.convert(courtApplicationSubject))
                .build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROSECUTION_CASEFILE_COMMAND_ADD_APPLICATION_MATERIAL)
                .build();

        final Envelope<JsonObject> commandEnvelope = Envelope.envelopeFrom(metadata, applicationMaterialCommandPayload);
        sender.send(commandEnvelope);

        sendEmailNotification(envelope, courtApplication.getRespondents().get(0), eventPayload.getString("senderEmail"), courtApplication.getApplicationReference(), courtApplication.getId().toString());
    }

    public void sendEmailNotification(final JsonEnvelope event, final CourtApplicationParty respondent, final String senderEmail, final String arn, final String applicationId) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("sending email notification for event - {} ", event.toObfuscatedDebugString());
        }

        final Person person = respondent.getMasterDefendant().getPersonDefendant().getPersonDetails();

        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, UUID.randomUUID().toString());
        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, "4a40c000-2ac4-4464-bf7c-f944b31a67f4");
        notifyObjectBuilder.add(SEND_TO_ADDRESS, senderEmail);

        final JsonObjectBuilder personalisationObjectBuilder = createObjectBuilder();
        personalisationObjectBuilder.add("applicationId", applicationId);
        personalisationObjectBuilder.add("ARN", arn);
        personalisationObjectBuilder.add("firstName", person.getFirstName());
        personalisationObjectBuilder.add("lastName", person.getLastName());
        notifyObjectBuilder.add(PERSONALISATION, personalisationObjectBuilder.build());

        final Metadata metadata = Envelope.metadataFrom(event.metadata()).withName(NOTIFICATION_NOTIFY_SEND_EMAIL_NOTIFICATION).build();
        sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelopeFrom(metadata, notifyObjectBuilder.build())));
    }
}
