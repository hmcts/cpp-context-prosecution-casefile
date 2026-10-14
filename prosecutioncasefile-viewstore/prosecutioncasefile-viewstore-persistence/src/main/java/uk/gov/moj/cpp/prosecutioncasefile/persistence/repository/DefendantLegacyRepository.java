package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantLegacy;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DefendantLegacyRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public DefendantLegacy findBy(final UUID id) {
        return entityManager.find(DefendantLegacy.class, id);
    }

    public List<DefendantLegacy> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT d FROM DefendantLegacy d WHERE d.caseId = :caseId",
                        DefendantLegacy.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public DefendantLegacy save(final DefendantLegacy entity) {
        return entityManager.merge(entity);
    }

    public void remove(final DefendantLegacy entity) {
        final DefendantLegacy managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
