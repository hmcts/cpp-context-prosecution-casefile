package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.commands.Offence;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO(ATCM-3453): Remove the class, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
abstract class DefendantMixin {

    @JsonCreator
    public DefendantMixin(@JsonProperty("address") final Address address,
                          @JsonProperty("dateOfBirth")   final String dateOfBirth,
                          @JsonProperty("firstName")   final String firstName,
                          @JsonProperty("gender")   final Gender gender,
                          @JsonProperty("lastName")   final String lastName,
                          @JsonProperty("numPreviousConvictions")   final Integer numPreviousConvictions,
                          @JsonProperty("offences")   final List<Offence> offences,
                          @JsonProperty("title")   final String title) {
    }
}
