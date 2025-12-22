package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event(DefendantIdpcAdded.EVENT_NAME)
public class DefendantIdpcAdded {

    public static final String EVENT_NAME = "prosecutioncasefile.events.defendant-idpc-added";

    private final UUID caseId;

    private final CaseDocument caseDocument;

    private final UUID defendantId;

    @JsonCreator
    public DefendantIdpcAdded(final UUID caseId, final CaseDocument caseDocument, final UUID defendantId) {
        this.caseId = caseId;
        this.caseDocument = caseDocument;
        this.defendantId = defendantId;
    }

    public CaseDocument getCaseDocument() {
        return caseDocument;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

}
