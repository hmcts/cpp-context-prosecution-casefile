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

        final String feeStatus = normalize(caseDetails.getFeeStatus());
        final String contestedFeeStatus = normalize(caseDetails.getContestedFeeStatus());

        if (isEmpty(feeStatus) && isEmpty(contestedFeeStatus)) {
            return null;
        }

        List<CivilFees> civilFeesList = new ArrayList<>();

        if (StringUtils.isNotEmpty(feeStatus)) {
            civilFeesList.add(createCivilFee(caseDetails.getFeeId(),
                    caseDetails.getFeeType(),
                    feeStatus,
                    caseDetails.getPaymentReference()));
        }

        if (StringUtils.isNotEmpty(contestedFeeStatus)) {
            civilFeesList.add(createCivilFee(caseDetails.getContestedFeeId(),
                    caseDetails.getContestedFeeType(),
                    contestedFeeStatus,
                    caseDetails.getContestedFeePaymentReference()));
        }

        return civilFeesList;

    }

    private String normalize(final String status) {
        if (status == null) return null;
        final String trimmed = status.trim();
        if (trimmed.isEmpty()) return null;
        return "NOT_APPLICABLE".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private CivilFees createCivilFee(UUID feeId, String feeType, String feeStatus, String paymentReference) {
        return new CivilFees(feeId, FeeStatus.valueOf(feeStatus), FeeType.valueOf(feeType), paymentReference);
    }
}