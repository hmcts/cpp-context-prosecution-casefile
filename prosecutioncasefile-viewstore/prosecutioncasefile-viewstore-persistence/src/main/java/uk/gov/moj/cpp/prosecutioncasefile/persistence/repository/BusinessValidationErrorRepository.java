package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ApplicationScoped
public class BusinessValidationErrorRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public BusinessValidationErrorDetails findBy(final UUID id) {
        return entityManager.find(BusinessValidationErrorDetails.class, id);
    }

    public List<BusinessValidationErrorDetails> findByCaseId(final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT e FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId",
                        BusinessValidationErrorDetails.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }

    public List<BusinessValidationErrorDetails> findByDefendantId(final UUID defendantId) {
        return entityManager.createQuery(
                        "SELECT e FROM BusinessValidationErrorDetails e WHERE e.defendantId = :defendantId",
                        BusinessValidationErrorDetails.class)
                .setParameter("defendantId", defendantId)
                .getResultList();
    }

    public void deleteByCaseId(final UUID caseId) {
        entityManager.createQuery("DELETE FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId")
                .setParameter("caseId", caseId)
                .executeUpdate();
    }

    public void deleteByCaseIdAndDefendantIdIsNull(final UUID caseId) {
        entityManager.createQuery(
                        "DELETE FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId AND e.defendantId IS NULL")
                .setParameter("caseId", caseId)
                .executeUpdate();
    }

    public void deleteByDefendantId(final UUID defendantId) {
        entityManager.createQuery(
                        "DELETE FROM BusinessValidationErrorDetails e WHERE e.defendantId = :defendantId")
                .setParameter("defendantId", defendantId)
                .executeUpdate();
    }

    public void deleteByCaseIdAndDefendantId(final UUID caseId, final UUID defendantId) {
        entityManager.createQuery(
                        "DELETE FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId AND e.defendantId = :defendantId")
                .setParameter("caseId", caseId)
                .setParameter("defendantId", defendantId)
                .executeUpdate();
    }

    public void deleteByCaseIdAndFirstNameAndLastName(final UUID caseId, final String firstName,
            final String lastName) {
        entityManager.createQuery(
                        "DELETE FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId AND e.firstName = :firstName AND e.lastName = :lastName")
                .setParameter("caseId", caseId)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .executeUpdate();
    }

    public void deleteByCaseIdAndOrganisationName(final UUID caseId, final String organisationName) {
        entityManager.createQuery(
                        "DELETE FROM BusinessValidationErrorDetails e WHERE e.caseId = :caseId AND e.organisationName = :organisationName")
                .setParameter("caseId", caseId)
                .setParameter("organisationName", organisationName)
                .executeUpdate();
    }

    public Long countOfCasesWithOutstandingErrors(final Optional<String> courtLocation,
            final Optional<String> caseType) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> q = cb.createQuery(Long.class);
        final Root<BusinessValidationErrorDetails> e = q.from(BusinessValidationErrorDetails.class);
        final List<Predicate> predicates = new ArrayList<>();
        courtLocation.ifPresent(cl ->
                predicates.add(cb.equal(cb.lower(e.get("courtLocation")), cl.toLowerCase())));
        caseType.ifPresent(ct ->
                predicates.add(cb.equal(cb.lower(e.get("caseType")), ct.toLowerCase())));
        q.select(cb.countDistinct(e.get("caseId")));
        if (!predicates.isEmpty()) {
            q.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        return entityManager.createQuery(q).getSingleResult();
    }

    public List<BusinessValidationErrorDetails> fetchAllCaseErrorDetailsByCaseIds(
            final Collection<UUID> caseIds, final PaginationParameter paginationParameter) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<BusinessValidationErrorDetails> q =
                cb.createQuery(BusinessValidationErrorDetails.class);
        final Root<BusinessValidationErrorDetails> e = q.from(BusinessValidationErrorDetails.class);
        q.where(e.get("caseId").in(caseIds));
        final String orderByField = paginationParameter.getSortField().getFieldName();
        if (paginationParameter.getSortOrder().toString().equalsIgnoreCase(SortOrder.ASC.toString())) {
            q.orderBy(cb.asc(e.get(orderByField)));
        } else {
            q.orderBy(cb.desc(e.get(orderByField)));
        }
        return entityManager.createQuery(q).getResultList();
    }

    public BusinessValidationErrorDetails save(final BusinessValidationErrorDetails entity) {
        return entityManager.merge(entity);
    }

    public void remove(final BusinessValidationErrorDetails entity) {
        final BusinessValidationErrorDetails managed =
                entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
