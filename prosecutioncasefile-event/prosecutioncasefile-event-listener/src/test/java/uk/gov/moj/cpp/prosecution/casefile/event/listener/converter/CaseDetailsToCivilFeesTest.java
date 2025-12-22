package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


public class CaseDetailsToCivilFeesTest {

    private CaseDetailsToCivilFees underTest = new CaseDetailsToCivilFees();

    @Test
    public void shouldReturnNullWhenCivilFeesStatusEmpty() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();

        Set<CivilFees> civilFees = underTest.convert(caseDetails);

        assertNull(civilFees);
    }

    @Test
    public void shouldCreateCivilFeesObject() {
        UUID feeId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        String feeType = "someFeeType";
        String feeStatus = "someFeeStatus";
        String paymentReference = "somePaymentReference";

        CaseDetails caseDetails = CaseDetails.caseDetails()

                .withFeeId(feeId)
                .withFeeType(feeType)
                .withFeeStatus(feeStatus)
                .withPaymentReference(paymentReference)
                .withCaseId(caseId)
                .build();

        Set<CivilFees> civilFees = underTest.convert(caseDetails);
        civilFees.forEach(civilFee -> {
            assertEquals(civilFee.getCaseId(), caseId);
            assertEquals(civilFee.getFeeId(), feeId);
            assertEquals(civilFee.getFeeType(), feeType);
            assertEquals(civilFee.getFeeStatus(), feeStatus);
            assertEquals(civilFee.getPaymentReference(), paymentReference);
        });

    }
}