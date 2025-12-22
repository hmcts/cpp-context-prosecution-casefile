package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantIdpcAddedListenerTest {

    private final UUID defendantId = randomUUID();
    private final UUID materialId = randomUUID();

    @Mock
    private Envelope<DefendantIdpcAdded> defendantIdpcAddedEnvelope;
    @Mock
    CaseDocument caseDocument;
    @Mock
    private DefendantRepository defendantRepository;
    @Mock
    private DefendantDetails defendantDetails;
    @Mock
    private DefendantIdpcAdded defendantIdpcAdded;
    @InjectMocks
    private DefendantIdpcAddedListener defendantIdpcAddedListener;

    @Test
    public void shouldAddMaterialIdToDefendant() {
        when(defendantIdpcAddedEnvelope.payload()).thenReturn(defendantIdpcAdded);
        when(defendantIdpcAdded.getCaseDocument()).thenReturn(caseDocument);
        when(caseDocument.getMaterialId()).thenReturn(materialId);
        when(defendantIdpcAdded.getDefendantId()).thenReturn(defendantId);

        when(defendantRepository.findBy(defendantId.toString())).thenReturn(defendantDetails);

        defendantIdpcAddedListener.caseDocumentAdded(defendantIdpcAddedEnvelope);

        verify(defendantRepository).findBy(defendantId.toString());
        verify(defendantDetails).setIdpcMaterialId(eq(materialId));
        verify(defendantRepository).save(eq(defendantDetails));
    }

}
