package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.HearingTypesRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingTypesRefDataEnricherTest {

    private static final String FHG_HEARING_CODE = "FHG";
    private static final String FHG_HEARING_DESCRIPTION = "First Hearing";

    private static final String FPTP_HEARING_CODE = "FPTP";
    private static final String FPTP_HEARING_DESCRIPTION = "Plea and Trial Further";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private HearingTypesRefDataEnricher hearingTypesRefDataEnricher;

    @Test
    public void testShouldPopulateHearingTypeRefData() {
        final HearingTypes mockRefDataHearingType = getMockHearingTypesReferenceData(FHG_HEARING_CODE, FHG_HEARING_DESCRIPTION);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(mockRefDataHearingType);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData(FHG_HEARING_CODE);

        hearingTypesRefDataEnricher.enrich(prosecutionWithReferenceData);

        assertNotNull(prosecutionWithReferenceData.getReferenceDataVO().getHearingType());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getHearingType().getHearingCode(), is(FHG_HEARING_CODE));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getHearingType().getHearingDescription(), is(FHG_HEARING_DESCRIPTION));
    }

    @Test
    public void testShouldPopulateHearingTypeRefDataMultiData() {
        final HearingTypes mockRefDataHearingType = getMockHearingTypesReferenceData(FHG_HEARING_CODE, FHG_HEARING_DESCRIPTION);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(mockRefDataHearingType);
        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = asList(getMockProsecutionWithReferenceData(FHG_HEARING_CODE), getMockProsecutionWithReferenceData(FHG_HEARING_CODE));

        hearingTypesRefDataEnricher.enrich(prosecutionWithReferenceDataList);

        assertNotNull(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getHearingType());
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getHearingType().getHearingCode(), is(FHG_HEARING_CODE));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getHearingType().getHearingDescription(), is(FHG_HEARING_DESCRIPTION));
        assertNotNull(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getHearingType());
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getHearingType().getHearingCode(), is(FHG_HEARING_CODE));
        assertThat(prosecutionWithReferenceDataList.get(1).getReferenceDataVO().getHearingType().getHearingDescription(), is(FHG_HEARING_DESCRIPTION));
        verify(referenceDataQueryService, times(1)).retrieveHearingTypes();
    }

    @Test
    public void testShouldPopulateHearingTypeNotFHGRefData() {
        final HearingTypes mockRefDataHearingType = getMockHearingTypesReferenceData(FPTP_HEARING_CODE, FPTP_HEARING_DESCRIPTION);

        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(mockRefDataHearingType);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData(FPTP_HEARING_CODE);

        hearingTypesRefDataEnricher.enrich(prosecutionWithReferenceData);

        assertNotNull(prosecutionWithReferenceData.getReferenceDataVO().getHearingType());
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getHearingType().getHearingCode(), is(FPTP_HEARING_CODE));
        assertThat(prosecutionWithReferenceData.getReferenceDataVO().getHearingType().getHearingDescription(), is(FPTP_HEARING_DESCRIPTION));
    }


    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData(final String hearingCode) {
        final Prosecution prosecution = Prosecution.prosecution()
                .withDefendants(getMockDefendantList(hearingCode))
                .withCaseDetails(CaseDetails.caseDetails()
                        .withSummonsCode("summonsCode").build())
                .build();

        return new ProsecutionWithReferenceData(prosecution);
    }

    private HearingTypes getMockHearingTypesReferenceData(final String hearingCode, final String hearingDescription) {
        List<HearingType> hearingTypesReferenceData = new ArrayList<>();
        hearingTypesReferenceData.add(HearingType.hearingType()
                .withId(UUID.randomUUID())
                .withSeqId(20)
                .withHearingCode(hearingCode)
                .withHearingDescription(hearingDescription)
                .build()

        );
        return HearingTypes.hearingTypes().withHearingtypes(hearingTypesReferenceData).build();
    }

    private List<Defendant> getMockDefendantList(final String hearingTypeCode){
        return Collections.singletonList(Defendant.defendant()
                .withInitialHearing(InitialHearing.initialHearing()
                        .withHearingTypeCode(hearingTypeCode)
                        .build())
                .build());
    }
}