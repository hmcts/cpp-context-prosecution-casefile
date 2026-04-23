package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ResolvedCases;

import java.time.LocalDate;
import java.util.ArrayList;
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
public class ResolvedCasesRepository {

    @PersistenceContext(unitName = "prosecutioncasefile-persistence-unit")
    EntityManager entityManager;

    public ResolvedCases findBy(final UUID id) {
        return entityManager.find(ResolvedCases.class, id);
    }

    public Long countOfCasesFixedByDate(final LocalDate resolutionDate,
            final Optional<String> region, final Optional<String> courtLocation,
            final Optional<String> caseType) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> q = cb.createQuery(Long.class);
        final Root<ResolvedCases> e = q.from(ResolvedCases.class);
        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(e.get("resolutionDate"), resolutionDate));
        region.ifPresent(r ->
                predicates.add(cb.equal(cb.lower(e.get("region")), r.toLowerCase())));
        courtLocation.ifPresent(cl ->
                predicates.add(cb.equal(cb.lower(e.get("courtLocation")), cl.toLowerCase())));
        caseType.ifPresent(ct ->
                predicates.add(cb.equal(cb.lower(e.get("caseType")), ct.toLowerCase())));
        q.select(cb.countDistinct(e.get("caseId")));
        q.where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(q).getSingleResult();
    }

    public ResolvedCases save(final ResolvedCases entity) {
        return entityManager.merge(entity);
    }

    public void remove(final ResolvedCases entity) {
        final ResolvedCases managed = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managed);
    }
}
