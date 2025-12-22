package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "defendant_legacy")
public class DefendantLegacy implements Serializable {

    private static final long serialVersionUID = 97305852963115611L;

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @Column(name = "suspect_id", nullable = false)
    private UUID suspectId;

    @Column(name = "police_defendant_id")
    private String policeDefendantId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "case_urn", nullable = false)
    private String caseUrn;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant")
    @OrderBy("orderIndex ASC")
    private Set<OffenceLegacy> offences = new HashSet<>();

    @Embedded
    private Hearing hearing;

    public DefendantLegacy(UUID defendantId, UUID suspectId, String policeDefendantId, UUID personId, UUID caseId, String caseUrn, Set<OffenceLegacy> offences) {
        super();
        this.defendantId = defendantId;
        this.suspectId = suspectId;
        this.policeDefendantId = policeDefendantId;
        this.personId = personId;
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        setOffences(offences);
    }

    public DefendantLegacy() {
        super();
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    @SuppressWarnings("squid:S2384")
    public Set<OffenceLegacy> getOffences() {
        return offences;
    }

    public void setOffences(Set<OffenceLegacy> offences) {
        this.offences = offences == null ? emptySet() : new HashSet<>(offences);
        this.offences.forEach(offence -> offence.setDefendant(this));
    }

    public void addOffences(final Set<OffenceLegacy> newOffences) {
        if (newOffences != null && !newOffences.isEmpty()) {
            newOffences.forEach(offence -> offence.setDefendant(this));
            this.offences.addAll(newOffences);
        }
    }

    public void addOffence(OffenceLegacy offence) {
        Objects.requireNonNull(offence);
        offences.add(offence);
        offence.setDefendant(this);
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public void setPoliceDefendantId(String policeDefendantId) {
        this.policeDefendantId = policeDefendantId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public void setCaseUrn(String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setHearing(Hearing hearing) {
        this.hearing = hearing;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public UUID getSuspectId() {
        return suspectId;
    }

    public void setSuspectId(UUID suspectId) {
        this.suspectId = suspectId;
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

