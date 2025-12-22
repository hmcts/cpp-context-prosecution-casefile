package uk.gov.moj.cpp.prosecution.casefile.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class SjpStub {

    public static final String UPLOAD_CASE_DOCUMENT_COMMAND = "/sjp-service/command/api/rest/sjp/cases/%s/upload-case-document/%s";
    public static final String SJP_PLEAD_ONLINE_COMMAND = "/sjp-service/command/api/rest/sjp/cases/(.*)/defendants/(.*)/plead-online";
    public static final String SJP_PLEAD_ONLINE_PCQ_VISITED_COMMAND = "/sjp-service/command/api/rest/sjp/cases/(.*)/defendants/(.*)/plead-online-pcq-visited";
    private static final String UPLOAD_CASE_DOCUMENT_COMMAND_TYPE = "sjp.upload-case-document";
    private static final String SJP_QUERY_CASE_ENDPOINT = "/sjp-service/query/api/rest/sjp/cases";
    private static final String SJP_QUERY_CASE_CONTENT_TYPE = "application/vnd.sjp.query.case+json";

    public static void stubForSjpPleadOnline() {
        stubPingFor("sjp-service");

        stubFor(post(urlPathMatching(format(SJP_PLEAD_ONLINE_COMMAND)))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));

        stubFor(post(urlPathMatching(format(SJP_PLEAD_ONLINE_PCQ_VISITED_COMMAND)))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)));

    }

    public static void stubForUploadCaseDocumentCommand(final String caseId, final String caseDocumentType) {
        final String uploadCaseDocumentUrl = String.format(UPLOAD_CASE_DOCUMENT_COMMAND, caseId, caseDocumentType);
        InternalEndpointMockUtils.stubPingFor("sjp-service");

        stubFor(post(urlPathEqualTo(uploadCaseDocumentUrl))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(uploadCaseDocumentUrl))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(uploadCaseDocumentUrl, UPLOAD_CASE_DOCUMENT_COMMAND_TYPE);
    }

    public static void stubCaseByUrnPostcode(final String caseUrn, final String postcode, final String dob) {
        InternalEndpointMockUtils.stubPingFor("sjp-service");
        final String payload = resourceToString("stub-data/sjp.cases-for-citizen.json", caseUrn, dob, postcode);

        stubFor(get(urlPathMatching("/sjp-service/query/api/rest/sjp/cases-for-citizen"))
                .withQueryParam("urn", equalTo(caseUrn))
                .withQueryParam("postcode", equalTo(postcode))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.sjp.query.case-by-urn-postcode+json")
                        .withBody(payload)));

        waitForStubToBeReady(String.format("/sjp-service/query/api/rest/sjp/cases-for-citizen?urn=%s&postcode=%s", caseUrn, postcode),
                "application/vnd.sjp.query.case-by-urn-postcode+json");
    }

    public static void stubSjpQuery(final UUID caseId, final UUID offenceId) {
        stubFor(get(urlEqualTo(SJP_QUERY_CASE_ENDPOINT + "/" + caseId))
                .withHeader(ACCEPT, equalTo(SJP_QUERY_CASE_CONTENT_TYPE))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader(ID, UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, SJP_QUERY_CASE_CONTENT_TYPE)
                        .withBody(getExpectedCaseJson(caseId.toString(), offenceId.toString())))
        );
    }

    public static String getExpectedCaseJson(final String caseId, final String offenceId) {
        return resourceToString("stub-data/sjp-query-case-result-8e582195-639d-451d-8220-42777d7cf9f2.json", caseId, offenceId);
    }

}

