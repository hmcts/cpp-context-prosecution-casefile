package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.uploadFile;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_PENDING;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MIGRATED_CASE_RECORDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROGRESSION_CASE_CREATED_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithEitherWayModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitsReturnsMag;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordMigratedCaseIT extends BaseIT {

    private static final long RETRIEVE_TIMEOUT = 35000L;
    private static final String DEFENDANT_LEVEL_DOCUMENT_TYPE = "SJPN";
    private static final String CASE_MARKER_CODE = "ABC";

    private String caseId;
    private String caseUrn;
    private String defendantId;
    private String asn;

    @BeforeAll
    public static void setUpOnce() {
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        caseUrn = randomAlphanumeric(10);
        defendantId = randomUUID().toString();
        asn = "LIBRA-ASN-" + randomAlphanumeric(6);
    }

    @Test
    public void shouldRecordMigratedCaseWhenProgressionCreatesLibraCase() {
        givenLibraMigratedCaseRecorded();
    }

    @Test
    public void shouldAddCpsMaterialMatchedByAsnForLibraMigratedCase() throws Exception {
        givenLibraMigratedCaseRecorded();

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final UUID submissionId = randomUUID();
        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final JsonObject addCpsMaterialCommandPayload = readJsonResource("stub-data/prosecutioncasefile.add-cps-material.json",
                asn, uploadId, DEFENDANT_LEVEL_DOCUMENT_TYPE, PDF_MIME_TYPE);

        addMaterialHelper.addCpsMaterial(UUID.fromString(caseId), submissionId, addCpsMaterialCommandPayload);

        final Optional<JsonEnvelope> materialAdded = addMaterialHelper.retrieveMessageWithMatchers(EVENT_SELECTOR_MATERIAL_ADDED,
                isJson(withJsonPath("$.caseId", is(caseId))));
        assertThat(materialAdded.isPresent(), is(true));
        assertThat(materialAdded.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_ADDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId)),
                withJsonPath("caseType", is("CC")),
                withJsonPath("documentCategory", is("Defendant level")),
                withJsonPath("prosecutorDefendantId", is(asn)),
                withJsonPath("defendantId", is(defendantId)),
                withJsonPath("material.fileStoreId", is(uploadId.toString()))
        ))));

        addMaterialHelper.verifyUploadMaterialCalled(uploadId.toString());
    }

    @Test
    public void shouldLeaveLibraCaseSubmittedViaMccUnchangedWhenProgressionCreatesIt() throws Exception {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubGetOrganisationUnitsReturnsMag("B04NM04");
        stubGetOrganisationUnitsReturnsMag("B02NM03");
        stubGetOrganisationUnitWithOneCourtroom();
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();

        final String mccLibraPayload = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc.json")
                .replace("DEFENDANT_ID1", defendantId)
                .replace("DEFENDANT_ID2", randomUUID().toString())
                .replace("OFFENCE_ID1", randomUUID().toString())
                .replace("OFFENCE_ID2", randomUUID().toString())
                .replace("\"initiationCode\": \"C\"", "\"initiationCode\": \"J\"")
                .replace("\"channel\": \"MCC\"",
                        "\"migrationSourceSystem\": { \"migrationSourceSystemName\": \"LIBRA\" },\n  \"channel\": \"MCC\"");

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(mccLibraPayload);
        final JsonObject mccCaseDetails = initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised()
                .payloadAsJsonObject()
                .getJsonObject("prosecutionWithReferenceData")
                .getJsonObject("prosecution")
                .getJsonObject("caseDetails");
        final String mccCaseId = mccCaseDetails.getString("caseId");
        final String mccCaseUrn = mccCaseDetails.getString("prosecutorCaseReference");

        sendPublicEvent(PUBLIC_PROGRESSION_CASE_CREATED_EVENT, "stub-data/public.progression.prosecution-case-created-libra.json",
                mccCaseId, mccCaseUrn, defendantId, asn);

        final UUID uploadId = uploadFile(PDF_MIME_TYPE);
        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();
        final JsonObject addCpsMaterialCommandPayload = readJsonResource("stub-data/prosecutioncasefile.add-cps-material.json",
                asn, uploadId, DEFENDANT_LEVEL_DOCUMENT_TYPE, PDF_MIME_TYPE);

        addMaterialHelper.addCpsMaterial(UUID.fromString(mccCaseId), randomUUID(), addCpsMaterialCommandPayload);

        final Optional<JsonEnvelope> materialPending = addMaterialHelper.retrieveMessageWithMatchers(EVENT_SELECTOR_MATERIAL_PENDING,
                isJson(withJsonPath("$.caseId", is(mccCaseId))));
        assertThat(materialPending.isPresent(), is(true));
        assertThat(materialPending.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MATERIAL_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(mccCaseId)),
                withJsonPath("material.fileStoreId", is(uploadId.toString()))
        ))));
    }

    private void givenLibraMigratedCaseRecorded() {
        final JmsMessageConsumerClient migratedCaseRecordedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(EVENT_SELECTOR_MIGRATED_CASE_RECORDED)
                .getMessageConsumerClient();

        sendPublicEvent(PUBLIC_PROGRESSION_CASE_CREATED_EVENT, "stub-data/public.progression.prosecution-case-created-libra.json",
                caseId, caseUrn, defendantId, asn);

        final Optional<JsonEnvelope> migratedCaseRecorded = migratedCaseRecordedConsumer.retrieveMessageAsJsonEnvelope(RETRIEVE_TIMEOUT);
        assertThat(migratedCaseRecorded.isPresent(), is(true));
        assertThat(migratedCaseRecorded.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_MIGRATED_CASE_RECORDED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId)),
                withJsonPath("prosecutionCaseReference", is(caseUrn)),
                withJsonPath("migrationSourceSystemName", is("LIBRA")),
                withJsonPath("migrationSourceSystemCaseIdentifier", is("LIB-IT-0001")),
                withJsonPath("defendants[0].id", is(defendantId)),
                withJsonPath("defendants[0].asn", is(asn)),
                withJsonPath("defendants[0].firstName", is("Harry")),
                withJsonPath("defendants[0].lastName", is("Kane Junior"))
        ))));
    }
}
