package uk.gov.moj.cpp.prosecution.casefile.refdata.proscase;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class GroupCasesInitiationCodeReferenceDataEnricherTest {

    private static final List<String> INITIATION_CODES = asList("C", "S");

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private GroupCasesInitiationCodeReferenceDataEnricher groupCasesInitiationCodeReferenceDataEnricher;

    @Test
    public void shouldPopulateInitiationCodeReferenceData() {

        when(referenceDataQueryService.getInitiationCodes()).thenReturn(INITIATION_CODES);

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(), getMockProsecutionWithReferenceData());
        groupCasesInitiationCodeReferenceDataEnricher.enrich(prosecutionWithReferenceDataList);

        verify(referenceDataQueryService, times(1)).getInitiationCodes();
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().size(), is(2));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().get(0), is("C"));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getInitiationTypes().get(1), is("S"));

        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes(), notNullValue());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().size(), is(2));
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().get(0), is("C"));
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getInitiationTypes().get(1), is("S"));

    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData() {

        return new ProsecutionWithReferenceData(
                Prosecution.prosecution()
                        .withCaseDetails(
                                CaseDetails.caseDetails().
                                        withSummonsCode("summonsCode")
                                        .build()
                        )
                        .build()
        );

    }

}