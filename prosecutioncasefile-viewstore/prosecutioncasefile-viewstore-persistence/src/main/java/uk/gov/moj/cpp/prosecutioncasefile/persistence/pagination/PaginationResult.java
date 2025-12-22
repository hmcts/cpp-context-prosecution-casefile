package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PaginationResult<E> {
    private final List<E> result;
    private final long pageCount;
    private final long totalResultCount;

    public PaginationResult(final List<E> result, final long totalResultCount, final long pageCount) {
        this.result = result != null ? unmodifiableList(result) : new ArrayList<>();
        this.totalResultCount = totalResultCount;
        this.pageCount = pageCount;
    }

    public List<E> getResult() {
        return unmodifiableList(result);
    }

    public long getPageCount() {
        return pageCount;
    }

    public long getTotalResultCount() {
        return totalResultCount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PaginationResult<?> that = (PaginationResult<?>) o;
        return pageCount == that.pageCount &&
                totalResultCount == that.totalResultCount &&
                Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, pageCount, totalResultCount);
    }
}
