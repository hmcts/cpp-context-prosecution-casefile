package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter.filterParameterBuilder;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorSummary;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.OrderByField;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationResult;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BusinessValidationErrorSummaryRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private BusinessValidationErrorSummaryRepository repository;

    @BeforeEach
    public void createRepository() {
        repository = new BusinessValidationErrorSummaryRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldFindSummaryById() {
        final UUID caseId = randomUUID();
        saveSummary(caseId, "London", "Summary", "URN001", LocalDate.now());

        final BusinessValidationErrorSummary found = repository.findBy(caseId);

        assertThat(found, notNullValue());
        assertThat(found.getCourtLocation(), is("London"));
    }

    @Test
    void shouldReturnAllResultsWhenFilterIsEmpty() {
        saveSummary(randomUUID(), "London", "Summary", "URN001", LocalDate.now());
        saveSummary(randomUUID(), "Manchester", "Crown", "URN002", LocalDate.now());

        final FilterParameter filter = filterParameterBuilder().build();
        final PaginationParameter pagination = new PaginationParameter(10, 1, OrderByField.HEARING_DATE, SortOrder.ASC);

        final PaginationResult<BusinessValidationErrorSummary> result = repository.fetchFilteredCaseErrorSummary(filter, pagination);

        assertThat(result.getTotalResultCount() >= 2L, is(true));
    }

    @Test
    void shouldFilterByCourtLocation() {
        saveSummary(randomUUID(), "London", "Summary", "URN100", LocalDate.now());
        saveSummary(randomUUID(), "Manchester", "Crown", "URN101", LocalDate.now());

        final FilterParameter filter = filterParameterBuilder().withCourt("London").build();
        final PaginationParameter pagination = new PaginationParameter(10, 1, OrderByField.HEARING_DATE, SortOrder.DESC);

        final PaginationResult<BusinessValidationErrorSummary> result = repository.fetchFilteredCaseErrorSummary(filter, pagination);

        result.getResult().forEach(r -> assertThat(r.getCourtLocation(), is("London")));
    }

    @Test
    void shouldFilterByCourtLocationAndCaseType() {
        final UUID matchingId = randomUUID();
        saveSummary(matchingId, "London", "Summary", "URN200", LocalDate.now());
        saveSummary(randomUUID(), "London", "Crown", "URN201", LocalDate.now());
        saveSummary(randomUUID(), "Manchester", "Summary", "URN202", LocalDate.now());

        final FilterParameter filter = filterParameterBuilder().withCourt("London").withCaseType("Summary").build();
        final PaginationParameter pagination = new PaginationParameter(10, 1, OrderByField.HEARING_DATE, SortOrder.ASC);

        final PaginationResult<BusinessValidationErrorSummary> result = repository.fetchFilteredCaseErrorSummary(filter, pagination);

        assertThat(result.getResult().stream().anyMatch(r -> r.getCaseId().equals(matchingId)), is(true));
        result.getResult().forEach(r -> {
            assertThat(r.getCourtLocation(), is("London"));
            assertThat(r.getCaseType(), is("Summary"));
        });
    }

    @Test
    void shouldFilterByUrn() {
        saveSummary(randomUUID(), "London", "Summary", "UNIQUE-URN-300", LocalDate.now());
        saveSummary(randomUUID(), "Manchester", "Crown", "OTHER-URN-301", LocalDate.now());

        final FilterParameter filter = filterParameterBuilder().withUrn("UNIQUE-URN-300").build();
        final PaginationParameter pagination = new PaginationParameter(10, 1, OrderByField.HEARING_DATE, SortOrder.ASC);

        final PaginationResult<BusinessValidationErrorSummary> result = repository.fetchFilteredCaseErrorSummary(filter, pagination);

        assertThat(result.getResult().size(), is(1));
        assertThat(result.getResult().get(0).getUrn(), is("UNIQUE-URN-300"));
    }

    @Test
    void shouldFilterByHearingDateRange() {
        saveSummary(randomUUID(), "London", "Summary", "URN400", LocalDate.of(2024, 1, 10));
        saveSummary(randomUUID(), "London", "Summary", "URN401", LocalDate.of(2024, 6, 15));
        saveSummary(randomUUID(), "London", "Summary", "URN402", LocalDate.of(2024, 12, 20));

        final FilterParameter filter = filterParameterBuilder()
                .withHearingDateFrom("2024-06-01")
                .withHearingDateTo("2024-06-30")
                .build();
        final PaginationParameter pagination = new PaginationParameter(10, 1, OrderByField.HEARING_DATE, SortOrder.ASC);

        final PaginationResult<BusinessValidationErrorSummary> result = repository.fetchFilteredCaseErrorSummary(filter, pagination);

        assertThat(result.getResult().stream().anyMatch(r -> r.getUrn().equals("URN401")), is(true));
        result.getResult().forEach(r -> {
            assertThat(r.getDefendantHearingDate().isBefore(LocalDate.of(2024, 7, 1)), is(true));
            assertThat(r.getDefendantHearingDate().isAfter(LocalDate.of(2024, 5, 31)), is(true));
        });
    }

    private void saveSummary(final UUID caseId, final String courtLocation, final String caseType,
            final String urn, final LocalDate hearingDate) {
        final BusinessValidationErrorSummary summary = new BusinessValidationErrorSummary();
        summary.setCaseId(caseId);
        summary.setCourtLocation(courtLocation);
        summary.setCaseType(caseType);
        summary.setUrn(urn);
        summary.setDefendantHearingDate(hearingDate);
        hibernateTestEntityManagerProvider.getEntityManager().persist(summary);
        hibernateTestEntityManagerProvider.getEntityManager().flush();
    }
}
