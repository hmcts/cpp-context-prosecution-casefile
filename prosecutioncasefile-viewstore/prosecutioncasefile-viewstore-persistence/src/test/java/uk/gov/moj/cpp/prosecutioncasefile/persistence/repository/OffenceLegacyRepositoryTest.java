package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceLegacy;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OffenceLegacyRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private OffenceLegacyRepository offenceLegacyRepository;
    private DefendantLegacyRepository defendantLegacyRepository;

    @BeforeEach
    public void createRepositories() {
        offenceLegacyRepository = new OffenceLegacyRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(offenceLegacyRepository);
        defendantLegacyRepository = new DefendantLegacyRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantLegacyRepository);
    }

    @Test
    void shouldSaveAndFindOffenceLegacy() {
        final DefendantLegacy defendant = new DefendantLegacy(DEFENDANT_ID, UUID.randomUUID(), null, UUID.randomUUID(), CASE_ID, "18/0000001A", new HashSet<>());
        defendantLegacyRepository.save(defendant);

        final OffenceLegacy offence = createOffenceLegacy(OFFENCE_ID, defendant);
        offenceLegacyRepository.save(offence);

        final OffenceLegacy found = offenceLegacyRepository.findBy(OFFENCE_ID);
        assertThat(found, notNullValue());
        assertThat(found.getOffenceId(), is(OFFENCE_ID));
        assertThat(found.getCode(), is("RT88191"));
        assertThat(found.getWording(), is("Speeding offence"));
    }

    @Test
    void shouldRemoveOffenceLegacy() {
        final DefendantLegacy defendant = new DefendantLegacy(DEFENDANT_ID, UUID.randomUUID(), null, UUID.randomUUID(), CASE_ID, "18/0000001A", new HashSet<>());
        defendantLegacyRepository.save(defendant);

        final OffenceLegacy offence = createOffenceLegacy(OFFENCE_ID, defendant);
        offenceLegacyRepository.save(offence);

        offenceLegacyRepository.remove(offence);

        assertThat(offenceLegacyRepository.findBy(OFFENCE_ID), nullValue());
    }

    private OffenceLegacy createOffenceLegacy(final UUID offenceId, final DefendantLegacy defendant) {
        final OffenceLegacy offence = new OffenceLegacy();
        offence.setOffenceId(offenceId);
        offence.setCode("RT88191");
        offence.setWording("Speeding offence");
        offence.setPlea("NOT_GUILTY");
        offence.setSequenceNumber(1);
        offence.setStartDate(LocalDate.now());
        offence.setDefendant(defendant);
        offence.setOrderIndex(1);
        return offence;
    }
}
