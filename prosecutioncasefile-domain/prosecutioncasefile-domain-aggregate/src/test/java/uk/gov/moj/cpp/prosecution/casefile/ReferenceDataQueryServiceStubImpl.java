package uk.gov.moj.cpp.prosecution.casefile;


import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;

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
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

public class ReferenceDataQueryServiceStubImpl implements ReferenceDataQueryService {
    @Override
    public JsonObjectBuilder getCourtName(JsonEnvelope envelope) {
        return null;
    }

    @Override
    public List<PoliceRankReferenceData> retrievePoliceRanks() {
        return null;
    }

    @Override
    public List<ReferenceDataCountryNationality> retrieveCountryNationality() {
        return new ArrayList<>();
    }

    @Override
    public List<SummonsCodeReferenceData> retrieveSummonsCodes() {
        return null;
    }

    @Override
    public List<DocumentTypeAccessReferenceData> retrieveDocumentsTypeAccess() {
        return asList(documentTypeAccessReferenceData()
                        .withId(UUID.fromString("3f88a08-cc44-435b-8037-3fb5ec8f74e3"))
                        .withSection("SJPN")
                        .withDocumentCategory("Defendant level")
                        .build(),
                documentTypeAccessReferenceData()
                        .withId(UUID.fromString("3f88a08-cc44-435b-8037-3fb5ec8f74e3"))
                        .withSection("CITN")
                        .withDocumentCategory("Defendant level")
                        .build());
    }

    @Override
    public List<AlcoholLevelMethodReferenceData> retrieveAlcoholLevelMethods() {
        return null;
    }

    @Override
    public List<CustodyStatusReferenceData> retrieveCustodyStatuses() {
        return null;
    }

    @Override
    public List<BailStatusReferenceData> retrieveBailStatuses() {
        return null;
    }

    @Override
    public List<OffenceDateCodeReferenceData> retrieveOffenceDateCodes() {
        return null;
    }

    @Override
    public List<OffenderCodeReferenceData> retrieveOffenderCodes() {
        return null;
    }

    @Override
    public List<SelfdefinedEthnicityReferenceData> retrieveSelfDefinedEthnicity() {
        return null;
    }

    @Override
    public List<ObservedEthnicityReferenceData> retrieveObservedEthnicity() {
        return null;
    }

    @Override
    public List<LicenceCodeReferenceData> retrieveLicenceCode() {
        return null;
    }

    @Override
    public List<VehicleCodeReferenceData> retrieveVehicleCodes() {
        return null;
    }

    @Override
    public Optional<OrganisationUnitWithCourtroomReferenceData> retrieveOrganisationUnitWithCourtroom(String ouCode) {
        return empty();
    }

    @Override
    public List<OrganisationUnitReferenceData> retrieveOrganisationUnits(String ouCode) {
        return null;
    }

    @Override
    public List<OrganisationUnitReferenceData> retrieveOrganisationUnitsByOuCode(String ouCode) {
        return null;
    }

    @Override
    public boolean isInitiationCodeValid(String initiationCode) {
        return false;
    }

    @Override
    public List<String> getInitiationCodes() {
        return null;
    }

    @Override
    public HearingTypes retrieveHearingTypes() {
        return null;
    }

    @Override
    public ProsecutorsReferenceData retrieveProsecutors(String originatingOrganisation) {
        return null;
    }

    @Override
    public List<CaseMarker> getCaseMarkerDetails() {
        return null;
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceData(Offence offence, String summonsCode) {
        final List<OffenceReferenceData> offenceReferenceDataList = new ArrayList<>();
        offenceReferenceDataList.add(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("OFCODE12").build());
        return offenceReferenceDataList;
    }

    @Override
    public List<OffenceReferenceData> retrieveOffenceDataList(final List<String> cjsOffenceCodeList, Optional<String> sowRef) {
        return null;
    }

    @Override
    public List<PoliceForceReferenceData> retrievePoliceForceCode() {
        return null;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorsByOuCode(Metadata metadata, String ouCode) {
        return null;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorsByOuCode(final String ouCode) {
        return null;
    }

    @Override
    public ProsecutorsReferenceData getProsecutorById(final UUID id) {
        return null;
    }

    @Override
    public ParentBundleSectionReferenceData getParentBundleSectionByCpsBundleCode(final Metadata metadata, final String cpsBundleCode) {
        return null;
    }

    @Override
    public DocumentTypeAccessReferenceData getDocumentTypeAccessBySectionCode(final Metadata metadata, final String sectionCode) {
        return null;
    }

    @Override
    public List<ModeOfTrialReasonsReferenceData> retrieveModeOfTrialReasons() { return null; }

    @Override
    public List<CourtApplicationType> retrieveApplicationTypes() {
        return null;
    }

    @Override
    public List<MojOffences> retrieveOffencesByType(final String type) {
        return null;
    }

    @Override
    public Optional<LjaDetails> getLjaDetails(final String lja, final String id) {
        return empty();
    }

    @Override
    public CourtApplicationType getApplicationType(UUID applicationId) {
        return null;
    }

    @Override
    public  Optional<OrganisationUnitWithCourtroomReferenceData> retrieveCourtCentreDetails(String courtName) {
        return empty();
    }

}
