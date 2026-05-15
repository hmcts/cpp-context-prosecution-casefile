package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.uploadFile;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.CreateSjpCaseStub.resetAndStubCreateSjpCase;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.ADD_COURT_DOCUMENT_COMMAND;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForAddCourtDocument;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForQueryApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForQueryApplicationDoesNotExist;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.QueueUtil;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AddApplicationMaterialIT extends BaseIT {

    private static final String MATERIAL_ADDED_IN_MATERIAL_CONTEXT = "material.material-added";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String DEFENDANT_ID1 = randomUUID().toString();
    private static final String FILE_NAME = "File name";
    private final static String SJPN_DOCUMENT_TYPE = "SJPN";
    private static final String SECTION = "Applications";
    private UUID caseId;
    private final UUID submissionId = randomUUID();
    private final JmsMessageConsumerClient publicEventsConsumerForDefendantIDPCUpdated = newPublicJmsMessageConsumerClientProvider()
            .withEventNames("public.prosecutioncasefile.defendant-idpc-added")
            .getMessageConsumerClient();

    @BeforeAll
    public static void setUpOnce() {
        stubWiremocks();
        resetAndStubCreateSjpCase();
        stubForAddCourtDocument();
        stubGetCaseMarkersWithCode("ABC");
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
    }

    private static void stubWiremocks() {
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForApplicationDefendantLevelForV2() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final UUID applicationId = randomUUID();
        final boolean isUnbundledDocument = new Random().nextBoolean();

        stubForQueryApplication(applicationId);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2ForApplication(uploadId, SECTION, PDF_MIME_TYPE, applicationId);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        whenMaterialIsUploadedV2ForApplication(applicationId, addMaterialCommandPayload, addMaterialHelper, submissionId);
        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));
        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("documentCategory", is(SECTION)),
                withJsonPath("materialType", is(SECTION)),
                withJsonPath("documentTypeId"),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("courtApplicationSubject.courtApplicationId", is(applicationId.toString())),
                hasNoJsonPath("isCpsCase")
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPApplicationLevelV2(applicationId.toString(), materialId, isUnbundledDocument,uploadId.toString());
        final JsonObject ccMetadata = materialAddedToMaterialContextPayload.metadata().asJsonObject().getJsonObject("ccMetadata");
        sendPublicEvent(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        addMaterialHelper.verifyAddCourtDocumentCalled(materialId);

        final JsonObject courtDocumentPayload;
        try (JsonReader jsonReader = JsonObjects.createReader(new StringReader(getLastLoggedRequest(ADD_COURT_DOCUMENT_COMMAND + materialId)))) {
            courtDocumentPayload = jsonReader.readObject();
        }

        final JsonEnvelope addCourtDocument = JsonEnvelope.envelopeFrom(metadataFrom(materialAddedToMaterialContextPayload.metadata()).withName(PROGRESSION_ADD_COURT_DOCUMENT).build(), courtDocumentPayload);

        assertThat(addCourtDocument, Matchers.is(jsonEnvelope(
                metadata()
                        .withName(PROGRESSION_ADD_COURT_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(materialAddedToMaterialContextPayload.payloadAsJsonObject().getJsonObject("fileDetails").getString("fileName"))),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadata.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadata.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(materialAddedToMaterialContextPayload.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.applicationDocument.applicationId", equalTo(ccMetadata.getString("applicationId"))),
                        withJsonPath("$.materialSubmittedV2.courtApplicationSubject.courtApplicationId", equalTo(applicationId.toString())),
                        withoutJsonPath("$.materialSubmittedV2.caseId")
                )))));


    }

    @Test
    public void shouldUploadToMaterialContextAndRejectedWhenMaterialAddedEventRaisedForApplicationDefendantLevelForV2() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final UUID applicationId = randomUUID();
        stubForQueryApplicationDoesNotExist(applicationId);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2ForApplication(uploadId, SECTION, PDF_MIME_TYPE, applicationId);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2ForApplication(applicationId, addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_REJECTED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_REJECTED_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SECTION)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("courtApplicationSubject.courtApplicationId", is(applicationId.toString())),
                withJsonPath("errors[0].code", is("APPLICATION_ID_NOT_FOUND")),
                hasNoJsonPath("isCpsCase")
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

    }

    public void verifyInMessagingQueueForDefendantIDPCUpdated(final String materialId) {
        final JsonEnvelope eventFromQueue = QueueUtil.getEventFromQueue(publicEventsConsumerForDefendantIDPCUpdated);
        assertThat(eventFromQueue, jsonEnvelope(
                metadata().withName("public.prosecutioncasefile.defendant-idpc-added"),
                payload().isJson(Matchers.allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.defendantId", equalTo(DEFENDANT_ID1)),
                        withJsonPath("$.publishedDate", notNullValue()),
                        withJsonPath("$.materialId", equalTo(materialId))))));
    }

    protected void whenMaterialIsUploadedV2(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addMaterialV2(caseId, submissionId, addMaterialCommandPayload);
    }

    protected void whenMaterialIsUploadedV2ForApplication(final UUID applicationId,  final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addApplicationMaterialV2(applicationId, submissionId, addMaterialCommandPayload);
    }

    private static UUID getSubmissionId(final JsonEnvelope envelope) {
        return Optional.ofNullable(envelope.metadata().asJsonObject().getString("submissionId", null))
                .map(UUID::fromString)
                .orElseThrow(() -> new AssertionError("Impossible retrieve submissionId from " + envelope.metadata()));
    }

    private static JsonObject buildAddMaterialCommandPayloadV2ForApplication(final UUID uploadId, final String documentType, final String mimeType, final UUID courtApplicationId) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-for-application.json", uploadId, documentType, mimeType, courtApplicationId);
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJPDefendantLevelV2(final String caseId, final String materialId, final boolean isUnbundledDocument, final String fileServiceId) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                        getCCMetadataJsonObjectDefendantLevelV2(caseId, fileServiceId))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("fileDetails", createObjectBuilder()
                        .add("fileName", FILE_NAME)
                        .add("alfrescoAssetId", randomUUID().toString())
                        .add("mimeType", PDF_MIME_TYPE)
                        .build())
                .add("materialAddedDate", "2019-09-17T07:54:37.539Z")
                .add("isUnbundledDocument", isUnbundledDocument)
                .build();
        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJPApplicationLevelV2(final String applicationId, final String materialId, final boolean isUnbundledDocument, final String fileServiceId) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                        getCCMetadataJsonObjectApplicationLevelV2(applicationId, fileServiceId))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("fileDetails", createObjectBuilder()
                        .add("fileName", FILE_NAME)
                        .add("alfrescoAssetId", randomUUID().toString())
                        .add("mimeType", PDF_MIME_TYPE)
                        .build())
                .add("materialAddedDate", "2019-09-17T07:54:37.539Z")
                .add("isUnbundledDocument", isUnbundledDocument)
                .build();
        return JsonEnvelope.envelopeFrom(metadata, payload);
    }


    private static JsonObject getCCMetadataJsonObjectDefendantLevelV2(final String caseId, final String fileServiceId) {
        return JsonObjects.createObjectBuilder(metadataBuilder()
                .withId(randomUUID())
                .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("caseId", caseId)
                        .add("defendantId", randomUUID().toString())
                        .add("documentCategory", "Defendant level")
                        .add("documentTypeDescription", SJPN_DOCUMENT_TYPE)
                        .add("documentTypeId", randomUUID().toString())
                        .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString()))
                .add("fileStoreId", fileServiceId)
                .build();
    }

    private static JsonObject getCCMetadataJsonObjectApplicationLevelV2(final String applicationId, final String fileServiceId) {
        return JsonObjects.createObjectBuilder(metadataBuilder()
                .withId(randomUUID())
                .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("applicationId", applicationId)
                        .add("defendantId", randomUUID().toString())
                        .add("documentCategory", "Applications")
                        .add("documentTypeDescription", SJPN_DOCUMENT_TYPE)
                        .add("documentTypeId", randomUUID().toString())
                        .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString()))
                .add("fileStoreId", fileServiceId)
                .build();
    }

    private String getLastLoggedRequest(final String path) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathMatching(path)));
        return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
    }

}
