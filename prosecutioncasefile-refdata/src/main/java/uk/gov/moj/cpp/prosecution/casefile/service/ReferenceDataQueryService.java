package uk.gov.moj.cpp.prosecution.casefile.service;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS, property = "className")
public interface ReferenceDataQueryService {
    JsonObjectBuilder getCourtName(JsonEnvelope envelope);

    List<PoliceRankReferenceData> retrievePoliceRanks();

    List<ReferenceDataCountryNationality> retrieveCountryNationality();

    List<SummonsCodeReferenceData> retrieveSummonsCodes();

    List<DocumentTypeAccessReferenceData> retrieveDocumentsTypeAccess();

    List<AlcoholLevelMethodReferenceData> retrieveAlcoholLevelMethods();

    List<CustodyStatusReferenceData> retrieveCustodyStatuses();

    List<BailStatusReferenceData> retrieveBailStatuses();

    List<OffenceDateCodeReferenceData> retrieveOffenceDateCodes();

    List<OffenderCodeReferenceData> retrieveOffenderCodes();

    List<SelfdefinedEthnicityReferenceData> retrieveSelfDefinedEthnicity();

    List<ObservedEthnicityReferenceData> retrieveObservedEthnicity();

    List<LicenceCodeReferenceData> retrieveLicenceCode();

    List<VehicleCodeReferenceData> retrieveVehicleCodes();

    Optional<OrganisationUnitWithCourtroomReferenceData> retrieveOrganisationUnitWithCourtroom(String ouCode);

    List<OrganisationUnitReferenceData> retrieveOrganisationUnits(String ouCode);

    List<OrganisationUnitReferenceData> retrieveOrganisationUnitsByOuCode(final String ouCode);

    boolean isInitiationCodeValid(String initiationCode);

    List<String> getInitiationCodes();

    HearingTypes retrieveHearingTypes();

    ProsecutorsReferenceData retrieveProsecutors(String originatingOrganisation);

    List<CaseMarker> getCaseMarkerDetails();

    List<OffenceReferenceData> retrieveOffenceData(Offence offence, String initiationCode);

    List<OffenceReferenceData> retrieveOffenceDataList(List<String> cjsOffenceCodeList, Optional<String> sowRef);

    List<PoliceForceReferenceData> retrievePoliceForceCode();

    ProsecutorsReferenceData getProsecutorsByOuCode(final Metadata metadata, final String ouCode);

    ProsecutorsReferenceData getProsecutorsByOuCode(final String ouCode);

    ProsecutorsReferenceData getProsecutorById(final UUID id);

    ParentBundleSectionReferenceData getParentBundleSectionByCpsBundleCode(final Metadata metadata, final String cpsBundleCode);

    DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode);

    List<ModeOfTrialReasonsReferenceData> retrieveModeOfTrialReasons();

    List<CourtApplicationType> retrieveApplicationTypes();

    CourtApplicationType getApplicationType(final UUID applicationId);

    Optional<OrganisationUnitWithCourtroomReferenceData> retrieveCourtCentreDetails(String courtName);

    List<MojOffences> retrieveOffencesByType(final String type);

    Optional<LjaDetails> getLjaDetails(String lja, String id);
}
