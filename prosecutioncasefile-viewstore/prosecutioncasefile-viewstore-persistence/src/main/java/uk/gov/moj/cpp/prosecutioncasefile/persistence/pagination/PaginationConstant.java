package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

public class PaginationConstant {
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final String SORT_FIELD = "sortField";
    public static final String SORT_ORDER = "sortOrder";
    public static final int FIRST_PAGE_NUMBER = 1;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;

    private PaginationConstant() {
    }

}
