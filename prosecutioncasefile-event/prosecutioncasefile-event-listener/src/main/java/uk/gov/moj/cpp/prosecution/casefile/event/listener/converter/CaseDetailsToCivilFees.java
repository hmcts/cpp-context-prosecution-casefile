package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("java:S1168")
public class CaseDetailsToCivilFees implements Converter<CaseDetails, Set<CivilFees>> {
    
    @SuppressWarnings("squid:S1135")
    public Set<CivilFees> convert(final CaseDetails caseDetails) {

        final String feeStatus = normalize(caseDetails.getFeeStatus());
        final String contestedFeeStatus = normalize(caseDetails.getContestedFeeStatus());

        if (isEmpty(feeStatus) && isEmpty(contestedFeeStatus)) {
            return null;
        }

        Set<CivilFees> civilFeesSet = new HashSet<>();

        if (StringUtils.isNotEmpty(feeStatus)) {
            civilFeesSet.add(createCivilFee(caseDetails.getFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getFeeType(),
                    feeStatus,
                    caseDetails.getPaymentReference()));
        }

        if (StringUtils.isNotEmpty(contestedFeeStatus)) {
            civilFeesSet.add(createCivilFee(caseDetails.getContestedFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getContestedFeeType(),
                    contestedFeeStatus,
                    caseDetails.getContestedFeePaymentReference()));
        }

        return civilFeesSet;

    }

    private String normalize(final String status) {
        if (status == null) return null;
        final String trimmed = status.trim();
        if (trimmed.isEmpty()) return null;
        return "NOT_APPLICABLE".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private CivilFees createCivilFee(UUID feeId, UUID caseId, String feeType, String feeStatus, String paymentReference) {
        return new CivilFees(feeId, caseId, feeType, feeStatus, paymentReference);
    }

}
