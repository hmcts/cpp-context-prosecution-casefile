package uk.gov.moj.cpp.prosecution.casefile.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.UUID;

public class MaterialStub {

    public static final String UPLOAD_FILE_URL = "/material-service/command/api/rest/material/material";
    public static final String UPLOAD_CASE_DOCUMENT_COMMAND_TYPE = "application/vnd.material.command.upload-file+json";

    public static void stubForUploadFileCommand() {
        InternalEndpointMockUtils.stubPingFor("material-service");

        stubFor(post(urlPathEqualTo(UPLOAD_FILE_URL))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)));

        stubFor(get(urlPathEqualTo(UPLOAD_FILE_URL))
                .willReturn(aResponse().withStatus(SC_OK)));

        waitForStubToBeReady(UPLOAD_FILE_URL, UPLOAD_CASE_DOCUMENT_COMMAND_TYPE);
    }
}

