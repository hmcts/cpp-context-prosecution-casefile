package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"squid:S1133", "squid:S1213"})
public class Address {

    private final String address1;
    private final String address2;
    private final String address3;
    private final String address4;
    private final String address5;
    private final String postcode;

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getAddress5() {
        return address5;
    }

    public String getPostcode() {
        return postcode;
    }

    public Address(final String address1, final String address2, final String address3, final String address4, final String address5, final String postcode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.address5 = address5;
        this.postcode = postcode;
    }

    public static Address.Builder address() {
        return new Address.Builder();
    }

    public static class Builder {
        private String address1;
        private String address2;
        private String address3;
        private String address4;
        private String address5;
        private String postcode;

        public Address.Builder withAddress1(final String address1) {
            this.address1 = address1;
            return this;
        }

        public Address.Builder withAddress2(final String address2) {
            this.address2 = address2;
            return this;
        }

        public Address.Builder withAddress3(final String address3) {
            this.address3 = address3;
            return this;
        }

        public Address.Builder withAddress4(final String address4) {
            this.address4 = address4;
            return this;
        }

        public Address.Builder withAddress5(final String address5) {
            this.address5 = address5;
            return this;
        }

        public Address.Builder withPostcode(final String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Address build() {
            return new Address(address1, address2, address3, address4, address5, postcode);
        }
    }
}
