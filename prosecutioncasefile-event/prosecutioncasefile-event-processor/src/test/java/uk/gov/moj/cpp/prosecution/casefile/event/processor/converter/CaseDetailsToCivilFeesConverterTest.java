package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.justice.core.courts.FeeStatus.NOT_APPLICABLE;
import static uk.gov.justice.core.courts.FeeType.INITIAL;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class CaseDetailsToCivilFeesConverterTest {

    private final CaseDetailsToCivilFeesConverter underTest = new CaseDetailsToCivilFeesConverter();

    @Test
    public void shouldReturnNullWhenCivilFeesStatusEmpty() {
        CaseDetails caseDetails = CaseDetails.caseDetails().build();

        List<CivilFees> civilFees = underTest.convert(caseDetails);

        assertNull(civilFees);
    }

    @Test
    public void shouldCreateCivilFeesObject() {
        UUID feeId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        FeeType feeType = INITIAL;
        FeeStatus feeStatus = NOT_APPLICABLE;
        String paymentReference = "somePaymentReference";

        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withFeeId(feeId)
                .withFeeType(String.valueOf(feeType))
                .withFeeStatus(String.valueOf(feeStatus))
                .withPaymentReference(paymentReference)
                .withCaseId(caseId)
                .build();

        List<CivilFees> civilFees = underTest.convert(caseDetails);

        assertEquals(civilFees.get(0).getFeeId(), feeId);
        assertEquals(feeType, civilFees.get(0).getFeeType());
        assertEquals(feeStatus, civilFees.get(0).getFeeStatus());
        assertEquals(paymentReference, civilFees.get(0).getPaymentReference());
    }

    @Test
    public void shouldCreateCivilFeesObjectWhenFeeStatusIsNotApplicableAndFeeTypeIsMissing() {
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withFeeStatus(String.valueOf(NOT_APPLICABLE))
                .build();

        List<CivilFees> civilFees = underTest.convert(caseDetails);

        assertEquals(NOT_APPLICABLE, civilFees.get(0).getFeeStatus());
        assertNull(civilFees.get(0).getFeeType());
        assertNull(civilFees.get(0).getFeeId());
    }

    @Test
    public void shouldCreateCivilFeesObjectWhenContestedFeeStatusIsNotApplicableAndContestedFeeTypeIsMissing() {
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withContestedFeeStatus(String.valueOf(NOT_APPLICABLE))
                .build();

        List<CivilFees> civilFees = underTest.convert(caseDetails);

        assertEquals(NOT_APPLICABLE, civilFees.get(0).getFeeStatus());
        assertNull(civilFees.get(0).getFeeType());
        assertNull(civilFees.get(0).getFeeId());
    }
}