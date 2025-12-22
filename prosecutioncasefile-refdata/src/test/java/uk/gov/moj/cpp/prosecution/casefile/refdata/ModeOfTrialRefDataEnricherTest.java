package uk.gov.moj.cpp.prosecution.casefile.refdata;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.ModeOfTrialRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ModeOfTrialRefDataEnricherTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private ModeOfTrialRefDataEnricher enricher;

    @Test
    public void shouldHaveModeOfTrialRefData() {
        UUID reasonId = randomUUID();
        List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceData = Arrays.asList(
                createModeOfTrial(randomUUID(), "01", "Summary-only offence", "90"),
                createModeOfTrial(reasonId, "02", "Indictable-only offence", "100"),
                createModeOfTrial(randomUUID(), "03", "Summary-only offence", "100"));
        when(referenceDataQueryService.retrieveModeOfTrialReasons()).thenReturn(modeOfTrialReferenceData);
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(of(Defendant
                .defendant()
                .withOffences(of(Offence.offence().withMotReasonId(reasonId).build()))
                .build()));
        enricher.enrich(defendantsWithReferenceData);
        assertThat(defendantsWithReferenceData, is(notNullValue()));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().size(), is(1));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().get(0).getCode(), is("02"));
    }

    @Test
    public void shouldHaveModeOfTrialRefDataWithSummaryOnlyOffenceWhenMOTReasonIdIsNull() {
        UUID reasonId = randomUUID();
        List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceData = Arrays.asList(
                createModeOfTrial(randomUUID(), "01", "EitherWay offence", "90"),
                createModeOfTrial(reasonId, "02", "Indictable-only offence", "100"),
                createModeOfTrial(randomUUID(), "03", "Summary-only offence", "100"));
        when(referenceDataQueryService.retrieveModeOfTrialReasons()).thenReturn(modeOfTrialReferenceData);
        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(of(Defendant
                .defendant()
                .withOffences(of(Offence.offence().withMotReasonId(reasonId).build(),
                        Offence.offence().build()))
                .build()));
        enricher.enrich(defendantsWithReferenceData);
        assertThat(defendantsWithReferenceData, is(notNullValue()));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().size(), is(2));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().get(0).getCode(), is("02"));
        assertThat(defendantsWithReferenceData.getReferenceDataVO().getModeOfTrialReasonsReferenceData().get(1).getCode(), is("03"));
    }


    @Test
    public void shouldHaveModeOfTrialRefDataMultiData() {
        UUID reasonId = randomUUID();

        List<ModeOfTrialReasonsReferenceData> modeOfTrialReferenceData = asList(
                createModeOfTrial(randomUUID(), "01", "EitherWay offence", "90"),
                createModeOfTrial(reasonId, "02", "Indictable-only offence", "100"),
                createModeOfTrial(randomUUID(), "03", "Summary-only offence", "100"));

        when(referenceDataQueryService.retrieveModeOfTrialReasons()).thenReturn(modeOfTrialReferenceData);

        final List<DefendantsWithReferenceData> prosecutionWithReferenceDataList = asList(
                createDefendantWithReferenceData(reasonId),
                createDefendantWithReferenceData(reasonId));

        enricher.enrich(prosecutionWithReferenceDataList);
        assertThat(prosecutionWithReferenceDataList, is(notNullValue()));
        assertThat(prosecutionWithReferenceDataList.get(0).getReferenceDataVO().getModeOfTrialReasonsReferenceData().size(), is(2));
        verify(referenceDataQueryService, times(1)).retrieveModeOfTrialReasons();
    }

    private DefendantsWithReferenceData createDefendantWithReferenceData(final UUID reasonId){
        return new DefendantsWithReferenceData(asList(Defendant
                .defendant()
                .withOffences(of(Offence.offence().withMotReasonId(reasonId).build(),
                        Offence.offence().build()))
                .build()));
    }

    private ModeOfTrialReasonsReferenceData createModeOfTrial(final UUID id, final String code, final String description, final String sequenceNum) {
        return ModeOfTrialReasonsReferenceData.modeOfTrialReasonsReferenceData()
                .withId(id.toString())
                .withCode(code)
                .withDescription(description)
                .withSeqNum(sequenceNum)
                .build();
    }
}