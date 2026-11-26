package uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.OrderByField.HEARING_DATE;
import static uk.gov.moj.cpp.prosecutioncasefile.persistence.pagination.SortOrder.ASC;

import java.util.List;

import org.apache.deltaspike.data.api.QueryResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PaginationServiceTest {

    @InjectMocks
    private PaginationService paginationService;

    @Mock
    private QueryResult queryResult;

    private List resultList;

    private PaginationParameter paginationParameter;

    @Before
    public void setUp() {
        resultList = emptyList();
        when(queryResult.orderAsc(anyString())).thenReturn(queryResult);
        when(queryResult.withPageSize(anyInt())).thenReturn(queryResult);
        when(queryResult.toPage(anyInt())).thenReturn(queryResult);
        when(queryResult.countPages()).thenReturn(1);
        when(queryResult.getResultList()).thenReturn(resultList);
        paginationParameter = new PaginationParameter(1, 1, HEARING_DATE, ASC);
    }


    @Test
    public void shouldPaginate() throws Exception {
        final PaginationResult result = paginationService.paginate(paginationParameter, queryResult);
        verify(queryResult).orderAsc(paginationParameter.getSortField().getFieldName());
        verify(queryResult, never()).orderDesc(anyString());
        verify(queryResult).withPageSize(paginationParameter.getPageSize());
        verify(queryResult).toPage(paginationParameter.getPageNumber() - 1);
        verify(queryResult).count();
        verify(queryResult).getResultList();

        assertThat(result.getResult(), is(resultList));
        assertThat(result.getPageCount(), is(1L));
    }


    @Test
    public void shouldGetPageCount() {
        assertThat(paginationService.toPageCount(11, 5), is(3L));
        assertThat(paginationService.toPageCount(1, 5), is(1L));
        assertThat(paginationService.toPageCount(1, 13), is(1L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnZeroPageNumber() {
        new PaginationParameter(1, 0, HEARING_DATE, ASC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnZeroPageSize() {
        new PaginationParameter(0, 1, HEARING_DATE, ASC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnNullSortField() {
        new PaginationParameter(1, 1, null, ASC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionOnNullSortOrder() {
        new PaginationParameter(1, 1, HEARING_DATE, null);
    }

}