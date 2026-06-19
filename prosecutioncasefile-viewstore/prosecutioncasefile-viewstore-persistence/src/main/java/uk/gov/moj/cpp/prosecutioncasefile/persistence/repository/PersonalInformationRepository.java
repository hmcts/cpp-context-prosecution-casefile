package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class PersonalInformationRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public PersonalInformationDetails findBy(final UUID id) {
        return entityManager.find(PersonalInformationDetails.class, id.toString());
    }

    public PersonalInformationDetails save(final PersonalInformationDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final PersonalInformationDetails entity) {
        final PersonalInformationDetails managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
