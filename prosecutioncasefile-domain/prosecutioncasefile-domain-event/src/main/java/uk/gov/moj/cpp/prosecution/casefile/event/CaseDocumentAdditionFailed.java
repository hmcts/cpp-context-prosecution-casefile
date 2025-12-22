package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.case-document-addition-failed")
public class CaseDocumentAdditionFailed {

    private final UUID documentId;
    private final String description;

    @JsonCreator
    public CaseDocumentAdditionFailed(UUID documentId, String description) {
        this.documentId = documentId;
        this.description = description;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getDescription() {
        return description;
    }
}
