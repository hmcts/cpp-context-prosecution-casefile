package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.sjp-case-assigned")
public class SjpCaseAssigned {

    private UUID caseId;

    @JsonCreator
    public SjpCaseAssigned(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
