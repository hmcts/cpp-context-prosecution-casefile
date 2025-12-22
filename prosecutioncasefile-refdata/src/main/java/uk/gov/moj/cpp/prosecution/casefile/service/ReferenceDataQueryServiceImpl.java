package uk.gov.moj.cpp.prosecution.casefile.service;

import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes.hearingTypes;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asAlcoholLevelMethodRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asApplicationTypeRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asBailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asCaseMarkerRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asCountryNationalityRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asCustodyStatusRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asDocumentsMetadataRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asHearingTypesRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asLicenceCodeRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asModeOfTrialReasonsRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asMojOffencesRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asObservedEnthnicityRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asOffenceDateCodeRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asOffenceRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asOffenderCodeRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asOrganisationUnitRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asOrganisationUnitWithCourtroomRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asParentBundleSectionRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asPoliceForceRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asPoliceRankRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asProsecutorRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asSelfDefinedEnthnicityRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asSummonsCodeRefData;
import static uk.gov.moj.cpp.prosecution.casefile.service.RefDataHelper.asVehicleCodeRefData;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.hearing.courts.referencedata.EnforcementArea;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.LicenceCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MojOffences;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReferenceDataQueryServiceImpl implements ReferenceDataQueryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataQueryServiceImpl.class);
    public static final String ID = "id";
    private static final String REFERENCEDATA_QUERY_COURT_LOCATIONS = "referencedata.query.court-locations";
    private static final String REFERENCEDATA_QUERY_COURTROOMS_UNITS = "referencedata.query.ou.courtrooms.ou-courtroom-code";
    private static final String REFERENCEDATA_QUERY_ORGANISATION_UNIT = "referencedata.query.organisationunits";
    private static final String REFERENCEDATA_QUERY_MOJ_OFFENCES = "referencedata.query.moj-offences";
    private static final String REFERENCEDATA_QUERY_ALCOHOL_LEVEL_METHODS = "referencedata.query.alcohol-level-methods";
    private static final String REFERENCE_DATA_QUERY_OFFENCE_DATE_CODE = "referencedata.query.offence-date-codes";
    private static final String REFERENCEDATA_QUERY_CUSTODY_STATUS = "referencedata.query.custody-statuses";
    private static final String REFERENCEDATA_QUERY_BAIL_STATUS = "referencedata.query.bail-statuses";
    private static final String REFERENCEDATA_QUERY_HEARING_TYPES = "referencedata.query.hearing-types";
    private static final String REFERENCEDATA_QUERY_LICENCE_CODE = "referencedata.query.licence-codes";
    private static final String REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS = "referencedataoffences.query.offences-all-versions";
    private static final String REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST = "referencedataoffences.query.offences-list";
    private static final String REFERENCEDATA_QUERY_SUMMONS_CODES = "referencedata.query.summons-codes";
    private static final String REFERENCEDATA_QUERY_POLICE_RANKS = "referencedata.query.police-ranks";
    private static final String REFERENCEDATA_QUERY_COUNTRY_NATIONALITIES = "referencedata.query.country-nationality";
    private static final String REFERENCEDATA_QUERY_INITIATION_TYPES = "referencedata.query.initiation-types";
    private static final String REFERENCEDATA_QUERY_OFFENDER_CODES = "referencedata.query.offender-codes";
    private static final String REFERENCEDATA_QUERY_GET_PROSECUTOR_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS = "referencedata.get-all-document-type-access";
    private static final String REFERENCEDATA_QUERY_POLICE_FORCE = "referencedata.query.police-forces";
    private static final String REFERENCEDATA_QUERY_PARENT_BUNDLE_SECTION = "referencedata.query.parent-bundle-section";
    private static final String REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE = "referencedata.query.document-type-access-by-sectioncode";
    private static final String REFERENCEDATA_QUERY_MODE_OF_TRIAL_REASONS = "referencedata.query.mode-of-trial-reasons";
    private static final String REFERENCEDATA_QUERY_APPLICATION_TYPES = "referencedata.query.application-types";
    private static final String REFERENCEDATA_QUERY_APPLICATION_TYPE = "referencedata.query.application-type";
    private static final String REFERENCE_DATA_QUERY_OU_COURTROOM_BY_NAME = "referencedata.query.ou.courtrooms.ou-courtroom-name";
    private static final String KEY_COURT_ID = "courtId";
    private static final String KEY_COURT_NAME = "courtName";
    private static final String NEXT_HEARING = "nextHearing";
    private static final String OPAMI_LEVEL_1_NAME = "level1Name";
    private static final String OPAMI_LEVEL_3_NAME = "level3Name";
    private static final String OPAMI_COURT_LOCATIONS = "courtLocations";
    private static final String LOCATION_CODE = "locationCode";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_USAGE = "usage";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_TEAMS = "teams";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_CASE_MARKER = "C";
    private static final String REFERENCE_DATA_QUERY_CATEGORIES_TEAM_CC = "CC";
    private static final String REFERENCE_DATA_QUERY_CASE_MARKERS = "referencedata.case-markers.v2";
    private static final String FIELD_CASE_MARKERS = "caseMarkers";
    private static final String ORGANISATION_UNITS = "organisationunits";
    private static final String OUCODE = "oucode";
    private static final String SPIOUCODE = "spiOucode";
    private static final String MOJ_OFFENCES = "mojOffences";
    private static final String OU_COURTROOM_CODE = "ouCourtRoomCode";
    private static final String REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY = "referencedata.query.ethnicities";
    private static final String REFERENCE_DATA_QUERY_OBSERVED_ETHNICITY = "referencedata.query.observed-ethnicities";
    private static final String REFERENCE_DATA_QUERY_PROSECUTORS_BY_OUCODE = "referencedata.query.get.prosecutor.by.oucode";
    private static final String REFERENCE_DATA_QUERY_NSP_PROSECUTORS_BY_ID = "referencedata.query.prosecutor";
    private static final String FIELD_OFFENCE_TYPE = "offenceType";
    private static final String FIELD_SELF_ETHNICITIES = "ethnicities";
    private static final String FIELD_OBSERVED_ETHNICITIES = "observedEthnicities";
    private static final String REFERENCEDATA_QUERY_VEHICLE_CODE = "referencedata.query.vehicle-codes";
    private static final String FIELD_VEHICLE_CODES = "vehicleCodes";
    private static final String FIELD_ALCOHOL_LEVEL_METHODS = "alcoholLevelMethods";
    private static final String FIELD_OFFENCE_DATE_CODES = "offenceDateCodes";
    private static final String FIELD_CUSTODY_STATUSES = "custodyStatuses";
    private static final String FIELD_BAIL_STATUSES = "bailStatuses";
    private static final String FIELD_LICENCE_CODES = "licenceCodes";
    private static final String FIELD_HEARING_TYPES = "hearingTypes";
    private static final String FIELD_SUMMONS_CODES = "summonsCodes";
    private static final String FIELD_POLICE_RANKS = "policeRanks";
    private static final String FIELD_COUNTRY_NATIONALITIES = "countryNationality";
    private static final String FIELD_INITIATION_TYPES = "initiationTypes";
    private static final String FIELD_OFFENDER_CODES = "offenderCodes";
    private static final String FIELD_DOCUMENTS_TYPE_ACCESS = "documentsTypeAccess";
    private static final String FIELD_POLICE_FORCES = "policeForces";
    private static final String FIELD_CPS_BUNDLE_CODE = "cpsBundleCode";
    private static final String FIELD_MODE_OF_TRIAL_REASONS = "modeOfTrialReasons";
    private static final String FIELD_APPLICATION_TYPES = "courtApplicationTypes";
    public static final String QUERY_PARAM_OU_COURTROOM_NAME = "ouCourtRoomName";
    private static final String ENFORCEMENT_AREA_QUERY_NAME = "referencedata.query.enforcement-area";
    private static final String COURT_CODE_QUERY_PARAMETER = "localJusticeAreaNationalCourtCode";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Override
    public JsonObjectBuilder getCourtName(final JsonEnvelope envelope) {

        final JsonObject hearing = envelope.payloadAsJsonObject().getJsonObject(NEXT_HEARING);
        final String courtId = hearing.getString(KEY_COURT_ID, null);

        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(envelope.metadata().id())
                .withName(REFERENCEDATA_QUERY_COURT_LOCATIONS);

        envelope.metadata().userId().ifPresent(metadataBuilder::withUserId);

        final JsonEnvelope responseEnvelope = requester.request(envelopeFrom(metadataBuilder, createObjectBuilder()));

        final JsonArray locations = responseEnvelope.payloadAsJsonObject().getJsonArray(OPAMI_COURT_LOCATIONS);

        final Optional<JsonObject> matchedCourtLocation = locations.stream()
                .map(JsonObject.class::cast)
                .filter(location -> location.getString(LOCATION_CODE).equals(courtId))
                .findFirst();

        return buildPayloadFor(matchedCourtLocation);
    }

    private JsonObjectBuilder buildPayloadFor(final Optional<JsonObject> courtLocation) {
        final JsonObjectBuilder courtLocationBuilder = createObjectBuilder();

        courtLocation.ifPresent(jsonObject ->
                courtLocationBuilder
                        .add(KEY_COURT_NAME, jsonObject.getString(OPAMI_LEVEL_1_NAME) + " - " + jsonObject.getString(OPAMI_LEVEL_3_NAME))
        );

        return courtLocationBuilder;
    }


    @Override
    public List<PoliceRankReferenceData> retrievePoliceRanks() {
        return getRefDataStream(REFERENCEDATA_QUERY_POLICE_RANKS, FIELD_POLICE_RANKS, createObjectBuilder()).map(asPoliceRankRefData()).collect(Collectors.toList());
    }

    @Override
    public List<ReferenceDataCountryNationality> retrieveCountryNationality() {
        return getRefDataStream(REFERENCEDATA_QUERY_COUNTRY_NATIONALITIES, FIELD_COUNTRY_NATIONALITIES, createObjectBuilder()).map(asCountryNationalityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<SummonsCodeReferenceData> retrieveSummonsCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_SUMMONS_CODES, FIELD_SUMMONS_CODES, createObjectBuilder()).map(asSummonsCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<AlcoholLevelMethodReferenceData> retrieveAlcoholLevelMethods() {
        return getRefDataStream(REFERENCEDATA_QUERY_ALCOHOL_LEVEL_METHODS, FIELD_ALCOHOL_LEVEL_METHODS, createObjectBuilder()).map(asAlcoholLevelMethodRefData()).collect(Collectors.toList());
    }

    @Override
    public List<CustodyStatusReferenceData> retrieveCustodyStatuses() {
        return getRefDataStream(REFERENCEDATA_QUERY_CUSTODY_STATUS, FIELD_CUSTODY_STATUSES, createObjectBuilder()).map(asCustodyStatusRefData()).collect(Collectors.toList());
    }

    @Override
    public List<BailStatusReferenceData> retrieveBailStatuses() {
        return getRefDataStream(REFERENCEDATA_QUERY_BAIL_STATUS, FIELD_BAIL_STATUSES, createObjectBuilder()).map(asBailStatusReferenceData()).collect(Collectors.toList());
    }

    @Override
    public List<OffenceDateCodeReferenceData> retrieveOffenceDateCodes() {
        return getRefDataStream(REFERENCE_DATA_QUERY_OFFENCE_DATE_CODE, FIELD_OFFENCE_DATE_CODES, createObjectBuilder()).map(asOffenceDateCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<OffenderCodeReferenceData> retrieveOffenderCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_OFFENDER_CODES, FIELD_OFFENDER_CODES, createObjectBuilder()).map(asOffenderCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<SelfdefinedEthnicityReferenceData> retrieveSelfDefinedEthnicity() {
        return getRefDataStream(REFERENCEDATA_QUERY_SELF_DEFINED_ETHNICITY, FIELD_SELF_ETHNICITIES, createObjectBuilder()).map(asSelfDefinedEnthnicityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<ObservedEthnicityReferenceData> retrieveObservedEthnicity() {
        return getRefDataStream(REFERENCE_DATA_QUERY_OBSERVED_ETHNICITY, FIELD_OBSERVED_ETHNICITIES, createObjectBuilder()).map(asObservedEnthnicityRefData()).collect(Collectors.toList());
    }

    @Override
    public List<LicenceCodeReferenceData> retrieveLicenceCode() {
        return getRefDataStream(REFERENCEDATA_QUERY_LICENCE_CODE, FIELD_LICENCE_CODES, createObjectBuilder()).map(asLicenceCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<PoliceForceReferenceData> retrievePoliceForceCode() {
        return getRefDataStream(REFERENCEDATA_QUERY_POLICE_FORCE, FIELD_POLICE_FORCES, createObjectBuilder()).map(asPoliceForceRefData()).collect(Collectors.toList());
    }

    @Override
    public List<VehicleCodeReferenceData> retrieveVehicleCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_VEHICLE_CODE, FIELD_VEHICLE_CODES, createObjectBuilder()).map(asVehicleCodeRefData()).collect(Collectors.toList());
    }

    @Override
    public List<ModeOfTrialReasonsReferenceData> retrieveModeOfTrialReasons() {
        return getRefDataStream(REFERENCEDATA_QUERY_MODE_OF_TRIAL_REASONS, FIELD_MODE_OF_TRIAL_REASONS, createObjectBuilder()).map(asModeOfTrialReasonsRefData()).collect(Collectors.toList());
    }

    @Override
    public List<CourtApplicationType> retrieveApplicationTypes() {
        return getRefDataStream(REFERENCEDATA_QUERY_APPLICATION_TYPES, FIELD_APPLICATION_TYPES, createObjectBuilder()).map(asApplicationTypeRefData()).collect(Collectors.toList());
    }

    @Override
    public CourtApplicationType getApplicationType(final UUID applicationId) {
        final JsonObject jsonObject = getRefData(REFERENCEDATA_QUERY_APPLICATION_TYPE, createObjectBuilder().add("id", applicationId.toString()));
        return asApplicationTypeRefData().apply(jsonObject);
    }

    @Override
    public Optional<OrganisationUnitWithCourtroomReferenceData> retrieveCourtCentreDetails(final String courtName) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_OU_COURTROOM_BY_NAME), createObjectBuilder().add(QUERY_PARAM_OU_COURTROOM_NAME, courtName));
        final JsonValue response = requester.requestAsAdmin(envelope, JsonObject.class).payload();
        OrganisationUnitWithCourtroomReferenceData organisationUnitWithCourtroomReferenceData = null;
        if (response != null) {
            organisationUnitWithCourtroomReferenceData = asOrganisationUnitWithCourtroomRefData().apply(response);
        }
        return ofNullable(organisationUnitWithCourtroomReferenceData);
    }

    @Override
    public List<MojOffences> retrieveOffencesByType(final String type) {
        return getRefDataStream(REFERENCEDATA_QUERY_MOJ_OFFENCES, MOJ_OFFENCES, createObjectBuilder().add(FIELD_OFFENCE_TYPE, type)).map(asMojOffencesRefData()).collect(Collectors.toList());
    }

    @Override
    public Optional<OrganisationUnitWithCourtroomReferenceData> retrieveOrganisationUnitWithCourtroom(final String ouCode) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_COURTROOMS_UNITS), createObjectBuilder().add(OU_COURTROOM_CODE, ouCode));
        final JsonValue response = requester.requestAsAdmin(envelope, JsonObject.class).payload();
        OrganisationUnitWithCourtroomReferenceData organisationUnitWithCourtroomReferenceData = null;
        if (response != null) {
            organisationUnitWithCourtroomReferenceData = asOrganisationUnitWithCourtroomRefData().apply(response);
        }
        return ofNullable(organisationUnitWithCourtroomReferenceData);
    }

    @Override
    public List<DocumentTypeAccessReferenceData> retrieveDocumentsTypeAccess() {
        return getRefDataStream(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS, FIELD_DOCUMENTS_TYPE_ACCESS, createObjectBuilder().add("date", LocalDate.now().toString())).map(asDocumentsMetadataRefData()).collect(Collectors.toList());
    }

    @Override
    public List<OrganisationUnitReferenceData> retrieveOrganisationUnits(final String ouCode) {
        return getRefDataStream(REFERENCEDATA_QUERY_ORGANISATION_UNIT, ORGANISATION_UNITS, createObjectBuilder().add(SPIOUCODE, ouCode))
                .map(asOrganisationUnitRefData()).toList();
    }

    @Override
    public List<OrganisationUnitReferenceData> retrieveOrganisationUnitsByOuCode(final String ouCode) {
        return getRefDataStream(REFERENCEDATA_QUERY_ORGANISATION_UNIT, ORGANISATION_UNITS, createObjectBuilder().add(OUCODE, ouCode))
                .map(asOrganisationUnitRefData()).toList();
    }

    @Override
    public boolean isInitiationCodeValid(final String initiationCode) {
        return getRefDataStream(REFERENCEDATA_QUERY_INITIATION_TYPES, FIELD_INITIATION_TYPES, createObjectBuilder())
                .map(initiationType -> (JsonObject) initiationType)
                .anyMatch(initiationType -> initiationType.getString("code").equals(initiationCode));
    }

    @Override
    public List<String> getInitiationCodes() {
        return getRefDataStream(REFERENCEDATA_QUERY_INITIATION_TYPES, FIELD_INITIATION_TYPES, createObjectBuilder())
                .map(initiationType -> (JsonObject) initiationType)
                .map(initiationType -> initiationType.getString("code")).collect(Collectors.toList());
    }

    @Override
    public HearingTypes retrieveHearingTypes() {
        return hearingTypes()
                .withHearingtypes(getRefDataStream(REFERENCEDATA_QUERY_HEARING_TYPES, FIELD_HEARING_TYPES, createObjectBuilder()).map(asHearingTypesRefData()).collect(Collectors.toList()))
                .build();
    }

    @Override
    public ProsecutorsReferenceData retrieveProsecutors(final String originatingOrganisation) {

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_PROSECUTORS_BY_OUCODE), createObjectBuilder().add(OUCODE, originatingOrganisation));
        final JsonValue response = requester.requestAsAdmin(envelope, JsonObject.class).payload();
        ProsecutorsReferenceData prosecutorsReferenceData = null;
        if (null != response) {
            prosecutorsReferenceData = asProsecutorRefData().apply(response);
        }

        return prosecutorsReferenceData;
    }

    @Override
    public List<CaseMarker> getCaseMarkerDetails() {
        return getRefDataStream(REFERENCE_DATA_QUERY_CASE_MARKERS, FIELD_CASE_MARKERS, createObjectBuilder()
                .add(REFERENCE_DATA_QUERY_CATEGORIES_TEAMS, REFERENCE_DATA_QUERY_CATEGORIES_TEAM_CC)
                .add(REFERENCE_DATA_QUERY_CATEGORIES_USAGE, REFERENCE_DATA_QUERY_CATEGORIES_CASE_MARKER))
                .map(asCaseMarkerRefData())
                .collect(Collectors.toList());
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceData(final Offence offence, final String initiationCode) {
        LOGGER.info("Requesting {} for initiationCode {} with offence {} ", REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS, initiationCode, offence.getOffenceId());

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add("cjsoffencecodes", offence.getOffenceCode());

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_OFFENCE_ALL_VERSIONS), jsonObjectBuilder);

        final JsonArray response = requester.requestAsAdmin(envelope, JsonObject.class).payload().getJsonArray("offences");

        List<OffenceReferenceData> offenceReferenceDataList = null;
        if (null != response) {
            offenceReferenceDataList =
                    response.stream().map(asOffenceRefData()).collect(Collectors.toList());
        }
        return offenceReferenceDataList;
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceDataList(final List<String> cjsOffenceCodeList, Optional<String> sowRef) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add("cjsoffencecode", String.join(",", cjsOffenceCodeList));
        sowRef.ifPresent(sowRefValue -> jsonObjectBuilder.add("sowRef", sowRefValue));

        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST), jsonObjectBuilder);
        final JsonArray response = requester.requestAsAdmin(envelope, JsonObject.class).payload().getJsonArray("offences");

        List<OffenceReferenceData> offenceReferenceDataList = null;
        if (null != response) {
            offenceReferenceDataList =
                    response.stream().map(asOffenceRefData()).toList();
        }
        return offenceReferenceDataList;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorsByOuCode(final Metadata metadata, final String ouCode) {
        LOGGER.info("Requesting {} for OuCode {}", REFERENCEDATA_QUERY_GET_PROSECUTOR_BY_OUCODE, ouCode);
        final JsonEnvelope prosecutorsQueryEnvelope = envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_GET_PROSECUTOR_BY_OUCODE),
                createObjectBuilder().
                        add(OUCODE, ouCode));

        final JsonValue response = requester.requestAsAdmin(prosecutorsQueryEnvelope, JsonObject.class).payload();

        ProsecutorsReferenceData prosecutor = null;
        if (null != response) {
            prosecutor = asProsecutorRefData().apply(response);
        }

        return prosecutor;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorsByOuCode(final String ouCode){
        final JsonEnvelope request = envelopeFrom(getMetadataBuilder(REFERENCEDATA_QUERY_GET_PROSECUTOR_BY_OUCODE),
                createObjectBuilder().
                        add(OUCODE, ouCode));

        final JsonValue response = requester.requestAsAdmin(request, JsonObject.class).payload();

        ProsecutorsReferenceData prosecutor = null;
        if (null != response) {
            prosecutor = asProsecutorRefData().apply(response);
        }

        return prosecutor;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorById(final UUID id) {
        LOGGER.info("Requesting {} for ProsecutorId {}", REFERENCE_DATA_QUERY_NSP_PROSECUTORS_BY_ID, id);
        final JsonEnvelope request = envelopeFrom(getMetadataBuilder(REFERENCE_DATA_QUERY_NSP_PROSECUTORS_BY_ID),
                createObjectBuilder().add(ID, id.toString()));

        final JsonValue response = requester.requestAsAdmin(request, JsonObject.class).payload();

        ProsecutorsReferenceData prosecutor = null;
        if (null != response) {
            prosecutor = asProsecutorRefData().apply(response);
        }

        return prosecutor;
    }

    @Override
    public DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode) {


        final JsonEnvelope documentTypeAccessBySectionCodeEnvelope = envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_DOCUMENTS_TYPE_ACCESS_BY_SECTION_CODE),
                createObjectBuilder().
                        add("sectionCode", sectionCode).build());

        final JsonValue response = requester.requestAsAdmin(documentTypeAccessBySectionCodeEnvelope, JsonObject.class).payload();

        DocumentTypeAccessReferenceData documentTypeAccessReferenceData = null;
        if (null != response && JsonValue.NULL != response) {
            documentTypeAccessReferenceData = asDocumentsMetadataRefData().apply(response);
        }

        return documentTypeAccessReferenceData;
    }

    @Override
    public ParentBundleSectionReferenceData getParentBundleSectionByCpsBundleCode(final Metadata metadata, final String cpsBundleCode) {
        final JsonEnvelope parentBundleSectionQueryEnvelope = envelopeFrom(metadataFrom(metadata)
                        .withName(REFERENCEDATA_QUERY_PARENT_BUNDLE_SECTION),
                createObjectBuilder().
                        add(FIELD_CPS_BUNDLE_CODE, cpsBundleCode));

        final JsonValue response = requester.requestAsAdmin(parentBundleSectionQueryEnvelope, JsonObject.class).payload();

        ParentBundleSectionReferenceData parentBundleSectionReferenceData = null;
        if (null != response) {
            parentBundleSectionReferenceData = asParentBundleSectionRefData().apply(response);
        }

        return parentBundleSectionReferenceData;
    }

    @Override
    public Optional<LjaDetails> getLjaDetails(final String ljaCode, final String courtCentreId) {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withName(ENFORCEMENT_AREA_QUERY_NAME)
                        .withId(randomUUID())
                        .build(),
                createObjectBuilder().add(COURT_CODE_QUERY_PARAMETER, ljaCode).build());

        final Envelope<EnforcementArea> responseEnvelope = requester.requestAsAdmin(requestEnvelope, EnforcementArea.class);

        final EnforcementArea enforcementArea = responseEnvelope.payload();

        if (isNull(enforcementArea)) {
            return empty();
        }

        return of(LjaDetails.ljaDetails()
                .withLjaCode(enforcementArea.getLocalJusticeArea().getNationalCourtCode())
                .withLjaName(enforcementArea.getLocalJusticeArea().getName())
                .withWelshLjaName(enforcementArea.getLocalJusticeArea().getWelshName())
                .build());
    }

    private Stream<JsonValue> getRefDataStream(final String queryName, final String fieldName, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(queryName), jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class)
                .payload()
                .getJsonArray(fieldName)
                .stream();
    }

    private JsonObject getRefData(final String queryName, final JsonObjectBuilder jsonObjectBuilder) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(queryName), jsonObjectBuilder);
        return requester.requestAsAdmin(envelope, JsonObject.class)
                .payload();
    }

    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }
}
