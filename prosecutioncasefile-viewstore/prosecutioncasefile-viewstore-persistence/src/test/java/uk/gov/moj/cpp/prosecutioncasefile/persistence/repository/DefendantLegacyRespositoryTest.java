package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantLegacyRespositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID SUSPECT_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String CASE_URN = "18/0000001A";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DefendantLegacyRepository defendantLegacyRepository;

    @BeforeEach
    public void createRepository() {
        defendantLegacyRepository = new DefendantLegacyRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantLegacyRepository);
    }

    @Test
    void shouldSaveAndFindDefendantLegacy() {
        final DefendantLegacy defendantLegacy = createDefendantLegacy(DEFENDANT_ID, CASE_ID);

        defendantLegacyRepository.save(defendantLegacy);

        final DefendantLegacy found = defendantLegacyRepository.findBy(DEFENDANT_ID);
        assertThat(found, notNullValue());
        assertThat(found.getDefendantId(), is(DEFENDANT_ID));
        assertThat(found.getSuspectId(), is(SUSPECT_ID));
        assertThat(found.getCaseId(), is(CASE_ID));
        assertThat(found.getCaseUrn(), is(CASE_URN));
    }

    @Test
    void shouldFindByCaseId() {
        final UUID anotherCaseId = UUID.randomUUID();
        defendantLegacyRepository.save(createDefendantLegacy(DEFENDANT_ID, CASE_ID));
        defendantLegacyRepository.save(createDefendantLegacy(UUID.randomUUID(), anotherCaseId));

        final List<DefendantLegacy> results = defendantLegacyRepository.findByCaseId(CASE_ID);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getDefendantId(), is(DEFENDANT_ID));
    }

    @Test
    void shouldRemoveDefendantLegacy() {
        final DefendantLegacy defendantLegacy = createDefendantLegacy(DEFENDANT_ID, CASE_ID);
        defendantLegacyRepository.save(defendantLegacy);

        defendantLegacyRepository.remove(defendantLegacy);

        assertThat(defendantLegacyRepository.findBy(DEFENDANT_ID), nullValue());
    }

    private DefendantLegacy createDefendantLegacy(final UUID defendantId, final UUID caseId) {
        return new DefendantLegacy(defendantId, SUSPECT_ID, "POL-001", UUID.randomUUID(), caseId, CASE_URN, new HashSet<>());
    }
}
