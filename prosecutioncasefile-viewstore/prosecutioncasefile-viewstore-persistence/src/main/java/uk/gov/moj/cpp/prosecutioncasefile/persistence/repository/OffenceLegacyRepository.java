package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceLegacy;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class OffenceLegacyRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public OffenceLegacy findBy(final UUID id) {
        return entityManager.find(OffenceLegacy.class, id);
    }

    public OffenceLegacy save(final OffenceLegacy entity) {
        return entityManager.merge(entity);
    }

    public void remove(final OffenceLegacy entity) {
        final OffenceLegacy managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
