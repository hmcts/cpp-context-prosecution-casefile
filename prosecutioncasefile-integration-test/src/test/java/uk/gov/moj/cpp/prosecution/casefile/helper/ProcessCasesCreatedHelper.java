package uk.gov.moj.cpp.prosecution.casefile.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;

public class ProcessCasesCreatedHelper extends AbstractTestHelper {

    private final String acceptCasePayload;
    private final String clientCorrelationId;

    public ProcessCasesCreatedHelper(final JsonObject acceptCasePayload, String clientCorrelationId ){
        createPublicConsumer(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);

        this.acceptCasePayload = acceptCasePayload.toString();
        this.clientCorrelationId = clientCorrelationId;
    }

    public void verifyInActiveMQ() {
        final Optional<JsonPath> messageData = retrieveEventAsJsonPath(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
        assertThat(messageData.isPresent(), is(true));

        final Map<String, Object> eventOnTheQueue = messageData.get().get();

        final String eventClientCorrelationId = messageData.get().get("_metadata.correlation.client");
        assertThat(eventClientCorrelationId, is(clientCorrelationId));

        eventOnTheQueue.remove(JsonEnvelope.METADATA);
        assertThat(eventOnTheQueue, equalTo(new JsonPath(acceptCasePayload).get()));
    }

}
