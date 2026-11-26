package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "defendant")
public class DefendantDetails implements Serializable {

    @Id
    @Column(name = "defendant_id", unique = true, nullable = false)
    private String defendantId;

    @Column(name = "asn")
    private String asn;

    @Column(name = "documentation_language")
    @Enumerated(EnumType.STRING)
    private Language documentationLanguage;

    @Column(name = "hearing_language")
    @Enumerated(EnumType.STRING)
    private Language hearingLanguage;

    @Column(name = "language_requirement")
    private String languageRequirement;

    @Column(name = "specific_requirements")
    private String specificRequirements;

    @Column(name = "number_previous_convictions")
    private Integer numPreviousConvictions;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "driver_number")
    private String driverNumber;

    @Column(name = "national_insurance_number")
    private String nationalInsuranceNumber;

    @Column(name = "prosecutor_defendant_reference")
    private String prosecutorDefendantReference;

    @Column(name = "applied_prosecutor_costs")
    private BigDecimal appliedProsecutorCosts;

    @Column(name = "idpc_material_id")
    private UUID idpcMaterialId;

    @Column(name="defendant_initiation_code")
    private String defendantInitiationCode;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "personal_information_id")
    private PersonalInformationDetails personalInformation;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "self_defined_information_id")
    private SelfDefinedInformationDetails selfDefinedInformation;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant")
    private Set<OffenceDetails> offences;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "defendant_individual_aliases", joinColumns = @JoinColumn(name = "defendant_id"))
    private List<IndividualAliasDetail> individualAliases;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "organisation_information_id")
    private OrganisationInformationDetails organisationInformation;

    public DefendantDetails() {

    }

    @SuppressWarnings("squid:S00107")
    public DefendantDetails(final String defendantId,
                            final String asn,
                            final Language documentationLanguage,
                            final Language hearingLanguage,
                            final String languageRequirement,
                            final String specificRequirements,
                            final Integer numPreviousConvictions,
                            final LocalDate postingDate,
                            final String driverNumber,
                            final String nationalInsuranceNumber,
                            final String prosecutorDefendantReference,
                            final BigDecimal appliedProsecutorCosts,
                            final PersonalInformationDetails personalInformation,
                            final SelfDefinedInformationDetails selfDefinedInformation,
                            final Set<OffenceDetails> offences,
                            final List<IndividualAliasDetail> individualAliases,
                            final OrganisationInformationDetails organisationInformation,
                            final String defendantInitiationCode
    ) {
        this.defendantId = defendantId;
        this.asn = asn;
        this.documentationLanguage = documentationLanguage;
        this.hearingLanguage = hearingLanguage;
        this.languageRequirement = languageRequirement;
        this.specificRequirements = specificRequirements;
        this.numPreviousConvictions = numPreviousConvictions;
        this.postingDate = postingDate;
        this.driverNumber = driverNumber;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.prosecutorDefendantReference = prosecutorDefendantReference;
        this.appliedProsecutorCosts = appliedProsecutorCosts;
        this.personalInformation = personalInformation;
        this.selfDefinedInformation = selfDefinedInformation;
        this.individualAliases = individualAliases;
        this.organisationInformation = organisationInformation;
        this.defendantInitiationCode=defendantInitiationCode;
        setOffences(offences);
        if (personalInformation != null) {
            personalInformation.setDefendantDetails(this);
        }
        if (selfDefinedInformation != null) {
            selfDefinedInformation.setDefendantDetails(this);
        }
        if(organisationInformation !=null) {
            organisationInformation.setDefendantDetails(this);
        }
    }


    public String getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final String defendantId) {
        this.defendantId = defendantId;
    }

    public String getAsn() {
        return asn;
    }

    public void setAsn(final String asn) {
        this.asn = asn;
    }

    public Language getDocumentationLanguage() {
        return documentationLanguage;
    }

    public void setDocumentationLanguage(final Language documentationLanguage) {
        this.documentationLanguage = documentationLanguage;
    }

    public Language getHearingLanguage() {
        return hearingLanguage;
    }

    public void setHearingLanguage(final Language hearingLanguage) {
        this.hearingLanguage = hearingLanguage;
    }

    public String getLanguageRequirement() {
        return languageRequirement;
    }

    public void setLanguageRequirement(final String languageRequirement) {
        this.languageRequirement = languageRequirement;
    }

    public String getSpecificRequirements() {
        return specificRequirements;
    }

    public void setSpecificRequirements(final String specificRequirements) {
        this.specificRequirements = specificRequirements;
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public void setNumPreviousConvictions(final Integer numPreviousConvictions) {
        this.numPreviousConvictions = numPreviousConvictions;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public void setDriverNumber(final String driverNumber) {
        this.driverNumber = driverNumber;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public void setNationalInsuranceNumber(final String nationalInsuranceNumber) {
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public String getProsecutorDefendantReference() {
        return prosecutorDefendantReference;
    }

    public void setProsecutorDefendantReference(final String prosecutorDefendantReference) {
        this.prosecutorDefendantReference = prosecutorDefendantReference;
    }

    public BigDecimal getAppliedProsecutorCosts() {
        return appliedProsecutorCosts;
    }

    public void setAppliedProsecutorCosts(BigDecimal appliedProsecutorCosts) {
        this.appliedProsecutorCosts = appliedProsecutorCosts;
    }

    public PersonalInformationDetails getPersonalInformation() {
        return personalInformation;
    }

    public void setPersonalInformation(final PersonalInformationDetails personalInformation) {
        this.personalInformation = personalInformation;
    }

    public SelfDefinedInformationDetails getSelfDefinedInformation() {
        return selfDefinedInformation;
    }

    public void setSelfDefinedInformation(final SelfDefinedInformationDetails selfDefinedInformation) {
        this.selfDefinedInformation = selfDefinedInformation;
    }

    @SuppressWarnings("squid:S2384")
    public Set<OffenceDetails> getOffences() {
        return offences;
    }

    public void setOffences(final Set<OffenceDetails> offences) {
        this.offences = offences == null ? emptySet() : new HashSet<>(offences);
        this.offences.forEach(offence -> offence.setDefendant(this));
    }

    public List<IndividualAliasDetail> getIndividualAliases() {
        return individualAliases;
    }

    public void setIndividualAliases(final List<IndividualAliasDetail> individualAliases) {
        this.individualAliases = individualAliases == null ? emptyList() : new LinkedList<>(individualAliases);
    }

    public UUID getIdpcMaterialId() {
        return idpcMaterialId;
    }

    public void setIdpcMaterialId(final UUID idpcMaterialId) {
        this.idpcMaterialId = idpcMaterialId;
    }

    public OrganisationInformationDetails getOrganisationInformation() {
        return organisationInformation;
    }

    public void setOrganisationInformation(final OrganisationInformationDetails organisationInformation) {
        this.organisationInformation = organisationInformation;
    }

    public String getDefendantInitiationCode() {
        return defendantInitiationCode;
    }

    public void setDefendantInitiationCode(final String defendantInitiationCode) {
        this.defendantInitiationCode = defendantInitiationCode;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}

