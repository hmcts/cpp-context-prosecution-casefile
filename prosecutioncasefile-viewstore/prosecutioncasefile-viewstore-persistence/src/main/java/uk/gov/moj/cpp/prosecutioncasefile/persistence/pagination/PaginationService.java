package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;


import java.util.List;

import org.apache.deltaspike.data.api.QueryResult;

public class PaginationService<E> {

    public PaginationResult<E> paginate(final PaginationParameter paginationParameter, final QueryResult<E> queryResult) {
        final List<E> resultList;
        final long countPages = queryResult.countPages();
        final long totalCount = queryResult.count();

        if (paginationParameter.getSortOrder().toString().equalsIgnoreCase(SortOrder.ASC.toString())) {
            resultList = queryResult.orderAsc(paginationParameter.getSortField().getFieldName())
                    .withPageSize(paginationParameter.getPageSize())
                    .toPage(adjustToZeroIndexedPageNumber(paginationParameter.getPageNumber()))
                    .getResultList();
        } else {
            resultList = queryResult.orderDesc(paginationParameter.getSortField().getFieldName())
                    .withPageSize(paginationParameter.getPageSize())
                    .toPage(adjustToZeroIndexedPageNumber(paginationParameter.getPageNumber()))
                    .getResultList();
        }
        return new PaginationResult<>(resultList, totalCount, countPages);
    }

    private int adjustToZeroIndexedPageNumber(final int pageNumber) {
        return pageNumber - 1;
    }

    protected long toPageCount(final long totalCount, final int pageSize) {
        return (long) Math.ceil((double) totalCount / (double) pageSize);
    }
}
