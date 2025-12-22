package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.InitiationTypesRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class InitiationTypesRefDataEnricherTest {

    private static final String INITIATIONTYPE = "S";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private InitiationTypesRefDataEnricher initiationTypesRefDataEnricher;

    @Test
    public void testShouldPopulateInitiationTypesRefData() {
        when(referenceDataQueryService.getInitiationCodes()).thenReturn(asList(INITIATIONTYPE, "J"));
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData();
        initiationTypesRefDataEnricher.enrich(prosecutionWithReferenceData);
        assertNotNull(prosecutionWithReferenceData.getReferenceDataVO().getInitiationTypes());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getInitiationTypes().size(), is(1));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getInitiationTypes().get(0), is(INITIATIONTYPE));
        verify(referenceDataQueryService, times(1)).getInitiationCodes();
    }

    @Test
    public void testShouldPopulateInitiationTypesRefDataMultiData() {
        when(referenceDataQueryService.getInitiationCodes()).thenReturn(asList(INITIATIONTYPE));
        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(), getMockProsecutionWithReferenceData());
        initiationTypesRefDataEnricher.enrich(prosecutionWithReferenceDataList);
        assertNotNull(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().get(0), is(INITIATIONTYPE));
        assertNotNull(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().get(0), is(INITIATIONTYPE));
        verify(referenceDataQueryService, times(1)).getInitiationCodes();
    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData() {

        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(CaseDetails.caseDetails()
                        .withSummonsCode("summonsCode")
                        .withInitiationCode(INITIATIONTYPE)
                        .build())
                .build();

        return new ProsecutionWithReferenceData(prosecution);
    }

}