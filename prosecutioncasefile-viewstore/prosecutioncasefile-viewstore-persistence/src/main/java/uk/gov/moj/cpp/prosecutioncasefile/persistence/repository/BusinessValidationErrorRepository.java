package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails_;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.deltaspike.data.api.AbstractFullEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class BusinessValidationErrorRepository
        extends AbstractFullEntityRepository<BusinessValidationErrorDetails, UUID> {

    public abstract List<BusinessValidationErrorDetails> findByCaseId(final UUID caseId);

    public abstract List<BusinessValidationErrorDetails> findByDefendantId(final UUID defendantId);

    public abstract void deleteByCaseId(final UUID caseId);

    public abstract void deleteByCaseIdAndDefendantIdIsNull(final UUID caseId);

    public abstract void deleteByDefendantId(UUID defendantId);

    public abstract void deleteByCaseIdAndDefendantId(final UUID caseId, final UUID defendantId);

    public abstract void deleteByCaseIdAndFirstNameAndLastName(final UUID caseId, final String firstName, final String lastName);

    public abstract void deleteByCaseIdAndOrganisationName(final UUID caseId, final String organisationName);


    public Long countOfCasesWithOutstandingErrors(
            final Optional<String> courtLocation, final Optional<String> caseType) {
        return criteria()
                .select(Long.class, countDistinct(BusinessValidationErrorDetails_.caseId))
                .eqIgnoreCase(BusinessValidationErrorDetails_.courtLocation, courtLocation.orElse(null))
                .eqIgnoreCase(BusinessValidationErrorDetails_.caseType, caseType.orElse(null))
                .getSingleResult();
    }

    @Query(value = "SELECT e FROM BusinessValidationErrorDetails e WHERE e.caseId in (:caseIds)")
    public abstract QueryResult<BusinessValidationErrorDetails> findAllCaseErrorDetailsByCaseIds(
            @QueryParam("caseIds") final Collection<UUID> caseIds);

    public List<BusinessValidationErrorDetails> fetchAllCaseErrorDetailsByCaseIds(
            final Collection<UUID> caseIds, final PaginationParameter paginationParameter) {

        final CriteriaBuilder criteriaBuilder = entityManager().getCriteriaBuilder();

        final CriteriaQuery<BusinessValidationErrorDetails> criteriaQuery =
                criteriaBuilder.createQuery(BusinessValidationErrorDetails.class);

        final Root<BusinessValidationErrorDetails> e =
                criteriaQuery.from(BusinessValidationErrorDetails.class);

        final Predicate predicate = e.get(BusinessValidationErrorDetails_.caseId).in(caseIds);
        criteriaQuery.where(predicate);

        final String orderByField = paginationParameter.getSortField().getFieldName();
        if (paginationParameter.getSortOrder().toString().equalsIgnoreCase(SortOrder.ASC.toString())) {
            criteriaQuery.orderBy(criteriaBuilder.asc(e.get(orderByField)));
        } else {
            criteriaQuery.orderBy(criteriaBuilder.desc(e.get(orderByField)));
        }

        return entityManager().createQuery(criteriaQuery).getResultList();
    }
}
