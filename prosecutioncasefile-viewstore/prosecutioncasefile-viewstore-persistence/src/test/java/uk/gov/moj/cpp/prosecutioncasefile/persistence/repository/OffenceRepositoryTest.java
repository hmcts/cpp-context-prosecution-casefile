package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.APPLIED_COMPENSATION;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.BACK_DUTY;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.BACK_DUTY_FROM;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.BACK_DUTY_TO;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.CHARGE_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_COMMITTED_END_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_DATE_CODE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_LOCATION;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_SEQUENCE_NUMBER;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_WORDING;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.OFFENCE_WORDING_WELSH;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.STATEMENT_OF_FACT;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.STATEMENT_OF_FACT_WELSH;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.VEHICLE_MAKE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.VEHICLE_REGISTRATION_MARK;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils.createFirstDefendantOffence;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.util.TestUtils;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class OffenceRepositoryTest {

    @Inject
    private OffenceRepository offenceRepository;

    @Test
    public void shouldFindOffenceById() {
        final OffenceDetails offence = TestUtils.createFirstDefendantOffence();
        offenceRepository.save(offence);
        final OffenceDetails offenceDetailsSvd = offenceRepository.findBy(offence.getOffenceId());
        assertOffenceMatches(offence, offenceDetailsSvd);
    }

    @Test
    public void shouldConstructOffence() {
        final OffenceDetails offenceDetails = new OffenceDetails(OFFENCE_ID, APPLIED_COMPENSATION, BACK_DUTY, BACK_DUTY_FROM, BACK_DUTY_TO, CHARGE_DATE, OFFENCE_CODE, OFFENCE_COMMITTED_DATE,
                OFFENCE_COMMITTED_END_DATE, OFFENCE_DATE_CODE, OFFENCE_LOCATION, OFFENCE_SEQUENCE_NUMBER, OFFENCE_WORDING, OFFENCE_WORDING_WELSH, STATEMENT_OF_FACT, STATEMENT_OF_FACT_WELSH,
                VEHICLE_MAKE, VEHICLE_REGISTRATION_MARK, null);
        final OffenceDetails offenceDetailsSvd = createFirstDefendantOffence();
        assertOffenceMatches(offenceDetails, offenceDetailsSvd);
    }

    private void assertOffenceMatches(final OffenceDetails offence, final OffenceDetails offenceDetailsSvd) {
        assertThat(offence.getOffenceId(), equalTo(offenceDetailsSvd.getOffenceId()));
        assertThat(offence.getAppliedCompensation(), equalTo(offenceDetailsSvd.getAppliedCompensation()));
        assertThat(offence.getBackDuty(), equalTo(offenceDetailsSvd.getBackDuty()));
        assertThat(offence.getOffenceCode(), equalTo(offenceDetailsSvd.getOffenceCode()));
        assertThat(offence.getChargeDate(), equalTo(offenceDetailsSvd.getChargeDate()));
        assertThat(offence.getOffenceLocation(), equalTo(offenceDetailsSvd.getOffenceLocation()));
        assertThat(offence.getBackDutyDateFrom(), equalTo(offenceDetailsSvd.getBackDutyDateFrom()));
        assertThat(offence.getBackDutyDateTo(), equalTo(offenceDetailsSvd.getBackDutyDateTo()));
        assertThat(offence.getOffenceCommittedDate(), equalTo(offenceDetailsSvd.getOffenceCommittedDate()));
        assertThat(offence.getDefendant(), equalTo(offenceDetailsSvd.getDefendant()));
        assertThat(offence.getOffenceCommittedEndDate(), equalTo(offenceDetailsSvd.getOffenceCommittedEndDate()));
        assertThat(offence.getOffenceDateCode(), equalTo(offenceDetailsSvd.getOffenceDateCode()));
        assertThat(offence.getOffenceSequenceNumber(), equalTo(offenceDetailsSvd.getOffenceSequenceNumber()));
        assertThat(offence.getOffenceWording(), equalTo(offenceDetailsSvd.getOffenceWording()));
        assertThat(offence.getOffenceWordingWelsh(), equalTo(offenceDetailsSvd.getOffenceWordingWelsh()));
        assertThat(offence.getStatementOfFacts(), equalTo(offenceDetailsSvd.getStatementOfFacts()));
        assertThat(offence.getStatementOfFactsWelsh(), equalTo(offenceDetailsSvd.getStatementOfFactsWelsh()));
        assertThat(offence.getVehicleMake(), equalTo(offenceDetailsSvd.getVehicleMake()));
        assertThat(offence.getVehicleRegistrationMark(), equalTo(offenceDetailsSvd.getVehicleRegistrationMark()));
    }
}
