package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalEntityDefendant {

    private final String name;
    private final Address address;
    private final ContactDetails contactDetails;
    private final String incorporationNumber;

    public LegalEntityDefendant(final String name, final Address address, final ContactDetails contactDetails, final String incorporationNumber) {
        this.name = name;
        this.address = address;
        this.contactDetails = contactDetails;
        this.incorporationNumber = incorporationNumber;
    }

    public String getName() {
        return name;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public String getIncorporationNumber() {
        return incorporationNumber;
    }

    public Address getAddress() {
        return address;
    }

    public static LegalEntityDefendant.Builder legalEntityDefendant() {
        return new LegalEntityDefendant.Builder();
    }

    public static class Builder {
        private String name;
        private Address address;
        private ContactDetails contactDetails;
        private String incorporationNumber;

        public LegalEntityDefendant.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public LegalEntityDefendant.Builder withAddress(final Address address) {
            this.address = address;
            return this;
        }

        public LegalEntityDefendant.Builder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public LegalEntityDefendant.Builder withIncorporationNumber(final String incorporationNumber) {
            this.incorporationNumber = incorporationNumber;
            return this;
        }

        public LegalEntityDefendant build() {
            return new LegalEntityDefendant(name, address, contactDetails, incorporationNumber);
        }
    }
}
