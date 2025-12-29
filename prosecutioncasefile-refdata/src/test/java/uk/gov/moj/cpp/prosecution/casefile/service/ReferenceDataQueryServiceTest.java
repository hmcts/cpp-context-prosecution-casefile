package uk.gov.moj.cpp.prosecution.casefile.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildCommandWith;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildCourtLocations;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildHearingTypes;
//import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildLjaDetails;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildOrganisationUnitWithCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildSelfDefinedInformationEthnicity;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.buildVehicleCodes;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.createHearingWith;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.createHearingWithoutCourtId;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataAlcoholLevelMethods;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataCountryNationalities;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataCustodyStatus;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataDocumentsTypeAccess;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataInitiationTypeJsonObject;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataLicenceCode;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataObservedEthnicity;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataOffenceData;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataOffenceDateCodes;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataOffenderCodes;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataParentBundleSectionReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataPoliceForces;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataPoliceRanks;
import static uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryServiceTestHelper.getMockReferenceDataSummonsCodes;

import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.hearing.courts.referencedata.EnforcementArea;
import uk.gov.justice.hearing.courts.referencedata.LocalJusticeArea;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.LicenceCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceDateCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceRankReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleCodeReferenceData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataQueryServiceTest {

    public static final String VALID_FROM = "validFrom";

    public static final String SEQUENCE = "sequence";
    public static final String ID = "id";
    private static final String VALUE_COURT_ID_MATCHING = "B05BK00";
    private static final String VALUE_COURT_ID_NOT_MATCHING = "XYZ123";
    private static final String VALUE_COURT_NAME = "Birkenhead";
    private static final String VALUE_TYPE_OF_COURT = "Magistrates' Courts";
    private static final String KEY_ID = "id";
    private static final String VALID_INITIATION_CODE = "S";
    private static final String INVALID_INITIATION_CODE = "A";
    private static final String ORGANISATIONUNITS = "organisationunits";
    private static final String OUCODE = "oucode";
    private static final String OUCODEL1CODE = "oucodeL1Code";
    private static final String COURT_ROOM_ID_VALUE = "123";
    private static final String COURTROOMNAMEVALUE_1 = "Courtroom 01";
    private static final String WELSHVENUENAME = "oucodeL3WelshName";
    private static final String WELSHVENUENAMEVALUE = "Canolfan Gyfiawnder Yr Wyddgrug";
    private static final String OUCODEL3NAMEVALUE = "Wrexham Magistrates' Court";
    private static final String HEARING_CODE1 = "PTP";
    private static final String HEARING_DESCRIPTION1 = "Plea & Trial Preparation";
    private static final String HEARING_CODE2 = "PTP2";
    @InjectMocks
    private ReferenceDataQueryServiceImpl referenceDataService;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Test
    public void shouldRetrieveCourtNameWhenCourtIdIsMatched() {

        final JsonObjectBuilder hearing = createHearingWith(VALUE_COURT_ID_MATCHING);
        final JsonEnvelope envelope = buildCommandWith(hearing);

        when(requester.request(any(JsonEnvelope.class))).thenReturn(buildCourtLocations());

        // when
        final JsonObjectBuilder courtNameBuilder = referenceDataService.getCourtName(envelope);

        with(courtNameBuilder.build().toString())
                .assertThat("$.courtName", is(VALUE_TYPE_OF_COURT + " - " + VALUE_COURT_NAME));

    }

    @Test
    public void shouldNotRetrieveCourtNameWhenCourtIdCantBeMatched() {

        final JsonObjectBuilder hearing = createHearingWith(VALUE_COURT_ID_NOT_MATCHING);
        final JsonEnvelope envelope = buildCommandWith(hearing);

        when(requester.request(any(JsonEnvelope.class))).thenReturn(buildCourtLocations());

        // when
        final JsonObjectBuilder courtNameBuilder = referenceDataService.getCourtName(envelope);

        final JsonObject courtName = courtNameBuilder.build();
        assertThat(courtName.getString("courtName", null), nullValue());
    }

    @Test
    public void shouldNotRetrieveCourtNameWhenCourtIdIsNull() {

        final JsonObjectBuilder hearing = createHearingWithoutCourtId();
        final JsonEnvelope envelope = buildCommandWith(hearing);

        when(requester.request(any(JsonEnvelope.class))).thenReturn(buildCourtLocations());

        // when
        final JsonObjectBuilder courtNameBuilder = referenceDataService.getCourtName(envelope);

        final JsonObject courtName = courtNameBuilder.build();
        assertThat(courtName.getString("courtName", null), nullValue());
    }


    @Test
    public void shouldRetrieveCountryNationalityList() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.country-nationality");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataCountryNationalities();
        final Envelope<JsonObject> countryNationalities = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(countryNationalities);

        final List<ReferenceDataCountryNationality> referenceDataCountryNationalities = referenceDataService.retrieveCountryNationality();
        assertThat(referenceDataCountryNationalities, is(notNullValue()));
        assertThat(referenceDataCountryNationalities.size(), is(3));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.country-nationality");
    }


    @Test
    public void shouldRetrieveOffenceDateCodesList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.offence-date-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenceDateCodes();
        final Envelope<JsonObject> offenceDateCodes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(offenceDateCodes);

        final List<OffenceDateCodeReferenceData> referenceDataList = referenceDataService.retrieveOffenceDateCodes();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(2));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.offence-date-codes");
    }

    @Test
    public void shouldRetrieveOffenderCodeList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.offender-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenderCodes();
        final Envelope<JsonObject> offenderCodes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(offenderCodes);

        final List<OffenderCodeReferenceData> referenceDataList = referenceDataService.retrieveOffenderCodes();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(2));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.offender-codes");
    }

    @Test
    public void shouldRetrieveSummonsCodesList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.summons-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataSummonsCodes();
        final Envelope<JsonObject> summonsCodes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(summonsCodes);

        final List<SummonsCodeReferenceData> referenceDataList = referenceDataService.retrieveSummonsCodes();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(2));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.summons-codes");
    }

    @Test
    public void shouldRetrieveDocumentsMetadataList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.documents-type-access");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataDocumentsTypeAccess();
        final Envelope<JsonObject> documentsMetadata = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class),eq(JsonObject.class)))
                .thenReturn(documentsMetadata);

        final List<DocumentTypeAccessReferenceData> referenceDataList = referenceDataService.retrieveDocumentsTypeAccess();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(1));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.get-all-document-type-access");
    }

    @Test
    public void shouldRetrieveAlcoholLevelMethodsList() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.alcohol-level-methods");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataAlcoholLevelMethods();

        final Envelope<JsonObject> alcoholLevelMetMethods = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class)))
                .thenReturn(alcoholLevelMetMethods);

        final List<AlcoholLevelMethodReferenceData> referenceDataList = referenceDataService.retrieveAlcoholLevelMethods();
        assertThat(referenceDataList, is(notNullValue()));
        assertThat(referenceDataList.size(), is(3));

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(), eq(JsonObject.class));
        verifyEnvelopeData(jsonEnvelopeCaptor.getValue(), "referencedata.query.alcohol-level-methods");
    }

    @Test
    public void shouldReturnSelfDefinedInformationEthnicity() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildSelfDefinedInformationEthnicity();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<SelfdefinedEthnicityReferenceData> refEthnicityData = referenceDataService.retrieveSelfDefinedEthnicity();
        assertThat(refEthnicityData, is(notNullValue()));
        assertThat(refEthnicityData.size(), is(2));

    }

    @Test
    public void shouldReturnVehicleCodeRefData() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildVehicleCodes();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<VehicleCodeReferenceData> refVehicleCodeRefData = referenceDataService.retrieveVehicleCodes();
        assertThat(refVehicleCodeRefData, is(notNullValue()));
        assertThat(refVehicleCodeRefData.size(), is(2));

    }

    @Test
    public void shouldRetrieveOrganisationUnitWithCourtRoom() {

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(buildOrganisationUnitWithCourtroom());

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomRefData = referenceDataService.retrieveOrganisationUnitWithCourtroom(VALUE_COURT_ID_MATCHING);
        if (organisationUnitWithCourtroomRefData.isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData organisationUnitWithCourtroomsReferenceData = organisationUnitWithCourtroomRefData.get();

            assertThat(organisationUnitWithCourtroomsReferenceData, notNullValue());
            assertThat(organisationUnitWithCourtroomsReferenceData.getId(), notNullValue());
            assertThat(organisationUnitWithCourtroomsReferenceData.getOucode(), is(VALUE_COURT_ID_MATCHING));
            assertThat(organisationUnitWithCourtroomsReferenceData.getOucodeL3WelshName(), is(WELSHVENUENAMEVALUE));
            assertThat(organisationUnitWithCourtroomsReferenceData.getCourtRoom().getCourtroomName(), is(COURTROOMNAMEVALUE_1));
            assertThat(organisationUnitWithCourtroomsReferenceData.getCourtRoom().getCourtroomId().toString(), is(COURT_ROOM_ID_VALUE));
        }

    }

    @Test
    public void shouldRetrieveOrganisationUnitBySpiOucode() {

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(buildOrganisationUnit());

        final OrganisationUnitReferenceData organisationUnitReferenceData = referenceDataService.retrieveOrganisationUnits(VALUE_COURT_ID_MATCHING).get(0);

        assertThat(organisationUnitReferenceData, notNullValue());
        assertThat(organisationUnitReferenceData.getId(), notNullValue());
        assertThat(organisationUnitReferenceData.getOucode(), is(VALUE_COURT_ID_MATCHING));
        assertThat(organisationUnitReferenceData.getOucodeL3WelshName(), is(WELSHVENUENAMEVALUE));
    }

    @Test
    public void shouldRetrieveOrganisationUnitByOuCode() {

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(buildOrganisationUnit());

        final OrganisationUnitReferenceData organisationUnitReferenceData = referenceDataService.retrieveOrganisationUnitsByOuCode(VALUE_COURT_ID_MATCHING).get(0);

        assertThat(organisationUnitReferenceData, notNullValue());
        assertThat(organisationUnitReferenceData.getId(), notNullValue());
        assertThat(organisationUnitReferenceData.getOucode(), is(VALUE_COURT_ID_MATCHING));
        assertThat(organisationUnitReferenceData.getOucodeL3WelshName(), is(WELSHVENUENAMEVALUE));
    }

    @Test
    public void shouldReturnTrueWhenInitiationCodeIsValid() {
        checkInitiationCodeValidity(VALID_INITIATION_CODE, true);
    }

    @Test
    public void shouldReturnFalseWhenInitiationCodeIsInvalid() {
        checkInitiationCodeValidity(INVALID_INITIATION_CODE, false);
    }

    @Test
    public void shouldReturnHearingTypesRefData() {

        final Envelope<JsonObject> mockRefDataEnvelope = buildHearingTypes();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final HearingTypes hearingTypesRefData = referenceDataService.retrieveHearingTypes();
        assertThat(hearingTypesRefData, is(notNullValue()));
        assertThat(hearingTypesRefData.getHearingtypes().size(), is(2));
        assertThat(hearingTypesRefData.getHearingtypes().get(0).getHearingCode(), is(HEARING_CODE1));
        assertThat(hearingTypesRefData.getHearingtypes().get(0).getHearingDescription(), is(HEARING_DESCRIPTION1));
        assertThat(hearingTypesRefData.getHearingtypes().get(1).getHearingCode(), is(HEARING_CODE2));

    }

    @Test
    public void shouldReturnPoliceRanksRefData() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.Police-Ranks");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataPoliceRanks();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<PoliceRankReferenceData> policeRankReferenceData = referenceDataService.retrievePoliceRanks();

        assertThat(policeRankReferenceData, is(notNullValue()));
        assertThat(policeRankReferenceData.size(), is(2));
    }


    @Test
    public void shouldReturnCustodyStatusRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.custody-statuses");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataCustodyStatus();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<CustodyStatusReferenceData> custodyStatusReferenceData = referenceDataService.retrieveCustodyStatuses();

        assertThat(custodyStatusReferenceData, is(notNullValue()));
        assertThat(custodyStatusReferenceData.size(), is(2));

    }


    @Test
    public void shouldReturnObservedEthnicityRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.observed-ethnicities");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataObservedEthnicity();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<ObservedEthnicityReferenceData> observedEthnicityReferenceData = referenceDataService.retrieveObservedEthnicity();

        assertThat(observedEthnicityReferenceData, is(notNullValue()));
        assertThat(observedEthnicityReferenceData.size(), is(2));

    }

    @Test
    public void shouldReturnLicenceCodeRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.licence-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataLicenceCode();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<LicenceCodeReferenceData> licenceCodeReferenceData = referenceDataService.retrieveLicenceCode();

        assertThat(licenceCodeReferenceData.get(0), instanceOf(LicenceCodeReferenceData.class));
        assertThat(licenceCodeReferenceData.size(), is(2));
    }

    @Test
    public void shouldReturnPoliceForcesRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.police-forces");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataPoliceForces();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<PoliceForceReferenceData> policeForceReferenceDataList = referenceDataService.retrievePoliceForceCode();

        assertThat(policeForceReferenceDataList.get(0), instanceOf(PoliceForceReferenceData.class));
        assertThat(policeForceReferenceDataList.size(), is(2));
    }

    @Test
    public void shouldReturnInitiationCodesRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.initiation-types");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataInitiationCodes();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<String> initiationCodes = referenceDataService.getInitiationCodes();

        assertThat(initiationCodes, is(notNullValue()));
        assertThat(initiationCodes.size(), is(2));

    }

    private JsonObject getMockReferenceDataInitiationCodes() {
        return JsonObjects.createObjectBuilder().add("initiationTypes",
                JsonObjects.createArrayBuilder()
                        .add(
                                JsonObjects.createObjectBuilder()
                                        .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 10)
                                        .add("code", "AA")
                                        .add(VALID_FROM, "2019-03-11")
                                        .build())
                        .add(
                                JsonObjects.createObjectBuilder()
                                        .add("id", "5aaecac5-222b-402d-9047-84803679edac")
                                        .add(SEQUENCE, 20)
                                        .add("code", "BB")
                                        .add(VALID_FROM, "2019-03-01")
                                        .build())
                        .build())

                .build();
    }

    @Test
    public void shouldReturnProsecutorsRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.licence-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataProsecutors();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final ProsecutorsReferenceData prosecutors = referenceDataService.retrieveProsecutors("string");
        assertThat(prosecutors, is(notNullValue()));
        assertThat(prosecutors.getSequenceNumber(), is(10));
        assertThat(prosecutors.getFullName(), is("fullName"));
        assertThat(prosecutors.getContactEmailAddress(), is("contact@cpp.co.uk"));
    }

    @Test
    public void shouldReturnNspProsecutorsRefData() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.licence-codes");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataProsecutors();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final ProsecutorsReferenceData prosecutors = referenceDataService.getProsecutorById(randomUUID());
        assertThat(prosecutors, is(notNullValue()));
        assertThat(prosecutors.getSequenceNumber(), is(10));
        assertThat(prosecutors.getFullName(), is("fullName"));
        assertThat(prosecutors.getContactEmailAddress(), is("contact@cpp.co.uk"));
    }

    private JsonObject getMockReferenceDataProsecutors() {
        return JsonObjects.createObjectBuilder()
                .add("id", "4aaecac5-222b-402d-9047-84803679edac")
                .add("sequenceNumber", 10)
                .add("fullName", "fullName")
                .add("contactEmailAddress", "contact@cpp.co.uk")
                .build();
    }

    @Test
    public void shouldReturnOffenceDataRefData() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedataoffences.query.offences-list");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenceData();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final Offence offenceData = offence().withOffenceCode("OFCODE_12").withOffenceCommittedDate(LocalDate.of(2018, 10, 10)).withChargeDate(LocalDate.of(2018, 10, 10)).build();

        final List<OffenceReferenceData> offenceReferenceData = referenceDataService.retrieveOffenceData(offenceData, "S");

        assertThat(offenceReferenceData, is(notNullValue()));
        assertThat(offenceReferenceData.size(), is(2));
        assertThat(offenceReferenceData.get(0).getCjsOffenceCode(), is("cjsOffenceCode"));
    }

    @Test
    void shouldReturnCivilOffenceDataRefData() {

        final Metadata metadata = getMockMetadataWithName("NOT.referencedataoffences.query.offences-list");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataOffenceData();

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final List<OffenceReferenceData> offenceReferenceData = referenceDataService.retrieveOffenceDataList(List.of("OFCODE_12"), Optional.of("MoJ"));

        assertThat(offenceReferenceData, is(notNullValue()));
        assertThat(offenceReferenceData.size(), is(2));
        assertThat(offenceReferenceData.get(0).getCjsOffenceCode(), is("cjsOffenceCode"));
    }

    @Test
    public void shouldReturnParentBundleRefData() {

        final Metadata metadata = getMockMetadataWithName("referencedata.query.parent-bundle-section");

        final JsonObject mockRefDataJsonObject = getMockReferenceDataParentBundleSectionReferenceData(10, "1", "IDPC", Boolean.TRUE, "IDPC");

        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, mockRefDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);

        final ParentBundleSectionReferenceData parentBundleSectionReferenceData = referenceDataService.getParentBundleSectionByCpsBundleCode(mockRefDataEnvelope.metadata(), "1");
        assertThat(parentBundleSectionReferenceData.getId(), is(notNullValue()));
        assertThat(parentBundleSectionReferenceData.getCpsBundleCode(), is("1"));
        assertThat(parentBundleSectionReferenceData.getTargetSectionCode(), is("IDPC"));

    }


    private void checkInitiationCodeValidity(final String initiationCode, final boolean isValid) {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.initiation-types");

        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataInitiationTypeJsonObject();

        final Envelope<JsonObject> initiationTypes = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class)))
                .thenReturn(initiationTypes);

        final boolean isInitiationCodeValid = referenceDataService.isInitiationCodeValid(initiationCode);
        assertEquals(isInitiationCodeValid, isValid);

        verify(requester).requestAsAdmin(jsonEnvelopeCaptor.capture(),eq(JsonObject.class));
        final JsonEnvelope captorValue = jsonEnvelopeCaptor.getValue();
        verifyEnvelopeData(captorValue, "referencedata.query.initiation-types");
    }

    private void verifyEnvelopeData(final JsonEnvelope envelope, final String expectedName) {
        assertThat(envelope, is(notNullValue()));
        final Metadata requestMetadata = envelope.metadata();
        assertThat(requestMetadata.name(), is(expectedName));
    }

    private Metadata getMockMetadataWithName(final String name) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(name)
                .withStreamId(randomUUID())
                .withUserId("mr user")
                .build();
    }

    private Envelope<JsonObject> buildOrganisationUnit() {
        final JsonObject organisationUnit = createObjectBuilder()
                .add(KEY_ID, randomUUID().toString())
                .add(OUCODE, VALUE_COURT_ID_MATCHING)
                .add(OUCODEL1CODE, "B")
                .add(WELSHVENUENAME, WELSHVENUENAMEVALUE)
                .build();

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("Test"),
                createObjectBuilder()
                        .add(ORGANISATIONUNITS, createArrayBuilder()
                                .add(organisationUnit))
                        .build());

    }

    @Test
    public void shouldReturnProsecutorDetailsWhenProsecutorIdIsSent() {
        final Metadata metadata = getMockMetadataWithName("NOT.referencedata.query.licence-codes");
        final JsonObject expectedReferenceDataJsonObject = getMockReferenceDataProsecutors();
        final Envelope<JsonObject> mockRefDataEnvelope = Envelope.envelopeFrom(metadata, expectedReferenceDataJsonObject);
        final UUID prosecutorId = randomUUID();
        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(mockRefDataEnvelope);
        final ProsecutorsReferenceData prosecutors = referenceDataService.getProsecutorById(prosecutorId);
        assertThat(prosecutors, is(notNullValue()));
        assertThat(prosecutors.getSequenceNumber(), is(10));
        assertThat(prosecutors.getFullName(), is("fullName"));
    }

    @Test
    public void shouldRetrieveOrganisationUnitWithCourtName() {

        when(requester.requestAsAdmin(any(JsonEnvelope.class), eq(JsonObject.class))).thenReturn(buildOrganisationUnitWithCourtroom());

        final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomRefData = referenceDataService.retrieveCourtCentreDetails(VALUE_TYPE_OF_COURT);
        if (organisationUnitWithCourtroomRefData.isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData organisationUnitWithCourtroomsReferenceData = organisationUnitWithCourtroomRefData.get();

            assertThat(organisationUnitWithCourtroomsReferenceData, notNullValue());
            assertThat(organisationUnitWithCourtroomsReferenceData.getId(), notNullValue());
            assertThat(organisationUnitWithCourtroomsReferenceData.getOucode(), is(VALUE_COURT_ID_MATCHING));
            assertThat(organisationUnitWithCourtroomsReferenceData.getOucodeL3WelshName(), is(WELSHVENUENAMEVALUE));
            assertThat(organisationUnitWithCourtroomsReferenceData.getOucodeL3Name(), is(OUCODEL3NAMEVALUE));
            assertThat(organisationUnitWithCourtroomsReferenceData.getCourtRoom().getCourtroomName(), is(COURTROOMNAMEVALUE_1));
            assertThat(organisationUnitWithCourtroomsReferenceData.getCourtRoom().getCourtroomId().toString(), is(COURT_ROOM_ID_VALUE));
        }
    }

    @Test
    public void shouldRetrieveLjaDetails() {

        when(requester.requestAsAdmin(any(), eq(EnforcementArea.class)))
                .thenReturn(Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("Test"), EnforcementArea.enforcementArea()
                .withLocalJusticeArea(LocalJusticeArea.localJusticeArea()
                        .withNationalCourtCode("2800")
                        .withName("South Yorkshire Magistrates' Court")
                        .build()).build()));

        final Optional<LjaDetails> ljaDetailsOptional = referenceDataService.getLjaDetails("2800", "d3d94468-02a4-3259-b55d-38e6d163e820");

        if (ljaDetailsOptional.isPresent()) {
            final LjaDetails ljaDetails = ljaDetailsOptional.get();

            assertThat(ljaDetails, notNullValue());
            assertThat(ljaDetails.getLjaCode(), is("2800"));
            assertThat(ljaDetails.getLjaName(), is("South Yorkshire Magistrates' Court"));
        }
    }

}