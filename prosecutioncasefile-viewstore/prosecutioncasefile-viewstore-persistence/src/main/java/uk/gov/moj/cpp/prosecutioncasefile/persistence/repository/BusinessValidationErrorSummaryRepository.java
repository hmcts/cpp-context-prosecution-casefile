package uk.gov.moj.cpp.prosecutioncasefile.persistence.repository;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorSummary;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorSummary_;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationResult;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.deltaspike.data.api.AbstractFullEntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public abstract class BusinessValidationErrorSummaryRepository
        extends AbstractFullEntityRepository<BusinessValidationErrorSummary, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessValidationErrorSummaryRepository.class);
    private static final String SELECT_ERROR_SUMMARY = "SELECT e FROM BusinessValidationErrorSummary e";

    @Query(value = SELECT_ERROR_SUMMARY)
    public abstract QueryResult<BusinessValidationErrorSummary> fetchAllCaseErrorSummary();

    @SuppressWarnings({"squid:S2221"})
    public PaginationResult<BusinessValidationErrorSummary> fetchFilteredCaseErrorSummary(
            final FilterParameter filterParameter, final PaginationParameter paginationParameter) {

        try {
            final CriteriaBuilder criteriaBuilder = entityManager().getCriteriaBuilder();

            final CriteriaQuery<BusinessValidationErrorSummary> criteriaQuery =
                    criteriaBuilder.createQuery(BusinessValidationErrorSummary.class);

            final CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);

            final Root<BusinessValidationErrorSummary> entityRoot = countQuery.from(criteriaQuery.getResultType());
            countQuery.select(criteriaBuilder.count(entityRoot));
            final List<Predicate> countQueryPredicates = buildPredicates(filterParameter, criteriaBuilder, entityRoot);
            if (!countQueryPredicates.isEmpty()) {
                final Predicate[] predicateArray = countQueryPredicates.toArray(new Predicate[countQueryPredicates.size()]);
                countQuery.where(criteriaBuilder.and(predicateArray));
            }

            final Long totalCount = entityManager().createQuery(countQuery).getSingleResult();
            final int countPages = (int) Math.ceil((double) totalCount / (double) paginationParameter.getPageSize());

            final Root<BusinessValidationErrorSummary> e = criteriaQuery.from(BusinessValidationErrorSummary.class);
            final List<Predicate> criteriaQueryPredicates = buildPredicates(filterParameter, criteriaBuilder, e);
            if (!buildPredicates(filterParameter, criteriaBuilder, entityRoot).isEmpty()) {
                final Predicate[] predicateArray = criteriaQueryPredicates.toArray(new Predicate[criteriaQueryPredicates.size()]);
                criteriaQuery.where(criteriaBuilder.and(predicateArray));
            }

            final String orderByField = paginationParameter.getSortField().getFieldName();
            if (paginationParameter.getSortOrder().toString().equalsIgnoreCase(SortOrder.ASC.toString())) {
                criteriaQuery.orderBy(criteriaBuilder.asc(e.get(orderByField)));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(e.get(orderByField)));
            }

            final TypedQuery<BusinessValidationErrorSummary> typedQuery = entityManager()
                    .createQuery(criteriaQuery)
                    .setFirstResult((paginationParameter.getPageNumber() - 1) * paginationParameter.getPageSize())
                    .setMaxResults(paginationParameter.getPageSize());

            final List<BusinessValidationErrorSummary> errorSummaries = typedQuery.getResultList();
            return new PaginationResult<>(errorSummaries, totalCount, countPages);
        } catch (Exception e) {
            LOGGER.error("Error occurred while executing query", e);
            return new PaginationResult<>(Collections.emptyList(), 0, 0);
        }
    }

    private List<Predicate> buildPredicates(final FilterParameter filterParameter,
                                            final CriteriaBuilder criteriaBuilder, final Root<BusinessValidationErrorSummary> e) {

        final List<Predicate> predicates = new ArrayList<>();
        if (nonNull(filterParameter.getCourt())) {
            predicates.add(criteriaBuilder.equal(
                    e.get(BusinessValidationErrorSummary_.courtLocation), filterParameter.getCourt()));
        }
        if (nonNull(filterParameter.getCaseType())) {
            predicates.add(criteriaBuilder.equal(
                    e.get(BusinessValidationErrorSummary_.caseType), filterParameter.getCaseType()));
        }
        if (nonNull(filterParameter.getUrn())) {
            predicates.add(criteriaBuilder.equal(
                    e.get(BusinessValidationErrorSummary_.urn), filterParameter.getUrn()));
        }
        if (nonNull(filterParameter.getHearingDateFrom())) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    e.get(BusinessValidationErrorSummary_.defendantHearingDate),
                    parse(filterParameter.getHearingDateFrom())));
        }
        if (nonNull(filterParameter.getHearingDateTo())) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    e.get(BusinessValidationErrorSummary_.defendantHearingDate),
                    parse(filterParameter.getHearingDateTo())));
        }
        return predicates;
    }
}
