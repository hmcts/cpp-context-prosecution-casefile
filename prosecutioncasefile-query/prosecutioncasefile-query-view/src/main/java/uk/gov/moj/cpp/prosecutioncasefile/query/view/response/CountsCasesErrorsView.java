package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

public class CountsCasesErrorsView {

    private final Integer casesWithOutstandingErrors;

    private final Integer casesWithErrorsResolvedToday;

    public CountsCasesErrorsView(int casesWithOutstandingErrors, int casesWithErrorsResolvedToday) {
        this.casesWithOutstandingErrors = casesWithOutstandingErrors;
        this.casesWithErrorsResolvedToday = casesWithErrorsResolvedToday;
    }
    public Integer getCasesWithOutstandingErrors() {
        return casesWithOutstandingErrors;
    }

    public Integer getCasesWithErrorsResolvedToday() {
        return casesWithErrorsResolvedToday;
    }

}
