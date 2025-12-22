package uk.gov.moj.cpp.prosecution.casefile.state;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;
import uk.gov.moj.cpp.prosecution.casefile.handler.DocumentCountByDocumentType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the case aggregate state.
 */
public class CaseAggregateState implements AggregateState {

    private UUID caseId;
    private String urn;
    private final Map<UUID, CaseDocument> caseDocuments = new HashMap<>();


    private DocumentCountByDocumentType documentCountByDocumentType = new DocumentCountByDocumentType();

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(final String urn) {
        this.urn = urn;
    }


    public Map<UUID, CaseDocument> getCaseDocuments() {
        return caseDocuments;
    }

    public DocumentCountByDocumentType getDocumentCountByDocumentType() {
        return documentCountByDocumentType;
    }
}
