package uk.gov.moj.cpp.prosecution.casefile.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils.stubPingFor;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.resourceToString;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.time.LocalDate;
import java.util.UUID;

import javax.ws.rs.core.Response;

public class ReferenceDataStub extends StubUtil {

    private static final String REFERENCE_DATA_ACTION_COURT_LOCATIONS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/court-locations";
    private static final String REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/ethnicities";
    private static final String REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_MEDIA_TYPE = "application/vnd.referencedata.ethnicity+json";
    private static final String REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/observed-ethnicities";
    private static final String REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_MEDIA_TYPE = "application/vnd.referencedata.observed-ethnicities+json";
    private static final String REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/alcohol-level-methods";
    private static final String REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/offence-date-codes";
    private static final String REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/prosecutors";
    private static final String REFERENCE_DATA_ACTION_POLICE_RANKS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/police-ranks";
    private static final String REFERENCE_DATA_ACTION_SUMMONS_CODES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/summons-codes";
    private static final String REFERENCE_DATA_ACTION_OFFENDER_CODES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/offender-codes";
    private static final String REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/courtrooms";
    private static final String REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/organisationunits";
    private static final String REFERENCE_DATA_ACTION_CUSTODY_STATUSES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/custody-statuses";
    private static final String REFERENCE_DATA_ACTION_BAIL_STATUSES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/bail-statuses";
    private static final String REFERENCE_DATA_ACTION_VEHICLE_CODES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/vehicle-codes";
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/documents-type-access/" + LocalDate.now().toString();
    private static final String REFERENCE_DATA_ACTION_PARENT_BUNDLE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/parent-bundle-section";
    private static final String REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/documents-type-access";
    private static final String REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/parent-bundle-section";
    private static final String MODE_OF_TRIAL_REASONS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/mode-of-trial-reasons";
    private static final String APPLICATION_TYPES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/application-types";
    private static final String FIRST_HEARING_APPLICATION_TYPE_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/application-types/bfa61811-b917-3bce-9cc1-7ea8e554bd3b";
    private static final String LOCAL_JUSTICE_AREAS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/enforcement-area";

    private static final String REFERENCE_DATA_ACTION_INITIATION_TYPES_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/initiation-types";
    private static final String REFERENCE_DATA_ACTION_COURT_LOCATIONS_MEDIA_TYPE = "application/vnd.referencedata.query.court-locations+json";
    private static final String REFERENCE_DATA_ACTION_COUNTRY_NATIONALITY_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/country-nationality";
    private static final String REFERENCE_DATA_ACTION_CASE_MARKERS_QUERY_URL = "/referencedata-service/query/api/rest/referencedata/case-markers";
    private static final String REFERENCE_DATA_ACTION_APPLICATION_TYPES_SEARCH = "/referencedata-service/query/api/rest/referencedata/application-types-search";
    private static final String REFERENCE_DATA_ACTION_COUNTRY_NATIONALITIY_MEDIA_TYPE = "application/vnd.referencedata.query.country-nationality+json";
    private static final String REFERENCE_DATA_ACTION_GET_NS_PROSECUTOR_MEDIA_TYPE = "application/vnd.referencedata.query.prosecutor+json";
    private static final String REFERENCE_DATA_ACTION_CASE_MARKERS_MEDIA_TYPE = "application/vnd.referencedata.case-markers.v2+json";
    private static final String REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE = "application/vnd.referencedata.application-types-search+json";
    private static final String REFERENCE_DATA_ACTION_INITIATION_TYPE_MEDIA_TYPE = "application/vnd.reference-data.initiation-types+json";
    private static final String REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_MEDIA_TYPE = "application/vnd.referencedata.alcohol-level-methods+json";
    private static final String REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_MEDIA_TYPE = "application/vnd.referencedata.offence-date-codes+json";
    private static final String REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE = "application/vnd.referencedata.query.get.prosecutor+json";
    private static final String REFERENCE_DATA_ACTION_SUMMONS_CODES_MEDIA_TYPE = " application/vnd.referencedata.summons-codes+json";
    private static final String REFERENCE_DATA_ACTION_POLICE_RANKS_MEDIA_TYPE = "application/vnd.referencedata.police-ranks+json";
    private static final String REFERENCE_DATA_ACTION_OFFENDER_CODES_MEDIA_TYPE = "application/vnd.referencedata.offender-codes+json";
    private static final String REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE = "application/vnd.referencedata.ou.courtrooms.ou-courtroom-code+json";
    private static final String REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE = "application/vnd.referencedata.query.organisationunits+json";
    private static final String REFERENCE_DATA_ACTION_CUSTODY_STATUSES_MEDIA_TYPE = "application/vnd.referencedata.custody-statuses+json";
    private static final String REFERENCE_DATA_ACTION_BAIL_STATUSES_MEDIA_TYPE = "application/vnd.referencedata.bail-statuses+json";
    private static final String REFERENCE_DATA_ACTION_VEHICLE_CODE_MEDIA_TYPE = "application/vnd.referencedata.vehicle-codes+json";
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_MEDIA_TYPE = "application/vnd.referencedata.get-all-document-type-access+json";
    private static final String REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_MEDIA_TYPE = "application/vnd.reference-data.parent-bundle-section+json";
    private static final String REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE = "application/vnd.reference-data.parent-bundle-section+json";
    private static final String REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE = "application/vnd.referencedata.query.document-type-access-by-sectioncode+json";
    private static final String MODE_OF_TRIAL_REASONS_MEDIA_TYPE = "application/vnd.referencedata.mode-of-trial-reasons+json";
    private static final String APPLICATION_TYPES_MEDIA_TYPE = "application/vnd.referencedata.query.application-types+json";
    private static final String FIRST_HEARING_APPLICATION_TYPE_MEDIA_TYPE = "application/vnd.referencedata.application-type+json";
    private static final String REFERENCE_DATA_LOCAL_JUSTICE_AREA_MEDIA_TYPE = "application/vnd.referencedata.query.enforcement-area.v2+json";

    public static void stubPingForReferenceDataService() {
        stubPingFor("referencedata-service");
    }

    public static void stubGetDocumentsTypeAccess(final String filePath) {
        stubPingFor("referencedata-service");

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_MEDIA_TYPE)
                        .withBody(resourceToString(filePath))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_QUERY_URL, REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_MEDIA_TYPE);
    }

    public static void stubGetParentBundleSection() {
        stubPingFor("referencedata-service");

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_PARENT_BUNDLE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-parent-bundle-section.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_PARENT_BUNDLE_QUERY_URL, REFERENCE_DATA_ACTION_PARENT_BUNDLE_SECTION_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataBySectionCode( ) {
        stubPingFor("referencedata-service");
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.query.document-type-access-by-sectionCode.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_SECTION_CODE_QUERY_URL, REFERENCE_DATA_ACTION_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE_MEDIA_TYPE);
    }


    public static void stubGetAllCourtLocations() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_COURT_LOCATIONS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_COURT_LOCATIONS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/courtlocation.get-all-court-locations.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_COURT_LOCATIONS_QUERY_URL, REFERENCE_DATA_ACTION_COURT_LOCATIONS_MEDIA_TYPE);
    }

    public static void stubGetSelfDefinedEthnicities() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.ethnicity.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_QUERY_URL, REFERENCE_DATA_ACTION_SELFDEFINED_ETHNICITIES_MEDIA_TYPE);
    }

    public static void stubGetObservedEthnicities() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-observed-ethnicity.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_QUERY_URL, REFERENCE_DATA_ACTION_OBSERVED_ETHNICITIES_MEDIA_TYPE);
    }

    public static void stubGetVehicleCodes() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_VEHICLE_CODES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_VEHICLE_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-vehicle-codes.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_VEHICLE_CODES_QUERY_URL, REFERENCE_DATA_ACTION_VEHICLE_CODE_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitWithOneCourtroom() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.ou-one-courtroom.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitWithOneCourtroomForSubmitApplication() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.ou-one-courtroom-submit-application.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE);
    }



    public static void stubGetOrganisationUnitWithOneCourtroomForMags() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.ou-one-courtroom-mags.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitWithOneCourtroomForCrown() {

        stubFor(get(urlPathMatching(LOCAL_JUSTICE_AREAS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.ou-one-courtroom-crown.json"))));

        waitForStubToBeReady(LOCAL_JUSTICE_AREAS_QUERY_URL, REFERENCE_DATA_LOCAL_JUSTICE_AREA_MEDIA_TYPE);
    }


    public static void stubGetLocalJusticeAreas() {

        stubFor(get(urlPathMatching(LOCAL_JUSTICE_AREAS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_LOCAL_JUSTICE_AREA_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.query.enforcement-area.json"))));

        waitForStubToBeReady(LOCAL_JUSTICE_AREAS_QUERY_URL, REFERENCE_DATA_LOCAL_JUSTICE_AREA_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitWithNotFound() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_NOT_FOUND)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE)));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOMS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_WITH_COURTROOM_BY_OU_CODE_MEDIA_TYPE, Response.Status.NOT_FOUND);
    }

    public static void stubGetOrganisationUnits() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.organisation-units.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitsReturnsMag(String spiOuCode) {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE))
                .withQueryParam("spiOucode", equalTo(spiOuCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.organisation-units-with-mags.json"))));


        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL.concat("?spiOucode="+spiOuCode), REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE);
    }

    public static void stubGetOrganisationUnitsReturnsEmptyList() {

        stubFor(get(urlPathMatching(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.organisation-units-empty-list.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ORGANISATION_UNITS_QUERY_URL, REFERENCE_DATA_ACTION_ORGANISATION_UNITS_MEDIA_TYPE);
    }


    public static void stubGetHearingTypes() {

        stubFor(get(urlPathMatching("/referencedata-service/query/api/rest/referencedata/hearing-types"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.query.hearing-types+json")
                        .withBody(resourceToString("stub-data/referencedata.get.hearing-types.json"))));

        waitForStubToBeReady("/referencedata-service/query/api/rest/referencedata/hearing-types", "application/vnd.referencedata.query.hearing-types+json");
    }

    public static void stubGetPoliceForces() {

        stubFor(get(urlPathMatching("/referencedata-service/query/api/rest/referencedata/police-forces"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", "application/vnd.referencedata.police-forces+json")
                        .withBody(resourceToString("stub-data/referencedata.get.police-forces.json"))));

        waitForStubToBeReady("/referencedata-service/query/api/rest/referencedata/police-forces", "application/vnd.referencedata.police-forces+json");
    }

    public static void stubGetAllCountryNationalities() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_COUNTRY_NATIONALITY_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_COUNTRY_NATIONALITIY_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-country-nationality.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_COUNTRY_NATIONALITY_QUERY_URL, REFERENCE_DATA_ACTION_COUNTRY_NATIONALITIY_MEDIA_TYPE);
    }

    public static void stubGetCaseMarkersWithCode(final String codeToSTub) {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_CASE_MARKERS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_CASE_MARKERS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-case-markers.json").replace("CODE_STUB", codeToSTub))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_CASE_MARKERS_QUERY_URL, REFERENCE_DATA_ACTION_CASE_MARKERS_MEDIA_TYPE);
    }

    public static void stubGetApplicationType(final String applicationTypeCode) {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_APPLICATION_TYPES_SEARCH))
                .withQueryParam("q", equalTo(applicationTypeCode))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.application-types-search.json").replaceAll("APP_TYPE_CODE", applicationTypeCode))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_APPLICATION_TYPES_SEARCH.concat("?q=").concat(applicationTypeCode), REFERENCE_DATA_ACTION_APPLICATION_TYPES_MEDIA_TYPE);
    }

    public static void stubGetInitiationTypes() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_INITIATION_TYPES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_INITIATION_TYPE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-initiation-types.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_INITIATION_TYPES_QUERY_URL, REFERENCE_DATA_ACTION_INITIATION_TYPE_MEDIA_TYPE);
    }

    public static void stubGetSummonsCodes() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_SUMMONS_CODES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_SUMMONS_CODES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-summons-codes.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_SUMMONS_CODES_QUERY_URL, REFERENCE_DATA_ACTION_SUMMONS_CODES_MEDIA_TYPE);
    }

    public static void stubGetAlcoholLevelMethods() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-alcohol-level-methods.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_QUERY_URL, REFERENCE_DATA_ACTION_ALCOHOL_LEVEL_METHODS_MEDIA_TYPE);
    }

    public static void stubGetOffenceDateCodes() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-offence-date-codes.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_QUERY_URL, REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_MEDIA_TYPE);
    }

    public static void stubGetOffenderCodes() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_OFFENDER_CODES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_OFFENCE_DATE_CODES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-offender-codes.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_OFFENDER_CODES_QUERY_URL, REFERENCE_DATA_ACTION_OFFENDER_CODES_MEDIA_TYPE);
    }

    public static void stubProsecutors() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .atPriority(5)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-prosecutor.json"))));
    }

    public static void stubNonSjpProsecutors() {

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .withQueryParam("oucode", equalTo("nonsjp"))
                .atPriority(5)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-non-sjp-prosecutor.json"))));
    }
    public static void stubProsecutorsReturns404() {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .withQueryParam("oucode", equalTo("123"))
                .willReturn(aResponse().withStatus(SC_NOT_FOUND)));
    }

    public static void stubGetProsecutor() {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .atPriority(5)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-prosecutor.json"))));

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .withQueryParam("oucode", equalTo("GAEAA01"))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-prosecutor.json"))));
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_PROSECUTORS_QUERY_URL))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE))
                .withQueryParam("oucode", equalTo("invalidAut"))
                .atPriority(2)
                .willReturn(aResponse().withStatus(SC_NOT_FOUND)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_PROSECUTORS_MEDIA_TYPE)
                        ));

        stubFor(get(urlPathEqualTo("/referencedata-service/query/api/rest/referencedata/prosecutors/87a1953c-3b04-495e-a6cd-6f7557b49ccc"))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_GET_NS_PROSECUTOR_MEDIA_TYPE))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_GET_NS_PROSECUTOR_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-ns-prosecutor.json"))));

        stubFor(get(urlPathEqualTo("/referencedata-service/query/api/rest/referencedata/prosecutors/21cac7fb-387c-4d19-9c80-8963fa8cf233"))
                .withHeader("Accept", equalTo(REFERENCE_DATA_ACTION_GET_NS_PROSECUTOR_MEDIA_TYPE))
                .atPriority(1)
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_GET_NS_PROSECUTOR_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-prosecutor-without-oucode.json"))));


    }

    public static void stubPoliceRanks() {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_POLICE_RANKS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_POLICE_RANKS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-police-ranks.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_POLICE_RANKS_QUERY_URL, REFERENCE_DATA_ACTION_POLICE_RANKS_MEDIA_TYPE);
    }

    public static void stubGetCustodyStatuses() {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_CUSTODY_STATUSES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_CUSTODY_STATUSES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-custody-statuses.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_CUSTODY_STATUSES_QUERY_URL, REFERENCE_DATA_ACTION_CUSTODY_STATUSES_MEDIA_TYPE);
    }

    public static void stubGetBailStatuses() {
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_BAIL_STATUSES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_BAIL_STATUSES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-bail-statuses.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_BAIL_STATUSES_QUERY_URL, REFERENCE_DATA_ACTION_BAIL_STATUSES_MEDIA_TYPE);
    }

    public static void stubGetParentBundleSections() {
        InternalEndpointMockUtils.stubPingFor("referencedata-service");
        stubFor(get(urlPathEqualTo(REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_QUERY_URL))
                .withQueryParam("cpsBundleCode", equalTo("1"))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.get-parent-bundle-sections.json"))));

        waitForStubToBeReady(REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_QUERY_URL.concat("?cpsBundleCode=1"), REFERENCE_DATA_ACTION_GET_PARENT_BUNDLE_SECTIONS_MEDIA_TYPE);
    }

    public static void stubModeOfTrialReasons() {

        stubFor(get(urlPathMatching(MODE_OF_TRIAL_REASONS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", MODE_OF_TRIAL_REASONS_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.mode-of-trial-reasons.json"))));

        waitForStubToBeReady(MODE_OF_TRIAL_REASONS_QUERY_URL, MODE_OF_TRIAL_REASONS_MEDIA_TYPE);
    }

    public static void stubApplicationTypes() {
        stubFor(get(urlPathMatching(APPLICATION_TYPES_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_TYPES_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.application-types.json"))));

        waitForStubToBeReady(APPLICATION_TYPES_QUERY_URL, APPLICATION_TYPES_MEDIA_TYPE);
    }
    public static void stubFirstHearingApplicationType() {
        stubFor(get(urlPathMatching(FIRST_HEARING_APPLICATION_TYPE_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", FIRST_HEARING_APPLICATION_TYPE_MEDIA_TYPE)
                        .withBody(resourceToString("stub-data/referencedata.first-hearing-application-type.json"))));

        waitForStubToBeReady(FIRST_HEARING_APPLICATION_TYPE_QUERY_URL, FIRST_HEARING_APPLICATION_TYPE_MEDIA_TYPE);
    }
}
