package uk.gov.moj.cpp.prosecutioncasefile.query.view.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.CASE_ID;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.INVALID_PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cpp.prosecutioncasefile.query.view.utils.TestUtils.createFirstDefendantCaseDetails;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;

import jakarta.persistence.NoResultException;

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

    // --- BC-02 investigation repro (added, not part of the original suite) ---
    //
    // The mock above (shouldNotReturnCaseDetailsViewForProsecutionCaseReference) stubs a
    // `null` return for an unknown reference. That was DeltaSpike's contract, but it is not
    // what the migrated CaseDetailsRepository.findCaseDetailsByProsecutionCaseReference(...)
    // actually does any more: it is a bare `entityManager.createQuery(...).getSingleResult()`
    // with no try/catch (see prosecutioncasefile-viewstore-persistence's
    // CaseDetailsRepository.java), and JPA's getSingleResult() throws NoResultException on no
    // match instead of returning null (confirmed by the repository's own
    // CaseDetailsRepositoryTest.shouldThrowException_whenGivenProsecutionCaseReference_notExist).
    // This test mocks the REAL contract and shows that CaseDetailsService's own
    // catch (EntityNotFoundException) never fires for it -- jakarta.persistence.
    // EntityNotFoundException and jakarta.persistence.NoResultException are unrelated sibling
    // subclasses of PersistenceException, so the exception propagates out of
    // findCaseByProsecutionReferenceId uncaught. (It IS caught one layer further up, in
    // ProsecutionCasefileQueryView.getCaseDetailsByProsecutionCaseReference's own
    // catch (NoResultException) -- byte-identical on main and team/25.104.x -- so this does
    // NOT reach production as an HTTP 500 on either J17 or J25. This test exists to pin down,
    // at the unit level, exactly which layer is doing the real work and confirm the inner
    // catch here is dead code on both sides, not a J25-introduced regression.)
    @Test
    public void shouldPropagateNoResultException_whenRepositoryThrowsForUnknownReference() {

        when(caseDetailsRepository.findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE))
                .thenThrow(new NoResultException("no CaseDetails matches prosecutionCaseReference"));

        assertThrows(NoResultException.class, () ->
                caseDetailsService.findCaseByProsecutionReferenceId(INVALID_PROSECUTOR_CASE_REFERENCE));

        verify(caseDetailsRepository).findCaseDetailsByProsecutionCaseReference(INVALID_PROSECUTOR_CASE_REFERENCE);
    }

    private void assertViewFieldsWithEntity(final CaseDetailsView caseDetails, final CaseDetails caseDetailsEntity) {
        assertThat(caseDetails.getCaseId(), is(caseDetailsEntity.getCaseId()));
        assertThat(caseDetails.getProsecutionAuthority(), is(caseDetailsEntity.getProsecutionAuthority()));
        assertThat(caseDetails.getProsecutionCaseReference(), is(caseDetailsEntity.getProsecutionCaseReference()));
    }
}
