package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BusinessValidationErrorRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final UUID CASE_1 = randomUUID();
    private static final UUID CASE_2 = randomUUID();
    private static final UUID CASE_3 = randomUUID();
    private static final UUID CASE_4 = randomUUID();
    private static final UUID CASE_5 = randomUUID();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @BeforeEach
    public void createRepository() {
        businessValidationErrorRepository = new BusinessValidationErrorRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(businessValidationErrorRepository);
    }

    @Test
    void shouldReturnZeroIfThereIsNoOutstandingErrors() {
        final Long count = businessValidationErrorRepository.countOfCasesWithOutstandingErrors(empty(), empty());

        assertThat(count, is(0L));
    }

    @Test
    void shouldReturnAllOutstandingErrorsCountsWithoutParameters() {
        insert10ErrorsFor(CASE_1, "London", "Summary");
        insert10ErrorsFor(CASE_2, "London", "Indictment");
        insert10ErrorsFor(CASE_3, "Manchester", "Summary");
        insert10ErrorsFor(CASE_4, "Manchester", "Indictment");
        insert10ErrorsFor(CASE_5, "Birmingham", "Crown");

        final Long count = businessValidationErrorRepository.countOfCasesWithOutstandingErrors(empty(), empty());

        assertThat(count, is(5L));
    }

    @Test
    void shouldReturnAllOutStandingErrorsCountsWithCourtLocationOnly() {
        insert10ErrorsFor(CASE_1, "London", "Summary");
        insert10ErrorsFor(CASE_2, "London", "Indictment");
        insert10ErrorsFor(CASE_3, "Manchester", "Summary");
        insert10ErrorsFor(CASE_4, "Manchester", "Indictment");
        insert10ErrorsFor(CASE_5, "Birmingham", "Crown");

        final Long count = businessValidationErrorRepository.countOfCasesWithOutstandingErrors(of("London"), empty());

        assertThat(count, is(2L));
    }

    @Test
    void shouldReturnAllOutStandingErrorsCountsWithCourtLocationCaseType() {
        insert10ErrorsFor(CASE_1, "London", "Summary");
        insert10ErrorsFor(CASE_2, "London", "Indictment");
        insert10ErrorsFor(CASE_3, "Manchester", "Summary");
        insert10ErrorsFor(CASE_4, "Manchester", "Indictment");
        insert10ErrorsFor(CASE_5, "Birmingham", "Crown");

        final Long count = businessValidationErrorRepository.countOfCasesWithOutstandingErrors(of("London"), of("Summary"));

        assertThat(count, is(1L));
    }

    @Test
    void shouldFindBusinessValidationErrors() {
        final UUID errorId = randomUUID();
        final UUID defendantId = randomUUID();
        final BusinessValidationErrorDetails error = createError(errorId, CASE_1, defendantId, "Manchester", "Summary");

        businessValidationErrorRepository.save(error);

        final List<BusinessValidationErrorDetails> byCaseId = businessValidationErrorRepository.findByCaseId(CASE_1);
        assertThat(byCaseId.size(), is(1));

        final List<BusinessValidationErrorDetails> byDefendantId = businessValidationErrorRepository.findByDefendantId(defendantId);
        assertThat(byDefendantId.size(), is(1));

        final BusinessValidationErrorDetails found = businessValidationErrorRepository.findBy(errorId);
        assertThat(found, notNullValue());
        assertThat(found.getCourtLocation(), is("Manchester"));

        found.setCourtLocation("Leeds");
        businessValidationErrorRepository.save(found);

        final BusinessValidationErrorDetails updated = businessValidationErrorRepository.findBy(errorId);
        assertThat(updated.getCourtLocation(), is("Leeds"));

        businessValidationErrorRepository.remove(updated);
        assertThat(businessValidationErrorRepository.findBy(errorId), is(nullValue()));
    }

    @Test
    void shouldDeleteErrorWithCaseIdAndNullDefendantId() {
        final UUID errorId1 = randomUUID();
        final UUID errorId2 = randomUUID();
        businessValidationErrorRepository.save(createError(errorId1, CASE_1, null, "London", "Summary"));
        businessValidationErrorRepository.save(createError(errorId2, CASE_1, randomUUID(), "London", "Summary"));

        businessValidationErrorRepository.deleteByCaseIdAndDefendantIdIsNull(CASE_1);
        hibernateTestEntityManagerProvider.getEntityManager().clear();

        assertThat(businessValidationErrorRepository.findBy(errorId1), is(nullValue()));
        assertThat(businessValidationErrorRepository.findBy(errorId2), notNullValue());
    }

    @Test
    void shouldDeleteErrorWithCaseIdAndDefendantFirstNameLastName() {
        final UUID errorId = randomUUID();
        final BusinessValidationErrorDetails error = createError(errorId, CASE_1, null, "London", "Summary");
        error.setFirstName("John");
        error.setLastName("Smith");
        businessValidationErrorRepository.save(error);

        businessValidationErrorRepository.deleteByCaseIdAndFirstNameAndLastName(CASE_1, "John", "Smith");
        hibernateTestEntityManagerProvider.getEntityManager().clear();

        assertThat(businessValidationErrorRepository.findBy(errorId), is(nullValue()));
    }

    private void insert10ErrorsFor(final UUID caseId, final String courtLocation, final String caseType) {
        for (int i = 0; i < 10; i++) {
            businessValidationErrorRepository.save(createError(randomUUID(), caseId, randomUUID(), courtLocation, caseType));
        }
    }

    private BusinessValidationErrorDetails createError(final UUID id, final UUID caseId, final UUID defendantId,
            final String courtLocation, final String caseType) {
        return new BusinessValidationErrorDetails(
                id, "errorValue", "fieldId", "displayName",
                caseId, defendantId, "fieldName", "courtName",
                courtLocation, caseType, "urn", "bailStatus",
                "firstName", "lastName", "organisationName",
                LocalDate.now(), LocalDate.now().plusDays(1), LocalDate.now().minusYears(30));
    }
}
