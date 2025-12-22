package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildSpiProsecutionDefendantsAdded;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.SpiProsecutionDefendantsAdded;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionToCCAddDefendantConverterTest {

    @Mock
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Mock
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @InjectMocks
    private SjpProsecutionToCCAddDefendantConverter converter;

    @Test
    public void convert() {
        final SpiProsecutionDefendantsAdded spiProsecutionDefendantsAdded = buildSpiProsecutionDefendantsAdded();

        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(getMockConvertedDefendants());
        when(prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(getMockListHearingRequest());

        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = converter.convert(spiProsecutionDefendantsAdded);
        assertNotNull(addDefendantsToCourtProceedings);
        assertThat(addDefendantsToCourtProceedings.getDefendants().size(), is(1));
        assertThat(addDefendantsToCourtProceedings.getListHearingRequests().size(), is(1));
    }

    private List<uk.gov.justice.core.courts.Defendant> getMockConvertedDefendants() {
        List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<>();
        defendants.add(uk.gov.justice.core.courts.Defendant.defendant().withId(UUID.randomUUID()).build());
        return defendants;
    }

    private List<uk.gov.justice.core.courts.ListHearingRequest> getMockListHearingRequest() {
        List<uk.gov.justice.core.courts.ListHearingRequest> hearingRequests = new ArrayList<>();
        hearingRequests.add(ListHearingRequest.listHearingRequest().build());
        return hearingRequests;
    }
}