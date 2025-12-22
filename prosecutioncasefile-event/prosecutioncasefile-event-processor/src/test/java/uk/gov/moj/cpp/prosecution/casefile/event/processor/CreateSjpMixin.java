package uk.gov.moj.cpp.prosecution.casefile.event.processor;


import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// TODO(ATCM-3453): Remove the class, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
abstract class CreateSjpMixin {

    @JsonCreator
    public CreateSjpMixin(@JsonProperty("costs") BigDecimal costs,
                          @JsonProperty("defendant") uk.gov.justice.json.schemas.domains.sjp.commands.Defendant defendant,
                          @JsonProperty("enterpriseId") String enterpriseId,
                          @JsonProperty("id") String id,
                          @JsonProperty("postingDate") String postingDate,
                          @JsonProperty("prosecutingAuthority") String prosecutingAuthority,
                          @JsonProperty("ptiUrn") String ptiUrn,
                          @JsonProperty("urn") String urn) {
    }

}
