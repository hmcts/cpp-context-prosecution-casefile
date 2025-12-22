package uk.gov.moj.cpp.prosecution.casefile.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector;
import uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil;

import java.util.UUID;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class CreateSjpCaseStub extends StubUtil {

    private static final String CREATE_SJP_CASE_ACTION_QUERY_URL = "/sjp-service/command/api/rest/sjp/cases";
    private static final String CREATE_SJP_CASE_ACTION_MEDIA_TYPE = "application/vnd.sjp.create-sjp-case+json";
    private static final String SYSTEM_ID_MAPPER_API_REST_SYSTEMID_MAPPINGS = "/system-id-mapper-api/rest/systemid/mappings";

    public static void resetAndStubCreateSjpCase() {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        stubFor(post(urlPathEqualTo(CREATE_SJP_CASE_ACTION_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, CREATE_SJP_CASE_ACTION_MEDIA_TYPE)
                        .withBody("")
                ));

        stubFor(post(urlPathEqualTo(SYSTEM_ID_MAPPER_API_REST_SYSTEMID_MAPPINGS))
                .willReturn(aResponse().withStatus(200)
                        .withBody(resourceToString("stub-data/system-id.json"))
                ));

    }

    public static JsonEnvelope getSentCreateSjpCaseCommand(final UUID caseId) {

        final JsonEnvelopeMatcher commandMatcher = jsonEnvelope(
                metadata().withName(EventSelector.EXTERNAL_COMMAND_SJP_CREATE_CASE),
                payload().isJson(withJsonPath("id", is(caseId.toString()))));

        return await("sjp.create-sjp-case command sent")
                .until(() -> findAll(postRequestedFor(urlPathEqualTo(CREATE_SJP_CASE_ACTION_QUERY_URL))
                                .withHeader(CONTENT_TYPE, equalTo(CREATE_SJP_CASE_ACTION_MEDIA_TYPE)))
                                .stream()
                                .map(LoggedRequest::getBodyAsString)
                                .map(body -> new DefaultJsonObjectEnvelopeConverter().asEnvelope(body))
                                .filter(commandMatcher::matches)
                                .findFirst()
                                .orElse(null)
                        , notNullValue());
    }

    public static String getEnterpriseIdFromSystemIdMapperRequest() {
        return await("system-id-mapper-api called")
                .until(() -> findAll(postRequestedFor(urlPathEqualTo(SYSTEM_ID_MAPPER_API_REST_SYSTEMID_MAPPINGS)))
                                .stream()
                                .map(LoggedRequest::getBodyAsString)
                                .map(FileUtil::readJson)
                                .map(json -> json.getString("sourceId", null))
                                .findFirst()
                                .orElse(null),
                        notNullValue());
    }

}
