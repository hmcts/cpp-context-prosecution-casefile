package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event(CaseEjected.EVENT_NAME)
public class CaseEjected {

    public static final String EVENT_NAME = "prosecutioncasefile.events.case-ejected";

    private final UUID caseId;

    @JsonCreator
    public CaseEjected(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
