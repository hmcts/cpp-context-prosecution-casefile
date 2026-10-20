package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.NoResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CaseDetailsRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID FIRST_DEFENDANT_CASE_ID = UUID.randomUUID();
    private static final String PROSECUTOR_CASE_REFERENCE = "18CPS1234567";
    private static final String PROSECUTOR_CASE_REFERENCE2 = "18CPS7654321";
    private static final String INVALID_PROSECUTOR_CASE_REFERENCE = "INVALID_REFERENCE";
    private static final String PROSECUTOR_INFORMANT = "Test Informant";
    private static final String PROSECUTOR_AUTHORITY = "CPS";
    private static final String ORIGINATING_ORGANISATION = "Test Organisation";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CaseDetailsRepository caseDetailsRepository;

    @BeforeEach
    public void createRepository() {
        caseDetailsRepository = new CaseDetailsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseDetailsRepository);
    }

    @Test
    void shouldFindCaseDetailsByProsecutionCaseReference() {
        caseDetailsRepository.save(createFirstDefendantCaseDetails());

        final CaseDetails caseDetails = caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE);

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getProsecutionCaseReference(), is(PROSECUTOR_CASE_REFERENCE));
        assertThat(caseDetails.getProsecutorInformant(), is(PROSECUTOR_INFORMANT));
        assertThat(caseDetails.getProsecutionAuthority(), is(PROSECUTOR_AUTHORITY));
        assertThat(caseDetails.getOriginatingOrganisation(), is(ORIGINATING_ORGANISATION));
    }

    @Test
    void shouldConstructCaseDetails() {
        final CaseDetails savedCase = caseDetailsRepository.save(createFirstDefendantCaseDetails());

        assertThat(savedCase, notNullValue());
        assertThat(savedCase.getCaseId(), is(CASE_ID));
        assertThat(savedCase.getProsecutionCaseReference(), is(PROSECUTOR_CASE_REFERENCE));
    }

    @Test
    void shouldFindAllCaseDetailsByProsecutionCaseReferences() {
        caseDetailsRepository.save(createFirstDefendantCaseDetails());
        caseDetailsRepository.save(createSecondDefendantCaseDetails());

        final List<CaseDetails> results = caseDetailsRepository.findAllCaseDetailsByProsecutionCaseReferences(
                asList(PROSECUTOR_CASE_REFERENCE, PROSECUTOR_CASE_REFERENCE2));

        assertThat(results.size(), is(2));
    }

    @Test
    void shouldThrowException_whenGivenProsecutionCaseReference_notExist() {
        assertThrows(NoResultException.class, () ->
                caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE));
    }

    private CaseDetails createFirstDefendantCaseDetails() {
        return new CaseDetails(
                CASE_ID,
                PROSECUTOR_CASE_REFERENCE,
                PROSECUTOR_INFORMANT,
                PROSECUTOR_AUTHORITY,
                ORIGINATING_ORGANISATION,
                emptySet(),
                null);
    }

    private CaseDetails createSecondDefendantCaseDetails() {
        return new CaseDetails(
                FIRST_DEFENDANT_CASE_ID,
                PROSECUTOR_CASE_REFERENCE2,
                PROSECUTOR_INFORMANT,
                PROSECUTOR_AUTHORITY,
                ORIGINATING_ORGANISATION,
                emptySet(),
                null);
    }
}
