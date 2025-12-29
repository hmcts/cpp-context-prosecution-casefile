package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.jboss.resteasy.util.HttpHeaderNames.ACCEPT;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

public class SubmitCCApplicationHelper extends AbstractTestHelper {

    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION = "application/vnd.progression.initiate-court-proceedings-for-application+json";
    public static final String PROGRESSION_COMMAND_API_INITIATE_APPLICATION = "/progression-service/command/api/rest/progression/initiate-application";

    private final String expectedPrivateEventName;

    public SubmitCCApplicationHelper(final String expectedPrivateEventName) {
        this.expectedPrivateEventName = expectedPrivateEventName;
        createPrivateConsumer(expectedPrivateEventName);
    }

    public void submitCCApplication(final String payload) {
        makePostCall(getWriteUrl("/application"),
                "application/vnd.prosecutioncasefile.command.submit-application+json",
                payload);
    }

    public void thenPrivateEventShouldBeRaised() {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(this.expectedPrivateEventName);
        assertThat(jsonEnvelope.isPresent(), is(true));
    }

    public void verifyCourtProceedingsForApplicationHasBeenInitiated(final String applicationId) {
        await().timeout(35, SECONDS).pollInterval(500, MILLISECONDS).pollDelay(500, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(PROGRESSION_COMMAND_API_INITIATE_APPLICATION))
                                .withRequestBody(containing(applicationId))).size(), is(1));


        LoggedRequest lastLoggedRequest = getLastLoggedRequest(applicationId);
        assertThat(lastLoggedRequest.getMethod(), is(RequestMethod.POST));
        assertThat(lastLoggedRequest.getHeader(ACCEPT), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION));

        final JsonObject jsonObject = getRequestBodyAsJsonObject(lastLoggedRequest.getBodyAsString());
        assertThat(jsonObject.getJsonObject("courtApplication").getString("id"), is(applicationId));
    }

    private JsonObject getRequestBodyAsJsonObject(final String requestBody) {
        try (final JsonReader jsonReader = createReader(new StringReader(requestBody))) {
            return jsonReader.readObject();
        }
    }

    private static LoggedRequest getLastLoggedRequest(final String applicationId) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching(PROGRESSION_COMMAND_API_INITIATE_APPLICATION))
                .withRequestBody(containing(applicationId)));

        return loggedRequests.get(loggedRequests.size() - 1);
    }
}
