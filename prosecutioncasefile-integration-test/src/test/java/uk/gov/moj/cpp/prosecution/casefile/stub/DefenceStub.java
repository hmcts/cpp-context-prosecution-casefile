package uk.gov.moj.cpp.prosecution.casefile.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.waitForStubToBeReady;

import java.util.UUID;

public class DefenceStub {
    private static final String QUERY = "/defence-service/query/api/rest/defence/defendants/%s/associatedOrganisation";
    private static final String MEDIA_TYPE = "application/vnd.defence.query.associated-organisation+json";

    public static void stubDefenceQueryServiceForForm(final UUID defendantId, final String fileName) {
        final String responsePayload = resourceToString("stub-data/" + fileName);

        callDefenceAssociationOrganisation(defendantId, responsePayload);
    }

    private static void callDefenceAssociationOrganisation(final UUID defendantId, final String responsePayload) {
        stubPingFor("defence-service");
        final String queryUrl = format(QUERY, defendantId);

        stubFor(get(urlPathMatching(queryUrl))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", MEDIA_TYPE)
                        .withBody(responsePayload)));

        waitForStubToBeReady(queryUrl, MEDIA_TYPE);
    }
}
