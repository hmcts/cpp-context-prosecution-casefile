package uk.gov.moj.cpp.prosecution.casefile.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.waitAtMost;

import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.awaitility.Duration;

/**
 * Stands in for service-cp-crime-caseadmin-case-management POST /groupcases (CAD-1850). The path is
 * the default value of PCF's casemanagement.base.url JNDI key plus /groupcases.
 */
public class CaseManagementStub {

    public static final String CREATE_GROUP_CASE_PATH = "/casemanagement/groupcases";

    private CaseManagementStub() {
    }

    public static void stubCreateGroupCase(final int status) {
        stubFor(post(urlPathEqualTo(CREATE_GROUP_CASE_PATH))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
    }

    public static List<LoggedRequest> createGroupCaseRequestsFor(final UUID groupId) {
        return findAll(postRequestedFor(urlPathEqualTo(CREATE_GROUP_CASE_PATH))
                .withRequestBody(containing(groupId.toString())));
    }

    public static LoggedRequest waitForCreateGroupCaseRequest(final UUID groupId) {
        waitAtMost(Duration.ONE_MINUTE).until(() -> !createGroupCaseRequestsFor(groupId).isEmpty());
        return createGroupCaseRequestsFor(groupId).get(0);
    }
}
