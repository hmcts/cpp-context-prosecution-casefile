package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DefendantRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public DefendantDetails findBy(final String id) {
        return entityManager.find(DefendantDetails.class, id);
    }

    public List<DefendantDetails> findByDefendantId(final String defendantId) {
        return entityManager.createQuery(
                        "SELECT d FROM DefendantDetails d WHERE d.defendantId = :defendantId",
                        DefendantDetails.class)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public DefendantDetails save(final DefendantDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final DefendantDetails entity) {
        final DefendantDetails managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
