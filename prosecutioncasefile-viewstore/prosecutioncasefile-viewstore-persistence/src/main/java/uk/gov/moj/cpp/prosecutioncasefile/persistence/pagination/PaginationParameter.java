package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

import java.util.Objects;

public class PaginationParameter {

    private final int pageSize;
    private final int pageNumber;
    private final SortOrder sortOrder;
    private final OrderByField sortField;

    public PaginationParameter(final int pageSize, final int pageNumber, final OrderByField sortField, final SortOrder sortOrder) {
        validateParam(sortField, "sortField");
        validateParam(sortOrder, "sortOrder");
        validateNumber(pageSize, "pageSize", PaginationConstant.MIN_PAGE_SIZE);
        validateNumber(pageNumber, "pageNumber", PaginationConstant.FIRST_PAGE_NUMBER);

        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.sortOrder = sortOrder;
        this.sortField = sortField;
    }

    private void validateParam(final Object valueToValidate, final String parameterName) {
        if (valueToValidate == null) {
            throw new IllegalArgumentException(String.format("%s is required", parameterName));
        }
    }

    private void validateNumber(final int valueToValidate, final String parameterName, final int minAllowedValue) {
        if (valueToValidate < minAllowedValue) {
            throw new IllegalArgumentException(String.format("value %s of %s is invalid and should be greater than %s", valueToValidate, parameterName, minAllowedValue));
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public OrderByField getSortField() {
        return sortField;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PaginationParameter that = (PaginationParameter) o;
        return pageSize == that.pageSize &&
                pageNumber == that.pageNumber &&
                sortOrder == that.sortOrder &&
                sortField == that.sortField;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageSize, pageNumber, sortOrder, sortField);
    }
}
