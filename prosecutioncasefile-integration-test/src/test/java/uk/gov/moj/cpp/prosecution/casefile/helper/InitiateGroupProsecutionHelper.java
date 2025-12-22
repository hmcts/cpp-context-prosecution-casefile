package uk.gov.moj.cpp.prosecution.casefile.helper;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Joiner;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.prosecution.casefile.stub.DocumentGeneratorStub;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_GROUP_CASES_PARKED_FOR_APPROVAL;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_GROUP_CASES_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_GROUP_ID_RECORDED_FOR_SUMMONS_APPLICATION;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_GROUP_PROSECUTION_REJECTED_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.UPLOAD_CASE_DOCUMENT_COMMAND_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.UPLOAD_FILE_URL;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.ADD_COURT_DOCUMENT_COMMAND;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.INITIATE_COURT_PROCEEDINGS_COMMAND;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_COMMAND;

public class InitiateGroupProsecutionHelper extends AbstractTestHelper {
    public static final String USER_ID = randomUUID().toString();
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateGroupProsecutionHelper.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String WRITE_BASE_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile";
    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION = "application/vnd.progression.initiate-court-proceedings-for-application+json";
    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES = "application/vnd.progression.initiate-court-proceedings-for-group-cases+json";

    protected JmsMessageConsumerClient groupCasesReceivedEventsConsumer = createPrivateConsumer(EVENT_GROUP_CASES_RECEIVED);
    protected JmsMessageConsumerClient groupCasesParkedForApprovalEventsConsumer = createPrivateConsumer(EVENT_GROUP_CASES_PARKED_FOR_APPROVAL);
    protected JmsMessageConsumerClient groupProsecutionRejectedEventsConsumer = createPublicConsumer(PUBLIC_GROUP_PROSECUTION_REJECTED_EVENT);
    protected JmsMessageConsumerClient groupIdRecorderdForSummonsApplicationEventsConsumer = createPrivateConsumer(EVENT_GROUP_ID_RECORDED_FOR_SUMMONS_APPLICATION);

    protected final RestClient restClient = new RestClient();

    public void initiateGroupProsecution(final String payload) {
        makePostCall(getWriteUrl("/initiate-group-prosecution"),
                "application/vnd.prosecutioncasefile.command.initiate-group-prosecution+json",
                payload);
    }

    public JsonEnvelope thenPrivateGroupIdRecorderdForSummonsApplicationEventShouldBeRaised() {
        final JsonEnvelope jsonEnvelope = QueueUtil.getEventFromQueue(this.groupIdRecorderdForSummonsApplicationEventsConsumer);
        assertThat(jsonEnvelope, notNullValue());
        return jsonEnvelope;
    }

    public JsonEnvelope thenPrivateGroupCasesParkedForApprovalEventShouldBeRaised() {
        final JsonEnvelope jsonEnvelope = QueueUtil.getEventFromQueue(this.groupCasesParkedForApprovalEventsConsumer);
        assertThat(jsonEnvelope, notNullValue());
        return jsonEnvelope;
    }

    public JsonEnvelope thenPrivateGroupCasesReceivedEventShouldBeRaised() {
        final JsonEnvelope jsonEnvelope = QueueUtil.getEventFromQueue(this.groupCasesReceivedEventsConsumer);
        assertThat(jsonEnvelope, notNullValue());
        return jsonEnvelope;
    }

    public JsonEnvelope thenPublicGroupProsecutionRejectedEventShouldBeRaised() {
        final JsonEnvelope jsonEnvelope = QueueUtil.getEventFromQueue(this.groupProsecutionRejectedEventsConsumer);
        assertThat(jsonEnvelope, notNullValue());
        return jsonEnvelope;
    }

    public void verifyInitiateCourtProceedingsForGroupCasesCommand(final String caseId){

        try {
            waitAtMost(Duration.ONE_MINUTE).until(() ->
                    getInitiateCourtProceedingsForGroupCasesCommand(caseId)
                            .anyMatch(payload -> payload.has("courtReferral"))
            );

        } catch (Exception e) {
            throw new AssertionError("verifyInitiateCourtProceedingsForGroupCasesCommand failed with: " + e);
        }

    }

    public void verifyInitiateCourtProceedingsForApplicationCommand(){

        try {
            waitAtMost(Duration.ONE_MINUTE).until(() ->
                    getInitiateCourtProceedingsForApplicationCommand()
                    .anyMatch(payload -> payload.has("courtApplication"))
            );

        } catch (Exception e) {
            throw new AssertionError("verifyInitiateCourtProceedingsForApplicationCommand failed with: " + e);
        }

    }

    public void verifyUploadMaterialCommandCalled(){

        try {
            waitAtMost(Duration.ONE_MINUTE).until(() ->
                    getUploadMaterialCommand()
                            .anyMatch(payload -> payload.has("materialId"))
            );

        } catch (Exception e) {
            throw new AssertionError("verifyRecordMaterialRequestCommand failed with: " + e);
        }

    }

    public void verifyCreateDocumentCalled(List<String> expectedValues){
        DocumentGeneratorStub.verifyCreateDocumentCalled(expectedValues);
    }

    private static Stream<JSONObject> getInitiateCourtProceedingsForGroupCasesCommand(final String caseId) {
        return findAll(postRequestedFor(urlPathMatching(INITIATE_COURT_PROCEEDINGS_COMMAND)).withRequestBody(containing(caseId))
                .withHeader(CONTENT_TYPE, equalTo(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES))
        )
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }

    private static Stream<JSONObject> getInitiateCourtProceedingsForApplicationCommand() {
        return findAll(postRequestedFor(urlPathMatching(INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_COMMAND))
                .withHeader(CONTENT_TYPE, equalTo(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION))
        )
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }

    public void verifyAddCourtDocumentCalled(final String caseId) {
        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_SECS, TimeUnit.SECONDS)
                .pollDelay(POLL_DELAY_SECS, TimeUnit.SECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(ADD_COURT_DOCUMENT_COMMAND.concat( "(.*)"))).withRequestBody(containing(caseId))).size(),
                        CoreMatchers.is(1));
    }

    private static Stream<JSONObject> getUploadMaterialCommand() {
        return findAll(postRequestedFor(urlPathMatching(UPLOAD_FILE_URL))
                .withHeader(CONTENT_TYPE, equalTo(UPLOAD_CASE_DOCUMENT_COMMAND_TYPE))
        )
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .map(JSONObject::new);
    }

    protected void makePostCall(String url, String mediaType, String payload) {
        makePostCall(UUID.fromString(USER_ID), url, mediaType, payload);
    }

    protected void makePostCall(UUID userId, String url, String mediaType, String payload) {
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", url, mediaType, payload);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.ACCEPTED.getStatusCode()));
    }

    public static String getWriteUrl(String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

}
