package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("java:S1168")
public class CaseDetailsToCivilFeesConverter implements Converter<CaseDetails, List<CivilFees>> {

    @Override
    @SuppressWarnings("squid:S1188")
    public List<CivilFees> convert(final CaseDetails caseDetails) {

        if(isEmpty(caseDetails.getFeeStatus()) && isEmpty(caseDetails.getContestedFeeStatus())) {
            return null;
        }

        List<CivilFees> civilFeesList = new ArrayList<>();

        if(StringUtils.isNotEmpty(caseDetails.getFeeStatus())) {
            civilFeesList.add(createCivilFee(caseDetails.getFeeId(),
                    caseDetails.getFeeType(),
                    caseDetails.getFeeStatus(),
                    caseDetails.getPaymentReference()));
        }

        if(StringUtils.isNotEmpty(caseDetails.getContestedFeeStatus())) {
            civilFeesList.add(createCivilFee(caseDetails.getContestedFeeId(),
                    caseDetails.getContestedFeeType(),
                    caseDetails.getContestedFeeStatus(),
                    caseDetails.getContestedFeePaymentReference()));
        }

        return civilFeesList;

    }

    private CivilFees createCivilFee(UUID feeId, String feeType, String feeStatus, String paymentReference) {
        return new CivilFees(feeId, FeeStatus.valueOf(feeStatus), FeeType.valueOf(feeType), paymentReference);
    }
}