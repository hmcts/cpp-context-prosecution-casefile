package uk.gov.moj.cpp.prosecution.casefile.helper;

import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.stub.IdMapperStub.stubForIdMapperSuccess;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.stubForUploadFileCommand;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForAddDefendantsToCourtProceeding;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForInitiateCourtProceedings;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForInitiateCourtProceedingsForApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubApplicationTypes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubFirstHearingApplicationType;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetAlcoholLevelMethods;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetAllCountryNationalities;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetAllCourtLocations;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetBailStatuses;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCustodyStatuses;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetHearingTypes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetInitiationTypes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetObservedEthnicities;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOffenceDateCodes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOffenderCodes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetParentBundleSections;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetPoliceForces;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetProsecutor;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetSelfDefinedEthnicities;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetSummonsCodes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetVehicleCodes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubModeOfTrialReasons;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubPingForReferenceDataService;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubPoliceRanks;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubProsecutors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.StubUtil.resetStubs;
import static uk.gov.moj.cpp.prosecution.casefile.stub.StubUtil.setupUsersGroupQueryStub;

import uk.gov.justice.services.test.utils.core.http.RequestParams;

import javax.ws.rs.core.Response.Status;

/**
 * Provides helper methods for tests to interact with Wiremock instance
 */
public class WiremockTestHelper {

    private WiremockTestHelper() {
    }

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String BASE_URI = "http://" + HOST + ":8080";

    public static void createCommonMockEndpoints() {
        resetStubs();
        setupUsersGroupQueryStub();
        stubPingForReferenceDataService();
        stubGetAllCourtLocations();
        stubGetAllCountryNationalities();
        stubGetOrganisationUnitWithOneCourtroom();
        stubOffencesForOffenceCode();
        stubOffencesForOffenceCodeList();
        stubGetProsecutor();
        stubGetInitiationTypes();
        stubGetSummonsCodes();
        stubGetAlcoholLevelMethods();
        stubGetOffenceDateCodes();
        stubGetOffenderCodes();
        stubProsecutors();
        stubPoliceRanks();
        stubGetBailStatuses();
        stubGetVehicleCodes();
        stubForIdMapperSuccess();
        stubGetParentBundleSections();
        stubModeOfTrialReasons();
        stubGetPoliceForces();
        stubApplicationTypes();
        stubFirstHearingApplicationType();
        stubForUploadFileCommand();
        stubGetSelfDefinedEthnicities();
        stubGetObservedEthnicities();
        stubForInitiateCourtProceedings();
        stubGetHearingTypes();
        stubGetCustodyStatuses();
        stubForAddDefendantsToCourtProceeding();
        stubForInitiateCourtProceedingsForApplication();
    }

    public static void waitForStubToBeReady(String resource, String mediaType) {
        waitForStubToBeReady(resource, mediaType, OK);
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus, String headerName, String headerValue) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType)
                .withHeader(headerName, headerValue)
                .build();
        poll(requestParams)
                .until(
                        status().is(expectedStatus)
                );
    }

    public static void waitForStubToBeReady(String resource, String mediaType, Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .until(
                        status().is(expectedStatus)
                );
    }
}
