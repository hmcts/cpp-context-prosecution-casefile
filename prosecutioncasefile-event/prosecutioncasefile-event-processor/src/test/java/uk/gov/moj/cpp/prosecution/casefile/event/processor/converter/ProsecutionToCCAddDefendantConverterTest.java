package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionToCCAddDefendantConverterTest {

    @Mock
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Mock
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @InjectMocks
    private ProsecutionToCCAddDefendantConverter converter;

    @Test
    public void shouldConvertProsecutionDefendantsAddedToAddDefendantsToCourtProceedings() {
        final ProsecutionDefendantsAdded prosecutionDefendantsAdded = CaseReceivedHelper.buildProsecutionDefendantsAdded();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(getMockConvertedDefendants());
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(getMockListHearingRequest());

        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = converter.convert(prosecutionDefendantsAdded);
        assertNotNull(addDefendantsToCourtProceedings);
        assertThat(addDefendantsToCourtProceedings.getDefendants(), hasSize(1));
        assertThat(addDefendantsToCourtProceedings.getListHearingRequests(), hasSize(1));
    }

    private List<Defendant> getMockConvertedDefendants() {
        List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant().withId(randomUUID()).build());
        return defendants;
    }

    private List<ListHearingRequest> getMockListHearingRequest() {
        List<ListHearingRequest> hearingRequests = new ArrayList<>();
        hearingRequests.add(ListHearingRequest.listHearingRequest().build());
        return hearingRequests;
    }
}