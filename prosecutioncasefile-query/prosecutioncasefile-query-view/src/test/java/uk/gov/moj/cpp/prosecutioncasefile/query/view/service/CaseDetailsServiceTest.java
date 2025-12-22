package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.INVALID_PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createFirstDefendantCaseDetails;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDetailsServiceTest {

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @InjectMocks
    private CaseDetailsService caseDetailsService;

    @Test
    public void shouldReturnCaseDetailsObjectForId() {
        final CaseDetails caseDetailsEntity = createFirstDefendantCaseDetails();

        when(caseDetailsRepository.findBy(CASE_ID))
                .thenReturn(caseDetailsEntity);

        final CaseDetailsView caseDetails = caseDetailsService.findCase(CASE_ID);

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getCaseId(), is(CASE_ID));
        assertViewFieldsWithEntity(caseDetails, caseDetailsEntity);

        verify(caseDetailsRepository).findBy(CASE_ID);
    }

    @Test
    public void shouldReturnCaseDetailsViewForProsecutionCaseReference() {
        final CaseDetails caseDetailsEntity = createFirstDefendantCaseDetails();

        when(caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE))
                .thenReturn(caseDetailsEntity);

        final CaseDetailsView caseDetails = caseDetailsService.findCaseByProsecutionReferenceId(PROSECUTOR_CASE_REFERENCE);

        assertThat(caseDetails, notNullValue());
        assertThat(caseDetails.getProsecutionCaseReference(), is(PROSECUTOR_CASE_REFERENCE));
        assertViewFieldsWithEntity(caseDetails, caseDetailsEntity);

        verify(caseDetailsRepository).findCaseDetailsByProsecutionCaseReference(PROSECUTOR_CASE_REFERENCE);
    }

    @Test
    public void shouldNotReturnCaseDetailsViewForProsecutionCaseReference() {

        when(caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE))
                .thenReturn(null);

        final CaseDetailsView caseDetails = caseDetailsService.findCaseByProsecutionReferenceId(INVALID_PROSECUTOR_CASE_REFERENCE);

        assertThat(caseDetails, nullValue());
        verify(caseDetailsRepository).findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE);
    }

    private void assertViewFieldsWithEntity(final CaseDetailsView caseDetails, final CaseDetails caseDetailsEntity) {
        assertThat(caseDetails.getCaseId(), is(caseDetailsEntity.getCaseId()));
        assertThat(caseDetails.getProsecutionAuthority(), is(caseDetailsEntity.getProsecutionAuthority()));
        assertThat(caseDetails.getProsecutionCaseReference(), is(caseDetailsEntity.getProsecutionCaseReference()));
    }
}
