package uk.gov.moj.cpp.prosecution.casefile.it;

import com.jayway.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector;
import uk.gov.moj.cpp.prosecution.casefile.helper.FileServiceHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.SubmitCCApplicationHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.bigDecimal;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.NotifyStub.stubNotificationForEmail;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForQueryApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubProgressionQueryService;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetApplicationType;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroomForSubmitApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

public class SubmitCCApplicationIT extends BaseIT {

    private static final String applicationTypeCode = "CJ03564";
    public static final String NOTIFICATIONNOTIFY_EMAIL_JSON_CONTENT_TYPE = "application/vnd.notificationnotify.email+json";
    private UUID applicationId;
    public static final String PUBLIC_PROGRESSION_EVENT_COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    public static final String NOTIFICATIONNOTIFY_SEND_EMAIL_NOTIFICATION = "notificationnotify.send-email-notification";
    public static final String PROSECUTION_CASEFILE_EVENT = "prosecutioncasefile.events.court-application-created-from-progression";
    final String NOTIFICATION_NOTIFY_COMMAND_URL = "/notificationnotify-service/command/api/rest/notificationnotify/notifications/.*";
    private String prosecutorCost;
    private boolean summonsSuppressed;
    private boolean personalService;


    @BeforeAll
    public static void setUpClass() {
        stubGetApplicationType(applicationTypeCode);
        stubGetCaseMarkersWithCode("ABC");
        stubGetOrganisationUnitWithOneCourtroomForSubmitApplication();
        stubNotificationForEmail();
    }


    @BeforeEach
    public void setUp() {
        applicationId = randomUUID();
        prosecutorCost = format("£%s", bigDecimal(100, 10000, 2).next());
        summonsSuppressed = BOOLEAN.next();
        personalService = BOOLEAN.next();
    }

    @Test
    public void shouldRaiseSubmitApplicationValidationFailedWithInvalidApplicationTypeWhenApiRequestMadeToSubmitCCApplication() {

        final String caseUrn = randomAlphanumeric(10);
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.submit-application.json");
        final String ccApplicationPayLoad = replaceValues(staticPayLoad, "invalidAppType", caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();
    }


    @Test
    public void shouldRaiseSubmitApplicationAcceptedWhenApiRequestMadeToSubmitCCApplication() {

        final String caseUrn = randomAlphanumeric(10);
        final UUID caseId = randomUUID();
        stubProgressionQueryService(caseId, prepareProgressionResponse(readFile("stub-data/progression.query.prosecutioncase.json"), caseUrn));
        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);
        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application.json"), applicationTypeCode, caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();
    }

    @Test
    public void shouldRaiseSubmitApplicationAcceptedWhenApiRequestMadeToSubmitCCApplicationWithPocaDetail() throws SQLException, FileServiceException {
        stubForQueryApplication(UUID.fromString("381e06a5-14bb-4ae0-ac71-b7e63d52f3bb"));
        stubGetDocumentsTypeAccess("stub-data/referencedata.get-all-document-type-access.json");
        final String caseUrn = randomAlphanumeric(10);
        final UUID caseId = randomUUID();
        final UUID fileStoreId = FileServiceHelper.create("iw018-eng-new.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", getSystemResourceAsStream("docx/iw018-eng-new.docx"));
        stubProgressionQueryService(caseId, prepareProgressionResponse(readFile("stub-data/progression.query.prosecutioncase-pocadetails.json"), caseUrn, fileStoreId.toString()));
        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);
        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application-poca.json"), caseUrn, fileStoreId);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(EVENT_SELECTOR_INITIATE_APPLICATION_ACCEPTED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();

        sendPublicEvent(PUBLIC_PROGRESSION_EVENT_COURT_APPLICATION_CREATED, "stub-data/public.progression.court-application-event-created.json",
                this.applicationId.toString(), caseId.toString(), this.prosecutorCost, String.valueOf(this.summonsSuppressed), String.valueOf(this.personalService));
        final SubmitCCApplicationHelper submitCCApplicationHelper2 = new SubmitCCApplicationHelper(PROSECUTION_CASEFILE_EVENT);
        submitCCApplicationHelper2.thenPrivateEventShouldBeRaised();
    }

    @Test
    public void shouldRaiseSubmitApplicationValidationFailedWhenApiRequestMadeToSubmitCCApplicationWithInvalidValue() {
        final String caseUrn = randomAlphanumeric(10);
        final UUID caseId = randomUUID();

        stubProgressionQueryService(caseId, prepareProgressionResponse(readFile("stub-data/progression.query.prosecutioncase-pocadetails.json"), caseUrn));
        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);

        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application-poca-invalid-details.json"), applicationTypeCode, caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();

        verifyNotificationNotifyAPICalled(NOTIFICATION_NOTIFY_COMMAND_URL,1);
    }

    public void verifyNotificationNotifyAPICalled(final String url, final int expectedServiceCallCount) {
        await().timeout(Duration.FIVE_SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(
                                postRequestedFor(urlPathMatching(url))
                                        .withHeader("Content-Type", equalTo(NOTIFICATIONNOTIFY_EMAIL_JSON_CONTENT_TYPE))
                                        .withRequestBody(containing("Sender email/PocaFile Id info missing"))
                                        .withRequestBody(containing("test@test.com"))
                        ).size(),
                        CoreMatchers.is(expectedServiceCallCount));

    }

    private String prepareProgressionResponse(final String readFile, final String caseUrn) {
        return readFile.replaceAll("CASE_URN", caseUrn);
    }

    private String prepareProgressionResponse(final String readFile, final String caseUrn, final String pocaFileId) {
        return readFile.replaceAll("CASE_URN", caseUrn)
                .replaceAll("POCA_FILE_ID", pocaFileId);
    }

    @Test
    public void shouldRaiseSubmitApplicationValidationFailedWithInvalidThirdPartyPersonNameWhenApiRequestMadeToSubmitCCApplication() {

        final String caseUrn = randomAlphanumeric(10);
        final UUID caseId = randomUUID();
        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);
        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application-when-thirdparty-persondetails-invalid-firstname.json"), applicationTypeCode, caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();
    }

    @Test
    public void shouldRaiseSubmitApplicationValidationFailedWithInvalidThirdPartyOrganisationAddressAndNameWhenApiRequestMadeToSubmitCCApplication() {

        final String caseUrn = randomAlphanumeric(10);
        final UUID caseId = randomUUID();
        verifyCCEventAndProgressionCommand(readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json"), caseUrn, caseId);
        final String ccApplicationPayLoad = replaceValues(readFile("command-json/prosecutioncasefile.command.submit-application-when-thirdparty-organisation-invalid-name-address.json"), applicationTypeCode, caseUrn);
        final SubmitCCApplicationHelper submitCCApplicationHelper = new SubmitCCApplicationHelper(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_VALIDATION_FAILED);
        submitCCApplicationHelper.submitCCApplication(ccApplicationPayLoad);
        submitCCApplicationHelper.thenPrivateEventShouldBeRaised();
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
                .replaceAll("APPLICATION_ID", this.applicationId.toString())
                .replaceAll("SOME-RANDOM-APP-CODE", applicationTypeCode)
                .replaceAll("APPLICATION_DUE_DATE", LocalDate.now().plusDays(2).toString())
                .replaceAll("CASE_URN", caseUrn);
    }

    private String replaceValues(final String payload ,final String caseUrn, final UUID pocaFileId ) {
        return payload
                .replaceAll("POCA_FILE_ID", pocaFileId.toString())
                .replaceAll("SOME-RANDOM-APP-CODE", applicationTypeCode)
                .replaceAll("APPLICATION_DUE_DATE", LocalDate.now().plusDays(2).toString())
                .replaceAll("CASE_URN", caseUrn);
    }

    private void verifyCCEventAndProgressionCommand(final String staticPayLoad, final String caseUrn, final UUID caseId) {
        final String ccPayLoad = replaceValuesForCreateCase(staticPayLoad, caseUrn, caseId);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();


        await().timeout(35, TimeUnit.SECONDS)
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
}
