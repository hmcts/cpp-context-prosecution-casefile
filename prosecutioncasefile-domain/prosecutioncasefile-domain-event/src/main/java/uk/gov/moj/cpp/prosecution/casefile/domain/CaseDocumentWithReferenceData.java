package uk.gov.moj.cpp.prosecution.casefile.domain;

import java.util.HashMap;
import java.util.Map;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtApplicationSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S2384")
public class CaseDocumentWithReferenceData {

    private String prosecutorDefendantId;

    private String documentType;

    private UUID defendantId;

    private List<Defendant> defendants;

    private DocumentTypeAccessReferenceData documentTypeAccessReferenceData;

    private Material material;

    private boolean caseReferredToCourt;

    private UUID referralReasonId;

    private boolean caseAssigned;

    private boolean caseEjected;

    private CourtApplicationSubject courtApplicationSubject;

    private ProsecutionCaseSubject prosecutionCaseSubject;

    private boolean hasApplication;

    private String documentCategory;

    private String materialType;

    private String materialContentType;

    private Map<String, UUID> validDefendantIds = new HashMap<>();

    private Boolean isHeaderOuCodeCPS = null;

    public CaseDocumentWithReferenceData(final UUID referralReasonId, final boolean caseReferredToCourt,
                                         final Material material, final String prosecutorDefendantId, final List<Defendant> defendants, final String documentType, boolean caseAssigned, boolean caseEjected) {
        this.prosecutorDefendantId = prosecutorDefendantId;
        this.documentType = documentType;
        this.defendants = defendants;
        this.material = material;
        this.caseReferredToCourt = caseReferredToCourt;
        this.referralReasonId = referralReasonId;
        this.caseAssigned = caseAssigned;
        this.caseEjected = caseEjected;
    }

    public CaseDocumentWithReferenceData(final UUID referralReasonId, final boolean caseReferredToCourt, final List<Defendant> defendants, final String documentType, boolean caseAssigned, boolean caseEjected, final CourtApplicationSubject courtApplicationSubject, final ProsecutionCaseSubject prosecutionCaseSubject, String materialType, String materialContentType, Map<String, UUID> validDefendantIds) {
        this.documentType = documentType;
        this.defendants = defendants;
        this.caseReferredToCourt = caseReferredToCourt;
        this.referralReasonId = referralReasonId;
        this.caseAssigned = caseAssigned;
        this.caseEjected = caseEjected;
        this.courtApplicationSubject = courtApplicationSubject;
        this.prosecutionCaseSubject = prosecutionCaseSubject;
        this.materialType = materialType;
        this.materialContentType = materialContentType;
        this.validDefendantIds = validDefendantIds;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public String getProsecutorDefendantId() {
        return prosecutorDefendantId;
    }

    public List<Defendant> getDefendants() {
        if (defendants == null) {
            defendants = new ArrayList<>();
        }
        return defendants;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public DocumentTypeAccessReferenceData getDocumentTypeAccessReferenceData() {
        return documentTypeAccessReferenceData;
    }

    public void setDocumentTypeAccessReferenceData(final DocumentTypeAccessReferenceData documentTypeAccessReferenceData) {
        this.documentTypeAccessReferenceData = documentTypeAccessReferenceData;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean isCaseReferredToCourt() {
        return caseReferredToCourt;
    }

    public UUID getReferralReasonId() {
        return this.referralReasonId;
    }

    public boolean isCaseAssigned() {
        return caseAssigned;
    }

    public boolean isCaseEjected() {
        return caseEjected;
    }

    public CourtApplicationSubject getCourtApplicationSubject() {
        return courtApplicationSubject;
    }

    public void setCourtApplicationSubject(final CourtApplicationSubject courtApplicationSubject) {
        this.courtApplicationSubject = courtApplicationSubject;
    }

    public ProsecutionCaseSubject getProsecutionCaseSubject() {
        return prosecutionCaseSubject;
    }

    public void setProsecutionCaseSubject(final ProsecutionCaseSubject prosecutionCaseSubject) {
        this.prosecutionCaseSubject = prosecutionCaseSubject;
    }

    public boolean isHasApplication() {
        return hasApplication;
    }

    public void setHasApplication(final boolean hasApplication) {
        this.hasApplication = hasApplication;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(final String materialType) {
        this.materialType = materialType;
    }

    public String getMaterialContentType() {
        return materialContentType;
    }

    public void setMaterialContentType(final String materialContentType) {
        this.materialContentType = materialContentType;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public void setDocumentCategory(final String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public Map<String, UUID> getValidDefendantIds() {
        return validDefendantIds;
    }

    public Boolean isHeaderOuCodeCPS() {
        return isHeaderOuCodeCPS;
    }

    public void setHeaderOuCodeCPS(final Boolean headerOuCodeCPS) {
        isHeaderOuCodeCPS = headerOuCodeCPS;
    }
}