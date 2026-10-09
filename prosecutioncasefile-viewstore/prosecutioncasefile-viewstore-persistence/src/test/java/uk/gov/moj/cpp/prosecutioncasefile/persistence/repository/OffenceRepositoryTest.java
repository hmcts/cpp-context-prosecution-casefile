package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OffenceRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private OffenceRepository offenceRepository;
    private DefendantRepository defendantRepository;

    @BeforeEach
    public void createRepositories() {
        offenceRepository = new OffenceRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(offenceRepository);
        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);
    }

    @Test
    void shouldSaveAndFindOffence() {
        final DefendantDetails defendant = saveDefendant();
        final OffenceDetails offence = createOffence(OFFENCE_ID, defendant);

        offenceRepository.save(offence);

        final OffenceDetails found = offenceRepository.findBy(OFFENCE_ID);
        assertThat(found, notNullValue());
        assertThat(found.getOffenceId(), is(OFFENCE_ID));
        assertThat(found.getOffenceCode(), is("RT88191"));
        assertThat(found.getOffenceWording(), is("Speeding offence"));
    }

    @Test
    void shouldRemoveOffence() {
        final DefendantDetails defendant = saveDefendant();
        final OffenceDetails offence = createOffence(OFFENCE_ID, defendant);
        offenceRepository.save(offence);

        offenceRepository.remove(offence);

        assertThat(offenceRepository.findBy(OFFENCE_ID), nullValue());
    }

    private DefendantDetails saveDefendant() {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails(
                "Mr", "John", "Smith", null, null, null, null);
        final DefendantDetails defendant = new DefendantDetails(
                DEFENDANT_ID, "ASN123", null, null, null, null, null, null,
                null, null, null, null,
                personalInformation, null, new HashSet<>(), new ArrayList<>(), null, null);
        return defendantRepository.save(defendant);
    }

    private OffenceDetails createOffence(final UUID offenceId, final DefendantDetails defendant) {
        final OffenceDetails offence = new OffenceDetails(
                offenceId, null, null, null, null,
                LocalDate.now(), "RT88191", LocalDate.now(), null,
                1, "London", 1, "Speeding offence", null,
                null, null, null, null, null);
        offence.setDefendant(defendant);
        return offence;
    }
}
