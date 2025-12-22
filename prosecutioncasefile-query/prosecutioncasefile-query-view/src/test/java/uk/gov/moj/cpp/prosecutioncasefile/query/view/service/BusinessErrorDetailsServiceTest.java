package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.OrderByField.HEARING_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder.DESC;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.BAIL_STATUS_CUSTODY;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.BAIL_STATUS_REMAND;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_ID_2;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_TYPE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_URN;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_URN_2;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.COURT_NAME;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.FIRST_NAME;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.LAST_NAME;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.VALUE_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.VALUE_DEFENDANT_ID_2;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.VALUE_DEFENDANT_ID_3;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createCaseLevelError;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createDefendantLevelError;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createOffenceLevelError;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.newBusinessValidationErrorSummary;

import uk.gov.moj.cpp.prosecutioncasefile.mapping.FilterParameter;
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
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.DefendantErrorsView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BusinessErrorDetailsServiceTest {

    public static final String ERROR_FIELD_NAME = "CaseFieldName";
    public static final String ERROR_FIELD_VALUE = "Case Error Value";
    private static final LocalDate HEARING_DATE1 = LocalDate.of(2015, 1, 1);

    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Mock
    private BusinessValidationErrorSummaryRepository businessValidationErrorSummaryRepository;

    @Mock
    private ResolvedCasesRepository resolvedCasesRepository;

    @InjectMocks
    private BusinessErrorDetailsService businessErrorDetailsService;

    @Test
    public void shouldReturnCountsCasesErrorsView() {
        when(businessValidationErrorRepository.countOfCasesWithOutstandingErrors(empty(), empty())).thenReturn(10L);
        final CountsCasesErrorsView countsCasesErrorsView =
                businessErrorDetailsService.countsCasesErrorsView(empty(), empty(), empty());
        assertThat(countsCasesErrorsView.getCasesWithOutstandingErrors(), is(10));
        assertThat(countsCasesErrorsView.getCasesWithErrorsResolvedToday(), is(0));
    }

    @Test
    public void shouldFindAllBusinessErrorsInCase() {
        final List<BusinessValidationErrorDetails> businessValidationErrorViewList =
                asList(createCaseLevelError(), createDefendantLevelError(), createOffenceLevelError());

        when(businessValidationErrorRepository.findByCaseId(CASE_ID))
                .thenReturn(businessValidationErrorViewList);

        final List<BusinessValidationErrorView> businessErrorDetailsServiceAllErrorsByCaseId = businessErrorDetailsService.findAllErrorsByCaseId(CASE_ID);

        assertThat(businessErrorDetailsServiceAllErrorsByCaseId, notNullValue());
        assertThat(businessErrorDetailsServiceAllErrorsByCaseId.size(), is(1));
        final int caseLevelErrorCount = businessErrorDetailsServiceAllErrorsByCaseId.get(0).getErrors().size();
        final int defendantLevelErrorCount = businessErrorDetailsServiceAllErrorsByCaseId.get(0).getDefendants().get(0).getErrors().size();
        final int offenceLevelErrorCount = businessErrorDetailsServiceAllErrorsByCaseId.get(0).getDefendants().get(0).getOffences().get(0).getErrors().size();

        assertThat(caseLevelErrorCount +
                defendantLevelErrorCount +
                offenceLevelErrorCount, is(3));
        final List<DefendantErrorsView> defendantErrorsViews = businessErrorDetailsServiceAllErrorsByCaseId.get(0).getDefendants();

        assertThat(defendantErrorsViews.stream()
                .filter(defendantErrorsView -> null != defendantErrorsView.getFirstName())
                .anyMatch(errorView -> errorView.getFirstName().equalsIgnoreCase(FIRST_NAME)), is(true));

        assertThat(defendantErrorsViews.stream()
                .filter(defendantErrorsView -> null != defendantErrorsView.getLastName())
                .anyMatch(errorView -> errorView.getLastName().equalsIgnoreCase(LAST_NAME)), is(true));

        assertThat(businessErrorDetailsServiceAllErrorsByCaseId.get(0).getHearingDate(), is(now()));

        verify(businessValidationErrorRepository).findByCaseId(CASE_ID);
    }


    @Test
    public void shouldFindAllBusinessErrors() {

        final BusinessValidationErrorDetails caseLevelErrorDetails = createCaseLevelError();
        caseLevelErrorDetails.setCaseId(CASE_ID);
        caseLevelErrorDetails.setUrn(CASE_URN);
        caseLevelErrorDetails.setFieldName(ERROR_FIELD_NAME);
        caseLevelErrorDetails.setErrorValue(ERROR_FIELD_VALUE);
        caseLevelErrorDetails.setDefendantHearingDate(HEARING_DATE1);


        // Defendant level error with bail status L
        final BusinessValidationErrorDetails defendantLevelErrorDetails = createDefendantLevelError();
        defendantLevelErrorDetails.setCaseId(CASE_ID);
        defendantLevelErrorDetails.setUrn(CASE_URN);
        defendantLevelErrorDetails.setDefendantId(VALUE_DEFENDANT_ID);
        defendantLevelErrorDetails.setDefendantBailStatus(BAIL_STATUS_REMAND);
        defendantLevelErrorDetails.setDefendantHearingDate(HEARING_DATE1);

        // Defendant level error with bail status C
        final BusinessValidationErrorDetails defendantLevelErrorDetails2 = createDefendantLevelError();
        defendantLevelErrorDetails2.setCaseId(CASE_ID_2);
        defendantLevelErrorDetails2.setUrn(CASE_URN_2);
        defendantLevelErrorDetails2.setDefendantId(VALUE_DEFENDANT_ID_2);
        defendantLevelErrorDetails2.setDefendantBailStatus(BAIL_STATUS_CUSTODY);
        defendantLevelErrorDetails2.setDefendantHearingDate(HEARING_DATE1);

        // Defendant level error with bail status null
        final BusinessValidationErrorDetails defendantLevelErrorDetails3 = createDefendantLevelError();
        defendantLevelErrorDetails3.setCaseId(CASE_ID_2);
        defendantLevelErrorDetails3.setUrn(CASE_URN_2);
        defendantLevelErrorDetails3.setDefendantId(VALUE_DEFENDANT_ID_3);
        defendantLevelErrorDetails3.setDefendantBailStatus(null);
        defendantLevelErrorDetails3.setDefendantHearingDate(HEARING_DATE1.plusDays(1));


        final BusinessValidationErrorDetails offenceLevelErrorDetails = createOffenceLevelError();
        offenceLevelErrorDetails.setCaseId(CASE_ID_2);
        offenceLevelErrorDetails.setUrn(CASE_URN_2);
        offenceLevelErrorDetails.setFieldId(randomUUID().toString());
        offenceLevelErrorDetails.setDefendantId(VALUE_DEFENDANT_ID);
        offenceLevelErrorDetails.setDefendantHearingDate(HEARING_DATE1.plusDays(2));


        final BusinessValidationErrorDetails offenceLevelErrorDetails2 = createOffenceLevelError();
        offenceLevelErrorDetails2.setCaseId(CASE_ID_2);
        offenceLevelErrorDetails2.setUrn(CASE_URN_2);
        offenceLevelErrorDetails2.setFieldId(randomUUID().toString());
        offenceLevelErrorDetails2.setDefendantId(VALUE_DEFENDANT_ID_2);

        final BusinessValidationErrorDetails offenceLevelErrorDetails3 = createOffenceLevelError();
        offenceLevelErrorDetails3.setCaseId(CASE_ID_2);
        offenceLevelErrorDetails3.setUrn(CASE_URN_2);
        offenceLevelErrorDetails3.setFieldId(randomUUID().toString());
        offenceLevelErrorDetails3.setDefendantId(VALUE_DEFENDANT_ID_3);

        // Create 10 error details records
        final List<BusinessValidationErrorDetails> businessValidationErrorDetailsList =
                asList(createCaseLevelError(), createDefendantLevelError(), createOffenceLevelError(),
                        caseLevelErrorDetails, defendantLevelErrorDetails, defendantLevelErrorDetails2,
                        defendantLevelErrorDetails3, offenceLevelErrorDetails, offenceLevelErrorDetails2,
                        offenceLevelErrorDetails3);

        // Create distinct case summary record
        final BusinessValidationErrorSummary businessValidationErrorSummary = newBusinessValidationErrorSummary(CASE_ID);
        final BusinessValidationErrorSummary businessValidationErrorSummary2 = newBusinessValidationErrorSummary(CASE_ID_2);
        final List<BusinessValidationErrorSummary> businessValidationErrorSummaryList = asList(businessValidationErrorSummary, businessValidationErrorSummary2);

        final FilterParameter filterParameter = new FilterParameter(COURT_NAME, CASE_TYPE, null, now().minusDays(1).toString(), now().plusDays(1).toString());
        final PaginationParameter paginationParameter = new PaginationParameter(10, 1, HEARING_DATE, DESC);
        final PaginationResult<BusinessValidationErrorSummary> expectedPaginationResult = new PaginationResult<>(businessValidationErrorSummaryList, 2, 1);
        final List<UUID> caseIds = Stream.of(CASE_ID, CASE_ID_2).collect(toList());

        // Return distinct case error records with pagination
        when(businessValidationErrorSummaryRepository.fetchFilteredCaseErrorSummary(filterParameter, paginationParameter)).thenReturn(expectedPaginationResult);
        // Return all error detail records for case ids
        when(businessValidationErrorRepository.fetchAllCaseErrorDetailsByCaseIds(caseIds, paginationParameter)).thenReturn(businessValidationErrorDetailsList);

        final PaginationResult<BusinessValidationErrorView> actualPaginationResult = businessErrorDetailsService.findAllErrors(paginationParameter, filterParameter);

        assertThat(actualPaginationResult, notNullValue());
        final List<BusinessValidationErrorView> businessValidationErrorCases = actualPaginationResult.getResult();
        assertThat(businessValidationErrorCases, notNullValue());
        assertThat(businessValidationErrorCases.size(), is(2));

        // Verify error counts
        final int totalCaseErrors = businessValidationErrorCases.stream().mapToInt(s -> s.getErrors().size()).sum();
        final int totalDefendantErrors = businessValidationErrorCases.stream().flatMapToInt(s -> s.getDefendants().stream()
                .mapToInt(err -> err.getErrors().size())).sum();
        final int totalOffenceErrors = businessValidationErrorCases.stream().flatMapToInt(s -> s.getDefendants().stream()
                .mapToInt(err -> err.getOffences().get(0).getErrors().size())).sum();
        assertThat(totalCaseErrors + totalDefendantErrors + totalOffenceErrors, is(10));

        assertThat(businessValidationErrorCases.get(0).getDefendants().get(0).getBailStatus(), is(BAIL_STATUS_REMAND));
        assertThat(businessValidationErrorCases.get(0).getDefendants().get(0).getHearingDate(), is(HEARING_DATE1));

        assertThat(businessValidationErrorCases.get(0).getErrors().size(), is(2));
        assertThat(businessValidationErrorCases.get(0).getHearingDate(), is(now()));


        assertThat(businessValidationErrorCases.get(1).getDefendants().get(0).getBailStatus().equalsIgnoreCase(BAIL_STATUS_CUSTODY), is(true));
        assertThat(businessValidationErrorCases.get(1).getDefendants().get(0).getHearingDate(), is(HEARING_DATE1));

        assertThat(businessValidationErrorCases.get(1).getDefendants().get(1).getBailStatus(), is(nullValue()));
        assertThat(businessValidationErrorCases.get(1).getDefendants().get(1).getHearingDate(), is(HEARING_DATE1.plusDays(1)));

        assertThat(businessValidationErrorCases.get(1).getDefendants().get(2).getHearingDate(), is(HEARING_DATE1.plusDays(2)));
        assertThat(businessValidationErrorCases.get(1).getDefendants().get(2).getBailStatus(), is(nullValue()));

        assertThat(businessValidationErrorCases.stream()
                .filter(x -> x.getUrn().equalsIgnoreCase(CASE_URN_2))
                .anyMatch(errorCase -> errorCase.getDefendants().get(2).getBailStatus() == null), is(true));

        final List<DefendantErrorsView> defendantErrorsViews = new ArrayList<>();
        businessValidationErrorCases.forEach(x -> defendantErrorsViews.addAll(x.getDefendants()));

        assertThat(defendantErrorsViews.stream()
                .filter(defendantErrorsView -> null != defendantErrorsView.getFirstName())
                .anyMatch(errorView -> errorView.getFirstName().equalsIgnoreCase(FIRST_NAME)), is(true));
        assertThat(defendantErrorsViews.stream()
                .filter(defendantErrorsView -> null != defendantErrorsView.getLastName())
                .anyMatch(errorView -> errorView.getLastName().equalsIgnoreCase(LAST_NAME)), is(true));

        verify(businessValidationErrorSummaryRepository).fetchFilteredCaseErrorSummary(filterParameter, paginationParameter);
        verify(businessValidationErrorRepository).fetchAllCaseErrorDetailsByCaseIds(caseIds, paginationParameter);
    }

    @Test
    public void shouldReturnEmptyPaginationResult() {
        final List<BusinessValidationErrorSummary> businessValidationErrorSummaryList = new ArrayList<>();

        final FilterParameter filterParameter = new FilterParameter(COURT_NAME, CASE_TYPE, CASE_URN, now().toString(), now().plusDays(1).toString());
        final PaginationParameter paginationParameter = new PaginationParameter(2, 2, HEARING_DATE, DESC);
        final PaginationResult<BusinessValidationErrorSummary> expectedPaginationResult = new PaginationResult<>(businessValidationErrorSummaryList, 0, 0);

        when(businessValidationErrorSummaryRepository.fetchFilteredCaseErrorSummary(filterParameter, paginationParameter)).thenReturn(expectedPaginationResult);

        final PaginationResult<BusinessValidationErrorView> actualPaginationResult = businessErrorDetailsService.findAllErrors(paginationParameter, filterParameter);

        assertThat(actualPaginationResult, notNullValue());
        final List<BusinessValidationErrorView> businessValidationErrorCases = actualPaginationResult.getResult();
        assertThat(businessValidationErrorCases, notNullValue());
        assertThat(businessValidationErrorCases.size(), is(0));
        verify(businessValidationErrorSummaryRepository).fetchFilteredCaseErrorSummary(filterParameter, paginationParameter);
        verifyNoInteractions(businessValidationErrorRepository);
    }

}