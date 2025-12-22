package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;


import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CaseDetailsView {

    private final UUID caseId;
    private final String prosecutionCaseReference;
    private final String originatingOrganisation;
    private final String prosecutorInformant;
    private final String prosecutionAuthority;
    private final List<DefendantView> defendants;

    public CaseDetailsView(final CaseDetails caseDetails) {
        this.caseId = caseDetails.getCaseId();
        this.prosecutionCaseReference = caseDetails.getProsecutionCaseReference();
        this.prosecutorInformant = caseDetails.getProsecutorInformant();
        this.prosecutionAuthority = caseDetails.getProsecutionAuthority();
        this.originatingOrganisation = caseDetails.getOriginatingOrganisation();
        this.defendants = caseDetails.getDefendants().stream()
                .map(DefendantView::new)
                .collect(Collectors.toList());
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getProsecutionCaseReference() {
        return prosecutionCaseReference;
    }

    public String getProsecutorInformant() {
        return prosecutorInformant;
    }

    public String getProsecutionAuthority() {
        return prosecutionAuthority;
    }

    public String getOriginatingOrganisation() {
        return originatingOrganisation;
    }

    @SuppressWarnings("squid:S2384")
    public List<DefendantView> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
