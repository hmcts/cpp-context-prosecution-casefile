package uk.gov.moj.cpp.prosecution.casefile.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.justice.services.common.http.HeaderConstants.ID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.waitForStubToBeReady;

import java.util.UUID;

public class ProgressionStub {

    public static final String INITIATE_COURT_PROCEEDINGS_COMMAND = "/progression-service/command/api/rest/progression/initiatecourtproceedings";
    public static final String INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_COMMAND = "/progression-service/command/api/rest/progression/initiate-application";
    public static final String ADD_DEFENDANT_TO_COURT_PROCEEDING = "/progression-service/command/api/rest/progression/adddefendantstocourtproceedings";
    public static final String ADD_DEFENDANT_TO_COURT_PROCEEDING_TYPE = "progression.add-defendants-to-court-proceedings";
    public static final String ADD_COURT_DOCUMENT_COMMAND = "/progression-service/command/api/rest/progression/courtdocument/";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.prosecutioncase+json";
    public static final String QUERY_APPLICATION = "/progression-service/query/api/rest/progression/applications/";
    public static final String QUERY_SEARCH = "/progression-service/query/api/rest/progression/search";
    public static final String PROSECUTION_QUERY_CASE = "/progression-service/query/api/rest/progression/prosecutioncases/%s";
    public static final String PROGRESSION_SERVICE = "progression-service";
    public static final String PROSECUTION_CASE_MEDIA_TYPE = "application/vnd.progression.query.case+json";
    public static final String PROSECUTION_SEARCH_CASES_MEDIA_TYPE = "application/vnd.progression.query.search-cases+json";
    public static final String QUERY_CASE_DEFENDANT_HEARINGS = "/progression-service/query/api/rest/progression/prosecutioncases/%s/defendants/(.*?)";
    public static final String PROGRESSION_CASE_DEFENDANT_HEARINGS_MEDIA_TYPE = "application/vnd.progression.query.case-defendant-hearings+json";
    public static final String PROGRESSION_COMMAND_PLEAD_ONLINE = "/progression-service/command/api/rest/progression/cases/(.*)/defendants/(.*)/plead-online";
    public static final String PROGRESSION_COMMAND_PLEAD_ONLINE_PCQ = "/progression-service/command/api/rest/progression/cases/(.*)/defendants/(.*)/plead-online-pcq-visited";


    public static void stubForPleadOnline() {
        stubPingFor(PROGRESSION_SERVICE);

        stubFor(post(urlPathMatching(format(PROGRESSION_COMMAND_PLEAD_ONLINE)))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );

        stubFor(post(urlPathMatching(format(PROGRESSION_COMMAND_PLEAD_ONLINE_PCQ)))
                .willReturn(aResponse().withStatus(SC_ACCEPTED))
        );
    }

    public static void stubForQueryApplication(final UUID applicationId) {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(QUERY_APPLICATION + applicationId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.progression.query.application-only+json")
                        .withBody("{\"courtApplication\" : {\"id\" : \"" + applicationId + "\", \"type\" : {\"code\" : \"AAAA\"}, \"courtCivilApplication\": {\n" +
                                "      \"isCivil\": true\n" +
                                "    } }}")));

        waitForStubToBeReady(QUERY_APPLICATION + applicationId, "application/vnd.progression.query.application-only+json");
    }

    public static void stubForQueryApplicationDoesNotExist(final UUID applicationId) {
        stubPingFor("progression-service");

        stubFor(get(urlPathEqualTo(QUERY_APPLICATION + applicationId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.progression.query.application-only+json")
                        .withBody("{}")));

        waitForStubToBeReady(QUERY_APPLICATION + applicationId, "application/vnd.progression.query.application-only+json");
    }

    public static void stubForAddCourtDocument() {
        stubPingFor("progression-service");

        stubFor(post(urlPathMatching(ADD_COURT_DOCUMENT_COMMAND + "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON))
        );

    }

    public static void stubForInitiateCourtProceedings() {
        stubPingFor("progression-service");

        stubFor(post(urlPathEqualTo(INITIATE_COURT_PROCEEDINGS_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(resourceToString("stub-data/referencedata.get-country-nationality.json")))
        );
    }

    public static void stubForInitiateCourtProceedingsForApplication() {
        stubPingFor("progression-service");

        stubFor(post(urlPathEqualTo(INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION_COMMAND))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON))
        );
    }

    public static void stubForAddDefendantsToCourtProceeding() {
        final String addDefendantToCourtProceeding = ADD_DEFENDANT_TO_COURT_PROCEEDING;
        stubPingFor("progression-service");

        stubFor(post(urlPathEqualTo(addDefendantToCourtProceeding))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(resourceToString("stub-data/referencedata.get-country-nationality.json")))
        );

        stubFor(get(urlPathEqualTo(addDefendantToCourtProceeding))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withBody(resourceToString("stub-data/referencedata.get-country-nationality.json"))));

        waitForStubToBeReady(addDefendantToCourtProceeding, ADD_DEFENDANT_TO_COURT_PROCEEDING_TYPE);
    }

    public static void stubProgressionQueryServiceForForm(final UUID caseId, final String fileName) {
        final String responsePayload = resourceToString("stub-data/" + fileName);

        callProsecutionCaseQuery(caseId, responsePayload);
    }

    public static void stubProgressionQueryServiceForForm(final UUID caseId, final String fileName, final String... defendantIds) {
        final String responsePayload = resourceToString("stub-data/" + fileName, defendantIds[0], defendantIds[1]);

        callProsecutionCaseQuery(caseId, responsePayload);
    }

    private static void callProsecutionCaseQuery(final UUID caseId, final String responsePayload) {
        stubPingFor("progression-service");

        stubFor(get(urlPathMatching(PROGRESSION_QUERY_PROSECUTION_CASE + caseId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(responsePayload)));

        waitForStubToBeReady(PROGRESSION_QUERY_PROSECUTION_CASE + caseId, PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE);
    }

    public static void stubSearchCases(final String caseUrn, final String caseId) {
        stubPingFor(PROGRESSION_SERVICE);

        final String urlPath = QUERY_SEARCH;

        stubFor(get(urlPathMatching(urlPath))
                .withQueryParam("q", equalTo(caseUrn))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(resourceToString("stub-data/progression-search-cases.json", caseId))));
        waitForStubToBeReady(urlPath + "?q=" + caseUrn, PROSECUTION_SEARCH_CASES_MEDIA_TYPE);
    }

    public static void stubDefendantHearingDays(final String caseId, final String defendantId) {
        stubPingFor(PROGRESSION_SERVICE);
        final String sittingDay = "2122-05-30T18:32:04.238Z";

        stubFor(get(urlPathMatching(format(QUERY_CASE_DEFENDANT_HEARINGS, caseId, defendantId)))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(resourceToString("stub-data/progression.query.case-defendant-hearings.json", caseId, defendantId, sittingDay))));
        waitForStubToBeReady(format(QUERY_CASE_DEFENDANT_HEARINGS, caseId, defendantId), PROGRESSION_CASE_DEFENDANT_HEARINGS_MEDIA_TYPE);
    }

    public static void stubGetProsecutionCase(final String caseId, final String caseUrn, final String postcode, final String dob) {
        String respBody = resourceToString("stub-data/progression-query-case.json", postcode, dob, caseId, caseUrn);
        stubQueryProsecutionCase(caseId, respBody);
    }

    public static void stubGetProsecutionCaseLegalEntity(final String caseId, final String caseUrn, final String postcode) {
        String respBody = resourceToString("stub-data/progression-query-case-legal-entity.json", postcode, caseId, caseUrn);
        stubQueryProsecutionCase(caseId, respBody);
    }

    private static void stubQueryProsecutionCase(final String caseId, final String responsePayload) {
        stubPingFor(PROGRESSION_SERVICE);

        final String urlPath = format(PROSECUTION_QUERY_CASE, caseId);

        stubFor(get(urlPathMatching(urlPath))
                .willReturn(aResponse()
                        .withStatus(SC_OK)
                        .withHeader(ID, randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(responsePayload)));
        waitForStubToBeReady(urlPath, PROSECUTION_CASE_MEDIA_TYPE);
    }

    public static void stubProgressionQueryService(final UUID caseId, final String mockResource) {
        stubPingFor("progression-service");
        stubFor(get(urlPathMatching(format(PROSECUTION_QUERY_CASE, caseId)))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE)
                        .withBody(mockResource)));

        waitForStubToBeReady(format(PROSECUTION_QUERY_CASE, caseId), PROGRESSION_QUERY_PROSECUTION_CASE_MEDIA_TYPE);
    }

}