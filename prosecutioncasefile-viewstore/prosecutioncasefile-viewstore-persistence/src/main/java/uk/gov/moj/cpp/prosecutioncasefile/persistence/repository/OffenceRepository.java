package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class OffenceRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public OffenceDetails findBy(final UUID id) {
        return entityManager.find(OffenceDetails.class, id);
    }

    public OffenceDetails save(final OffenceDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final OffenceDetails entity) {
        final OffenceDetails managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
