package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "case_details")
public class CaseDetails implements Serializable {

    @Id
    @Column(name = "case_id", unique = true, nullable = false)
    private UUID caseId;

    @Column(name = "prosecution_case_reference")
    private String prosecutionCaseReference;

    @Column(name = "prosecutor_informant")
    private String prosecutorInformant;

    @Column(name = "prosecution_authority")
    private String prosecutionAuthority;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id")
    private Set<DefendantDetails> defendants = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private Set<CivilFees> civilFees;

    @Column(name = "originating_organisation")
    private String originatingOrganisation;

    public CaseDetails() {

    }

    public CaseDetails(final UUID caseId,
                       final String prosecutionCaseReference,
                       final String prosecutorInformant,
                       final String prosecutionAuthority,
                       final String originatingOrganisation,
                       final Set<DefendantDetails> defendants,
                       final Set<CivilFees> civilFees) {
        this.caseId = caseId;
        this.prosecutionCaseReference = prosecutionCaseReference;
        this.prosecutorInformant = prosecutorInformant;
        this.prosecutionAuthority = prosecutionAuthority;
        this.originatingOrganisation = originatingOrganisation;
        this.defendants = unmodifiableSet(defendants);
        this.civilFees = civilFees;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getProsecutionCaseReference() {
        return prosecutionCaseReference;
    }

    public void setProsecutionCaseReference(String prosecutionCaseReference) {
        this.prosecutionCaseReference = prosecutionCaseReference;
    }

    public String getProsecutorInformant() {
        return prosecutorInformant;
    }

    public void setProsecutorInformant(String prosecutorInformant) {
        this.prosecutorInformant = prosecutorInformant;
    }

    public String getProsecutionAuthority() {
        return prosecutionAuthority;
    }

    public void setProsecutionAuthority(String prosecutionAuthority) {
        this.prosecutionAuthority = prosecutionAuthority;
    }

    public String getOriginatingOrganisation() {
        return originatingOrganisation;
    }

    public void setOriginatingOrganisation(final String originatingOrganisation) {
        this.originatingOrganisation = originatingOrganisation;
    }

    @SuppressWarnings("squid:S2384")
    public Set<DefendantDetails> getDefendants() {
        return this.defendants;
    }

    public void setDefendants(Set<DefendantDetails> defendants) {
        if (defendants != null) {
            this.defendants.addAll(defendants);
        }
    }

    public Set<CivilFees> getCivilFees() {
        return civilFees;
    }

    public void setCivilFees(final Set<CivilFees> civilFees) {
        this.civilFees = civilFees;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
