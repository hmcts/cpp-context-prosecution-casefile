package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BusinessValidationErrorCaseDetailsRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private BusinessValidationErrorCaseDetailsRepository repository;

    @BeforeEach
    public void createRepository() {
        repository = new BusinessValidationErrorCaseDetailsRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndFindByCaseId() {
        final UUID caseId = randomUUID();
        repository.save(new BusinessValidationErrorCaseDetails(caseId, "{\"detail\": \"value\"}"));

        final BusinessValidationErrorCaseDetails found = repository.findBy(caseId);

        assertThat(found, notNullValue());
        assertThat(found.getCaseDetails(), is("{\"detail\": \"value\"}"));
    }

    @Test
    void shouldFindListByCaseId() {
        final UUID caseId = randomUUID();
        repository.save(new BusinessValidationErrorCaseDetails(caseId, "{}"));

        final List<BusinessValidationErrorCaseDetails> results = repository.findByCaseId(caseId);

        assertThat(results.size(), is(1));
        assertThat(results.get(0).getCaseId(), is(caseId));
    }

    @Test
    void shouldDeleteByCaseId() {
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        repository.save(new BusinessValidationErrorCaseDetails(caseId1, "{}"));
        repository.save(new BusinessValidationErrorCaseDetails(caseId2, "{}"));

        repository.deleteByCaseId(caseId1);
        hibernateTestEntityManagerProvider.getEntityManager().clear();

        assertThat(repository.findBy(caseId1), is(nullValue()));
        assertThat(repository.findBy(caseId2), notNullValue());
    }

    @Test
    void shouldUpdateCaseDetailsOnSave() {
        final UUID caseId = randomUUID();
        repository.save(new BusinessValidationErrorCaseDetails(caseId, "original"));

        final BusinessValidationErrorCaseDetails found = repository.findBy(caseId);
        found.setCaseDetails("updated");
        repository.save(found);

        assertThat(repository.findBy(caseId).getCaseDetails(), is("updated"));
    }
}
