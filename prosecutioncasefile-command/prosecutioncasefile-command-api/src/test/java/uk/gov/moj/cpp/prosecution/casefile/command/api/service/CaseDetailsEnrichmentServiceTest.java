package uk.gov.moj.cpp.prosecution.casefile.command.api.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDetailsEnrichmentServiceTest {

    @Mock
    private IdGenerationService idGenerationService;

    @Mock
    private Prosecutor prosecutor;

    @InjectMocks
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    @Test
    public void shouldReturnOriginalCaseIdAndProsecutorReference() {

        final UUID originalCaseId = randomUUID();
        final String originalProsecutorCaseReference = "Xdw89HER7R";
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(originalCaseId)
                .withProsecutorCaseReference(originalProsecutorCaseReference)
                .withProsecutor(Prosecutor.prosecutor()
                        .build())
                .build();

        final CaseDetails response = caseDetailsEnrichmentService.enrichCaseDetails(caseDetails, prosecutor);

        assertThat(originalCaseId, is(response.getCaseId()));
        assertThat(originalProsecutorCaseReference, is(response.getProsecutorCaseReference()));
    }

    @Test
    public void shouldReturnGeneratedCaseIdAndProsecutorReference() {

        final UUID generatedCaseId = randomUUID();
        final String generatedProsecutorCaseReference = "Aeg54GHT6S";

        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(Prosecutor.prosecutor()
                        .build())
                .build();

        when(idGenerationService.generateCaseReference()).thenReturn(generatedProsecutorCaseReference);
        when(idGenerationService.generateCaseId(anyString())).thenReturn(generatedCaseId);
        final CaseDetails response = caseDetailsEnrichmentService.enrichCaseDetails(caseDetails, prosecutor);

        assertThat(generatedCaseId, is(response.getCaseId()));
        assertThat(generatedProsecutorCaseReference, is(response.getProsecutorCaseReference()));
    }

    @Test
    public void shouldReturnGeneratedCaseIdAndProsecutorReferenceAndDateOfCommittalAndDateOfSending() {

        final UUID generatedCaseId = randomUUID();
        final String generatedProsecutorCaseReference = "Aeg54GHT6S";
        final LocalDate dateOfSending = LocalDate.now().minusDays(1);
        final LocalDate dateOfCommittal = LocalDate.now().plusDays(1);

        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withProsecutor(Prosecutor.prosecutor()
                        .build())
                .withDateOfSending(dateOfSending)
                .withDateOfCommittal(dateOfCommittal)
                .build();

        when(idGenerationService.generateCaseReference()).thenReturn(generatedProsecutorCaseReference);
        when(idGenerationService.generateCaseId(anyString())).thenReturn(generatedCaseId);
        final CaseDetails response = caseDetailsEnrichmentService.enrichCaseDetails(caseDetails, prosecutor);

        assertThat(generatedCaseId, is(response.getCaseId()));
        assertThat(generatedProsecutorCaseReference, is(response.getProsecutorCaseReference()));
        assertThat(response.getDateOfCommittal(), is(dateOfCommittal));
        assertThat(response.getDateOfSending(), is(dateOfSending));
    }

    @Test
    public void shouldNotInvokeIdGenerationServiceForCaseReferenceAndCaseId() {

        final UUID originalCaseId = randomUUID();
        final String originalProsecutorCaseReference = "Xdw89HER7R";
        CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(originalCaseId)
                .withProsecutorCaseReference(originalProsecutorCaseReference)
                .withProsecutor(Prosecutor.prosecutor()
                        .build())
                .build();

        final CaseDetails response = caseDetailsEnrichmentService.enrichCaseDetails(caseDetails, prosecutor);
        verify(idGenerationService, times(0)).generateCaseId(originalProsecutorCaseReference);
        verify(idGenerationService, times(0)).generateCaseReference();

        assertThat(originalCaseId, is(response.getCaseId()));
        assertThat(originalProsecutorCaseReference, is(response.getProsecutorCaseReference()));
    }
}
