package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedWithWarningsToCase;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionReceivedWithWarningsListenerTest {

    private static final UUID CASE_ID = randomUUID();

    @InjectMocks
    private SjpProsecutionReceivedWithWarningsListener sjpProsecutionReceivedWithWarningsListener;

    @Mock
    private ProsecutionReceivedWithWarningsToCase prosecutionReceivedWithWarningsToCaseConverter;

    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Mock
    private SjpProsecutionReceivedWithWarnings sjpProsecutionReceivedWithWarnings;

    @Mock
    private Envelope<SjpProsecutionReceivedWithWarnings> sjpProsecutionReceivedWithWarningsEnvelope;

    @Mock
    private CaseDetails caseDetails;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    private ArgumentCaptor<CaseDetails> caseDetailsArgumentCaptor;

    @Test
    public void shouldProcessSjpProsecutionReceivedWithWarnings() {
        when(caseDetails.getCaseId()).thenReturn(CASE_ID);
        when(sjpProsecutionReceivedWithWarningsEnvelope.payload()).thenReturn(sjpProsecutionReceivedWithWarnings);
        when(prosecutionReceivedWithWarningsToCaseConverter.convert(sjpProsecutionReceivedWithWarnings)).thenReturn(caseDetails);

        sjpProsecutionReceivedWithWarningsListener.prosecutionReceived(sjpProsecutionReceivedWithWarningsEnvelope);


        verify(businessValidationErrorRepository).deleteByCaseId(uuidArgumentCaptor.capture());
        verify(businessValidationErrorCaseDetailsRepository).deleteByCaseId(CASE_ID);
        assertThat(uuidArgumentCaptor.getValue(), is(CASE_ID));

        verify(caseDetailsRepository).save(caseDetailsArgumentCaptor.capture());
        assertThat(caseDetailsArgumentCaptor.getValue(), is(caseDetails));
    }

}