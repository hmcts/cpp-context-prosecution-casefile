package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SelfDefinedInformationRepositoryTest {

    private static final String PERSISTENCE_UNIT = "prosecutioncasefile-test-persistence-unit";

    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SelfDefinedInformationRepository selfDefinedInformationRepository;
    private DefendantRepository defendantRepository;

    @BeforeEach
    public void createRepositories() {
        selfDefinedInformationRepository = new SelfDefinedInformationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(selfDefinedInformationRepository);
        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);
    }

    @Test
    void shouldFindSelfDefinedInformationById() {
        saveDefendantWithSelfDefinedInformation("British", Gender.MALE);

        final SelfDefinedInformationDetails found = selfDefinedInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));

        assertThat(found, notNullValue());
        assertThat(found.getNationality(), is("British"));
        assertThat(found.getGender(), is(Gender.MALE));
    }

    @Test
    void shouldSaveSelfDefinedInformation() {
        saveDefendantWithSelfDefinedInformation("British", Gender.MALE);

        final SelfDefinedInformationDetails found = selfDefinedInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));
        assertThat(found, notNullValue());

        found.setNationality("French");
        selfDefinedInformationRepository.save(found);

        final SelfDefinedInformationDetails updated = selfDefinedInformationRepository.findBy(UUID.fromString(DEFENDANT_ID));
        assertThat(updated.getNationality(), is("French"));
    }

    private void saveDefendantWithSelfDefinedInformation(final String nationality, final Gender gender) {
        final PersonalInformationDetails personalInformation = new PersonalInformationDetails(
                "Mr", "John", "Smith", null, null, null, null);
        final SelfDefinedInformationDetails selfDefinedInformation = new SelfDefinedInformationDetails(
                null, LocalDate.of(1990, 5, 15), "White British", gender, nationality);
        final DefendantDetails defendant = new DefendantDetails(
                DEFENDANT_ID, "ASN123", null, null, null, null, null, null,
                null, null, null, null,
                personalInformation, selfDefinedInformation, new HashSet<>(), new ArrayList<>(), null, null);
        defendantRepository.save(defendant);
    }
}
