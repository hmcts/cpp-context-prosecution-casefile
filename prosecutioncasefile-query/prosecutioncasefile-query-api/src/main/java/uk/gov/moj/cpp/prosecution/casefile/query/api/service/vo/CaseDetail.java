package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("squid:S00107")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDetail {

    private final String id;
    private final String urn;
    private final String type;
    private final Defendant defendant;
    private final String initiationCode;
    private final Boolean assigned; // sjp  (aocp/non-aocp)
    private final Boolean completed; // sjp  (aocp/non-aocp)
    private final String status; // sjp  (aocp/non-aocp)
    private final Boolean policeFlag; // sjp  (aocp/non-aocp)
    private final BigDecimal costs; // sjp  (aocp/non-aocp)
    private final BigDecimal aocpVictimSurcharge; // sjp  (aocp/non-aocp)
    private final BigDecimal aocpTotalCost; // sjp  (aocp/non-aocp)
    private final Boolean aocpEligible; // sjp  (aocp/non-aocp)
    private final Boolean readyForDecision; // sjp  (aocp/non-aocp)

    public CaseDetail(final String id, final String urn, final String type, final Defendant defendant, final String initiationCode,
                      final Boolean assigned, final Boolean completed, final String status, final Boolean policeFlag,
                      final BigDecimal costs, final BigDecimal aocpVictimSurcharge, final BigDecimal aocpTotalCost,
                      final Boolean aocpEligible, final Boolean readyForDecision) {
        this.id = id;
        this.urn = urn;
        this.type = type;
        this.defendant = defendant;
        this.initiationCode = initiationCode;
        this.assigned = assigned;
        this.completed = completed;
        this.status = status;
        this.policeFlag = policeFlag;
        this.costs = costs;
        this.aocpVictimSurcharge = aocpVictimSurcharge;
        this.aocpTotalCost = aocpTotalCost;
        this.aocpEligible = aocpEligible;
        this.readyForDecision = readyForDecision;
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public String getType() {
        return type;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public String getInitiationCode() {
        return initiationCode;
    }


    public Boolean getAssigned() {
        return assigned;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getPoliceFlag() {
        return policeFlag;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public BigDecimal getAocpVictimSurcharge() {
        return aocpVictimSurcharge;
    }

    public BigDecimal getAocpTotalCost() {
        return aocpTotalCost;
    }

    public Boolean getAocpEligible() {
        return aocpEligible;
    }

    public Boolean getReadyForDecision() {
        return readyForDecision;
    }

    public static CaseDetail fromCaseDetail(final CaseDetail caseDetail, final String initiationCode) {
        return CaseDetail.caseDetail()
                .withUrn(caseDetail.getUrn())
                .withId(caseDetail.getId())
                .withType(caseDetail.getType())
                .withDefendant(caseDetail.getDefendant())
                .withInitiationCode(initiationCode)
                .withAssigned(caseDetail.getAssigned())
                .withCompleted(caseDetail.getCompleted())
                .withStatus(caseDetail.getStatus())
                .withPoliceFlag(caseDetail.getPoliceFlag())
                .withCosts(caseDetail.getCosts())
                .withAocpVictimSurcharge(caseDetail.getAocpVictimSurcharge())
                .withAocpTotalCost(caseDetail.getAocpTotalCost())
                .withAocpEligible(caseDetail.getAocpEligible())
                .withReadyForDecision(caseDetail.getReadyForDecision())
                .build();
    }

    public static CaseDetail.Builder caseDetail() {
        return new CaseDetail.Builder();
    }

    public static class Builder {
        private String id;
        private String urn;
        private String type;
        private Defendant defendant;
        private String initiationCode;

        private Boolean assigned;

        private Boolean completed;

        private String status;

        private Boolean policeFlag;

        private BigDecimal costs;

        private BigDecimal aocpVictimSurcharge;

        private BigDecimal aocpTotalCost;

        private Boolean aocpEligible;

        private Boolean readyForDecision;

        public Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Builder withUrn(final String urn) {
            this.urn = urn;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Builder withDefendant(final Defendant defendant) {
            this.defendant = defendant;
            return this;
        }

        public Builder withInitiationCode(final String initiationCode) {
            this.initiationCode = initiationCode;
            return this;
        }

        public Builder withAssigned(final Boolean assigned) {
            this.assigned = assigned;
            return this;
        }

        public Builder withCompleted(final Boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder withStatus(final String status) {
            this.status = status;
            return this;
        }

        public Builder withPoliceFlag(final Boolean policeFlag) {
            this.policeFlag = policeFlag;
            return this;
        }

        public Builder withCosts(final BigDecimal costs) {
            this.costs = costs;
            return this;
        }

        public Builder withAocpVictimSurcharge(final BigDecimal aocpVictimSurcharge) {
            this.aocpVictimSurcharge = aocpVictimSurcharge;
            return this;
        }

        public Builder withAocpTotalCost(final BigDecimal aocpTotalCost) {
            this.aocpTotalCost = aocpTotalCost;
            return this;
        }

        public Builder withAocpEligible(final Boolean aocpEligible) {
            this.aocpEligible = aocpEligible;
            return this;
        }

        public Builder withReadyForDecision(final Boolean readyForDecision) {
            this.readyForDecision = readyForDecision;
            return this;
        }

        public CaseDetail build() {
           return new CaseDetail(id, urn, type, defendant, initiationCode, assigned, completed, status, policeFlag,
                    costs, aocpVictimSurcharge, aocpTotalCost, aocpEligible, readyForDecision);
        }

    }
}
