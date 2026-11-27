package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EXTERNAL_EVENT_MATERIAL_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EXTERNAL_EVENT_SJP_CASE_CREATED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileServiceHelper.create;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.CreateSjpCaseStub.resetAndStubCreateSjpCase;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.helper.AddIDPCMaterialHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateSjpProsecutionHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AddIDPCMaterialIT extends BaseIT {

    private final static String PDF_MIME_TYPE = "application/pdf";
    private final UUID caseId = randomUUID();
    private final UUID externalId = randomUUID();
    private final String caseUrn = randomAlphanumeric(10);
    final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper = new InitiateSjpProsecutionHelper();

    private static JsonObject buildAddMaterialCommandPayload(final UUID fileServiceId) {
        return readJsonResource("stub-data/prosecutioncasefile.add-idpc-material.json", fileServiceId);
    }

    private void sendMaterialAddedPublicEvent(final String idpcId, final UUID materialId) {
        final MetadataBuilder metadataBuilder = metadataFrom(
                createObjectBuilder(metadataWithRandomUUID(EXTERNAL_EVENT_MATERIAL_ADDED).build().asJsonObject())
                        .add("idpcId", idpcId)
                        .build());

        final JsonEnvelope publicEvent = envelopeFrom(
                metadataBuilder,
                readJsonResource("stub-data/material.material-added.json", materialId.toString()));

        sendPublicEvent(EXTERNAL_EVENT_MATERIAL_ADDED, publicEvent);
    }


    private static JsonObject buildInitiateSjpProsecutionCommandPayload(final UUID caseId, final String caseUrn, final UUID externalId, final String prosecutorDefendantReference, final LocalDate chargeDate) {
        return readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution.json",
                ImmutableMap.<String, Object>builder()
                        .put("case.id", caseId)
                        .put("case.urn", caseUrn)
                        .put("defendant.dob", LocalDate.now().minusYears(22))
                        .put("defendant.nationality", "GBR")
                        .put("defendant.id", prosecutorDefendantReference)
                        .put("offence.chargeDate", chargeDate.toString())
                        .put("offence.committedDate", chargeDate.minusMonths(6))
                        .put("external.id", externalId)
                        .put("channel", "CPPI")
                        .build());
    }

    private void sendSjpCaseCreatedPublicEvent(final UUID caseId) {
        sendPublicEvent(EXTERNAL_EVENT_SJP_CASE_CREATED, "stub-data/public.sjp.sjp-case-created.json", caseId.toString());
    }

    private static UUID uploadFile(final String mimeType) throws Exception {
        return create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }

    @BeforeAll
    public static void setUp() {
        stubWiremocks();
        resetAndStubCreateSjpCase();
    }

    private static void stubWiremocks() {
        stubGetCaseMarkersWithCode("ABC");
    }

    @Test
    public void shouldHandleAddIDPCMaterialCommandIdpcMatched() throws Exception {
        final UUID fileServiceId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);


        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addIDPCMaterialCommandPayload = buildAddMaterialCommandPayload(fileServiceId);


        final AddIDPCMaterialHelper addIDPCMaterialHelper = new AddIDPCMaterialHelper();

        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        addIDPCMaterialHelper.addIDPCMaterial(caseId, addIDPCMaterialCommandPayload);

        final Optional<JsonEnvelope> firstPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED);
        assertThat(firstPrivateEvent.isPresent(), is(true));

        assertThat(firstPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));

        final Optional<JsonEnvelope> secondPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED);
        assertThat(secondPrivateEvent.isPresent(), is(true));

        assertThat(secondPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED), payload().isJson(anyOf(
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType"))),
                withJsonPath("defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId")))
        ))));

        final JSONObject payload = addIDPCMaterialHelper.verifyUploadFileCalledAndGetPayload(fileServiceId.toString());
        final String materialId = payload.getString("fileServiceId");
        sendMaterialAddedPublicEvent(payload.getJSONObject("_metadata").getString("idpcId"), fromString(materialId));


        final Optional<JsonEnvelope> publicEvent = addIDPCMaterialHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(publicEvent.get(), jsonEnvelope(metadata().withName(PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED), payload().isJson(anyOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("materialId", is(materialId))
        ))));

    }

    @Test
    public void shouldRiaseIdpcMatchedEventAfterCaseDefendantMatch() throws Exception {
        final UUID fileServiceId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);

        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);

        final JsonObject addIDPCMaterialCommandPayload = buildAddMaterialCommandPayload(fileServiceId);

        final AddIDPCMaterialHelper addIDPCMaterialHelper = new AddIDPCMaterialHelper();
        addIDPCMaterialHelper.addIDPCMaterial(caseId, addIDPCMaterialCommandPayload);

        final Optional<JsonEnvelope> firstPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED);
        assertThat(firstPrivateEvent.isPresent(), is(true));

        assertThat(firstPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));


        final Optional<JsonEnvelope> secondPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING);
        assertThat(secondPrivateEvent.isPresent(), is(true));

        assertThat(secondPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));

        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        final Optional<JsonEnvelope> fourthPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED);
        assertThat(fourthPrivateEvent.isPresent(), is(true));

        assertThat(fourthPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED), payload().isJson(anyOf(
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType"))),
                withJsonPath("defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId")))
        ))));
    }

    @Test
    public void shouldHandleAddIDPCMaterialCommandIdpcMatchPending() throws Exception {

        final AddIDPCMaterialHelper addIDPCMaterialHelper = new AddIDPCMaterialHelper();

        final UUID caseId = randomUUID();
        final UUID fileServiceId = uploadFile(PDF_MIME_TYPE);
        final JsonObject addIDPCMaterialCommandPayload = buildAddMaterialCommandPayload(fileServiceId);
        addIDPCMaterialHelper.addIDPCMaterial(caseId, addIDPCMaterialCommandPayload);

        final Optional<JsonEnvelope> firstPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED);
        assertThat(firstPrivateEvent.isPresent(), is(true));

        assertThat(firstPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));


        final Optional<JsonEnvelope> secondPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING);
        assertThat(secondPrivateEvent.isPresent(), is(true));

        assertThat(secondPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));

    }

    @Test
    public void shouldRaiseIdpcAlreadyExistsEvent() throws Exception {
        final UUID fileServiceId = uploadFile(PDF_MIME_TYPE);
        final String prosecutorDefendantReference = UUID.randomUUID().toString();
        final LocalDate chargeDate = LocalDate.now().minusYears(2);


        final JsonObject initiateSjpProsecutionCommandPayload = buildInitiateSjpProsecutionCommandPayload(caseId, caseUrn, externalId, prosecutorDefendantReference, chargeDate);
        final JsonObject addIDPCMaterialCommandPayload = buildAddMaterialCommandPayload(fileServiceId);

        final AddIDPCMaterialHelper addIDPCMaterialHelper = new AddIDPCMaterialHelper();
        initiateSjpProsecution(initiateSjpProsecutionHelper, initiateSjpProsecutionCommandPayload);

        addIDPCMaterialHelper.addIDPCMaterial(caseId, addIDPCMaterialCommandPayload);

        final Optional<JsonEnvelope> firstPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED);
        assertThat(firstPrivateEvent.isPresent(), is(true));

        assertThat(firstPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));

        final Optional<JsonEnvelope> secondPrivateEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED);
        assertThat(secondPrivateEvent.isPresent(), is(true));

        assertThat(secondPrivateEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED), payload().isJson(anyOf(
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType"))),
                withJsonPath("defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId")))
        ))));

        addIDPCMaterialHelper.addIDPCMaterial(caseId, addIDPCMaterialCommandPayload);


        final Optional<JsonEnvelope> fifthEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED);
        assertThat(fifthEvent.isPresent(), is(true));

        assertThat(fifthEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED), payload().isJson(allOf(
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("caseUrn", is(addIDPCMaterialCommandPayload.getString("caseUrn"))),
                withJsonPath("defendant.defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId"))),
                withJsonPath("defendant.oucode", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("oucode"))),
                withJsonPath("defendant.surname", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("surname"))),
                withJsonPath("defendant.forenames", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("forenames"))),
                withJsonPath("defendant.dob", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("dob"))),
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("materialType", is(addIDPCMaterialCommandPayload.getString("materialType")))
        ))));


        final Optional<JsonEnvelope> sixthEvent = addIDPCMaterialHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS);
        assertThat(sixthEvent.isPresent(), is(true));

        assertThat(sixthEvent.get(), jsonEnvelope(metadata().withName(EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS), payload().isJson(anyOf(
                withJsonPath("fileServiceId", is(fileServiceId.toString())),
                withJsonPath("caseId", is(caseId.toString())),
                withJsonPath("defendantId", is(addIDPCMaterialCommandPayload.getJsonObject("defendant").getString("defendantId")))
        ))));
    }

    private void initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final JsonObject initiateSjpProsecutionCommandPayload) {
        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionCommandPayload);

        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED),
                payload().isJson(withJsonPath("prosecution.caseDetails.caseId", is(caseId.toString())))));

        sendSjpCaseCreatedPublicEvent(caseId);
    }
}
