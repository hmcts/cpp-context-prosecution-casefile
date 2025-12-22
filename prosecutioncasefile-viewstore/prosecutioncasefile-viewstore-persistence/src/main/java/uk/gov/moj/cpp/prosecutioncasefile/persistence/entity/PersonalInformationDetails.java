package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "personal_information")
public class PersonalInformationDetails implements Serializable {

    @Id
    @Column(name = "personal_information_id", unique = true, nullable = false)
    private String personalInformationId;

    @MapsId
    @OneToOne(mappedBy = "personalInformation")
    @JoinColumn(name = "personal_information_id")
    private DefendantDetails defendantDetails;

    @Column(name = "title")
    private String title;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "occupation_code")
    private Integer occupationCode;

    @Embedded
    private AddressDetails address;

    @Embedded
    private ContactDetails contactDetails;


    @SuppressWarnings("squid:S00107")
    public PersonalInformationDetails(final String title,
                                      final String firstName,
                                      final String lastName,
                                      final String occupation,
                                      final Integer occupationCode,
                                      final AddressDetails address,
                                      final ContactDetails contactDetails) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.occupation = occupation;
        this.occupationCode = occupationCode;
        this.title = title;
        this.address = address;
        this.contactDetails = contactDetails;
    }

    public PersonalInformationDetails() {
    }

    public String getPersonalInformationId() {
        return personalInformationId;
    }

    public void setPersonalInformationId(final String personalInformationId) {
        this.personalInformationId = personalInformationId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(final String occupation) {
        this.occupation = occupation;
    }

    public Integer getOccupationCode() {
        return occupationCode;
    }

    public void setOccupationCode(final Integer occupationCode) {
        this.occupationCode = occupationCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public AddressDetails getAddress() {
        return address;
    }

    public void setAddress(final AddressDetails address) {
        this.address = address;
    }

    public Optional<ContactDetails> getContactDetails() {
        return Optional.ofNullable(contactDetails);
    }

    public void setContactDetails(final ContactDetails contactDetails) {
        this.contactDetails = contactDetails;
    }

    public DefendantDetails getDefendantDetails() {
        return defendantDetails;
    }

    public void setDefendantDetails(final DefendantDetails defendantDetails) {
        this.defendantDetails = defendantDetails;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
