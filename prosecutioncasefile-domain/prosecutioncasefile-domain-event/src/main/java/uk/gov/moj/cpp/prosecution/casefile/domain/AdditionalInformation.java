package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;

import java.util.List;
@SuppressWarnings("squid:S2384")
public class AdditionalInformation {

    private List<ProsecutionCase> prosecutionCases;
    private List<CourtApplicationType> applicationTypes;
    private OrganisationUnitWithCourtroomReferenceData courtroomReferenceData;

    public AdditionalInformation(List<ProsecutionCase> prosecutionCases, List<CourtApplicationType> applicationTypes, OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        this.prosecutionCases = prosecutionCases;
        this.applicationTypes = applicationTypes;
        this.courtroomReferenceData = courtroomReferenceData;
    }

    public List<ProsecutionCase> getProsecutionCases() {
        return prosecutionCases;
    }

    public void setProsecutionCases(List<ProsecutionCase> prosecutionCases) {
        this.prosecutionCases = prosecutionCases;
    }

    public List<CourtApplicationType> getApplicationTypes() {
        return applicationTypes;
    }

    public void setApplicationTypes(List<CourtApplicationType> applicationTypes) {
        this.applicationTypes = applicationTypes;
    }

    public OrganisationUnitWithCourtroomReferenceData getCourtroomReferenceData() {
        return courtroomReferenceData;
    }

    public void setCourtroomReferenceData(OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        this.courtroomReferenceData = courtroomReferenceData;
    }
}
