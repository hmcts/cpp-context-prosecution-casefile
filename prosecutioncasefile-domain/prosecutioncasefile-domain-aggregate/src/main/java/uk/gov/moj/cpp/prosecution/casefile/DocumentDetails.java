package uk.gov.moj.cpp.prosecution.casefile;

public class DocumentDetails {

    private final String cmsDocumentId;
    private final  Integer materialType;
    private final String sectionCode;
    public DocumentDetails(final String cmsDocumentId, final  Integer materialType, final String sectionCode) {
        this.cmsDocumentId = cmsDocumentId;
        this.materialType = materialType;
        this.sectionCode = sectionCode;
    }

    public String getCmsDocumentId() {
        return cmsDocumentId;
    }

    public Integer getMaterialType() {
        return materialType;
    }

    public String getSectionCode() {
        return sectionCode;
    }
}
