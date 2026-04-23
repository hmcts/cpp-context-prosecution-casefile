package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class BusinessValidationErrorCaseDetailsRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public BusinessValidationErrorCaseDetails findBy(final UUID id) {
        return entityManager.find(BusinessValidationErrorCaseDetails.class, id);
    }

    public List<BusinessValidationErrorCaseDetails> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT e FROM BusinessValidationErrorCaseDetails e WHERE e.caseId = :caseId",
                        BusinessValidationErrorCaseDetails.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public void deleteByCaseId(final UUID caseId) {
        entityManager.createQuery("DELETE FROM BusinessValidationErrorCaseDetails e WHERE e.caseId = :caseId")
                .setParameter("caseId", caseId)
                .executeUpdate();
    }

    public BusinessValidationErrorCaseDetails save(final BusinessValidationErrorCaseDetails entity) {
        return entityManager.merge(entity);
    }
}
