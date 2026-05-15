package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.PROBLEM_CODE_DOCUMENT_NOT_MATCHED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PENDING_MATERIAL_EXPIRATION_PROCESS_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.buildAddMaterialCommandPayloadForCpsCaseDocument;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.getEventName;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.uploadFile;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_PENDING;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_PENDING_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_CASE_IS_EJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_MATERIAL_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.CreateSjpCaseStub.resetAndStubCreateSjpCase;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.ADD_COURT_DOCUMENT_COMMAND;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForAddCourtDocument;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetReferenceDataBySectionCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetParentBundleSection;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.helper.ActivitiHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector;
import uk.gov.moj.cpp.prosecution.casefile.helper.FileServiceHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateSjpProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.QueueUtil;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableMap;
import com.jayway.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AddMaterialIT extends BaseIT {

    private static final String MATERIAL_ADDED_IN_MATERIAL_CONTEXT = "material.material-added";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String FILE_NAME = "File name";
    private final static String CSV_MIME_TYPE = "application/csv";
    private final static String SJPN_DOCUMENT_TYPE = "SJPN";
    private final static String SJPN_DOCUMENT_TYPE_LOWER_CASE = "sjpn";
    private final static String CITN_DOCUMENT_TYPE = "CITN";
    public static final String IDPC_BUNDLE = "IDPC bundle";
    private UUID caseId;
    private String defendantId1 ;
    private String caseUrn;
    private UUID submissionId;
    private UUID externalId;

    final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper = new InitiateSjpProsecutionHelper();

    private final JmsMessageConsumerClient publicEventsConsumerForDefendantIDPCUpdated = newPublicJmsMessageConsumerClientProvider()
            .withEventNames("public.prosecutioncasefile.defendant-idpc-added")
            .getMessageConsumerClient();

    @BeforeAll
    public static void setUpOnce() {
        stubWiremocks();
        resetAndStubCreateSjpCase();
        stubForAddCourtDocument();
        stubGetCaseMarkersWithCode("ABC");
        stubGetReferenceDataBySectionCode();
        stubGetParentBundleSection();
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        defendantId1 = randomUUID().toString();
        submissionId = randomUUID();
        externalId = randomUUID();
        caseUrn = randomAlphanumeric(10);
    }

    private static void stubWiremocks() {
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseDefendantLevel() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                withJsonPath("material.isUnbundledDocument", is(isUnbundledDocument)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevel(materialId, isUnbundledDocument);
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
                        withJsonPath("$.courtDocument.isUnbundledDocument", equalTo(isUnbundledDocument)),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId")))
                )))));
    }


    @Test
    public void shouldEmitPublicEventDocumentBundleArrivedForUnbundlingWhenMaterialAddedEventRaisedForCCCaseDefendantLevel() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddCpsMaterialWithCmsDocumentIdentifierCommandPayload(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);
        whenCPSMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveMessageWithMatchers(EVENT_SELECTOR_MATERIAL_ADDED,
                isJson(allOf(withJsonPath("$.caseId", is(caseId.toString())))));
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is("IDPC bundle")),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                withJsonPath("receivedDateTime", is("2020-02-04T05:27:17.210Z")),
                withJsonPath("cmsDocumentIdentifier.materialType", is(1)),
                withJsonPath("cmsDocumentIdentifier.documentId", is("ABCDEF"))
        ))));


        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveMessageWithMatchers(EventSelector.PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE, isJson(Matchers.allOf(
                withJsonPath("$.caseId", CoreMatchers.is(caseId.toString())))));

        assertThat(publicEvent.isPresent(), is(true));
    }


    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseDefendantLevelForCPSCase() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForCpsCase(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(true))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevel(materialId, false);
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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId")))
                )))));
    }

    @Test
    public void shouldUploadToMaterialContextForCPSCases() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForCpsCase(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(true))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

    }

    @Test
    public void shouldUploadToMaterialContextForCPSCases_withUnbundledMaterials() {
        final JsonObject addMaterialCommandPayload = buildAddMaterialsCommandPayloadForCpsCase();

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);
        whenMaterialsAreUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        for (JsonObject material : addMaterialCommandPayload.getJsonArray("materials").getValuesAs(JsonObject.class)) {

            final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
            assertThat(privateEvent.isPresent(), is(true));

            assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                    withJsonPath("caseId", is(caseId.toString())),
                    withJsonPath("caseType", is("CC")),
                    withJsonPath("documentCategory", is("Defendant level")),
                    withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                    withJsonPath("documentTypeId"),
                    withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                    withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                    withJsonPath("defendantId", is(defendantId1)),
                    withJsonPath("material.fileStoreId", is(material.getString("fileStoreId"))),
                    withJsonPath("material.documentType", is(material.getString("documentType"))),
                    withJsonPath("material.fileType", is(material.getString("fileType"))),
                    withJsonPath("isCpsCase", is(true))
            ))));
            assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
        }
    }

    @Test
    public void shouldUploadToMaterialContextForCPSCasesNoIsCpsCaseFlag() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForNoIsCpsCaseFlag(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                hasNoJsonPath("isCpsCase")
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }


    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseCaseLevel() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, "abcd", PDF_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);
        verifyUploadToMaterialAndSendToProgressionForCaseLevelDocument(addMaterialCommandPayload, addMaterialHelper, uploadId, null);
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedForCpsCaseEventRaisedForCCCaseCaseLevel() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, "abcd", PDF_MIME_TYPE, true);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);
        verifyUploadToMaterialAndSendToProgressionForCaseLevelDocument(addMaterialCommandPayload, addMaterialHelper, uploadId, null);
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedForCps() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, "abcd", PDF_MIME_TYPE, true);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);
        verifyUploadToMaterialAndSendToProgressionForCaseLevelDocument(addMaterialCommandPayload, addMaterialHelper, uploadId, true);
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionForCaseWithWarningsWhenMaterialAddedEventRaisedForCCCaseCaseLevel() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, "abcd", PDF_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-out-of-time.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS, staticPayLoad);
        verifyUploadToMaterialAndSendToProgressionForCaseLevelDocument(addMaterialCommandPayload, addMaterialHelper, uploadId, null);
    }


    @Test
    public void shouldCreateMaterialPendingEventWhenAddMaterialCalled() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addMaterialRequest = buildAddMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploaded(addMaterialRequest, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("prosecutingAuthority", is(addMaterialRequest.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialRequest.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));

        final String pendingMaterialProcess = ActivitiHelper.pollUntilProcessExists(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, uploadId.toString());
        ActivitiHelper.executeTimerJobs(pendingMaterialProcess);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.MATERIAL_EXPIRED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("expiredAt")),
                withJsonPath("$.errors[0].values[0].value", notNullValue())
        )));

    }

    @Test
    public void shouldCreateMaterialPendingEventWhenAddMaterialForCpsCaseCalled() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);


        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForCpsCase(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("isCpsCase", is(true)),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        final String pendingMaterialProcess = ActivitiHelper.pollUntilProcessExists(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, uploadId.toString());
        ActivitiHelper.executeTimerJobs(pendingMaterialProcess);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.MATERIAL_EXPIRED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("expiredAt")),
                withJsonPath("$.errors[0].values[0].value", notNullValue())
        )));
    }

    @Test
    public void shouldCreateDocumentReviewRequiredWhenAddMaterialForCpsCaseCalled() throws Exception {
        final UUID fileStoreId = uploadFile(PDF_MIME_TYPE);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForCpsCaseDocument(fileStoreId, SJPN_DOCUMENT_TYPE_LOWER_CASE);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenCPSMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("isCpsCase", is(true)),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(fileStoreId.toString()))
        ))));

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_DOCUMENT_REVIEW);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errorCodes[0]", equalTo(PROBLEM_CODE_DOCUMENT_NOT_MATCHED))
        )));
    }

    @Test
    public void shouldRaiseBulkscanMaterialRejectedFollowupEventAfterTimeout() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final JsonObject addMaterialRequest = buildAddMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploaded(addMaterialRequest, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("prosecutingAuthority", is(addMaterialRequest.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialRequest.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));

        final String pendingMaterialProcess = ActivitiHelper.pollUntilProcessExists(BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, uploadId.toString());
        ActivitiHelper.executeTimerJobs(pendingMaterialProcess);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_BULK_SCAN_MATERIAL_REJECTED_EVENT);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.MATERIAL_EXPIRED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("expiredAt")),
                withJsonPath("$.errors[0].values[0].value", notNullValue())
        )));
    }

    @Test
    public void shouldCreateMaterialAddedEventWhenAddMaterialCalledAfterCaseCreated() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadCaseDocumentCalled(caseId, SJPN_DOCUMENT_TYPE);

        sendSjpCaseDocumentAddedPublicEvent(caseId);

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_UPLOAD_CASE_DOCUMENT_RECORDED);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EventSelector.EVENT_SELECTOR_UPLOAD_CASE_DOCUMENT_RECORDED),
                payload().isJson(allOf(
                        withJsonPath("caseId", is(caseId.toString())),
                        withJsonPath("documentId", notNullValue())
                ))));
    }

    @Test
    public void shouldCreateMaterialRejectedEventForCCCaseMaterialWithInvalidDocumentType() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, "DocumentType", CSV_MIME_TYPE, false);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.INVALID_DOCUMENT_TYPE.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("documentType")),
                withJsonPath("$.errors[0].values[0].value", equalTo(addMaterialCommandPayload.getJsonObject("material").getString("documentType")))
        )));
    }

    @Test
    public void shouldRaiseMaterialRejectedEventForCCCaseWhenTheCaseIsEjected() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        // when
        whenTheCaseIsEjected();

        Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_CASE_IS_EJECTED);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(getEventName(publicEvent.get()), is(PUBLIC_CASE_IS_EJECTED));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(defendantId1, uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE);
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        // then
        assertThat(getEventName(publicEvent.get()), is(PUBLIC_MATERIAL_REJECTED));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));
        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.CASE_ALREADY_EJECTED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("documentType")),
                withJsonPath("$.errors[0].values[0].value", equalTo("SJPN"))
        )));
    }

    @Test
    public void shouldCreateMaterialRejectedEventForCCDefendantLevelMaterialWithMissingProsecutorDefendantId() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE, false);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.DEFENDANT_ID_REQUIRED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("prosecutorDefendantId")),
                withJsonPath("$.errors[0].values[0].value", equalTo(""))
        )));

    }


    @Test
    public void shouldCreateMaterialRejectedEventForCPSForCCDefendantLevelMaterialWithMissingProsecutorDefendantId() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadNoDefendantID(uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE, true);
        buildAddMaterialCommandPayloadForCpsCase(uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("isCpsCase", is(true)),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.DEFENDANT_ID_REQUIRED.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("prosecutorDefendantId")),
                withJsonPath("$.errors[0].values[0].value", equalTo(""))
        )));
    }

    @Test
    public void shouldCreateMaterialRejectedEventForCCDefendantLevelMaterialWithInvalidProsecutorDefendantId() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload("invalid defendant id", uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.DEFENDANT_ID_INVALID.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("prosecutorDefendantId")),
                withJsonPath("$.errors[0].values[0].value", equalTo(addMaterialCommandPayload.getString("prosecutorDefendantId")))
        )));
    }


    @Test
    public void shouldCreateCPSMaterialRejectedEventForCCDefendantLevelMaterialWithInvalidProsecutorDefendantId() throws Exception {

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access-without-idpc.json");

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));


        final JsonObject addMaterialCommandPayload = buildAddCpsMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenCPSMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Set<String> checkedEventsSet = new HashSet<>();
        Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveMessageWithMatchers(PUBLIC_MATERIAL_REJECTED,
                isJson(allOf(withJsonPath("$.caseId", is(caseId.toString())))));

        assertThat(publicEvent.isPresent(), is(true));

        assertPublicEvent(publicEvent.get(), checkedEventsSet);
        publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_DOCUMENT_REVIEW);
        assertThat(publicEvent.isPresent(), is(true));
        assertPublicEvent(publicEvent.get(), checkedEventsSet);
        assertThat(checkedEventsSet.size(), is(2));
        assertThat(checkedEventsSet.contains(PUBLIC_MATERIAL_REJECTED), is(true));
        assertThat(checkedEventsSet.contains(EventSelector.PUBLIC_DOCUMENT_REVIEW), is(true));
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    @Test
    public void shouldUploadDocumentToCCCaseWhenSJPCaseIsReferredForCourtHearing() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();

        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        // when
        whenTheCaseIsReferredToCourt();

        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_CASE_REFERRED_TO_COURT_EVENT);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(EventSelector.PUBLIC_CASE_REFERRED_TO_COURT_EVENT));

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload("TFL001Defendant01", uploadId, SJPN_DOCUMENT_TYPE, PDF_MIME_TYPE);
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevel(materialId, false);
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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId")))
                )))));
    }

    @Test
    public void shouldCreateMaterialRejectedEventForMaterialWithUnsupportedFileType() throws Exception {

        final UUID uploadId = uploadFile(CSV_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE, CSV_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);
        final Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(PUBLIC_MATERIAL_REJECTED));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));

        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.INVALID_FILE_TYPE.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("fileType")),
                withJsonPath("$.errors[0].values[0].value", equalTo(addMaterialCommandPayload.getJsonObject("material").getString("fileType")))
        )));

    }

    @Test
    public void shouldRaiseMaterialRejectedEventWhenTheCaseIsInActiveSjpSession() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, CITN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        // when
        whenTheCaseIsInSjpSession();
        Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT));

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);
        publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        // then
        assertThat(getEventName(publicEvent.get()), is(PUBLIC_MATERIAL_REJECTED));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));
        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.CASE_IS_IN_SESSION.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("caseInSession")),
                withJsonPath("$.errors[0].values[0].value", equalTo("true"))
        )));
    }

    @Test
    public void shouldAcceptMaterialWhenTheCaseMovedOutOfSession() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        assertThat(FileServiceHelper.read(uploadId).isPresent(), is(true));

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayload(uploadId, CITN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        // when
        whenTheCaseIsInSjpSession();
        Optional<JsonEnvelope> publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT));

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_MATERIAL_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(PUBLIC_MATERIAL_REJECTED));
        assertThat(getSubmissionId(publicEvent.get()), is(submissionId));
        assertThat(publicEvent.get().payloadAsJsonObject(), payload().isJson(allOf(
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.CASE_IS_IN_SESSION.name())),
                withJsonPath("$.errors[0].values[0].key", equalTo("caseInSession")),
                withJsonPath("$.errors[0].values[0].value", equalTo("true"))
        )));

        // when
        whenTheCaseIsInOutOfSjpSession();
        publicEvent = addMaterialHelper.retrieveEvent(EventSelector.PUBLIC_SJP_CASE_UNASSIGNED_EVENT);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(getEventName(publicEvent.get()), is(EventSelector.PUBLIC_SJP_CASE_UNASSIGNED_EVENT));

        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(CITN_DOCUMENT_TYPE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId));

        addMaterialHelper.verifyUploadCaseDocumentCalled(caseId, CITN_DOCUMENT_TYPE);
    }

    @Test
    public void shouldHandleMultiplePendingMaterials() throws Exception {

        final UUID citnSubmissionId = randomUUID();
        final UUID sjpnSubmissionId = randomUUID();
        final UUID citnUploadId = uploadFile(CSV_MIME_TYPE);
        final UUID sjpnUploadId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);


        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addInvalidCitnDocumentCommandPayload = buildAddMaterialCommandPayload(citnUploadId, CITN_DOCUMENT_TYPE, CSV_MIME_TYPE, false);
        final JsonObject addValidSjpnDocumentCommandPayload = buildAddMaterialCommandPayload(sjpnUploadId, SJPN_DOCUMENT_TYPE, PDF_MIME_TYPE, false);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploaded(addInvalidCitnDocumentCommandPayload, addMaterialHelper, citnSubmissionId);

        final Optional<JsonEnvelope> citnPrivateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(citnPrivateEvent.isPresent(), is(true));

        assertThat(getEventName(citnPrivateEvent.get()), is(EVENT_SELECTOR_MATERIAL_PENDING));
        assertThat(getSubmissionId(citnPrivateEvent.get()), is(citnSubmissionId));

        whenMaterialIsUploaded(addValidSjpnDocumentCommandPayload, addMaterialHelper, sjpnSubmissionId);

        final Optional<JsonEnvelope> sjpnPrivateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING);
        assertThat(sjpnPrivateEvent.isPresent(), is(true));
        assertThat(getEventName(sjpnPrivateEvent.get()), is(EVENT_SELECTOR_MATERIAL_PENDING));
        assertThat(getSubmissionId(sjpnPrivateEvent.get()), is(sjpnSubmissionId));

        ActivitiHelper.pollUntilProcessExists(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, citnUploadId.toString());
        ActivitiHelper.pollUntilProcessExists(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, sjpnUploadId.toString());

        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        final Optional<JsonEnvelope> citnPrivateEvent2 = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED);
        assertThat(citnPrivateEvent2.isPresent(), is(true));

        final Optional<JsonEnvelope> sjpnPrivateEvent2 = addMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(sjpnPrivateEvent2.isPresent(), is(true));

        assertThat(getSubmissionId(citnPrivateEvent2.get()), is(citnSubmissionId));
        assertThat(getSubmissionId(sjpnPrivateEvent2.get()), is(sjpnSubmissionId));


        ActivitiHelper.pollUntilProcessDeleted(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, citnUploadId.toString(), "Timeout cancelled");
        ActivitiHelper.pollUntilProcessDeleted(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, sjpnUploadId.toString(), "Timeout cancelled");
    }

    @Test
    public void shouldCreateMaterialPendingEventWithReceivedDateWhenAddCpsCommandCalledAndCaseNotCreated() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final JsonObject addCpsMaterialCommandPayload = buildAddCpsMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        whenCPSMaterialIsUploaded(addCpsMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveMessageWithMatchers(EVENT_SELECTOR_MATERIAL_PENDING,
                isJson(allOf(withJsonPath("$.caseId", is(caseId.toString())))));
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(

                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("prosecutingAuthority", is(addCpsMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addCpsMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(IDPC_BUNDLE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE)),
                withJsonPath("receivedDateTime", notNullValue())
        ))));
    }

    @Test
    public void shouldUploadToMaterialContextViaAddCpsCommand() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addCpsMaterialCommandPayload = buildAddCpsMaterialCommandPayload(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE);
        whenCPSMaterialIsUploaded(addCpsMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveMessageWithMatchers(EVENT_SELECTOR_MATERIAL_ADDED,
                isJson(allOf(withJsonPath("$.caseId", is(caseId.toString())))));

        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("documentType", is(IDPC_BUNDLE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addCpsMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("prosecutorDefendantId", is(addCpsMaterialCommandPayload.getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is(IDPC_BUNDLE)),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentForCPSAddedPayloadForNonSJPDefendantLevel(materialId, caseId);
        final JsonObject ccMetadata = materialAddedToMaterialContextPayload.metadata().asJsonObject().getJsonObject("ccMetadata");
        sendPublicEvent(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        addMaterialHelper.verifyAddCourtDocumentCalled(materialId);

        verifyInMessagingQueueForDefendantIDPCUpdated(materialId);

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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId")))
                )))));
    }

    @Test
    public void shouldRejectMaterialSubmissionV2ForAddMaterialV2ForCase() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, "INVALID CONTENT TYPE", submissionId.toString(), isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_REJECTED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_REJECTED_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.INVALID_FILE_TYPE.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }



    @Test
    public void shouldRaisePendingWithWarningsEventWhenDefendantDetailsNotExist() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithoutPersonalInformation(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(), isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name())),
                withJsonPath("$.warnings[1].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));


        final UUID uploadId2 = uploadFile(PDF_MIME_TYPE);
        final UUID submissionId2 = randomUUID();

        final JsonObject addMaterialCommandPayload2 = buildAddMaterialCommandPayloadV2(uploadId2, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId2.toString(), isUnbundledDocument);

        whenMaterialIsUploadedV2(addMaterialCommandPayload2, addMaterialHelper, submissionId2);

        privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload2.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload2.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId2.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId2));

        privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }


    @Test
    public void shouldRaisePendingWithWarningsEventWhenDefendantIdNotExist() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithWrongDefendant(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, isUnbundledDocument);
        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name())),
                withJsonPath("$.warnings[1].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }


    @Test
    public void shouldUploadPendingMaterialWhenDefendantAddedToCase() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithWrongDefendant(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name())),
                withJsonPath("$.warnings[1].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        final String staticPayLoadWithNewDefendant = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-new-defendant.json");
        verifyCCEvent(EVENT_SELECTOR_DEFENDANT_ADDED, staticPayLoadWithNewDefendant);

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("prosecutionCaseSubject.ouCode", is("ouCode")),
                withJsonPath("cpsFlag", is(true)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId));
    }

    @Test
    public void shouldUploadPendingMaterialWhenDefendantAddedWithOrganisationToCase() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-corporate.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithDefandant2(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(),  isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        final String staticPayLoadWithNewDefendant = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-corporate-with-new-defendant.json");
        verifyCCEvent(EVENT_SELECTOR_DEFENDANT_ADDED, staticPayLoadWithNewDefendant);

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorOrganisationDefendantDetails.organisationName", is("Ministry of Justice2")),
                withJsonPath("prosecutionCaseSubject.ouCode", is("ouCode")),
                withJsonPath("cpsFlag", is(true)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId));

    }

    @Test
    public void shouldUploadPendingMaterialWhenDefendantWithOrganisationInCaseChanged() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-corporate.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithDefandant2(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(),  isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        sendPublicEvent(PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED,
                "stub-data/public.progression.case-defendant-with-organisation-changed-for-add-material.json", defendantId1, caseId.toString());

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorOrganisationDefendantDetails.organisationName", is("Ministry of Justice2")),
                withJsonPath("prosecutionCaseSubject.ouCode", is("ouCode")),
                withJsonPath("cpsFlag", is(true)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId));
    }

    @Test
    public void shouldUploadPendingMaterialWhenDefendantInCaseChanged() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithWrongDefendant(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name())),
                withJsonPath("$.warnings[1].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        sendPublicEvent(PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED,
                "stub-data/public.progression.case-defendant-changed-for-add-material.json", defendantId1, caseId.toString());

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("prosecutionCaseSubject.ouCode", is("ouCode")),
                withJsonPath("cpsFlag", is(true)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId));

    }

    @Test
    public void shouldRejectWithWarningsMaterialSubmissionV2ForAddMaterialV2ForCase() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-duplicatedefendants.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(), isUnbundledDocument);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("$.warnings[0].code", equalTo(ProblemCode.DEFENDANT_ON_CP.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseDefendantLevelForV2() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(), isUnbundledDocument);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);
        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevelV2(caseId.toString(), materialId, isUnbundledDocument, uploadId.toString());
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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId"))),
                        withJsonPath("$.materialSubmittedV2.materialContentType", equalTo("application/pdf")),
                        withJsonPath("$.materialSubmittedV2.defendantId", equalTo(defendantId1))
                )))));

        final UUID uploadId2 = uploadFile(PDF_MIME_TYPE);
        final UUID submissionId2 = randomUUID();
        final JsonObject addMaterialCommandPayloadWithoutPersonalInformation = buildAddMaterialCommandPayloadV2WithoutPersonalInformation(uploadId2, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId2.toString(), isUnbundledDocument);

        whenMaterialIsUploadedV2(addMaterialCommandPayloadWithoutPersonalInformation, addMaterialHelper, submissionId2);

        final Optional<JsonEnvelope> privateEvent2 = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("prosecutionCaseSubject.ouCode", is("ouCode")),
                withJsonPath("cpsFlag", is(true)),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId2.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent2.get()), is(submissionId2));
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseDefendantLevelAfterCaseCreatedForV2() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(), isUnbundledDocument);
        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_PENDING_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE_LOWER_CASE)),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));
        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorPersonDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevelV2(caseId.toString(), materialId, isUnbundledDocument, uploadId.toString());
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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId"))),
                        withJsonPath("$.materialSubmittedV2.materialContentType", equalTo("application/pdf")),
                        withJsonPath("$.materialSubmittedV2.defendantId", equalTo(defendantId1)),
                        withJsonPath("$.materialSubmittedV2.prosecutionCaseSubject.ouCode", equalTo("ouCode"))
                )))));

    }

    @Test
    public void shouldRaiseRejectEventWhenDefendantIdAndDetailsNotExist() throws Exception {
        final UUID uploadId1 = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadV2WithoutDefendantIdAndPersonalInformation(uploadId1, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, isUnbundledDocument);
        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_REJECTED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_REJECTED_V2), payload().isJson(allOf(
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("material", is(uploadId1.toString())),
                withJsonPath("$.errors.length()", equalTo(1)),
                withJsonPath("$.errors[0].code", equalTo(ProblemCode.DEFENDANT_ID_REQUIRED.name()))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));
    }

    @Test
    public void shouldUploadToMaterialContextAndSendToProgressionWhenMaterialAddedEventRaisedForCCCaseDefendantLevelWithOrganisationForV2() throws Exception {
        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final boolean isUnbundledDocument = new Random().nextBoolean();

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadWithOrganisationV2(uploadId, SJPN_DOCUMENT_TYPE_LOWER_CASE, PDF_MIME_TYPE, submissionId.toString(), isUnbundledDocument);


        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-corporate.json");
        verifyCCEventAndProgressionCommand(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, staticPayLoad);

        whenMaterialIsUploadedV2(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED_V2);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED_V2), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("materialType", is(SJPN_DOCUMENT_TYPE)),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutionCaseSubject.prosecutingAuthority", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getString("prosecutingAuthority"))),
                withJsonPath("prosecutionCaseSubject.defendantSubject.prosecutorOrganisationDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorOrganisationDefendantDetails").getString("prosecutorDefendantId"))),
                withJsonPath("defendantId", is(defendantId1)),
                withJsonPath("material", is(uploadId.toString())),
                withJsonPath("materialContentType", is(PDF_MIME_TYPE)),
                withJsonPath("isCpsCase", is(false))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPDefendantLevelV2(caseId.toString(), materialId, isUnbundledDocument, uploadId.toString());
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
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadata.getString("defendantId"))),
                        withJsonPath("$.materialSubmittedV2.materialContentType", equalTo("application/pdf")),
                        withJsonPath("$.materialSubmittedV2.defendantId", equalTo(defendantId1)),
                        withJsonPath("$.materialSubmittedV2.prosecutionCaseSubject.defendantSubject.prosecutorOrganisationDefendantDetails.prosecutorDefendantId", is(addMaterialCommandPayload.getJsonObject("prosecutionCaseSubject").getJsonObject("defendantSubject").getJsonObject("prosecutorOrganisationDefendantDetails").getString("prosecutorDefendantId")))
                )))));

    }


    public void verifyInMessagingQueueForDefendantIDPCUpdated(final String materialId) {
        final JsonEnvelope eventFromQueue = QueueUtil.getEventFromQueue(publicEventsConsumerForDefendantIDPCUpdated);
        assertThat(eventFromQueue, jsonEnvelope(
                metadata().withName("public.prosecutioncasefile.defendant-idpc-added"),
                payload().isJson(Matchers.allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.defendantId", equalTo(defendantId1)),
                        withJsonPath("$.publishedDate", notNullValue()),
                        withJsonPath("$.materialId", equalTo(materialId))))));
    }

    private void initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final JsonObject initiateSjpProsecutionCommandPayload) {
        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionCommandPayload);

        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED),
                payload().isJson(withJsonPath("prosecution.caseDetails.caseId", is(caseId.toString())))));

        sendSjpCaseCreatedPublicEvent(caseId);

        final Optional<JsonEnvelope> privateEvent2 = initiateSjpProsecutionHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY);
        assertThat(privateEvent2.isPresent(), is(true));

        assertThat(privateEvent2.get(), jsonEnvelope(
                metadata().withName(EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY),
                payload().isJson(withJsonPath("caseId", is(caseId.toString())))));
    }

    protected void whenMaterialIsUploaded(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addMaterial(caseId, submissionId, addMaterialCommandPayload);
    }

    protected void whenMaterialsAreUploaded(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addMaterials(caseId, submissionId, addMaterialCommandPayload);
    }

    protected void whenMaterialIsUploadedV2(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addMaterialV2(caseId, submissionId, addMaterialCommandPayload);
    }

    protected void whenMaterialIsUploadedV2ForApplication(final UUID applicationId,  final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addMaterialV2(applicationId, submissionId, addMaterialCommandPayload);
    }

    protected void whenCPSMaterialIsUploaded(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID submissionId) {
        addMaterialHelper.addCpsMaterial(caseId, submissionId, addMaterialCommandPayload);
    }

    protected void whenTheCaseIsReferredToCourt() {
        raiseCaseReferredToCourtPublicEvent(caseId);
    }

    protected void whenTheCaseIsInSjpSession() {
        raiseCaseAssignedPublicEvent(caseId);
    }

    protected void whenTheCaseIsInOutOfSjpSession() {
        raiseCaseUnAssignedPublicEvent(caseId);
    }


    private static UUID getSubmissionId(final JsonEnvelope envelope) {
        return Optional.ofNullable(envelope.metadata().asJsonObject().getString("submissionId", null))
                .map(UUID::fromString)
                .orElseThrow(() -> new AssertionError("Impossible retrieve submissionId from " + envelope.metadata()));
    }

    private void sendSjpCaseCreatedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.EXTERNAL_EVENT_SJP_CASE_CREATED, "stub-data/public.sjp.sjp-case-created.json", caseId.toString());
    }

    private void sendSjpCaseDocumentAddedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.EXTERNAL_EVENT_SJP_CASE_DOCUMENT_UPLOADED, "stub-data/public.sjp.sjp-case-document-uploaded.json", caseId.toString());
    }

    private void raiseCaseReferredToCourtPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.PUBLIC_CASE_REFERRED_TO_COURT_EVENT, "stub-data/public.resulting.decision-to-refer-case-for-court-hearing-saved.json", caseId.toString());
    }

    private void raiseCaseAssignedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT, "stub-data/public.sjp.case-assigned.json", caseId.toString());
    }

    private void raiseCaseUnAssignedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.PUBLIC_SJP_CASE_UNASSIGNED_EVENT, "stub-data/public.sjp.case-unassigned.json", caseId.toString());
    }

    private JsonObject buildAddMaterialCommandPayload(final UUID uploadId, final String documentType, final String mimeType, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material.json", defendantId1, uploadId, documentType, mimeType, isUnbundledDocument);
    }

    private JsonObject buildAddMaterialCommandPayloadV2(final UUID uploadId, final String documentType, final String mimeType,final String submissionId, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2.json", defendantId1, uploadId, documentType, mimeType, submissionId,  isUnbundledDocument);
    }

    private JsonObject buildAddMaterialCommandPayloadWithOrganisationV2(final UUID uploadId, final String documentType, final String mimeType,final String submissionId, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-for-organisation.json", defendantId1, uploadId, documentType, mimeType, submissionId,  isUnbundledDocument);
    }

    private JsonObject buildAddMaterialCommandPayloadV2WithoutPersonalInformation (final UUID uploadId, final String documentType, final String mimeType,final String submissionId, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-without-personal-information.json", defendantId1, uploadId, documentType, mimeType, submissionId, isUnbundledDocument);
    }

    private JsonObject buildAddMaterialCommandPayloadV2WithDefandant2 (final UUID uploadId, final String documentType, final String mimeType,final String submissionId, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-for-organisation-with-defendant2.json", defendantId1, uploadId, documentType, mimeType, submissionId, isUnbundledDocument);
    }

    private JsonObject buildAddMaterialCommandPayloadV2WithWrongDefendant(final UUID uploadId, final String documentType, final String mimeType, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-wrong-defendant.json", defendantId1, uploadId, documentType, mimeType, isUnbundledDocument);
    }

    private static JsonObject buildAddMaterialCommandPayloadV2WithoutDefendantIdAndPersonalInformation (final UUID uploadId, final String documentType, final String mimeType, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-without-defendantid-personal-information.json", uploadId, documentType, mimeType, isUnbundledDocument);
    }

    private static JsonObject buildAddMaterialCommandPayloadV2ForRejectionWithWarnings(final UUID uploadId, final String documentType, final String mimeType, final boolean isUnbundledDocument) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-for-rejection-with-warnings.json", "test asn", uploadId, documentType, mimeType, isUnbundledDocument);
    }

    private static JsonObject buildAddMaterialCommandPayloadV2ForApplication(final UUID uploadId, final String documentType, final String mimeType, final UUID courtApplicationId) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-v2-for-application.json", uploadId, documentType, mimeType, courtApplicationId);
    }

    private JsonObject buildAddCpsMaterialWithCmsDocumentIdentifierCommandPayload(final UUID uploadId, final String documentType, final String mimeType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material-cms-document-identifier.json", defendantId1, uploadId, documentType, mimeType);
    }

    private static JsonObject buildInitiateSjpProsecutionCommandPayload(final UUID caseId, final String caseUrn, final UUID externalId, final String prosecutorDefendantReference, final LocalDate chargeDate) {
        return readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution.json",
                ImmutableMap.<String, Object>builder()
                        .put("case.id", caseId)
                        .put("case.urn", caseUrn)
                        .put("defendant.dob", LocalDate.of(1960, 1, 1))
                        .put("defendant.nationality", "GBR")
                        .put("defendant.id", prosecutorDefendantReference)
                        .put("offence.chargeDate", chargeDate.toString())
                        .put("offence.committedDate", chargeDate.minusMonths(6))
                        .put("external.id", externalId)
                        .put("channel", "CPPI")
                        .build());
    }

    private void sendProgressionCaseCreatedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.PUBLIC_PROGRESSION_CASE_CREATED_EVENT, "stub-data/public.progression.prosecution-case-created.json", caseId.toString());
    }

    private JsonObject buildAddMaterialCommandPayloadForCpsCase(final UUID uploadId, final String documentType, final String mimeType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material_for_cps_case.json", defendantId1, uploadId, documentType, mimeType);
    }

    private JsonObject buildAddMaterialsCommandPayloadForCpsCase() {
        return readJsonResource("stub-data/prosecutioncasefile.add-materials_for_cps_case.json", defendantId1);
    }

    private JsonObject buildAddMaterialCommandPayloadForNoIsCpsCaseFlag(final UUID uploadId, final String documentType, final String mimeType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material_no_isCpsCase.json", defendantId1, uploadId, documentType, mimeType);
    }

    private static JsonObject buildAddMaterialCommandPayload(final String defendantId, final UUID uploadId, final String documentType, final String mimeType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material_defendant_id.json", defendantId, uploadId, documentType, mimeType);
    }

    private static JsonObject buildAddMaterialCommandPayloadNoDefendantID(final UUID uploadId, final String documentType, final String mimeType, final boolean cpsFlag) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material_no_defendant_id.json", uploadId, documentType, mimeType, cpsFlag);
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJPDefendantLevel(final String materialId, final boolean isUnbundledDocument) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectDefendantLevel())
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

    private JsonEnvelope createDocumentForCPSAddedPayloadForNonSJPDefendantLevel(final String materialId, final UUID caseId) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataForIDPCJsonObjectDefendantLevel(caseId))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("fileDetails", createObjectBuilder()
                        .add("fileName", FILE_NAME)
                        .add("alfrescoAssetId", randomUUID().toString())
                        .add("mimeType", PDF_MIME_TYPE)
                        .build())
                .add("materialAddedDate", "2019-09-17T07:54:37.539Z")
                .build();
        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJPCaseLevel(final String materialId, Boolean isCpsCase) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectCaseLevel(isCpsCase))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId)
                .add("fileDetails", createObjectBuilder()
                        .add("fileName", FILE_NAME)
                        .add("alfrescoAssetId", randomUUID().toString())
                        .add("mimeType", PDF_MIME_TYPE)
                        .build())
                .add("materialAddedDate", "2019-09-17T07:54:37.539Z")
                .build();
        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonObject getCCMetadataJsonObjectCaseLevel(Boolean isCpsCase) {
        JsonObjectBuilder ccMetadataBuilder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeDescription", SJPN_DOCUMENT_TYPE)
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString());
        if (isCpsCase != null) {
            ccMetadataBuilder.add("isCpsCase", isCpsCase);
        }
        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", ccMetadataBuilder)
                .build();
    }

    private static JsonObject getCCMetadataJsonObjectDefendantLevel() {
        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .add("defendantId", randomUUID().toString())
                        .add("documentCategory", "Defendant level")
                        .add("documentTypeDescription", SJPN_DOCUMENT_TYPE)
                        .add("documentTypeId", randomUUID().toString())
                        .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString()))
                .build();
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

    private JsonObject getCCMetadataForIDPCJsonObjectDefendantLevel(final UUID caseId) {
        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("defendantId", defendantId1)
                        .add("documentCategory", "Defendant level")
                        .add("documentTypeDescription", IDPC_BUNDLE)
                        .add("documentTypeId", randomUUID().toString())
                        .add("receivedDateTime", ZonedDateTime.now().toOffsetDateTime().toString())
                        .add("sectionCode", "IDPC"))
                .build();
    }

    private JsonObject buildAddCpsMaterialCommandPayload(final UUID uploadId, final String documentType, final String mimeType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-cps-material.json", defendantId1, uploadId, documentType, mimeType);
    }

    private String getLastLoggedRequest(final String path) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathMatching(path)));
        return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
    }

    private void verifyCCEventAndProgressionCommand(final String expectedPrivateEvent, final String staticPayLoad) {
        verifyCCEvent(expectedPrivateEvent, staticPayLoad);

        sendProgressionCaseCreatedPublicEvent(caseId);
    }

    private void verifyCCEvent(final String expectedPrivateEvent, final String staticPayLoad) {
        final String ccPayLoad = replaceValues(staticPayLoad, "C");
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(expectedPrivateEvent);
        assertThat(privateEvent.isPresent(), is(true));

        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(),
                        CoreMatchers.is(1));
    }

    private void verifyUploadToMaterialAndSendToProgressionForCaseLevelDocument(final JsonObject addMaterialCommandPayload, final AddMaterialHelper addMaterialHelper, final UUID uploadId, final Boolean isCpsCase) {
        whenMaterialIsUploaded(addMaterialCommandPayload, addMaterialHelper, submissionId);

        final Optional<JsonEnvelope> privateEvent = addMaterialHelper.retrieveEvent(EVENT_SELECTOR_MATERIAL_ADDED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Case level")),
                withJsonPath("documentType", is("ABCD")),
                withJsonPath("documentTypeId"),
                withJsonPath("prosecutingAuthority", is(addMaterialCommandPayload.getString("prosecutingAuthority"))),
                withJsonPath("material.fileStoreId", is(uploadId.toString())),
                withJsonPath("material.documentType", is("abcd")),
                withJsonPath("material.fileType", is(PDF_MIME_TYPE))
        ))));
        assertThat(getSubmissionId(privateEvent.get()), is(submissionId));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());

        final String materialId = randomUUID().toString();
        final JsonEnvelope materialAddedToMaterialContextPayload = createDocumentAddedPayloadForNonSJPCaseLevel(materialId, isCpsCase);
        final JsonObject ccMetadata = materialAddedToMaterialContextPayload.metadata().asJsonObject().getJsonObject("ccMetadata");
        sendPublicEvent(materialAddedToMaterialContextPayload.metadata().name(), materialAddedToMaterialContextPayload);

        addMaterialHelper.verifyAddCourtDocumentCalled(materialId);

        final JsonObject courtDocumentPayload;
        try (JsonReader jsonReader = JsonObjects.createReader(new StringReader(getLastLoggedRequest(ADD_COURT_DOCUMENT_COMMAND + materialId)))) {
            courtDocumentPayload = jsonReader.readObject();
        }

        final JsonEnvelope addCourtDocument = JsonEnvelope.envelopeFrom(metadataFrom(materialAddedToMaterialContextPayload.metadata()).withName(PROGRESSION_ADD_COURT_DOCUMENT).build(), courtDocumentPayload);

        Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", is(isCpsCase));
        }
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
                        withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(ccMetadata.getString("caseId"))),
                        isCpsCaseMatcher
                )))));
    }

    private String replaceValues(final String payload, final String initiationCode) {
        return payload
                .replace("CASE-ID", this.caseId.toString())
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", defendantId1)
                .replace("DEFENDANT_REFERENCE1", defendantId1)
                .replace("OFFENCE_ID1", randomUUID().toString())
                .replace("DEFENDANT_ID2", randomUUID().toString())
                .replace("DEFENDANT_REFERENCE2", randomUUID().toString())
                .replace("DEFENDANT_ID3", randomUUID().toString())
                .replace("OFFENCE_ID2", randomUUID().toString())
                .replace("OFFENCE_ID3", randomUUID().toString())
                .replace("OFFENCE_ID4", randomUUID().toString())
                .replace("OFFENCE_ID5", randomUUID().toString())
                .replace("OFFENCE_ID6", randomUUID().toString())
                .replace("INITIATION_CODE", initiationCode)
                .replace("CASE_MARKER", "ABC")
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", this.externalId.toString());
    }

    private void whenTheCaseIsEjected() {
        sendPublicEvent(PUBLIC_CASE_IS_EJECTED, "stub-data/public.progression.events.case-or-application-ejected.json", caseId.toString());
    }

    private void assertPublicEvent(final JsonEnvelope publicEvent, final Set<String> checkedEventsSet) {
        final String eventName = getEventName(publicEvent);
        if (PUBLIC_MATERIAL_REJECTED.equals(eventName)) {
            checkedEventsSet.add(eventName);
            assertThat(getSubmissionId(publicEvent), is(submissionId));

            assertThat(publicEvent.payloadAsJsonObject(), payload().isJson(allOf(
                    withJsonPath("$.errors[0].code", equalTo(ProblemCode.INVALID_DOCUMENT_TYPE.name()))
            )));
        } else if (EventSelector.PUBLIC_DOCUMENT_REVIEW.equals(eventName)) {
            checkedEventsSet.add(eventName);
            assertThat(getSubmissionId(publicEvent), is(submissionId));

            assertThat(publicEvent.payloadAsJsonObject(), payload().isJson(allOf(
                    withJsonPath("$.errorCodes[0]", equalTo(ProblemCode.INVALID_DOCUMENT_TYPE.name()))
            )));
        }
    }
}
