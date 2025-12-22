package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

import static java.util.Optional.ofNullable;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.IndividualAliasDetail;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@SuppressWarnings("pmd:NullAssignment")
public class DefendantView {

    private final String defendantId;
    private final String asn;
    private final Language documentationLanguage;
    private final Language hearingLanguage;
    private final String languageRequirement;
    private final String specificRequirements;
    private final Integer numPreviousConvictions;
    private final LocalDate postingDate;
    private final String driverNumber;
    private final String nationalInsuranceNumber;
    private final String prosecutorDefendantReference;
    private final BigDecimal appliedProsecutorCosts;

    private final PersonalInformation personalInformation;
    private final SelfDefinedInformation selfDefinedInformation;
    private final List<Offence> offences;
    private final List<IndividualAlias> individualAliases;

    private final UUID idpcMaterialId;
    private final String organisationName;

    public DefendantView(final DefendantDetails defendant) {
        this.defendantId = defendant.getDefendantId();
        this.asn = defendant.getAsn();
        this.documentationLanguage = defendant.getDocumentationLanguage();
        this.hearingLanguage = defendant.getHearingLanguage();
        this.languageRequirement = defendant.getLanguageRequirement();
        this.specificRequirements = defendant.getSpecificRequirements();
        this.numPreviousConvictions = defendant.getNumPreviousConvictions();
        this.postingDate = defendant.getPostingDate();
        this.driverNumber = defendant.getDriverNumber();
        this.nationalInsuranceNumber = defendant.getNationalInsuranceNumber();
        this.prosecutorDefendantReference = defendant.getProsecutorDefendantReference();
        this.appliedProsecutorCosts = defendant.getAppliedProsecutorCosts();

        this.personalInformation = createPersonalInformationFromPersonalInformationDetails(defendant.getPersonalInformation());
        this.selfDefinedInformation = createSelfDefinedInformationFromSelfDefinedInformationDetails(defendant.getSelfDefinedInformation());

        this.offences = defendant.getOffences().stream()
                .map(this::createOffenceFromOffenceDetails)
                .sorted(Comparator.comparing(Offence::getOffenceSequenceNumber))
                .collect(toList());
        this.idpcMaterialId = defendant.getIdpcMaterialId();
        this.individualAliases = ofNullable(defendant.getIndividualAliases())
                .map(aliases -> aliases.stream()
                        .map(this::toAliasDTO)
                        .collect(Collectors.toList())
                ).orElse(null);

        this.organisationName = nonNull(defendant.getOrganisationInformation())?defendant.getOrganisationInformation().getOrganisationName(): null;
    }

    private IndividualAlias toAliasDTO(final IndividualAliasDetail alias) {
        return new IndividualAlias(
                alias.getFirstName(),
                alias.getGivenName2(),
                alias.getGivenName3(),
                alias.getLastName(),
                alias.getTitle());
    }

    private Offence createOffenceFromOffenceDetails(final OffenceDetails offenceDetails) {

      return   Offence.offence()
                .withAlcoholRelatedOffence(getAlcoholRelatedOffence(offenceDetails))
                .withAppliedCompensation(offenceDetails.getAppliedCompensation())
                .withBackDuty(offenceDetails.getBackDuty())
                .withBackDutyDateFrom(offenceDetails.getBackDutyDateFrom())
                .withBackDutyDateTo(offenceDetails.getBackDutyDateTo())
                .withChargeDate(offenceDetails.getChargeDate())
                .withOffenceCode(offenceDetails.getOffenceCode())
                .withOffenceCommittedDate(offenceDetails.getOffenceCommittedDate())
                .withOffenceCommittedEndDate(offenceDetails.getOffenceCommittedEndDate())
                .withOffenceDateCode(offenceDetails.getOffenceDateCode())
                .withOffenceId(offenceDetails.getOffenceId())
                .withOffenceLocation(offenceDetails.getOffenceLocation())
                .withOffenceSequenceNumber(offenceDetails.getOffenceSequenceNumber())
                .withOffenceWording(offenceDetails.getOffenceWording())
                .withOffenceWordingWelsh(offenceDetails.getOffenceWordingWelsh())
                .withStatementOfFacts(offenceDetails.getStatementOfFacts())
                .withStatementOfFactsWelsh(offenceDetails.getStatementOfFactsWelsh())
                .withVehicleMake(offenceDetails.getVehicleMake())
                .withVehicleRegistrationMark(offenceDetails.getVehicleRegistrationMark())
                .withVehicleRelatedOffence(getVehicleRelatedOffence())
                .build();

    }

    private VehicleRelatedOffence getVehicleRelatedOffence() {
        return null;
    }

    private AlcoholRelatedOffence getAlcoholRelatedOffence(final OffenceDetails offenceDetails) {
        return ofNullable(offenceDetails.getAlcoholOffenceDetail())
                .map(alcoholOffenceDetail -> new AlcoholRelatedOffence(alcoholOffenceDetail.getAlcoholLevelAmount(), alcoholOffenceDetail.getAlcoholLevelMethod()))
                .orElse(null);
    }

    private SelfDefinedInformation createSelfDefinedInformationFromSelfDefinedInformationDetails(final SelfDefinedInformationDetails selfDefinedInformationDetails) {

        if (null == selfDefinedInformationDetails) {
            return null;
        }

        return new SelfDefinedInformation(selfDefinedInformationDetails.getAdditionalNationality(),
                selfDefinedInformationDetails.getDateOfBirth(),
                selfDefinedInformationDetails.getEthnicity(),
                selfDefinedInformationDetails.getGender(),
                selfDefinedInformationDetails.getNationality());
    }

    private Address createAddressFromAddressDetails(final AddressDetails addressDetails) {
        if (addressDetails != null) {
            return new Address(addressDetails.getAddress1(),
                    addressDetails.getAddress2(),
                    addressDetails.getAddress3(),
                    addressDetails.getAddress4(),
                    addressDetails.getAddress5(),
                    addressDetails.getPostcode());
        }
        return null;
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails createContactDetailsFromContactDetailsEntity(final ContactDetails contactDetails) {
        if (contactDetails != null) {
            return new uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails(
                    contactDetails.getHome(),
                    contactDetails.getMobile(),
                    contactDetails.getPrimaryEmail(),
                    contactDetails.getSecondaryEmail(),
                    contactDetails.getWork());
        }
        return null;
    }

    private PersonalInformation createPersonalInformationFromPersonalInformationDetails(final PersonalInformationDetails personalInformationDetails) {
        if (null == personalInformationDetails) {
            return null;
        }
        return new PersonalInformation(createAddressFromAddressDetails(personalInformationDetails.getAddress()),
                personalInformationDetails.getContactDetails().map(this::createContactDetailsFromContactDetailsEntity).orElse(null),
                personalInformationDetails.getFirstName(),
                null,
                null,
                personalInformationDetails.getLastName(),
                null,
                personalInformationDetails.getOccupation(),
                personalInformationDetails.getOccupationCode(),
                personalInformationDetails.getTitle());
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getAsn() {
        return asn;
    }

    public Language getDocumentationLanguage() {
        return documentationLanguage;
    }

    public Language getHearingLanguage() {
        return hearingLanguage;
    }

    public String getLanguageRequirement() {
        return languageRequirement;
    }

    public String getSpecificRequirements() {
        return specificRequirements;
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public String getProsecutorDefendantReference() {
        return prosecutorDefendantReference;
    }

    public BigDecimal getAppliedProsecutorCosts() {
        return appliedProsecutorCosts;
    }

    public PersonalInformation getPersonalInformation() {
        return personalInformation;
    }

    public SelfDefinedInformation getSelfDefinedInformation() {
        return selfDefinedInformation;
    }

    @SuppressWarnings("squid:S2384")
    public List<Offence> getOffences() {
        return offences;
    }

    public UUID getIdpcMaterialId() {
        return idpcMaterialId;
    }

    public List<IndividualAlias> getIndividualAliases() {
        return individualAliases;
    }

    @Override
    public boolean equals(final Object o) {
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

    public String getOrganisationName() {
        return organisationName;
    }
}
