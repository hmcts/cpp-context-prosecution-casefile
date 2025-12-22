package uk.gov.moj.cpp.prosecution.casefile.query.api.service.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
@SuppressWarnings("squid:S00107")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDetailWithReason extends CaseDetail {

    public static final String NO_MATCH_FOUND = "NO_MATCH_FOUND";
    public static final String TOO_MANY_DEFENDANTS = "TOO_MANY_DEFENDANTS";
    public static final String ALREADY_PLEADED = "ALREADY_PLEADED";
    public static final String OUT_OF_TIME = "OUT_OF_TIME";

    private Boolean canContinue;
    private String cantContinueReason;

    public CaseDetailWithReason(final String id, final String urn, final String type, final Defendant defendant, final String initiationCode,
                                final Boolean assigned, final Boolean completed, final String status, final Boolean policeFlag,
                                final BigDecimal costs, final BigDecimal aocpVictimSurcharge, final BigDecimal aocpTotalCost,
                                final Boolean aocpEligible, final Boolean readyForDecision,
                                final Boolean canContinue, final String cantContinueReason) {

        super(id, urn, type, defendant, initiationCode, assigned, completed, status, policeFlag, costs, aocpVictimSurcharge, aocpTotalCost, aocpEligible, readyForDecision);

        this.canContinue = canContinue;
        this.cantContinueReason = cantContinueReason;
    }

    public Boolean getCanContinue() {
        return canContinue;
    }

    public String getCantContinueReason() {
        return cantContinueReason;
    }

    public static CaseDetailWithReason fromCaseDetail(final CaseDetail caseDetail, final Boolean canContinue, final String cantContinueReason) {
       return new CaseDetailWithReason(caseDetail.getId(), caseDetail.getUrn(), caseDetail.getType(), caseDetail.getDefendant(), caseDetail.getInitiationCode(),
                                        caseDetail.getAssigned(), caseDetail.getCompleted(), caseDetail.getStatus(), caseDetail.getPoliceFlag(),
                                        caseDetail.getCosts(), caseDetail.getAocpVictimSurcharge(), caseDetail.getAocpTotalCost(), caseDetail.getAocpEligible(),
                                        caseDetail.getReadyForDecision(), canContinue, cantContinueReason);
    }

    public static CaseDetailWithReason fromCaseDetail(final CaseDetail caseDetail, final Boolean canContinue) {
       return new CaseDetailWithReason(caseDetail.getId(), caseDetail.getUrn(), caseDetail.getType(), caseDetail.getDefendant(), caseDetail.getInitiationCode(),
                                        caseDetail.getAssigned(), caseDetail.getCompleted(), caseDetail.getStatus(), caseDetail.getPoliceFlag(),
                                        caseDetail.getCosts(), caseDetail.getAocpVictimSurcharge(), caseDetail.getAocpTotalCost(), caseDetail.getAocpEligible(),
                                        caseDetail.getReadyForDecision(), canContinue, null);
    }

}
