package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DefendantRepository defendantRepository;

    @BeforeEach
    public void createRepository() {
        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);
    }

    @Test
    void shouldSaveAndFindDefendant() {
        final DefendantDetails defendant = createDefendant(DEFENDANT_ID);

        defendantRepository.save(defendant);

        final DefendantDetails found = defendantRepository.findBy(DEFENDANT_ID);
        assertThat(found, notNullValue());
        assertThat(found.getDefendantId(), is(DEFENDANT_ID));
        assertThat(found.getAsn(), is("ASN123"));
    }

    @Test
    void shouldFindByDefendantId() {
        final String anotherDefendantId = UUID.randomUUID().toString();
        defendantRepository.save(createDefendant(DEFENDANT_ID));
        defendantRepository.save(createDefendant(anotherDefendantId));

        final List<DefendantDetails> results = defendantRepository.findByDefendantId(DEFENDANT_ID);

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getDefendantId(), is(DEFENDANT_ID));
    }

    @Test
    void shouldRemoveDefendant() {
        final DefendantDetails defendant = createDefendant(DEFENDANT_ID);
        defendantRepository.save(defendant);

        defendantRepository.remove(defendant);

        assertThat(defendantRepository.findBy(DEFENDANT_ID), nullValue());
    }

    private DefendantDetails createDefendant(final String defendantId) {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails(
                "Mr", "John", "Smith", "Engineer", 100, null, null);
        return new DefendantDetails(
                defendantId, "ASN123", null, null, null, null, null, null,
                null, null, null, null,
                personalInformation, null, new HashSet<>(), new ArrayList<>(), null, null);
    }
}
