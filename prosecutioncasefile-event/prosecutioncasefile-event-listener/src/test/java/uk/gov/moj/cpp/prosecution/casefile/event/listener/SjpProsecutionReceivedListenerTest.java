package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionReceivedListenerTest {


    @Mock
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private SjpProsecutionReceived prosecutionReceived;

    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Mock
    private Envelope<SjpProsecutionReceived> sjpProsecutionReceivedEnvelope;

    @InjectMocks
    private SjpProsecutionReceivedListener prosecutionReceivedListener;


    @Test
    public void shouldCreateCaseDetails() {
        when(sjpProsecutionReceivedEnvelope.payload()).thenReturn(prosecutionReceived);
        when(prosecutionReceivedToCaseConverter.convert(prosecutionReceived.getProsecution())).thenReturn(caseDetails);

        prosecutionReceivedListener.prosecutionReceived(sjpProsecutionReceivedEnvelope);

        verify(prosecutionReceivedToCaseConverter).convert(prosecutionReceived.getProsecution());
        verify(caseDetailsRepository).save(eq(caseDetails));
    }

    @Test
    public void shouldUpdateCaseDetails() {
        when(sjpProsecutionReceivedEnvelope.payload()).thenReturn(prosecutionReceived);
        when(prosecutionReceivedToCaseConverter.convert(prosecutionReceived.getProsecution())).thenReturn(caseDetails);
        when(caseDetailsRepository.findBy(eq(caseDetails.getCaseId()))).thenReturn((caseDetails));

        prosecutionReceivedListener.prosecutionReceived(sjpProsecutionReceivedEnvelope);

        verify(caseDetailsRepository).remove(caseDetails);
        verify(prosecutionReceivedToCaseConverter).convert(prosecutionReceived.getProsecution());
        verify(caseDetailsRepository).save(eq(caseDetails));
    }

}
