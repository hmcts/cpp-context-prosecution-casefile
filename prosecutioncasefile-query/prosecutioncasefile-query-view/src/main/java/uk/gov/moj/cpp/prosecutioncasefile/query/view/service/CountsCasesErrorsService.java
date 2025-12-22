package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CountsCasesErrorsView;

import java.util.Optional;

import javax.inject.Inject;

public class CountsCasesErrorsService {

    @Inject
    private BusinessErrorDetailsService businessErrorDetailsService;

    public CountsCasesErrorsView countsCasesErrors(
            final Optional<String> region,
            final Optional<String> courtLocation,
            final Optional<String> caseType) {

        return businessErrorDetailsService.countsCasesErrorsView(region, courtLocation, caseType);
    }
}
