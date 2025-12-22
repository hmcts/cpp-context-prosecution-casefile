package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionReceivedWithWarningsListenerTest {

    private final UUID CASE_ID = randomUUID();
    @InjectMocks
    private ProsecutionReceivedWithWarningListener prosecutionReceivedWithWarningListener;
    @Mock
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;
    @Mock
    private CaseDetailsRepository caseDetailsRepository;
    @Mock
    private BusinessValidationErrorRepository businessValidationErrorRepository;
    @Mock
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;
    @Mock
    private ProsecutionWithReferenceData prosecutionWithReferenceData;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private Prosecution prosecution;
    @Mock
    private CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings;
    @Mock
    private Envelope<CcCaseReceivedWithWarnings> caseReceivedWithWarningsEnvelope;

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;
    @Captor
    private ArgumentCaptor<CaseDetails> caseDetailsArgumentCaptor;

    @Test
    public void shouldReceiveCCcaseWithWarningAndDeleteValidationError() {
        prosecution = Prosecution.prosecution().withChannel(Channel.MCC).build();
        when(prosecutionWithReferenceData.getProsecution()).thenReturn(prosecution);
        when(caseReceivedWithWarningsEnvelope.payload()).thenReturn(ccCaseReceivedWithWarnings);
        when(ccCaseReceivedWithWarnings.getProsecutionWithReferenceData()).thenReturn(prosecutionWithReferenceData);
        when(prosecutionReceivedToCaseConverter.convert(ccCaseReceivedWithWarnings.getProsecutionWithReferenceData().getProsecution())).thenReturn(caseDetails);
        when(caseDetails.getCaseId()).thenReturn(CASE_ID);

        prosecutionReceivedWithWarningListener.prosecutionCCCaseReceivedWithWarning(caseReceivedWithWarningsEnvelope);

        verify(businessValidationErrorRepository).deleteByCaseId(uuidArgumentCaptor.capture());
        verify(businessValidationErrorCaseDetailsRepository).deleteByCaseId(CASE_ID);
        assertThat(uuidArgumentCaptor.getValue(), is(CASE_ID));

        verify(caseDetailsRepository).save(caseDetailsArgumentCaptor.capture());
        assertThat(caseDetailsArgumentCaptor.getValue(), is(caseDetails));

        verify(caseDetailsRepository).save(eq(caseDetails));
        verify(businessValidationErrorRepository).deleteByCaseId(CASE_ID);
        verify(businessValidationErrorCaseDetailsRepository).deleteByCaseId(CASE_ID);
    }

}