package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jakarta.persistence.EntityManager;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PersonalInformationRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private PersonalInformationRepository personalInformationRepository;
    private DefendantRepository defendantRepository;

    @BeforeEach
    public void createRepositories() {
        personalInformationRepository = new PersonalInformationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(personalInformationRepository);
        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);
    }

    @Test
    void shouldFindPersonalInformationById() {
        final UUID defendantId = UUID.randomUUID();
        saveDefendantWithPersonalInformation(defendantId, "John", "Smith");

        final PersonalInformationDetails found = personalInformationRepository.findBy(defendantId);

        assertThat(found, notNullValue());
        assertThat(found.getFirstName(), is("John"));
        assertThat(found.getLastName(), is("Smith"));
    }

    @Test
    void shouldSavePersonalInformation() {
        final UUID defendantId = UUID.randomUUID();
        saveDefendantWithPersonalInformation(defendantId, "John", "Smith");

        final PersonalInformationDetails found = personalInformationRepository.findBy(defendantId);
        assertThat(found, notNullValue());

        found.setFirstName("Jane");
        personalInformationRepository.save(found);

        final PersonalInformationDetails updated = personalInformationRepository.findBy(defendantId);
        assertThat(updated.getFirstName(), is("Jane"));
    }

    @Test
    void shouldRemovePersonalInformation() {
        final UUID defendantId = UUID.randomUUID();
        saveDefendantWithPersonalInformation(defendantId, "John", "Smith");

        final PersonalInformationDetails found = personalInformationRepository.findBy(defendantId);
        assertThat(found, notNullValue());

        personalInformationRepository.remove(found);
        hibernateTestEntityManagerProvider.getEntityManager().clear();

        assertThat(personalInformationRepository.findBy(defendantId), is(nullValue()));
    }

    @Test
    void shouldRemoveDetachedPersonalInformation() {
        final UUID id = UUID.randomUUID();
        final PersonalInformationDetails personalInfo = new PersonalInformationDetails(
                "Mr", "John", "Smith", null, null, null, null);
        personalInfo.setPersonalInformationId(id.toString());
        personalInformationRepository.save(personalInfo);

        final EntityManager em = hibernateTestEntityManagerProvider.getEntityManager();
        em.flush();
        em.clear();

        final PersonalInformationDetails detached = personalInformationRepository.findBy(id);
        em.clear();

        personalInformationRepository.remove(detached);
        em.flush();
        em.clear();

        assertThat(personalInformationRepository.findBy(id), is(nullValue()));
    }

    private void saveDefendantWithPersonalInformation(final UUID defendantId, final String firstName, final String lastName) {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails(
                "Mr", firstName, lastName, null, null, null, null);
        final DefendantDetails defendant = new DefendantDetails(
                defendantId.toString(), "ASN123", null, null, null, null, null, null,
                null, null, null, null,
                personalInformation, null, new HashSet<>(), new ArrayList<>(), null, null);
        defendantRepository.save(defendant);
    }
}
