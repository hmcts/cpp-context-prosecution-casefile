package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalDetails {

    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final Address address;
    private final ContactDetails contactDetails;
    private final String nationalInsuranceNumber;
    private final String driverNumber;
    private final String driverLicenceDetails;

    public PersonalDetails(final String firstName, final String lastName, final String dateOfBirth, final Address address, final ContactDetails contactDetails,
                           final String nationalInsuranceNumber, final String driverNumber, final String driverLicenceDetails) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.contactDetails = contactDetails;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
        this.driverNumber = driverNumber;
        this.driverLicenceDetails = driverLicenceDetails;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getDriverLicenceDetails() {
        return driverLicenceDetails;
    }

    public static PersonalDetails.Builder personalDetails() {
        return new PersonalDetails.Builder();
    }

    public static class Builder {

        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private Address address;
        private ContactDetails contactDetails;
        private String nationalInsuranceNumber;
        private String driverNumber;
        private String driverLicenceDetails;

        public PersonalDetails.Builder withFirstname(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public PersonalDetails.Builder withLastname(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public PersonalDetails.Builder withDateOfBirth(final String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public PersonalDetails.Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public PersonalDetails.Builder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public PersonalDetails.Builder withNationalInsuranceNumber(final String nationalInsuranceNumber) {
            this.nationalInsuranceNumber = nationalInsuranceNumber;
            return this;
        }

        public PersonalDetails.Builder withDriverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public PersonalDetails.Builder withDriverLicenceDetails(final String driverLicenceDetails) {
            this.driverLicenceDetails = driverLicenceDetails;
            return this;
        }

        public PersonalDetails build() {
            return new PersonalDetails(firstName, lastName, dateOfBirth, address, contactDetails, nationalInsuranceNumber, driverNumber, driverLicenceDetails);
        }
    }
}
