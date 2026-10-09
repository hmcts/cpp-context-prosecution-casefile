package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PersonalInformationRespositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

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
        saveDefendantWithPersonalInformation("John", "Smith");

        final PersonalInformationDetails found = personalInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));

        assertThat(found, notNullValue());
        assertThat(found.getFirstName(), is("John"));
        assertThat(found.getLastName(), is("Smith"));
    }

    @Test
    void shouldSavePersonalInformation() {
        saveDefendantWithPersonalInformation("John", "Smith");

        final PersonalInformationDetails found = personalInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));
        assertThat(found, notNullValue());

        found.setFirstName("Jane");
        personalInformationRepository.save(found);

        final PersonalInformationDetails updated = personalInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));
        assertThat(updated.getFirstName(), is("Jane"));
    }

    private void saveDefendantWithPersonalInformation(final String firstName, final String lastName) {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails(
                "Mr", firstName, lastName, null, null, null, null);
        final DefendantDetails defendant = new DefendantDetails(
                DEFENDANT_ID, "ASN123", null, null, null, null, null, null,
                null, null, null, null,
                personalInformation, null, new HashSet<>(), new ArrayList<>(), null, null);
        defendantRepository.save(defendant);
    }
}
