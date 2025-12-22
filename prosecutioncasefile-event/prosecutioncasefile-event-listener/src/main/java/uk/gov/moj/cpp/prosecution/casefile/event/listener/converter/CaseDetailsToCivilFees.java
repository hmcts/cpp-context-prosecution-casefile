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

        if(isEmpty(caseDetails.getFeeStatus()) && isEmpty(caseDetails.getContestedFeeStatus())) {
            return null;
        }

        Set<CivilFees> civilFeesSet = new HashSet<>();

        if(StringUtils.isNotEmpty(caseDetails.getFeeStatus())) {
            civilFeesSet.add(createCivilFee(caseDetails.getFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getFeeType(),
                    caseDetails.getFeeStatus(),
                    caseDetails.getPaymentReference()));
        }

        if(StringUtils.isNotEmpty(caseDetails.getContestedFeeStatus())) {
            civilFeesSet.add(createCivilFee(caseDetails.getContestedFeeId(),
                    caseDetails.getCaseId(),
                    caseDetails.getContestedFeeType(),
                    caseDetails.getContestedFeeStatus(),
                    caseDetails.getContestedFeePaymentReference()));
        }

        return civilFeesSet;

    }

    private CivilFees createCivilFee(UUID feeId, UUID caseId, String feeType, String feeStatus, String paymentReference) {
        return new CivilFees(feeId, caseId, feeType, feeStatus, paymentReference);
    }

}
