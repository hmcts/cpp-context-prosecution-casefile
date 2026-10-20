package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseDetailsRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public CaseDetails findBy(final UUID id) {
        return entityManager.find(CaseDetails.class, id);
    }

    public CaseDetails findCaseDetailsByProsecutionCaseReference(
            final String prosecutionCaseReference) {
        return entityManager.createQuery(
                        "SELECT cd FROM CaseDetails cd WHERE cd.prosecutionCaseReference = :prosecutionCaseReference",
                        CaseDetails.class)
                .setParameter("prosecutionCaseReference", prosecutionCaseReference)
                .getSingleResult();
    }

    public List<CaseDetails> findAllCaseDetailsByProsecutionCaseReferences(
            final Collection<String> prosecutionCaseReferences) {
        return entityManager.createQuery(
                        "SELECT cd FROM CaseDetails cd WHERE cd.prosecutionCaseReference IN :prosecutionCaseReferences",
                        CaseDetails.class)
                .setParameter("prosecutionCaseReferences", prosecutionCaseReferences)
                .getResultList();
    }

    public CaseDetails save(final CaseDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseDetails entity) {
        final CaseDetails managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
