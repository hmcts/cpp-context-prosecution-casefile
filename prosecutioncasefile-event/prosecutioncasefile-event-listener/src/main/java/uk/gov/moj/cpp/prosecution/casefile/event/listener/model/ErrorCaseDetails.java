package uk.gov.moj.cpp.prosecution.casefile.event.listener.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ErrorCaseDetails implements Serializable {

    private UUID caseId;

    private List<ErrorDefendantDetails> defendants;

    public ErrorCaseDetails(final UUID caseId, final List<ErrorDefendantDetails> defendants) {
        this.caseId = caseId;
        this.defendants = new ArrayList<>(defendants);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<ErrorDefendantDetails> getDefendants() {
        return new ArrayList<>(defendants);
    }

    public void addDefendant(ErrorDefendantDetails defendantDetails) {
        if (defendants.stream().noneMatch(d -> d.getId().equals(defendantDetails.getId()))) {
            defendants.add(defendantDetails);
        }
    }

    public void addAllDefendant(List<ErrorDefendantDetails> defendantDetails) {
        defendantDetails.forEach(this::addDefendant);
    }
}
