package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CountsCasesErrorsView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CountsCasesErrorsServiceTest {

    @Mock
    private BusinessErrorDetailsService businessErrorDetailsService;

    @InjectMocks
    private CountsCasesErrorsService countsCasesErrorsService;

    @Test
    public void shouldReturnCountsCasesErrorsWithNoParameters(){
        final CountsCasesErrorsView expected = new CountsCasesErrorsView(15,10);

        when(businessErrorDetailsService.countsCasesErrorsView(empty(), empty(), empty())).thenReturn(expected);

        final CountsCasesErrorsView actual =countsCasesErrorsService
                .countsCasesErrors(empty(), empty(), empty());

        assertThat(expected.getCasesWithErrorsResolvedToday(),is(actual.getCasesWithErrorsResolvedToday()));
        assertThat(expected.getCasesWithOutstandingErrors(),is(actual.getCasesWithOutstandingErrors()));
    }

    @Test
    public void shouldReturnCountsCasesErrorsWithRegionParameterOnlyAndOthersEmpty(){
        final CountsCasesErrorsView expected = new CountsCasesErrorsView(15,10);

        when(businessErrorDetailsService.countsCasesErrorsView(of("region"), empty(), empty())).thenReturn(expected);

        final CountsCasesErrorsView actual =countsCasesErrorsService
                .countsCasesErrors(of("region"), empty(), empty());

        assertThat(expected.getCasesWithErrorsResolvedToday(),is(actual.getCasesWithErrorsResolvedToday()));
        assertThat(expected.getCasesWithOutstandingErrors(),is(actual.getCasesWithOutstandingErrors()));
    }

    @Test
    public void shouldReturnCountsCasesErrorsWithCourtLocationParameterOnlyAndOthersEmpty(){
        final CountsCasesErrorsView expected = new CountsCasesErrorsView(15,10);

        when(businessErrorDetailsService.countsCasesErrorsView(empty(), of("courtLocation"), empty())).thenReturn(expected);

        final CountsCasesErrorsView actual =countsCasesErrorsService
                .countsCasesErrors(empty(), of("courtLocation"), empty());

        assertThat(expected.getCasesWithErrorsResolvedToday(),is(actual.getCasesWithErrorsResolvedToday()));
        assertThat(expected.getCasesWithOutstandingErrors(),is(actual.getCasesWithOutstandingErrors()));
    }

    @Test
    public void shouldReturnCountsCasesErrorsWithCaseTypeParameterOnlyAndOthersEmpty(){
        final CountsCasesErrorsView expected = new CountsCasesErrorsView(15,10);

        when(businessErrorDetailsService.countsCasesErrorsView(empty(),empty(),of("caseType"))).thenReturn(expected);

        final CountsCasesErrorsView actual =countsCasesErrorsService
                .countsCasesErrors(empty(), empty(), of("caseType"));

        assertThat(expected.getCasesWithErrorsResolvedToday(),is(actual.getCasesWithErrorsResolvedToday()));
        assertThat(expected.getCasesWithOutstandingErrors(),is(actual.getCasesWithOutstandingErrors()));
    }

    @Test
    public void shouldReturnCountsCasesErrorsWithAllParameters(){
        final CountsCasesErrorsView expected = new CountsCasesErrorsView(15,10);

        when(businessErrorDetailsService.countsCasesErrorsView(of("region"), of("courtLocation"), of("caseType"))).thenReturn(expected);

        final CountsCasesErrorsView actual =countsCasesErrorsService
                .countsCasesErrors(of("region"), of("courtLocation"), of("caseType"));

        assertThat(expected.getCasesWithErrorsResolvedToday(),is(actual.getCasesWithErrorsResolvedToday()));
        assertThat(expected.getCasesWithOutstandingErrors(),is(actual.getCasesWithOutstandingErrors()));
    }
}