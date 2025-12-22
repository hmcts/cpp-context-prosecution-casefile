package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;

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
@Table(name = "organisation_information")
public class OrganisationInformationDetails implements Serializable {

    @Id
    @Column(name = "organisation_information_id", unique = true, nullable = false)
    private String organisationInformationId;

    @MapsId
    @OneToOne(mappedBy = "organisationInformation")
    @JoinColumn(name = "organisation_information_id")
    private DefendantDetails defendantDetails;

    @Column(name = "organisation_name")
    private String organisationName;

    @Embedded
    private AddressDetails address;

    public OrganisationInformationDetails(final String organisationName,
                                      final AddressDetails address) {
        this.organisationName = organisationName;
        this.address = address;
    }

    public OrganisationInformationDetails() {
    }

    public String getOrganisationInformationId() {
        return organisationInformationId;
    }

    public void setOrganisationInformationId(final String organisationInformationId) {
        this.organisationInformationId = organisationInformationId;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(final String organisationName) {
        this.organisationName = organisationName;
    }

    public AddressDetails getAddress() {
        return address;
    }

    public void setAddress(final AddressDetails address) {
        this.address = address;
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
