package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffenceDrugLevelMethodValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @InjectMocks
    private OffenceDrugLevelMethodValidationAndEnricherRule offenceDrugLevelMethodValidationAndEnricherRule;

    @Test
    void shouldReturnEmptyListWhenNoOffences() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);
        final Optional<Problem> optionalProblem = offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    void shouldReturnValidWhenDefendantIsNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(null, referenceDataVO, caseDetails);

        assertThat(offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }


    @Test
    void shouldReturnProblemWhenOffenceIsWithoutAlcoholLevelInfo() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffence(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();


        assertThat(optionalProblem.isPresent(), is(true));
    }

    @Test
    void shouldReturnProblemWhenOffenceIsWithAlcoholLevelInfo() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(getOffenceWithAlcoholLevelInfo(MOCK_OFFENCE_CODE));
        final Optional<Problem> optionalProblem = offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    void shouldEnrichRefDataAndReturnValidForCivilCaseWhenVOEmpty() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build()));
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(getOffenceWithAlcoholLevelInfo(MOCK_OFFENCE_CODE), realVO, true);

        assertThat(offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
        assertThat(realVO.getOffenceReferenceData().size(), is(1));
    }

    @Test
    void shouldEnrichRefDataFromNonCivilServiceWhenVOEmpty() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceData(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build()));
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(getOffenceWithAlcoholLevelInfo(MOCK_OFFENCE_CODE), realVO, false);

        assertThat(offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
        assertThat(realVO.getOffenceReferenceData().size(), is(1));
    }

    @Test
    void shouldReturnValidWhenServiceReturnsEmptyAndVOEmpty() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(emptyList());
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(getOffence(MOCK_OFFENCE_CODE), realVO, false);

        assertThat(offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }

    @Test
    void shouldReturnProblemWhenAlcoholMethodMissingButOffenceHasAlcoholRelatedRecord() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        realVO.setOffenceReferenceData(new ArrayList<>(Collections.singletonList(
                offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).withDrugsOrAlcoholRelated("Y").build())));
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withAlcoholRelatedOffence(AlcoholRelatedOffence.alcoholRelatedOffence().withAlcoholLevelAmount(1).build())
                .build();
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(offence, realVO, false);

        final Optional<Problem> optionalProblem = offenceDrugLevelMethodValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final Offence offence) {
        final String DEFENDANT_ID = "1234243";
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant.Builder defendantBuilder = new Defendant.Builder().withId(DEFENDANT_ID).withInitiationCode("C");
        if (offence != null) {

            defendantBuilder
                    .withOffences(Arrays.asList(offence));

        }
        final Defendant defendant = defendantBuilder.build();
        final List<OffenceReferenceData> offenceReferenceData = new ArrayList<>();
        offenceReferenceData.add(new OffenceReferenceData.Builder()
                .withCjsOffenceCode(MOCK_OFFENCE_CODE)
                .build());

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private DefendantWithReferenceData buildDefendant(final Offence offence, final ReferenceDataVO referenceDataVO, final boolean isCivil) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant defendant = new Defendant.Builder().withId("D1").withInitiationCode("C")
                .withOffences(Arrays.asList(offence)).build();
        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails, false, false, false, isCivil);
    }

    private Offence getOffence(final String offenceCode) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();
    }

    private Offence getOffenceWithAlcoholLevelInfo(final String offenceCode) {
        return Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withAlcoholRelatedOffence(AlcoholRelatedOffence.alcoholRelatedOffence().withAlcoholLevelAmount(1).withAlcoholLevelMethod("A").build())
                .build();
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode).withDrugsOrAlcoholRelated("Y")
                .build()
        );
    }

}
