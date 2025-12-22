package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO(ATCM-3453): Remove the class, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
abstract class AddressMixin {

    @JsonCreator
    public AddressMixin(@JsonProperty("address1") final String address1,
                        @JsonProperty("address2") final String address2,
                        @JsonProperty("address3") final String address3,
                        @JsonProperty("address4") final String address4,
                        @JsonProperty("address5") final String postcode) {
    }
}
