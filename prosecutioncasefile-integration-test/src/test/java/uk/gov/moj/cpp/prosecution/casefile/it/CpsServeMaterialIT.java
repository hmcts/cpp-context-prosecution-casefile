package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_COTR_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_PET_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_SERVE_PTPH_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CPS_UPDATE_COTR_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.DefenceStub.stubDefenceQueryServiceForForm;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubProgressionQueryService;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubProgressionQueryServiceForForm;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithEitherWayModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ANY_OTHER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.FORM_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OTHER_AREAS_AFTER_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.OTHER_AREAS_BEFORE_PTPH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.PROSECUTOR_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.WORDING;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.helper.CpsServeMaterialHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector;
import uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.QueueUtil;
import uk.gov.moj.cpp.prosecution.casefile.helper.SubmitCCApplicationHelper;
import uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.jayway.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class CpsServeMaterialIT extends BaseIT {

    private static final String CASE_MARKER_CODE = "ABC";
    private static final String PET_PAYLOAD = "stub-data/public.stagingprosecutors.cps-serve-pet-received.json";
    private static final String PET_PAYLOAD_ASN_ONLY = "stub-data/public.stagingprosecutors.cps-serve-pet-received_asn_only.json";
    private static final String PET_PAYLOAD_CPS_DEFENDANT_ID_ONLY = "stub-data/public.stagingprosecutors.cps-serve-pet-received_cpsDefendantId_only.json";
    private static final String PET_PAYLOAD_DEFENDANT_DATA_ONLY = "stub-data/public.stagingprosecutors.cps-serve-pet-received_defendantData_only.json";
    private static final String PET_PAYLOAD_ONLY_ONE_MISMATCHED_ASN = "stub-data/public.stagingprosecutors.cps-serve-pet-received_one_unmatched_asn_as_well.json";

    private static final String BCM_PAYLOAD = "stub-data/public.stagingprosecutors.cps-serve-bcm-received.json";
    private static final String BCM_PAYLOAD_ASN_ONLY = "stub-data/public.stagingprosecutors.cps-serve-bcm-received_for_asn_only.json";

    private static final String COTR_PAYLOAD = "stub-data/public.stagingprosecutors.cps-serve-cotr-received.json";
    private static final String COTR_UPDATE_PAYLOAD = "stub-data/public.stagingprosecutors.cps-update-cotr-received.json";

    private static final String COTR_PAYLOAD_DEFENDANT = "stub-data/public.stagingprosecutors.cps-serve-cotr-received-for-defendant.json";
    private static final String COTR_UPDATE_PAYLOAD_DEFENDANT = "stub-data/public.stagingprosecutors.cps-update-cotr-received-for-defendant.json";

    private static final String COTR_PAYLOAD_ASN = "stub-data/public.stagingprosecutors.cps-serve-cotr-received-for-asn.json";
    private static final String COTR_PAYLOAD_DEFENDANT_NAME = "stub-data/public.stagingprosecutors.cps-serve-cotr-received-for-defendant-name.json";
    private static final String COTR_PAYLOAD_DEFENDANT_CORPORATE = "stub-data/public.stagingprosecutors.cps-serve-cotr-received-for-corporate.json";

    private static final String PTPH_PAYLOAD_ASN_ONLY = "stub-data/public.stagingprosecutors.cps-serve-ptph-received_for_asn_only.json";
    private static final String BCM_PAYLOAD_ALL_INVALID_OFFENCES = "stub-data/public.stagingprosecutors.cps-serve-bcm-received_with_all_invlid_offences.json";
    private static final String BCM_PAYLOAD_DEFENDANT_DATA_ONLY = "stub-data/public.stagingprosecutors.cps-serve-bcm-received_for_defendantdata_only.json";

    private static final String PTPH_PAYLOAD_DEFENDANT_DATA_ONLY = "stub-data/public.stagingprosecutors.cps-serve-ptph-received_for_defendantdata_only.json";
    private static final String BCM_PAYLOAD_CPS_DEFENDANT_ID_ONLY = "stub-data/public.stagingprosecutors.cps-serve-bcm-received_for_cpsDefendantId_only.json";
    private static final String BCM_PAYLOAD_ONLY_ONE_MISMATCHED_ASN = "stub-data/public.stagingprosecutors.cps-serve-bcm-received_one_unmatched_asn_as_well.json";

    private static final String PTPH_PAYLOAD_ONLY_ONE_MISMATCHED_ASN = "stub-data/public.stagingprosecutors.cps-serve-ptph-received_one_unmatched_asn_as_well.json";
    private static final String CASE_ID = "caseId";
    private static final String SUBMISSION_ID = "submissionId";
    private static final String PET_DEFENDANTS = "petDefendants";
    private static final String PET_FORM_DATA = "petFormData";
    private static final String FORM_DATA = "formData";
    private static final String USER_NAME = "userName";
    private static final String USER_NAME_VALUE = "cps reviewing lawyer";

    private final JmsMessageConsumerClient publicEventConsumerCpsServeMaterialStatusUpdated = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient publicEventConsumerCpsServePetSubmitted = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PET_SUBMITTED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient publicEventConsumerCpsServeBcmSubmitted = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_BCM_SUBMITTED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient publicEventConsumerCpsServeCotrSubmitted = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient publicEventConsumerCpsUpdateCotrSubmitted = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient publicEventConsumerCpsServePtphSubmitted = newPublicJmsMessageConsumerClientProvider()
            .withEventNames(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_PTPH_SUBMITTED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient privateEventConsumerCpsServePetReceived = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_SELECTOR_CPS_SERVE_PET_RECEIVED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient privateEventConsumerCpsServeBcmReceived = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient privateEventConsumerCpsServeCotrReceived = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_SELECTOR_CPS_SERVE_COTR_RECEIVED)
            .getMessageConsumerClient();

    private final JmsMessageConsumerClient privateEventConsumerCpsUpdateCotrReceived = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_SELECTOR_CPS_UPDATE_COTR_RECEIVED)
            .getMessageConsumerClient();


    private final JmsMessageConsumerClient privateEventConsumerCpsServePtphReceived = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(EVENT_SELECTOR_CPS_SERVE_PTPH_RECEIVED)
            .getMessageConsumerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private UUID caseId;
    private String caseUrn;
    private UUID externalId;
    private String defendantId1;
    private String defendantId2;
    private String defendantId3;
    private String offenceId1;
    private String offenceId2;
    private String offenceId3;
    private String offenceId4;
    private String offenceId5;
    private String offenceId6;
    private String asn1;
    private String asn2;
    private String firstName1;
    private String lastName1;
    private String dateOfBirth1;
    private String firstName2;
    private String lastName2;
    private String dateOfBirth2;

    @BeforeAll
    public static void setup() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();
        externalId = randomUUID();
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        offenceId3 = randomUUID().toString();
        offenceId4 = randomUUID().toString();
        offenceId5 = randomUUID().toString();
        offenceId6 = randomUUID().toString();
        externalId = randomUUID();
        asn1 = randomAlphanumeric(20);
        asn2 = randomAlphanumeric(20);
        firstName1 = "John";
        lastName1 = "rambo";
        dateOfBirth1 = "1986-12-12";
        firstName2 = "Sara";
        lastName2 = "Conner";
        dateOfBirth2 = "2000-11-11";
    }

    @Test
    public void shouldHandle_PublicStagingProsecutors_CpsServePetReceived_WhenCaseIsNotPresent_RaisePrivateEventOnly() {
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD, asn1, caseUrn);

        JsonObject privateEventCpsServeReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePetReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeReceived, is(notNullValue()));
    }

    @Test
    public void shouldMovePetFromPendingStatus() {
        stubOffencesForOffenceCodeList();
        stubProgressionQueryService(caseId, prepareProgressionResponse(readFile("stub-data/progression.query.prosecutioncase.json"), caseUrn));

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD, asn1, caseUrn);

        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);
        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application.json"), caseUrn, caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
    }

    @Test
    public void shouldMoveBcmFromPendingStatus() {
        stubOffencesForOffenceCodeList();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_ASN_ONLY, asn1, caseUrn);

        final JsonObject privateEventCpsServeBcmReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeBcmReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeBcmReceived, is(notNullValue()));
        assertThat(privateEventCpsServeBcmReceived.getJsonObject("formData").getJsonArray("bcmDefendants").get(0).toString().contains("otherInformation"), is(true));
    }

    private String prepareProgressionResponse(final String readFile, final String caseUrn) {
        return readFile.replaceAll("CASE_URN", caseUrn);
    }

    private void verifyCCEventAndProgressionCommand(final String staticPayLoad, final String caseUrn, final UUID caseId) {
        final String ccPayLoad = replaceValuesForCreateCase(staticPayLoad, caseUrn, caseId);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();

        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(),
                        CoreMatchers.is(1));

        sendProgressionCaseCreatedPublicEvent(caseId);
    }

    private void sendProgressionCaseCreatedPublicEvent(final UUID caseId) {
        sendPublicEvent(EventSelector.PUBLIC_PROGRESSION_CASE_CREATED_EVENT, "stub-data/public.progression.prosecution-case-created.json", caseId.toString());
    }

    private String replaceValuesForCreateCase(final String payload, final String caseUrn, final UUID caseId) {
        return payload
                .replace("CASE-ID", caseId.toString())
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", randomUUID().toString())
                .replace("DEFENDANT_REFERENCE1", randomUUID().toString())
                .replace("OFFENCE_ID1", randomUUID().toString())
                .replace("DEFENDANT_ID2", randomUUID().toString())
                .replace("DEFENDANT_REFERENCE2", randomUUID().toString())
                .replace("DEFENDANT_ID3", randomUUID().toString())
                .replace("OFFENCE_ID2", randomUUID().toString())
                .replace("OFFENCE_ID3", randomUUID().toString())
                .replace("OFFENCE_ID4", randomUUID().toString())
                .replace("OFFENCE_ID5", randomUUID().toString())
                .replace("OFFENCE_ID6", randomUUID().toString())
                .replace("INITIATION_CODE", "C")
                .replace("CASE_MARKER", "ABC")
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", randomUUID().toString());
    }

    private String replaceValues(final String payload, final String applicationTypeCode, final String caseUrn) {
        return payload
                .replaceAll("APPLICATION_ID", randomUUID().toString())
                .replaceAll("SOME-RANDOM-APP-CODE", applicationTypeCode)
                .replaceAll("APPLICATION_DUE_DATE", LocalDate.now().plusDays(2).toString())
                .replaceAll("CASE_URN", caseUrn);
    }

    @Test
    public void shouldHandleCpsServePetReceived_WhenCaseUrnPresent_AsnMatchingOnly() {
        createCPPICase();

        stubDefenceQueryServiceForForm(UUID.fromString(defendantId1), "defence.query.associated-organisation.json");
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD_ASN_ONLY, asn1, caseUrn);

        JsonObject privateEventCpsServeReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePetReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeReceived, is(notNullValue()));

        JsonEnvelope petSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServePetSubmitted);
        assertThat(petSubmittedPublicEvent, is(notNullValue()));
        JsonObject petSubmittedPublicEventPayload = petSubmittedPublicEvent.payloadAsJsonObject();

        assertThat(petSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(petSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(petSubmittedPublicEventPayload.getJsonArray(PET_DEFENDANTS), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(PET_FORM_DATA), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(USER_NAME), is(USER_NAME_VALUE));
        assertThat(petSubmittedPublicEventPayload.getString(PET_FORM_DATA), containsString("66 Exeter Street"));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));

        assertTrialRepresentative (privateEventCpsServeReceived);
    }

    @Test
    public void shouldHandleCpsServePetReceived_WhenCaseUrnPresent_OneDefendantMatchedOnly() {
        createCPPICase();

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        final String mismatchedAsn = "ASEC33563LS";
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD_ONLY_ONE_MISMATCHED_ASN, asn1, mismatchedAsn, caseUrn);

        JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.toString()));
        List<JsonObject> errors = publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors").getValuesAs(JsonObject.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getString("code"), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errors.get(0).getJsonArray("values"), hasSize(1));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("key"), is(FormConstant.ASN));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("value"), is(mismatchedAsn));
    }


    @Test
    public void shouldHandleCpsServePetReceived_WhenCaseUrnPresent_DefendantDataOnly() {
        createCPPICase_WithoutAsn();

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD_DEFENDANT_DATA_ONLY, firstName1, lastName1, dateOfBirth1, caseUrn);

        JsonObject privateEventCpsServeReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePetReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeReceived, is(notNullValue()));

        JsonEnvelope petSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServePetSubmitted);
        assertThat(petSubmittedPublicEvent, is(notNullValue()));
        JsonObject petSubmittedPublicEventPayload = petSubmittedPublicEvent.payloadAsJsonObject();

        assertThat(petSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(petSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(petSubmittedPublicEventPayload.getJsonArray(PET_DEFENDANTS), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(PET_FORM_DATA), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(USER_NAME), is(USER_NAME_VALUE));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsServePetReceived_WhenCaseUrnPresent_CpsDefendantIdMatchForOneDefendant_ButNoRulesMatchForSecondDefendant() {
        final String cpsDefendantId = "thisIsACpsId";

        createCPPICase_WithoutAsn();

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-with-cps-defendant-id.json", cpsDefendantId, defendantId1);

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PET_RECEIVED, PET_PAYLOAD_CPS_DEFENDANT_ID_ONLY, cpsDefendantId, caseUrn);

        JsonObject privateEventCpsServeReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePetReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeReceived, is(notNullValue()));

        JsonEnvelope petSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServePetSubmitted);
        assertThat(petSubmittedPublicEvent, is(notNullValue()));
        JsonObject petSubmittedPublicEventPayload = petSubmittedPublicEvent.payloadAsJsonObject();

        assertThat(petSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(petSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));

        final JsonArray petDefendantsAsJsonArray = petSubmittedPublicEventPayload.getJsonArray(PET_DEFENDANTS);
        assertThat(petDefendantsAsJsonArray, hasSize(1));
        assertThat(petDefendantsAsJsonArray.getJsonObject(0).getString("defendantId"), is(defendantId1));

        assertThat(petSubmittedPublicEventPayload.getString(PET_FORM_DATA), notNullValue());
        assertThat(petSubmittedPublicEventPayload.getString(USER_NAME), is(USER_NAME_VALUE));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandlePublicStagingProsecutorsCpsServeBcmReceivedAndRaisePrivateEvent() {
        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD, caseUrn);
        final Optional<JsonEnvelope> privateEvent = cpsServeMaterialHelper.retrieveEvent(EventSelector.EVENT_SELECTOR_CPS_SERVE_BCM_RECEIVED);
        assertThat(privateEvent.isPresent(), is(true));
    }

    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseUrnPresent_AsnMatchingOnly() {
        stubOffencesForOffenceCodeList();
        createCPPICase();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_ASN_ONLY, asn1, caseUrn);

        final JsonObject privateEventCpsServeBcmReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeBcmReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeBcmReceived, is(notNullValue()));

        final JsonEnvelope bcmSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeBcmSubmitted);
        assertThat(bcmSubmittedPublicEvent, is(notNullValue()));
        final JsonObject bcmSubmittedPublicEventPayload = bcmSubmittedPublicEvent.payloadAsJsonObject();

        System.out.println(bcmSubmittedPublicEventPayload.toString());

        assertThat(bcmSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(bcmSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(bcmSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        assertThat(bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS), notNullValue());
        final List<JsonObject> formDefendantsList = bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(formDefendantsList, hasSize(1));
        assertThat(formDefendantsList.get(0).getString(DEFENDANT_ID), is(defendantId1));

        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());
        final JsonObject formDataJsonObject = stringToJsonObjectConverter.convert(bcmSubmittedPublicEventPayload.getString(FORM_DATA));
        assertThat(formDataJsonObject.getJsonArray(DEFENDANTS), notNullValue());
        final List<JsonObject> defendantList = formDataJsonObject.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantList, hasSize(1));
        assertThat(defendantList.get(0).getString(ID), is(defendantId1));
        assertThat(defendantList.get(0).getString(OTHER_AREAS_BEFORE_PTPH), is("evidencePrePTPH text"));
        assertThat(defendantList.get(0).containsKey(OTHER_AREAS_AFTER_PTPH), is(false));
        assertThat(defendantList.get(0).containsKey(ANY_OTHER), is(true));

        assertThat(defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES), notNullValue());
        final List<JsonObject> prosecutorOffencesList = defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(prosecutorOffencesList, hasSize(1));
        assertThat(prosecutorOffencesList.get(0).getString(OFFENCE_CODE), is("CA03013"));
        assertThat(prosecutorOffencesList.get(0).getString(WORDING), is("offenceWording"));
        assertThat(prosecutorOffencesList.get(0).getString(DATE), is("2022-05-11"));

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.SUCCESS.toString()));

    }

    @Test
    public void shouldHandleCpsServePtphReceivedWhenCaseUrnPresent_AsnMatchingOnly() {
        stubOffencesForOffenceCodeList();
        createCPPICase();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        ReferenceDataStub.stubGetOrganisationUnits();

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PTPH_RECEIVED, PTPH_PAYLOAD_ASN_ONLY, caseUrn, asn1);

        final JsonObject privateEventCpsServePtphReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePtphReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServePtphReceived, is(notNullValue()));

        final JsonEnvelope ptphSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServePtphSubmitted);
        assertThat(ptphSubmittedPublicEvent, is(notNullValue()));
        final JsonObject ptphSubmittedPublicEventPayload = ptphSubmittedPublicEvent.payloadAsJsonObject();

        assertThat(ptphSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(ptphSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(ptphSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(ptphSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        assertThat(ptphSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS), notNullValue());
        final List<JsonObject> formDefendantsList = ptphSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(formDefendantsList, hasSize(1));
        assertThat(formDefendantsList.get(0).getString(DEFENDANT_ID), is(defendantId1));

        String expectedFormData = FileUtil.resourceToString("expected/cps-serve-ptph-submitted-form-data.json")
                .replace("{DEFENDANT_1_ID}", defendantId1)
                .replace("{URN_VALUE}", caseUrn);
        JSONAssert.assertEquals(expectedFormData, ptphSubmittedPublicEventPayload.getString(FORM_DATA), JSONCompareMode.LENIENT);

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.SUCCESS.toString()));

    }

    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseUrnPresent_OneDefendantMatchedOnly() {
        final String mismatchedAsn = "ASEC33563LS";

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        createCPPICase();


        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_ONLY_ONE_MISMATCHED_ASN, asn1, mismatchedAsn, caseUrn);

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.REJECTED.toString()));

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors"), notNullValue());
        List<JsonObject> errors = publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors").getValuesAs(JsonObject.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getString("code"), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errors.get(0).getJsonArray("values"), hasSize(1));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("key"), is(FormConstant.ASN));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("value"), is(mismatchedAsn));

    }

    @Test
    public void shouldHandleCpsServePtphReceivedWhenCaseUrnPresent_OneDefendantMatchedOnly() {
        final String mismatchedAsn = "ASEC33563LS";

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        ReferenceDataStub.stubGetOrganisationUnits();
        createCPPICase();


        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PTPH_RECEIVED, PTPH_PAYLOAD_ONLY_ONE_MISMATCHED_ASN, caseUrn, asn1, mismatchedAsn);

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.REJECTED.toString()));

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors"), notNullValue());
        List<JsonObject> errors = publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors").getValuesAs(JsonObject.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getString("code"), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errors.get(0).getJsonArray("values"), hasSize(1));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("key"), is(FormConstant.ASN));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("value"), is(mismatchedAsn));

    }


    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseUrnPresent_DefendantDataOnly_NoAsnPresentInCase() {

        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_DEFENDANT_DATA_ONLY, firstName1, lastName1, dateOfBirth1, caseUrn);

        final JsonObject privateEventCpsServeBcmReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeBcmReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeBcmReceived, is(notNullValue()));

        final JsonEnvelope bcmSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeBcmSubmitted);
        assertThat(bcmSubmittedPublicEvent, is(notNullValue()));
        final JsonObject bcmSubmittedPublicEventPayload = bcmSubmittedPublicEvent.payloadAsJsonObject();

        System.out.println(bcmSubmittedPublicEventPayload.toString());

        assertThat(bcmSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(bcmSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(bcmSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        assertThat(bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS), notNullValue());
        final List<JsonObject> formDefendantsList = bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(formDefendantsList, hasSize(1));
        assertThat(formDefendantsList.get(0).getString(DEFENDANT_ID), is(defendantId1));

        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());
        final JsonObject formDataJsonObject = stringToJsonObjectConverter.convert(bcmSubmittedPublicEventPayload.getString(FORM_DATA));
        assertThat(formDataJsonObject.getJsonArray(DEFENDANTS), notNullValue());
        final List<JsonObject> defendantList = formDataJsonObject.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantList, hasSize(1));
        assertThat(defendantList.get(0).getString(ID), is(defendantId1));
        assertThat(defendantList.get(0).getString(OTHER_AREAS_BEFORE_PTPH), is("evidencePrePTPH text"));
        assertThat(defendantList.get(0).containsKey(OTHER_AREAS_AFTER_PTPH), is(false));
        assertThat(defendantList.get(0).containsKey(ANY_OTHER), is(false));

        assertThat(defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES), notNullValue());
        final List<JsonObject> prosecutorOffencesList = defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(prosecutorOffencesList, hasSize(1));
        assertThat(prosecutorOffencesList.get(0).getString(OFFENCE_CODE), is("CA03013"));
        assertThat(prosecutorOffencesList.get(0).getString(WORDING), is("offenceWording"));
        assertThat(prosecutorOffencesList.get(0).getString(DATE), is("2022-05-11"));

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.SUCCESS.toString()));

    }

    @Test
    public void shouldHandleCpsServePtphReceivedWhenCaseUrnPresent_DefendantDataOnly() {

        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        ReferenceDataStub.stubGetOrganisationUnits();
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_PTPH_RECEIVED, PTPH_PAYLOAD_DEFENDANT_DATA_ONLY, firstName1, lastName1, dateOfBirth1, caseUrn);

        final JsonObject privateEventCpsServePtphReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServePtphReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServePtphReceived, is(notNullValue()));

        final JsonEnvelope ptphSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServePtphSubmitted);
        assertThat(ptphSubmittedPublicEvent, is(notNullValue()));
        final JsonObject ptphSubmittedPublicEventPayload = ptphSubmittedPublicEvent.payloadAsJsonObject();

        System.out.println(ptphSubmittedPublicEventPayload.toString());

        assertThat(ptphSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(ptphSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(ptphSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(ptphSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        assertThat(ptphSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS), notNullValue());
        final List<JsonObject> formDefendantsList = ptphSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(formDefendantsList, hasSize(1));
        assertThat(formDefendantsList.get(0).getString(DEFENDANT_ID), is(defendantId1));

        assertThat(ptphSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        String expected = FileUtil.resourceToString("expected/cpsServePtphReceivedWhenCaseUrnPresent_DefendantDataOnly.json")
                .replace("{DEFENDANT_1_ID}", defendantId1)
                .replace("{URN_VALUE}", caseUrn);

        JSONAssert.assertEquals(expected, ptphSubmittedPublicEventPayload.getString(FORM_DATA), JSONCompareMode.LENIENT);

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.SUCCESS.toString()));

    }


    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseUrnPresent_CpsDefendantIdMatchForOneDefendant_ButNoRulesMatchForSecondDefendant() {
        final String cpsDefendantId = "thisIsACpsId";
        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-with-cps-defendant-id.json", cpsDefendantId, defendantId1);

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_CPS_DEFENDANT_ID_ONLY, cpsDefendantId, caseUrn);

        final JsonObject privateEventCpsServeBcmReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeBcmReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeBcmReceived, is(notNullValue()));

        final JsonEnvelope bcmSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeBcmSubmitted);
        assertThat(bcmSubmittedPublicEvent, is(notNullValue()));
        final JsonObject bcmSubmittedPublicEventPayload = bcmSubmittedPublicEvent.payloadAsJsonObject();

        System.out.println(bcmSubmittedPublicEventPayload.toString());

        assertThat(bcmSubmittedPublicEventPayload, is(notNullValue()));
        assertThat(bcmSubmittedPublicEventPayload.getString(CASE_ID), notNullValue());
        assertThat(bcmSubmittedPublicEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());

        assertThat(bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS), notNullValue());
        final List<JsonObject> formDefendantsList = bcmSubmittedPublicEventPayload.getJsonArray(FORM_DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(formDefendantsList, hasSize(1));
        assertThat(formDefendantsList.get(0).getString(DEFENDANT_ID), is(defendantId1));

        assertThat(bcmSubmittedPublicEventPayload.getString(FORM_DATA), notNullValue());
        final JsonObject formDataJsonObject = stringToJsonObjectConverter.convert(bcmSubmittedPublicEventPayload.getString(FORM_DATA));
        assertThat(formDataJsonObject.getJsonArray(DEFENDANTS), notNullValue());
        final List<JsonObject> defendantList = formDataJsonObject.getJsonArray(DEFENDANTS).getValuesAs(JsonObject.class);
        assertThat(defendantList, hasSize(1));
        assertThat(defendantList.get(0).getString(ID), is(defendantId1));
        assertThat(defendantList.get(0).getString(OTHER_AREAS_BEFORE_PTPH), is("evidencePrePTPH text"));
        assertThat(defendantList.get(0).containsKey(OTHER_AREAS_AFTER_PTPH), is(false));
        assertThat(defendantList.get(0).containsKey(ANY_OTHER), is(false));

        assertThat(defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES), notNullValue());
        final List<JsonObject> prosecutorOffencesList = defendantList.get(0).getJsonArray(PROSECUTOR_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(prosecutorOffencesList, hasSize(1));
        assertThat(prosecutorOffencesList.get(0).getString(OFFENCE_CODE), is("CA03013"));
        assertThat(prosecutorOffencesList.get(0).getString(WORDING), is("offenceWording"));
        assertThat(prosecutorOffencesList.get(0).getString(DATE), is("2022-05-11"));

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.SUCCESS.toString()));

    }

    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseUrnPresent_Rejection_AllDefendantInvalid() {

        createCPPICase();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        final String mismatchedAsn = "mismatchedAsnValue";
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_BCM_RECEIVED, BCM_PAYLOAD_ASN_ONLY, mismatchedAsn, caseUrn);

        final JsonEnvelope publicEventCpsServeMaterialStatusUpdatedEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdatedEvent, is(notNullValue()));
        final JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdatedEvent.payloadAsJsonObject();

        System.out.println(publicEventCpsServeMaterialStatusUpdatedEventPayload.toString());

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(SUBMISSION_ID), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString(FormConstant.SUBMISSION_STATUS), is(SubmissionStatus.REJECTED.toString()));

        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors"), notNullValue());
        List<JsonObject> errors = publicEventCpsServeMaterialStatusUpdatedEventPayload.getJsonArray("errors").getValuesAs(JsonObject.class);
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getString("code"), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(errors.get(0).getJsonArray("values"), hasSize(1));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("key"), is(ASN));
        assertThat(errors.get(0).getJsonArray("values").getValuesAs(JsonObject.class).get(0).getString("value"), is(mismatchedAsn));
    }


    private void createCPPICase() {
        final String ccPayLoad = getCcPayload();
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        Awaitility.await().timeout(60, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(),
                        CoreMatchers.is(1));
    }


    private String getCcPayload() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution1.json");
        return staticPayLoad
                .replace("CASE-ID", this.caseId.toString())
                .replace("CHANNEL", "CPPI")
                .replace("CASE-URN", caseUrn)
                .replace("ASN_ID1", asn1)
                .replace("ASN_ID2", asn2)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE3", this.defendantId3)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6)
                .replace("INITIATION_CODE", "C")
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replace("FIRST_NAME_1", firstName1)
                .replace("LAST_NAME_1", lastName1)
                .replace("DATE_OF_BIRTH_1", dateOfBirth1)
                .replace("FIRST_NAME_2", firstName2)
                .replace("LAST_NAME_2", lastName2)
                .replace("DATE_OF_BIRTH_2", dateOfBirth2);
    }

    private void createCPPICase_WithoutAsn() {
        final String ccPayLoad = getCcPayload_WithoutAsn();
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        Awaitility.await().timeout(60, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(),
                        CoreMatchers.is(1));
    }

    private String getCcPayload_WithoutAsn() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-without-asn.json");
        return staticPayLoad
                .replace("CASE-ID", this.caseId.toString())
                .replace("CHANNEL", "CPPI")
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE3", this.defendantId3)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6)
                .replace("INITIATION_CODE", "C")
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replace("FIRST_NAME_1", firstName1)
                .replace("LAST_NAME_1", lastName1)
                .replace("DATE_OF_BIRTH_1", dateOfBirth1)
                .replace("FIRST_NAME_2", firstName2)
                .replace("LAST_NAME_2", lastName2)
                .replace("DATE_OF_BIRTH_2", dateOfBirth2);
    }

    @Test
    public void shouldHandlePublicStagingProsecutorsCpsServeCotrReceived_RaisePublicEvent_WhenCaseUrnNotPresent() {
        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD, caseUrn);
        final Optional<JsonEnvelope> publicEvent = cpsServeMaterialHelper.retrieveEvent(EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED);
        assertThat(publicEvent.isPresent(), is(true));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseUrnPresent_SubmissionSucess() {
        final String cpsDefendantId = "thisIsACpsId";

        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-with-cps-defendant-id.json", cpsDefendantId, defendantId1);

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD_DEFENDANT, caseUrn, cpsDefendantId, defendantId1);

        final JsonObject privateEventCpsServeCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeCotrReceived, is(notNullValue()));

        final JsonEnvelope cotrSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeCotrSubmitted);
        assertThat(cotrSubmittedPublicEvent, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseUrnPresent_SubmissionRejected() {
        createCPPICase();

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD, caseUrn);

        final JsonObject privateEventCpsServeCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeCotrReceived, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.REJECTED.toString()));
    }

    @Test
    public void shouldHandlePublicStagingProsecutorsCpsUpdateCotrReceived_RaisePublicEvent_WhenCaseUrnNotPresent() {
        final CpsServeMaterialHelper cpsServeMaterialHelper = new CpsServeMaterialHelper();
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_UPDATE_COTR_RECEIVED, COTR_UPDATE_PAYLOAD, caseUrn);

        final Optional<JsonEnvelope> publicEvent = cpsServeMaterialHelper.retrieveEvent(EventSelector.PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_MATERIAL_STATUS_UPDATED);
        assertThat(publicEvent.isPresent(), is(true));
    }

    @Test
    public void shouldHandleCpsUpdateCotrReceivedWhenCaseUrnPresent_SubmissionSucess() {
        final String cpsDefendantId = "thisIsACpsId";

        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-with-cps-defendant-id.json", cpsDefendantId, defendantId1);

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_UPDATE_COTR_RECEIVED, COTR_UPDATE_PAYLOAD_DEFENDANT, caseUrn, cpsDefendantId, defendantId1);

        final JsonObject privateEventCpsUpdateCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsUpdateCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsUpdateCotrReceived, is(notNullValue()));

        final JsonEnvelope cotrUpdateSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsUpdateCotrSubmitted);
        assertThat(cotrUpdateSubmittedPublicEvent, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsUpdateCotrReceivedWhenCaseUrnPresent_SubmissionSuccess() {
        final String cotrId = randomUUID().toString();
        createCPPICase();

        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");
        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_UPDATE_COTR_RECEIVED, COTR_UPDATE_PAYLOAD, caseUrn);

        final JsonObject privateEventCpsUpdateCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsUpdateCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsUpdateCotrReceived, is(notNullValue()));

        JsonEnvelope publicEventCpsUpdateMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsUpdateMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsUpdateMaterialStatusUpdatedEventPayload = publicEventCpsUpdateMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsUpdateMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseUrn_ASN_Exists_SubmissionSucess() {
        final String cpsDefendantId = "thisIsACpsId";

        createCPPICase();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-with-cps-defendant-id.json", cpsDefendantId, defendantId1);

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD_ASN, caseUrn, asn1);

        final JsonObject privateEventCpsServeCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeCotrReceived, is(notNullValue()));

        final JsonEnvelope cotrSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeCotrSubmitted);
        assertThat(cotrSubmittedPublicEvent, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseUrn_Name_Exists_SubmissionSucess() {
        createCPPICase_WithoutAsn();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD_DEFENDANT_NAME, caseUrn, firstName1, lastName1, dateOfBirth1);

        final JsonObject privateEventCpsServeCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeCotrReceived, is(notNullValue()));

        final JsonEnvelope cotrSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeCotrSubmitted);
        assertThat(cotrSubmittedPublicEvent, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseUrn_corporate_Exists_SubmissionSucess() {
        createCPPICaseForCorporate();
        stubProgressionQueryServiceForForm(caseId, "progression.query.prosecutioncase-for-form-without-cps-defendant-id.json");

        sendPublicEvent(EventSelector.PUBLIC_STAGING_PROSECUTORS_CPS_SERVE_COTR_RECEIVED, COTR_PAYLOAD_DEFENDANT_CORPORATE, caseUrn);

        final JsonObject privateEventCpsServeCotrReceived = QueueUtil.getEventFromQueue(privateEventConsumerCpsServeCotrReceived).payloadAsJsonObject();
        assertThat(privateEventCpsServeCotrReceived, is(notNullValue()));

        final JsonEnvelope cotrSubmittedPublicEvent = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeCotrSubmitted);
        assertThat(cotrSubmittedPublicEvent, is(notNullValue()));

        JsonEnvelope publicEventCpsServeMaterialStatusUpdated = QueueUtil.getEventFromQueue(publicEventConsumerCpsServeMaterialStatusUpdated);
        assertThat(publicEventCpsServeMaterialStatusUpdated, is(notNullValue()));

        JsonObject publicEventCpsServeMaterialStatusUpdatedEventPayload = publicEventCpsServeMaterialStatusUpdated.payloadAsJsonObject();
        assertThat(publicEventCpsServeMaterialStatusUpdatedEventPayload.getString("submissionStatus"), is(SubmissionStatus.SUCCESS.toString()));
    }

    private void assertTrialRepresentative(final JsonObject privateEventCpsServeReceived) {
        final JsonObject jsonObject = privateEventCpsServeReceived.getJsonObject("petFormData")
                .getJsonObject("defence")
                .getJsonArray("defendants")
                .getJsonObject(0)
                .getJsonObject("trialRepresentative");

        assertThat(jsonObject.getString("representative"), is("Jedi and Sons LLP"));
        assertThat(jsonObject.getString("representativeAddress"), is("Jedi and Sons LLP, 35 Bridget Avenue, London, England, CR5 7UH"));
        assertThat(jsonObject.getString("email"), is("jedi@jedi.test.com"));
        assertThat(jsonObject.getString("phone"), is("01234555666"));
    }

    private void createCPPICaseForCorporate() {
        final String ccPayLoad = getCcPayload_corporate();
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        Awaitility.await().timeout(60, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(caseUrn))).size(),
                        CoreMatchers.is(1));
    }


    private String getCcPayload_corporate() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-organisation.json");
        return staticPayLoad
                .replace("CASE-ID", this.caseId.toString())
                .replace("CHANNEL", "CPPI")
                .replace("CASE-URN", caseUrn)
                .replace("ASN_ID1", asn1)
                .replace("ASN_ID2", asn2)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE3", this.defendantId3)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6)
                .replace("INITIATION_CODE", "C")
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replace("FIRST_NAME_1", firstName1)
                .replace("LAST_NAME_1", lastName1)
                .replace("DATE_OF_BIRTH_1", dateOfBirth1)
                .replace("FIRST_NAME_2", firstName2)
                .replace("LAST_NAME_2", lastName2)
                .replace("DATE_OF_BIRTH_2", dateOfBirth2);
    }
}
