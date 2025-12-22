package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static javax.json.Json.createReader;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;

import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.jayway.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;

public class ActivitiHelper {

    private final static String ACTIVITI_BASE_PATH = getBaseUri() + "/prosecutioncasefile-event-processor/internal/activiti/service/";

    private ActivitiHelper() {
    }

    public static void pollUntilProcessDeleted(final String processName, final String businessKey, final String reason) {
        await().until(() -> isProcessDeleted(businessKey, processName, reason), is(true));
    }

    public static String pollUntilProcessExists(final String processName, final String businessKey) {
        return await().until(() -> getProcessesInstanceIds(processName, businessKey), not(equalTo(empty()))).get();
    }

    public static void executeTimerJobs(final String processInstanceId) {
        getTimerJobs(processInstanceId)
                .getJsonArray("data")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(job -> job.getString("id"))
                .forEach(ActivitiHelper::executeJob);
    }

    private static Optional<String> getProcessesInstanceIds(final String processName, final String businessKey) {
        final String url = ACTIVITI_BASE_PATH + "runtime/process-instances?processDefinitionKey=" + processName + "&businessKey=" + businessKey;
        return runQuery(url)
                .getJsonArray("data")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(job -> job.getString("id", null))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static boolean isProcessDeleted(final String businessKey, final String processName, final String reason) {
        final Matcher<JsonValue> deletedMatcher = isJson(withJsonPath("$.data.[0].deleteReason", equalTo(reason)));
        try {
            pollForProcessHistory(businessKey, processName, deletedMatcher);
        } catch (final ConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    private static JsonObject pollForProcessHistory(final String businessKey, final String processName, final Matcher<JsonValue> responseMatcher) {
        return await().until(() -> getProcessesHistory(businessKey, processName), responseMatcher);
    }

    private static JsonObject getProcessesHistory(final String businessKey, final String processName) {
        final String url = ACTIVITI_BASE_PATH + "history/historic-process-instances?businessKey=" + businessKey + "&processDefinitionKey=" + processName;
        return runQuery(url);
    }

    private static JsonObject getTimerJobs(final String processInstanceId) {
        final String url = ACTIVITI_BASE_PATH + "management/jobs?timersOnly=true&processInstanceId=" + processInstanceId;
        return runQuery(url);
    }

    private static void executeJob(final String jobId) {
        final String url = ACTIVITI_BASE_PATH + "management/jobs/" + jobId;
        sendPostRequest(url, Json.createObjectBuilder().add("action", "execute").build());
    }

    private static JsonObject runQuery(final String url) {
        final RestClient restClient = new RestClient();
        final String contentType = "application/json";
        final Response response = restClient.query(url, contentType);
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private static void sendPostRequest(final String url, final JsonObject payload) {
        final String contentType = "application/json";
        final Entity<String> entity = Entity.entity(payload.toString(), MediaType.valueOf(contentType));
        ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().post(entity);
    }
}