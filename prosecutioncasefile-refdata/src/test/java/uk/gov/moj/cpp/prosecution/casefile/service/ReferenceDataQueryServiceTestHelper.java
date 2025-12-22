package uk.gov.moj.cpp.prosecution.casefile.service;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.hearing.courts.referencedata.EnforcementArea;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ReferenceDataQueryServiceTestHelper {

    public static final String VALID_FROM = "validFrom";
    public static final String SEQ_NO = "seqNo";
    public static final String METHOD_CODE = "methodCode";
    public static final String METHOD_DESCRIPTION = "methodDescription";
    public static final String DESCRIPTION = "description";
    public static final String SEQUENCE = "sequence";
    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String VALID_TO = "validTo";

    private static final String KEY_CASE_ID = "caseId";
    private static final String KEY_DEFENDANT_ID = "defendantId";
    private static final String KEY_NEXT_HEARING = "nextHearing";
    private static final String KEY_COURT_ID = "courtId";
    private static final String KEY_COURT_ROOM = "courtRoom";
    private static final String KEY_TYPE_OF_COURT = "typeOfCourt";
    private static final String KEY_HEARING_DATE = "hearingDate";
    private static final UUID VALUE_CASE_ID = randomUUID();
    private static final UUID VALUE_USER_ID = randomUUID();
    private static final UUID VALUE_DEFENDANT_ID = randomUUID();
    private static final String VALUE_COURT_ID_MATCHING = "B05BK00";
    private static final String VALUE_COURT_ID_MATCHING2 = "B05BK00";
    private static final String VALUE_COURT_NAME = "Birkenhead";
    private static final String VALUE_COURT_NAME2 = "Bootle";
    private static final Integer VALUE_COURT_ROOM = 10;
    private static final String VALUE_TYPE_OF_COURT = "Magistrates' Courts";
    private static final String VALUE_HEARING_DATE = "2010-08-02";
    private static final String KEY_ID = "id";
    private static final String KEY_LOCATION_CODE = "locationCode";
    private static final String KEY_LEVEL_1_NAME = "level1Name";
    private static final String KEY_LEVEL_3_NAME = "level3Name";
    private static final String KEY_COURT_LOCATIONS = "courtLocations";
    private static final String ORGANISATIONUNITS = "organisationunits";
    private static final String OUCODE = "oucode";
    private static final String OUCODEL1CODE = "oucodeL1Code";
    private static final String COURTROOM = "courtRoom";
    private static final String VENUENAME = "venueName";
    private static final String VENUENAMEVALUE = "FF LAW COURTS";
    private static final String COURTROOMID = "courtroomId";
    private static final String COURTROOMNAME = "courtroomName";
    private static final String COURTROOMNAMEVALUE_1 = "Courtroom 01";
    private static final String COURTROOMNAMEVALUE_2 = "Courtroom 02";
    private static final String WELSHVENUENAME = "oucodeL3WelshName";
    private static final String OUCODEL3NAME = "oucodeL3Name";
    private static final String OUCODEL3NAMEVALUE = "Wrexham Magistrates' Court";
    private static final String WELSHVENUENAMEVALUE = "Canolfan Gyfiawnder Yr Wyddgrug";
    private static final String KEY_ETHNICITIES = "ethnicities";
    private static final String KEY_VEHICLE_CODES = "vehicleCodes";
    private static final String KEY_HEARING_TYPES = "hearingTypes";
    private static final String HEARING_CODE1 = "PTP";
    private static final String HEARING_DESCRIPTION1 = "Plea & Trial Preparation";
    private static final String HEARING_CODE2 = "PTP2";
    private static final String REFERENCEDATA_QUERY_COURT_LOCATIONS = "referencedata.query.court-locations";
    private static final String PROSECUTIONCASEFILE_COMMAND_ASSIGN_HEARING = "prosecutioncasefile.command.assign-hearing";
    private static final String REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY = "reference-data.ethnicities";
    private static final String REFERENCEDATA_QUERY_VEHICLE_CODE = "referencedata.query.vehicle-codes";
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";


    public static JsonObject getMockReferenceDataCountryNationalities() {
        return Json.createObjectBuilder().add("countryNationality",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "22ef7a73-df50-4349-8c72-ca3b9ace6363")
                                        .add("cjseCode", 0)
                                        .add("isoCode", "GBR")
                                        .add("govCode", "GB")
                                        .add("countryName", "United Kingdom")
                                        .add("nationality", "British")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "05ad4861-7927-437e-8df1-32b4128e369f")
                                        .add("cjseCode", 0)
                                        .add("isoCode", "FRA")
                                        .add("govCode", "FR")
                                        .add("countryName", "France")
                                        .add("nationality", "French")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "ca2438fd-2eba-4c1b-967d-3fa7c46f24d4")
                                        .add("cjseCode", 3)
                                        .add("isoCode", "ITA")
                                        .add("govCode", "IT")
                                        .add("countryName", "ITALY")
                                        .add("nationality", "Italian")
                                        .build())

                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataOffenceDateCodes() {
        return Json.createObjectBuilder().add("offenceDateCodes",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add("seqNum", 10)
                                        .add("dateCode", "1")
                                        .add("dateCodeDescription", "on or in")
                                        .add(VALID_FROM, "2019-04-01")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add("seqNum", 20)
                                        .add("dateCode", "2")
                                        .add("dateCodeDescription", "before")
                                        .add(VALID_FROM, "2019-04-01")
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataOffenderCodes() {
        return Json.createObjectBuilder().add("offenderCodes",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add("seqNo", 10)
                                        .add("code", "1")
                                        .add("description", "on or in")
                                        .add(VALID_FROM, "2019-04-01")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add("seqNo", 20)
                                        .add("code", "2")
                                        .add("description", "before")
                                        .add(VALID_FROM, "2019-04-01")
                                        .build())
                        .build())
                .build();
    }


    public static JsonObject getMockReferenceDataSummonsCodes() {
        return Json.createObjectBuilder().add("summonsCodes",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQ_NO, 10)
                                        .add("summonsCode", "A")
                                        .add("summonsCodeDescription", "Application / Complaint")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQ_NO, 20)
                                        .add("summonsCode", "B")
                                        .add("summonsCodeDescription", "Breach offences")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())
                .build();
    }


    public static JsonObject getMockReferenceDataDocumentsTypeAccess() {

        JsonArrayBuilder autorArray = Json.createArrayBuilder();
        JsonObjectBuilder autorObject = Json.createObjectBuilder();

        JsonObjectBuilder rbacObject = Json.createObjectBuilder();



        JsonObjectBuilder readUserGroupsCppGroupObject = Json.createObjectBuilder();
        readUserGroupsCppGroupObject
                .add("id","ff9fb2c8-2738-4d77-87e5-56b5781b4113")
                .add("groupName", "Legal Advisors");

        JsonObjectBuilder uploadUserGroupsCppGroupObject = Json.createObjectBuilder();
        uploadUserGroupsCppGroupObject
                .add("id","ff9fb2c8-2738-4d77-87e5-56b5781b4113")
                .add("groupName", "Court Admin");

        JsonObjectBuilder downloadUserCppGroupObject = Json.createObjectBuilder();
        downloadUserCppGroupObject
                .add("id","ff9fb2c8-2738-4d77-87e5-56b5781b4113")
                .add("groupName", "Crown Court Clerk");

        JsonObjectBuilder deleteUserGroupsCppGroupObject = Json.createObjectBuilder();
        deleteUserGroupsCppGroupObject
                .add("id","ff9fb2c8-2738-4d77-87e5-56b5781b4113")
                .add("groupName", "Listing Officer");





        JsonObjectBuilder readUserGroupsInnerObject = Json.createObjectBuilder();
        readUserGroupsInnerObject
                .add("cppGroup",readUserGroupsCppGroupObject)
                .add("validFrom", "1983-05-13")
                .add("validTo", "2022-05-13");

        JsonObjectBuilder uploadUserGroupsInnerObject = Json.createObjectBuilder();
        uploadUserGroupsInnerObject
                .add("cppGroup",uploadUserGroupsCppGroupObject)
                .add("validFrom", "1983-05-13")
                .add("validTo", "2022-05-13");

        JsonObjectBuilder downloadUserInnerObject = Json.createObjectBuilder();
        downloadUserInnerObject
                .add("cppGroup",downloadUserCppGroupObject)
                .add("validFrom", "1983-05-13")
                .add("validTo", "2022-05-13");

        JsonObjectBuilder deleteUserGroupsInnerObject = Json.createObjectBuilder();
        deleteUserGroupsInnerObject
                .add("cppGroup",deleteUserGroupsCppGroupObject)
                .add("validFrom", "1983-05-13")
                .add("validTo", "2022-05-13");


        JsonArrayBuilder readUserGroupsArray = Json.createArrayBuilder();
        readUserGroupsArray.add(readUserGroupsInnerObject);

        JsonArrayBuilder uploadUserGroupsArray = Json.createArrayBuilder();
        uploadUserGroupsArray.add(uploadUserGroupsInnerObject);

        JsonArrayBuilder downloadUserArray = Json.createArrayBuilder();
        downloadUserArray.add(downloadUserInnerObject);

        JsonArrayBuilder deleteUserGroupsArray = Json.createArrayBuilder();
        deleteUserGroupsArray.add(deleteUserGroupsInnerObject);


        JsonObjectBuilder readUserGroupsAutorObject = Json.createObjectBuilder();
        readUserGroupsAutorObject.add("readUserGroups",readUserGroupsArray);
        readUserGroupsAutorObject.add("uploadUserGroups",uploadUserGroupsArray);
        readUserGroupsAutorObject.add("downloadUserGroups",downloadUserArray);
        readUserGroupsAutorObject.add("deleteUserGroups",deleteUserGroupsArray);

        autorObject.add("id", "460f6f7a-c002-11e8-a355-529269fb1459")
                .add("section", "CITN")
                .add("documentCategory", "Bail and Custody")
                .add("actionRequired", false)
                .add("validFrom", "1983-05-13")
                .add("validTo", "2022-05-13")
                .add("courtDocumentTypeRBAC",readUserGroupsAutorObject);

        autorArray.add(autorObject);

        return Json.createObjectBuilder().add("documentsTypeAccess", autorArray).build();



    }




    public static JsonObject getMockReferenceDataAlcoholLevelMethods() {
        return Json.createObjectBuilder().add("alcoholLevelMethods",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "c4ca4238-a0b9-3382-8dcc-509a6f75849b")
                                        .add(SEQ_NO, 1)
                                        .add(METHOD_CODE, "A")
                                        .add(METHOD_DESCRIPTION, "Blood")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "c81e728d-9d4c-3f63-af06-7f89cc14862c")
                                        .add(SEQ_NO, 2)
                                        .add(METHOD_CODE, "B")
                                        .add(METHOD_DESCRIPTION, "Breath")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "eccbc87e-4b5c-32fe-a830-8fd9f2a7baf3")
                                        .add(SEQ_NO, 3)
                                        .add(METHOD_CODE, "C")
                                        .add(METHOD_DESCRIPTION, "Urine")
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataParentBundleSectionReferenceData(final Integer seqNum,
                                                                                  final String sectionCode,
                                                                                  final String sectionName,
                                                                                  final Boolean splitBundleSubSection,
                                                                                  final String targetSectionCode) {
        final JsonObjectBuilder builder = createObjectBuilder();
        final String validityDate = "2017-08-01";
        builder.add("id", randomUUID().toString());

        ofNullable(seqNum).ifPresent(seq ->
                builder.add("seqNum", seq));
        builder.add("cpsBundleCode", "1");
        ofNullable(targetSectionCode).ifPresent(targetSectioncode ->
                builder.add("parentBundleCode", targetSectioncode));
        ofNullable(targetSectionCode).ifPresent(targetSectioncode ->
                builder.add("parentBundleDescription", targetSectioncode));

        ofNullable(targetSectionCode).ifPresent(targetSectioncode ->
                builder.add("targetSectionCode", targetSectioncode));
        builder.add("bundleAcceptanceFlag", true);
        builder.add("unbundleFlag", true);



        builder.add(VALID_FROM, validityDate);
        builder .add(VALID_TO, validityDate);

        return builder.build();

    }

    public static JsonObject getMockReferenceDataInitiationTypeJsonObject() {
        final String validityDate = "2017-08-01";
        return Json.createObjectBuilder().add("initiationTypes",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add(ID, "7e2f843e-d639-40b3-8611-8015f3a18957")
                                        .add(SEQUENCE, 1)
                                        .add(DESCRIPTION, "Charge")
                                        .add(CODE, "C")
                                        .add(VALID_FROM, validityDate)
                                        .add(VALID_TO, validityDate)
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add(ID, "8e2f843e-d639-40b3-8611-8015f3a18958")
                                        .add(SEQUENCE, 2)
                                        .add(DESCRIPTION, "Summons")
                                        .add(CODE, "S")
                                        .add(VALID_FROM, validityDate)
                                        .add(VALID_TO, validityDate)
                                        .build())
                        .build())
                .build();
    }

    public static Envelope<JsonObject> buildSelfDefinedInformationEthnicity() {
        final JsonArrayBuilder courtLocationsArrayBuilder = createArrayBuilder();

        final JsonObject ethnicity1 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add(CODE, "W1")
                .add(DESCRIPTION, "British")
                .build();

        final JsonObject ethnicity2 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add(CODE, "W2")
                .add(DESCRIPTION, "Irish")
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY),
                createObjectBuilder()
                        .add(KEY_ETHNICITIES, courtLocationsArrayBuilder
                                .add(ethnicity1)
                                .add(ethnicity2))
                        .build());

    }

    public static  Envelope<JsonObject> buildVehicleCodes() {
        final JsonArrayBuilder courtLocationsArrayBuilder = createArrayBuilder();

        final JsonObject vehicleCode1 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("vehicleCode", "1")
                .add("vehicleCodeDescription", "Large Goods Vehicle")
                .build();

        final JsonObject vehicleCode2 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("vehicleCode", "2")
                .add("vehicleCodeDescription", "Other")
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_QUERY_VEHICLE_CODE),
                createObjectBuilder()
                        .add(KEY_VEHICLE_CODES, courtLocationsArrayBuilder
                                .add(vehicleCode1)
                                .add(vehicleCode2))
                        .build());
    }

    public static Envelope<JsonObject> buildHearingTypes() {
        final JsonArrayBuilder hearingTypesArrayBuilder = createArrayBuilder();

        final JsonObject hearingType1 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("hearingCode", HEARING_CODE1)
                .add("hearingDescription", HEARING_DESCRIPTION1)
                .build();

        final JsonObject hearingType2 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("hearingCode", HEARING_CODE2)
                .add("hearingDescription", "Other")
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_QUERY_HEARING_TYPES),
                createObjectBuilder()
                        .add(KEY_HEARING_TYPES, hearingTypesArrayBuilder
                                .add(hearingType1)
                                .add(hearingType2))
                        .build());
    }

    public static JsonEnvelope buildCourtLocations() {
        final JsonArrayBuilder courtLocationsArrayBuilder = createArrayBuilder();

        final JsonObject location1 = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(KEY_LOCATION_CODE, VALUE_COURT_ID_MATCHING)
                .add(KEY_LEVEL_1_NAME, VALUE_TYPE_OF_COURT)
                .add(KEY_LEVEL_3_NAME, VALUE_COURT_NAME)
                .build();

        final JsonObject location2 = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(KEY_LOCATION_CODE, VALUE_COURT_ID_MATCHING2)
                .add(KEY_LEVEL_1_NAME, VALUE_TYPE_OF_COURT)
                .add(KEY_LEVEL_3_NAME, VALUE_COURT_NAME2)
                .build();

        return envelopeFrom(metadataBuilder().withId(randomUUID()).withName(REFERENCEDATA_QUERY_COURT_LOCATIONS),
                createObjectBuilder()
                        .add(KEY_COURT_LOCATIONS, courtLocationsArrayBuilder
                                .add(location1)
                                .add(location2))
                        .build());

    }

    public static JsonEnvelope buildCommandWith(final JsonObjectBuilder hearing) {
        return envelope()
                .with(metadataBuilder()
                        .withId(randomUUID())
                        .withName(PROSECUTIONCASEFILE_COMMAND_ASSIGN_HEARING)
                        .withUserId(VALUE_USER_ID.toString())
                )
                .withPayloadOf(VALUE_CASE_ID, KEY_CASE_ID)
                .withPayloadOf(VALUE_DEFENDANT_ID, KEY_DEFENDANT_ID)
                .withPayloadOf(hearing.build(), KEY_NEXT_HEARING)
                .build();
    }

    public static JsonObjectBuilder createHearingWith(final String courtId) {
        return createObjectBuilder()
                .add(KEY_COURT_ID, courtId)
                .add(KEY_COURT_ROOM, VALUE_COURT_ROOM)
                .add(KEY_TYPE_OF_COURT, VALUE_TYPE_OF_COURT)
                .add(KEY_HEARING_DATE, VALUE_HEARING_DATE);
    }

    public static JsonObjectBuilder createHearingWithoutCourtId() {
        return createObjectBuilder()
                .add(KEY_COURT_ROOM, VALUE_COURT_ROOM)
                .add(KEY_TYPE_OF_COURT, VALUE_TYPE_OF_COURT)
                .add(KEY_HEARING_DATE, VALUE_HEARING_DATE);
    }

    public static Envelope<JsonObject> buildOrganisationUnitWithCourtroom() {

        final JsonObject courtRoom = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(VENUENAME, VENUENAMEVALUE)
                .add(COURTROOMID, "123")
                .add(COURTROOMNAME, COURTROOMNAMEVALUE_1)
                .build();

        final JsonObject organisationUnit = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(OUCODE, VALUE_COURT_ID_MATCHING)
                .add(OUCODEL1CODE, "B")
                .add(WELSHVENUENAME, WELSHVENUENAMEVALUE)
                .add(OUCODEL3NAME,OUCODEL3NAMEVALUE)
                .add(COURTROOM, courtRoom)
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("Test"), organisationUnit);

    }

    public static JsonObject getMockReferenceDataOffenceData() {
        return Json.createObjectBuilder().add("offences",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add("cjsOffenceCode", "cjsOffenceCode")
                                        .add("fullName", "fullName")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add("cjsOffenceCode", "cjsOffenceCode2")
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataLicenceCode() {
        return Json.createObjectBuilder().add("licenceCodes",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 10)
                                        .add("licenceCode", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 20)
                                        .add("licenceCode", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataPoliceForces() {
        return Json.createObjectBuilder().add("policeForces",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "419a13f4-f1c6-3e1e-b0f6-f1c162d4c41a")
                                        .add(SEQUENCE, 2)
                                        .add("policeForceCode", "1")
                                        .add("policeForceName", "London")
                                        .add("validForSpiOut", true)
                                        .add("oucodeL2Code","01")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "47bb2c5d-cde9-3c13-9ae3-b3f5be9177e7")
                                        .add(SEQUENCE, 2)
                                        .add("policeForceCode", "2")
                                        .add("policeForceName", "London")
                                        .add("validForSpiOut", true)
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataObservedEthnicity() {
        return Json.createObjectBuilder().add("observedEthnicities",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQ_NO, 10)
                                        .add("ethnicityCode", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQ_NO, 20)
                                        .add("ethnicityCode", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())
                .build();
    }


    public static JsonObject getMockReferenceDataCustodyStatus() {
        return Json.createObjectBuilder().add("custodyStatuses",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 10)
                                        .add("statusCode", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 20)
                                        .add("statusCode", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())
                .build();
    }

    public static JsonObject getMockReferenceDataPoliceRanks() {
        return Json.createObjectBuilder().add("policeRanks",
                Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 10)
                                        .add("rankCode", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                Json.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 20)
                                        .add("rankCode", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())
                .build();
    }
}