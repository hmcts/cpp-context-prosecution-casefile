package uk.gov.moj.cpp.prosecution.casefile.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;

import javax.ws.rs.core.MediaType;

public class StubUtil {

    protected static final String DEFAULT_JSON_CONTENT_TYPE = MediaType.APPLICATION_JSON;

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final int HTTP_STATUS_OK = 200;

    static {
        configureFor(HOST, 8080);
        reset();
    }

    public static void resetStubs() {
        reset();
        stubPingFor("usersgroups-service");
    }

    public static void setupUsersGroupQueryStub() {
        stubPingFor("usersgroups-service");
        stubFor(get(urlMatching("/usersgroups-service/query/api/rest/usersgroups/users/.*"))
                .willReturn(aResponse().withStatus(HTTP_STATUS_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/json")
                        .withBody(resourceToString("stub-data/usersgroups.get-groups-by-user.json"))));
    }
}


