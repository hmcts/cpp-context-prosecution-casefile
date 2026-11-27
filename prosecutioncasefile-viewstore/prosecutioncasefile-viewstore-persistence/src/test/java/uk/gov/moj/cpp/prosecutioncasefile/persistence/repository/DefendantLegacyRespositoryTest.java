package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import uk.gov.justice.cps.prosecutioncasefile.Cpr;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CPRDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantOffenderDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.Hearing;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceLegacy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.collect.Sets;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantLegacyRespositoryTest {

    private static final String CASE_URN = "CASEURN";
    private static final String OFFENCE_CODE = "OF61131";
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_ONE = UUID.randomUUID();
    private static final UUID DEFENDANT_ID_TWO = UUID.randomUUID();
    private static final UUID SUSPECT_ID_ONE = UUID.randomUUID();
    private static final UUID SUSPECT_ID_TWO = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID_ONE = UUID.randomUUID();
    private static final UUID OFFENCE_ID_TWO = UUID.randomUUID();
    private static final String POLICE_DEFENDANT_ID = "POL-12345";
    private static final Integer COURT_ROOM = 1;
    private static final String COURT_NAME = "Croydon Magistrates Court";
    private static final String COURT_ID = "COURTID-12345";
    private static final String TYPE_OF_COURT = "MAGISTRATES";
    private static final String HEARING_DATE = LocalDate.of(2019, 12, 2).toString();
    private final LocalDate ARREST_DATE = LocalDate.of(2019, 01, 01);
    private final String CATEGORY = "CATEGORY";
    private final String INDICATED_PLEA = "Not Guilty";
    private final LocalDate CHARGE_DATE = LocalDate.of(2019, 03, 01);
    private final LocalDate START_DATE = LocalDate.of(2015, 01, 01);
    private final LocalDate END_DATE = LocalDate.of(2021, 01, 01);
    private final int ORDER_INDEX = 1;
    private final String PLEA = "Not Guilty";
    private final String POLICE_OFFENCE_ID = "policeid121212";
    private final Integer SEQUENCE_NUMBER = 1;
    private final String WORDING = "Not Guilty";
    private final String SECTION = "SECTION";
    private final String REASON = "Reason";
    private final String DESCRIPTION = "Description";
    private final String CPR_CHECK_DIGIT = "A";
    private final String CPR_NUMBER = "10000000001";
    private final String CPR_OU = "XX45GD00";
    private final String CPR_YEAR = "09";

    private DefendantLegacy defendantOne;
    private DefendantLegacy defendantTwo;

    @Inject
    private DefendantLegacyRepository defendantLegacyRepository;

    @Before
    public void setup() {
        createDefendants();
    }

    @After
    public void teardown() {
        removeDefendants();
    }

    @Test
    public void shouldFindDefendantById() throws Exception {
        DefendantLegacy defendant = defendantLegacyRepository.findBy(DEFENDANT_ID_ONE);
        assertThat(defendant.getDefendantId(), equalTo(DEFENDANT_ID_ONE));
        assertThat(defendant.getSuspectId(), equalTo(SUSPECT_ID_ONE));
        assertThat(defendant.getCaseId(), equalTo(CASE_ID));
        assertThat(defendant.getCaseUrn(), equalTo(CASE_URN));
        assertThat(defendant.getPersonId(), equalTo(PERSON_ID));
        assertThat(defendant.getPoliceDefendantId(), equalTo(POLICE_DEFENDANT_ID));
        assertThat(defendant.getSuspectId(), equalTo(SUSPECT_ID_ONE));

        final Hearing hearing = defendant.getHearing();
        assertThat(hearing.getCourtId(), equalTo(COURT_ID));
        assertThat(hearing.getCourtName(), equalTo(COURT_NAME));
        assertThat(hearing.getCourtRoom(), equalTo(COURT_ROOM));
        assertThat(hearing.getHearingDate(), equalTo(HEARING_DATE));
        assertThat(hearing.getTypeOfCourt(), equalTo(TYPE_OF_COURT));

        Optional<OffenceLegacy> offence = defendant.getOffences().stream().findFirst();
        assertThat(offence.isPresent(), is(true));
        assertThat(offence.get().getCode(), equalTo(OFFENCE_CODE));
        assertThat(offence.get().getOffenceId(), equalTo(OFFENCE_ID_ONE));
        assertThat(offence.get().getArrestDate(), equalTo(ARREST_DATE));
        assertThat(offence.get().getCategory(), equalTo(CATEGORY));
        assertThat(offence.get().getChargeDate(), equalTo(CHARGE_DATE));
        assertThat(offence.get().getDescription(), equalTo(DESCRIPTION));
        assertThat(offence.get().getEndDate(), equalTo(END_DATE));
        assertThat(offence.get().getIndicatedPlea(), equalTo(INDICATED_PLEA));
        assertThat(offence.get().getOrderIndex(), equalTo(ORDER_INDEX));
        assertThat(offence.get().getPlea(), equalTo(PLEA));
        assertThat(offence.get().getPoliceOffenceId(), equalTo(POLICE_OFFENCE_ID));
        assertThat(offence.get().getReason(), equalTo(REASON));
        assertThat(offence.get().getSection(), equalTo(SECTION));
        assertThat(offence.get().getSequenceNumber(), equalTo(SEQUENCE_NUMBER));
        assertThat(offence.get().getStartDate(), equalTo(START_DATE));
        assertThat(offence.get().getWording(), equalTo(WORDING));

        final CPRDetails cpr = offence.get().getCpr();
        assertThat(cpr.getDefendantOffender().getCheckDigit(), equalTo(CPR_CHECK_DIGIT));
        assertThat(cpr.getDefendantOffender().getNumber(), equalTo(CPR_NUMBER));
        assertThat(cpr.getDefendantOffender().getOrganisationUnit(), equalTo(CPR_OU));
        assertThat(cpr.getDefendantOffender().getYear(), equalTo(CPR_YEAR));
    }

    @Test
    public void shouldReturnNullWhenNotFound() throws Exception {
        DefendantLegacy defendant = defendantLegacyRepository.findBy(UUID.randomUUID());
        assertThat(defendant, nullValue());
    }

    @Test
    public void shouldReturnDefendants() {
        List<DefendantLegacy> defendants = defendantLegacyRepository.findByCaseId(CASE_ID);
        assertThat(defendants.size(), is(2));
    }

    private void createDefendants() {
        defendantOne = new DefendantLegacy(DEFENDANT_ID_ONE, SUSPECT_ID_ONE, POLICE_DEFENDANT_ID, PERSON_ID, CASE_ID, CASE_URN,
                Sets.newHashSet(createOffence(OFFENCE_ID_ONE)));

        defendantOne.setHearing(createHearing());

        defendantLegacyRepository.save(defendantOne);

        defendantTwo = new DefendantLegacy(DEFENDANT_ID_TWO, SUSPECT_ID_TWO, POLICE_DEFENDANT_ID, PERSON_ID, CASE_ID, CASE_URN,
                Sets.newHashSet(createOffence(OFFENCE_ID_TWO)));

        defendantLegacyRepository.save(defendantTwo);
    }

    private OffenceLegacy createOffence(UUID offenceId) {
        OffenceLegacy offence = new OffenceLegacy();
        offence.setOffenceId(offenceId);
        offence.setCode(OFFENCE_CODE);
        offence.setCpr(createCpr());
        offence.setCategory(CATEGORY);
        offence.setChargeDate(CHARGE_DATE);
        offence.setDescription(DESCRIPTION);
        offence.setStartDate(START_DATE);
        offence.setEndDate(END_DATE);
        offence.setIndicatedPlea(INDICATED_PLEA);
        offence.setOrderIndex(ORDER_INDEX);
        offence.setPoliceOffenceId(POLICE_OFFENCE_ID);
        offence.setPlea(PLEA);
        offence.setReason(REASON);
        offence.setSection(SECTION);
        offence.setSequenceNumber(SEQUENCE_NUMBER);
        offence.setArrestDate(ARREST_DATE);
        offence.setWording(WORDING);
        offence.setCpr(createCpr());

        return offence;
    }

    private Hearing createHearing() {
        final Hearing hearing = new Hearing(COURT_ID, COURT_NAME, COURT_ROOM, TYPE_OF_COURT, HEARING_DATE);
        return hearing;
    }

    private CPRDetails createCpr() {
        final DefendantOffenderDetails defendantOffenderDetails = new DefendantOffenderDetails(CPR_YEAR, CPR_OU, CPR_NUMBER, CPR_CHECK_DIGIT);
        final CPRDetails cpr = new CPRDetails(defendantOffenderDetails);
        return cpr;
    }

    private void removeDefendants() {
        defendantLegacyRepository.remove(defendantOne);
        defendantLegacyRepository.remove(defendantTwo);
    }

}
