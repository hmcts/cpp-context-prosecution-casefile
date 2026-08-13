package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("java:S1168")
public class CaseDetailsToCivilFees implements Converter<CaseDetails, Set<CivilFees>> {

    private static final String NOT_APPLICABLE = "NOT_APPLICABLE";

    @SuppressWarnings("squid:S1135")
    public Set<CivilFees> convert(final CaseDetails caseDetails) {

        final boolean hasFee = isApplicable(caseDetails.getFeeStatus());
        final boolean hasContestedFee = isApplicable(caseDetails.getContestedFeeStatus());

        if(!hasFee && !hasContestedFee) {
            return null;
        }

        Set<CivilFees> civilFeesSet = new HashSet<>();

        if(hasFee) {
            civilFeesSet.add(createCivilFee(caseDetails.getFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getFeeType(),
                    caseDetails.getFeeStatus(),
                    caseDetails.getPaymentReference()));
        }

        if(hasContestedFee) {
            civilFeesSet.add(createCivilFee(caseDetails.getContestedFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getContestedFeeType(),
                    caseDetails.getContestedFeeStatus(),
                    caseDetails.getContestedFeePaymentReference()));
        }

        return civilFeesSet;

    }

    private boolean isApplicable(final String feeStatus) {
        return !isBlank(feeStatus) && !NOT_APPLICABLE.equalsIgnoreCase(feeStatus);
    }

    private CivilFees createCivilFee(UUID feeId, UUID caseId, String feeType, String feeStatus, String paymentReference) {
        return new CivilFees(feeId, caseId, feeType, feeStatus, paymentReference);
    }

}
