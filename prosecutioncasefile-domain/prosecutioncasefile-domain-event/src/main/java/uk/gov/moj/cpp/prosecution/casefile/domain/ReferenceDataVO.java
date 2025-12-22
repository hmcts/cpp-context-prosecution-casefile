package uk.gov.moj.cpp.prosecution.casefile.domain;

import static java.util.Optional.empty;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ObservedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenderCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceForceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfdefinedEthnicityReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleCodeReferenceData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("squid:S2384")
public class ReferenceDataVO {

    private final List<ReferenceDataCountryNationality> countryNationalityReferenceData = new ArrayList<>();
    private final List<SelfdefinedEthnicityReferenceData> selfdefinedEthnicityReferenceData = new ArrayList<>();
    private final List<OrganisationUnitReferenceData> organisationUnitReferenceData = new ArrayList<>();
    private final List<String> initiationTypes = new ArrayList<>();
    private List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
    private List<CaseMarker> caseMarkers = new ArrayList<>();
    private List<ObservedEthnicityReferenceData> observedEthnicityReferenceData = new ArrayList<>();
    private List<VehicleCodeReferenceData> vehicleCodesReferenceData = new ArrayList<>();
    private Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData = empty();
    private List<AlcoholLevelMethodReferenceData> alcoholLevelMethodReferenceData;
    private List<OffenderCodeReferenceData> offenderCodeReferenceData = new ArrayList<>();
    private List<BailStatusReferenceData> bailStatusReferenceData = new ArrayList<>();
    private ProsecutorsReferenceData prosecutorsReferenceData;
    private List<SummonsCodeReferenceData> summonsCodeReferenceData;
    private List<PoliceForceReferenceData> policeForceReferenceData;
    private HearingType hearingType;
    private List<ModeOfTrialReasonsReferenceData> modeOfTrialReasonsReferenceData = new ArrayList<>();

    public ProsecutorsReferenceData getProsecutorsReferenceData() {
        return prosecutorsReferenceData;
    }

    public void setProsecutorsReferenceData(final ProsecutorsReferenceData prosecutorsReferenceData) {
        this.prosecutorsReferenceData = prosecutorsReferenceData;
    }

    public List<OffenceReferenceData> getOffenceReferenceData() {
        return offenceReferenceData;
    }

    public void setOffenceReferenceData(final List<OffenceReferenceData> offenceReferenceData) {
        this.offenceReferenceData = offenceReferenceData;
    }

    public void addOffenceReferenceData(final OffenceReferenceData referenceDataOffence) {
        offenceReferenceData.add(referenceDataOffence);
    }

    public List<SummonsCodeReferenceData> getSummonsCodeReferenceData() {
        return summonsCodeReferenceData;
    }

    public void setSummonsCodeReferenceData(final List<SummonsCodeReferenceData> summonsCodeReferenceData) {
        this.summonsCodeReferenceData = summonsCodeReferenceData;
    }

    public List<AlcoholLevelMethodReferenceData> getAlcoholLevelMethodReferenceData() {
        return alcoholLevelMethodReferenceData;
    }

    public void setAlcoholLevelMethodReferenceData(final List<AlcoholLevelMethodReferenceData> alcoholLevelMethodReferenceData) {
        this.alcoholLevelMethodReferenceData = alcoholLevelMethodReferenceData;
    }

    public List<CaseMarker> getCaseMarkers() {
        return caseMarkers;
    }

    public void setCaseMarkers(final List<CaseMarker> caseMarkers) {
        this.caseMarkers = caseMarkers;
    }

    public List<ObservedEthnicityReferenceData> getObservedEthnicityReferenceData() {
        return observedEthnicityReferenceData;
    }

    public void setObservedEthnicityReferenceData(final List<ObservedEthnicityReferenceData> observedEthnicityReferenceData) {
        this.observedEthnicityReferenceData = observedEthnicityReferenceData;
    }

    public List<SelfdefinedEthnicityReferenceData> getSelfdefinedEthnicityReferenceData() {
        return selfdefinedEthnicityReferenceData;
    }

    public void setSelfdefinedEthnicityReferenceData(final List<SelfdefinedEthnicityReferenceData> selfdefinedEthnicityReferenceData) {
        this.selfdefinedEthnicityReferenceData.addAll(selfdefinedEthnicityReferenceData);
    }

    public List<BailStatusReferenceData> getBailStatusReferenceData() {
        return this.bailStatusReferenceData;
    }

    public void setBailStatusReferenceData(final List<BailStatusReferenceData> bailStatusReferenceData) {
        this.bailStatusReferenceData = bailStatusReferenceData;
    }

    public void addBailStatusReferenceData(final BailStatusReferenceData bailStatusReferenceData) {
        this.bailStatusReferenceData.add(bailStatusReferenceData);
    }

    public List<VehicleCodeReferenceData> getVehicleCodesReferenceData() {
        return vehicleCodesReferenceData;
    }

    public void setVehicleCodesReferenceData(final List<VehicleCodeReferenceData> vehicleCodesReferenceData) {
        this.vehicleCodesReferenceData = vehicleCodesReferenceData;
    }

    public List<ReferenceDataCountryNationality> getCountryNationalityReferenceData() {
        return countryNationalityReferenceData;
    }

    public Optional<OrganisationUnitWithCourtroomReferenceData> getOrganisationUnitWithCourtroomReferenceData() {
        return organisationUnitWithCourtroomReferenceData;
    }

    public void setOrganisationUnitWithCourtroomReferenceData(final Optional<OrganisationUnitWithCourtroomReferenceData> organisationUnitWithCourtroomReferenceData) {
        this.organisationUnitWithCourtroomReferenceData = organisationUnitWithCourtroomReferenceData;
    }

    public List<OrganisationUnitReferenceData> getOrganisationUnitReferenceData() {
        return organisationUnitReferenceData;
    }

    public void addOrganisationUnitReferenceData(final OrganisationUnitReferenceData organisationUnitReferenceData) {
        this.organisationUnitReferenceData.add(organisationUnitReferenceData);
    }

    public HearingType getHearingType() {
        return hearingType;
    }

    public void setHearingType(final HearingType hearingType) {
        this.hearingType = hearingType;
    }

    public void addCountryNationalityReferenceData(final ReferenceDataCountryNationality referenceDataCountryNationality) {
        countryNationalityReferenceData.add(referenceDataCountryNationality);
    }

    public List<OffenderCodeReferenceData> getOffenderCodeReferenceData() {
        return offenderCodeReferenceData;
    }

    public void setOffenderCodeReferenceData(final List<OffenderCodeReferenceData> offenderCodeReferenceData) {
        this.offenderCodeReferenceData = offenderCodeReferenceData;
    }

    public List<String> getInitiationTypes() {
        return initiationTypes;
    }

    public void setInitiationTypes(List<String> initiationTypes) {
        this.initiationTypes.addAll(initiationTypes);
    }

    public List<PoliceForceReferenceData> getPoliceForceReferenceData() {
        return policeForceReferenceData;
    }

    public void setPoliceForceReferenceData(List<PoliceForceReferenceData> policeForceReferenceData) {
        this.policeForceReferenceData = policeForceReferenceData;
    }

    public List<ModeOfTrialReasonsReferenceData> getModeOfTrialReasonsReferenceData() {
        return modeOfTrialReasonsReferenceData;
    }

    public void setModeOfTrialReferenceData(final List<ModeOfTrialReasonsReferenceData> modeOfTrialReasonsReferenceData) {
        this.modeOfTrialReasonsReferenceData = modeOfTrialReasonsReferenceData;
    }
}
