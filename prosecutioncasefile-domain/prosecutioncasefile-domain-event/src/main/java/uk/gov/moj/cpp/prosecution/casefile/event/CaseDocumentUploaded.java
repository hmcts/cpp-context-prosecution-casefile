package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.case-document-uploaded")
public class CaseDocumentUploaded {

    private final UUID caseId;

    private final UUID documentReference;

    private final String documentType;

    @JsonCreator
    public CaseDocumentUploaded(UUID caseId, UUID documentReference, String documentType) {
        this.caseId = caseId;
        this.documentReference = documentReference;
        this.documentType = documentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDocumentReference() {
        return documentReference;
    }

    public String getDocumentType() {
        return documentType;
    }
}
