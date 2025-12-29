package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorCaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorSummary;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationParameter;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.PaginationResult;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorSummaryRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.ResolvedCasesRepository;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.BusinessValidationErrorView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CountsCasesErrorsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.ErrorCaseDetails;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonReader;

@SuppressWarnings({"squid:S1612"})
public class BusinessErrorDetailsService {

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorsRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private BusinessValidationErrorSummaryRepository businessValidationErrorSummaryRepository;

    @Inject
    private ResolvedCasesRepository resolvedCasesRepository;

    @SuppressWarnings({"squid:S117"})
    public CountsCasesErrorsView countsCasesErrorsView(final Optional<String> region,
                                                       final Optional<String> courtLocation,
                                                       final Optional<String> caseType) {
        final Long countOfCasesWithOutstandingErrors = businessValidationErrorsRepository.countOfCasesWithOutstandingErrors(courtLocation, caseType);

        final Long casesWithErrorsResolvedToday = getResolvedCasesRepository().countOfCasesFixedByDate(LocalDate.now(), region, courtLocation, caseType);
        return new CountsCasesErrorsView(countOfCasesWithOutstandingErrors.intValue(), casesWithErrorsResolvedToday.intValue());
    }

    public List<BusinessValidationErrorView> findAllErrorsByCaseId(final UUID caseId) {

        final List<BusinessValidationErrorDetails> businessValidationErrorDetails = businessValidationErrorsRepository.findByCaseId(caseId);
        if (businessValidationErrorDetails.isEmpty()) {
            return Collections.emptyList();
        }
        final ErrorCaseDetails errorCaseDetailsJson = getErrorCaseDetails(caseId);
        return asList(buildValidationErrorView(businessValidationErrorDetails, errorCaseDetailsJson));
    }

    public PaginationResult<BusinessValidationErrorView> findAllErrors(final PaginationParameter paginationParameter, final FilterParameter filterParameter) {
        final PaginationResult<BusinessValidationErrorSummary> paginationResult = businessValidationErrorSummaryRepository.fetchFilteredCaseErrorSummary(filterParameter, paginationParameter);
        return getAllErrorsPaginationResult(paginationResult, paginationParameter);
    }

    private PaginationResult<BusinessValidationErrorView> getAllErrorsPaginationResult(final PaginationResult<BusinessValidationErrorSummary> paginationResult, final PaginationParameter paginationParameter) {
        if (paginationResult.getResult().isEmpty()) {
            return new PaginationResult<>(Collections.emptyList(), 0, 0);
        }

        final List<UUID> caseIds = paginationResult.getResult().stream().map(result -> result.getCaseId()).collect(Collectors.toList());
        final List<BusinessValidationErrorDetails> businessValidationErrorDetailsList = businessValidationErrorsRepository.fetchAllCaseErrorDetailsByCaseIds(caseIds, paginationParameter);

        final Map<UUID, List<BusinessValidationErrorDetails>> collectionByCaseId = businessValidationErrorDetailsList.stream().collect(Collectors.groupingBy(s -> s.getCaseId(), LinkedHashMap::new, toList()));

        final List<BusinessValidationErrorView> businessValidationErrorViewList  =
                caseIds.stream().map(caseId -> {
                    ErrorCaseDetails errorCaseDetailsJson = getErrorCaseDetails(caseId);
                    return buildViewSortedByBailStatus(collectionByCaseId.get(caseId), errorCaseDetailsJson);
                }).collect(Collectors.toList());

        return new PaginationResult<>(businessValidationErrorViewList, paginationResult.getTotalResultCount(), paginationResult.getPageCount());
    }

    private ErrorCaseDetails getErrorCaseDetails(final UUID caseId) {
        final List<BusinessValidationErrorCaseDetails> errorCaseDetails = businessValidationErrorCaseDetailsRepository.findByCaseId(caseId);
        ErrorCaseDetails errorCaseDetailsJson = null;
        if (errorCaseDetails != null && !errorCaseDetails.isEmpty()) {
            try (JsonReader jsonReader = JsonObjects.createReader(new StringReader(errorCaseDetails.get(0).getCaseDetails()))) {
                errorCaseDetailsJson = new ErrorCaseDetails(jsonReader.readObject().getJsonArray("defendants"));
            }
        }
        return errorCaseDetailsJson;
    }

    private BusinessValidationErrorView buildValidationErrorView(final List<BusinessValidationErrorDetails> listWithErrors, ErrorCaseDetails errorCaseDetails) {
        final List<BusinessValidationErrorDetails> listWithDefendantErrors = new ArrayList<>();

        final List<BusinessValidationErrorDetails> listWithCaseErrors = new ArrayList<>();

        listWithDefendantErrors.addAll(listWithErrors.stream().filter(t -> t.getDefendantId() != null).collect(toList()));

        listWithCaseErrors.addAll(listWithErrors.stream().filter(t -> t.getDefendantId() == null).collect(toList()));

        return new BusinessValidationErrorView(listWithCaseErrors, listWithDefendantErrors, errorCaseDetails);
    }

    private BusinessValidationErrorView buildViewSortedByBailStatus(final List<BusinessValidationErrorDetails> listWithErrors, ErrorCaseDetails errorCaseDetails) {
        final Comparator<BusinessValidationErrorDetails> comparator = comparing(BusinessValidationErrorDetails::getDefendantHearingDate, nullsLast(naturalOrder()))
                .thenComparing(BusinessValidationErrorDetails::getCaseId, naturalOrder());
        final List<BusinessValidationErrorDetails> listWithDefendantErrors = listWithErrors.stream().filter(t -> t.getDefendantId() != null)
                .sorted(comparator)
                .collect(toList());

        final List<BusinessValidationErrorDetails> listWithCaseErrors = listWithErrors.stream().filter(t -> t.getDefendantId() == null)
                .collect(toList());

        return new BusinessValidationErrorView(listWithCaseErrors, listWithDefendantErrors, errorCaseDetails);
    }

    public ResolvedCasesRepository getResolvedCasesRepository() {
        return resolvedCasesRepository;
    }
}
