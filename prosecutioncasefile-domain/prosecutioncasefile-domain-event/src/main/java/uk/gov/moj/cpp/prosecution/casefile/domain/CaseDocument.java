package uk.gov.moj.cpp.prosecution.casefile.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final UUID materialId;

    private String documentName;

    private final String documentType;

    private final ZonedDateTime addedAt;

    public CaseDocument(UUID id, UUID materialId, String documentType, ZonedDateTime addedAt) {
        this.id = id;
        this.materialId = materialId;
        this.documentType = documentType;
        this.addedAt = addedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public ZonedDateTime getAddedAt() {
        return addedAt;
    }

}