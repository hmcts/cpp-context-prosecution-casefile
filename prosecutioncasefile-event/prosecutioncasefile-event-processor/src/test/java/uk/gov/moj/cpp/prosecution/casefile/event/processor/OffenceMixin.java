package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO(ATCM-3453): Remove the class, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
abstract class OffenceMixin {

    public OffenceMixin(@JsonProperty("chargeDate") final String chargeDate,
                        @JsonProperty("appliedCompensation") final BigDecimal compensation,
                        @JsonProperty("id")    final String id,
                        @JsonProperty("libraOffenceCode")    final String libraOffenceCode,
                        @JsonProperty("libraOffenceDateCode")    final Integer libraOffenceDateCode,
                        @JsonProperty("offenceCommittedDate")    final String offenceCommittedDate,
                        @JsonProperty("offenceSequenceNo")    final Integer offenceSequenceNo,
                        @JsonProperty("offenceWording")    final String offenceWording,
                        @JsonProperty("prosecutionFacts")    final String prosecutionFacts,
                        @JsonProperty("witnessStatement")    final String witnessStatement) {
    }
}
