package uk.gov.moj.cpp.prosecution.casefile.refdata.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class InitiationTypesRefDataEnricherTest {

    private static final String INITIATION_TYPE = "S";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @InjectMocks
    private InitiationTypesRefDataEnricher initiationTypesRefDataEnricher;

    @Test
    public void testShouldPopulateInitiationTypesRefData() {
        when(referenceDataQueryService.getInitiationCodes()).thenReturn(Arrays.asList(INITIATION_TYPE, "J"));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData();
        initiationTypesRefDataEnricher.enrich(defendantWithReferenceData);
        assertNotNull(defendantWithReferenceData.getReferenceDataVO().getInitiationTypes());
        assertThat(defendantWithReferenceData.getReferenceDataVO().getInitiationTypes().size(), is(1));
        assertThat(defendantWithReferenceData.getReferenceDataVO().getInitiationTypes().get(0), is(INITIATION_TYPE));
        verify(referenceDataQueryService, times(1)).getInitiationCodes();
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData() {
        Defendant defendant = Defendant.defendant().withId(UUID.randomUUID().toString())
                .withInitiationCode(INITIATION_TYPE)
                .build();

        return new DefendantWithReferenceData(defendant, new ReferenceDataVO(), CaseDetails.caseDetails().withSummonsCode("summonsCode")
                .withInitiationCode(INITIATION_TYPE)
                .build());
    }

}