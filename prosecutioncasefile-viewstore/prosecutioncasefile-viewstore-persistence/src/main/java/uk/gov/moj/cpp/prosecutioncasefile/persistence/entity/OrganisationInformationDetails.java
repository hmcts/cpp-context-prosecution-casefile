package uk.gov.moj.cpp.prosecutioncasefile.persistence.entity;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "organisation_information")
public class OrganisationInformationDetails implements Serializable {

    @Id
    @Column(name = "organisation_information_id", unique = true, nullable = false)
    private String organisationInformationId;

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
