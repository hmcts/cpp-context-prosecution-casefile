package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_REQUIRES_A_LOCATION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_LOCATION;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
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
class OffenceLocationValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenceLocationValidationAndEnricherRule offenceLocationValidationAndEnricherRule;

    @Test
    void shouldCreateProblemWhenOffenceLocationIsEmptyAndLocationRequiredFlagIsTrue() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(MOCK_OFFENCE_CODE);

        final Optional<Problem> problem = offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));

    }

    @Test
    void shouldValidateInvalidOffenceLocationCode() {
        when(referenceDataVO.getOffenceReferenceData()).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(MOCK_OFFENCE_CODE);

        final Optional<Problem> problem = offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_REQUIRES_A_LOCATION.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is(OFFENCE_LOCATION.getValue()));
        assertThat(problem.get().getValues().get(0).getValue(), is(""));
    }

    @Test
    void shouldReturnValidWhenDefendantIsNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(null, referenceDataVO, caseDetails);

        assertThat(offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }

    @Test
    void shouldReturnValidWhenOffencesAreEmpty() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant defendant = new Defendant.Builder().withId("D1").withInitiationCode("C").withOffences(emptyList()).build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);

        assertThat(offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }

    @Test
    void shouldReturnProblemWhenReferenceDataQueryServiceIsNull() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(MOCK_OFFENCE_CODE, realVO, false);

        final Optional<Problem> problem = offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, null)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_REQUIRES_A_LOCATION.name()));
    }

    @Test
    void shouldFetchFromServiceForCivilCaseWhenVOEmpty() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).withLocationRequired("Y").build()));
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(MOCK_OFFENCE_CODE, realVO, true);

        final Optional<Problem> problem = offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_REQUIRES_A_LOCATION.name()));
    }

    @Test
    void shouldAddNewRefDataToExistingVOListWhenVOListNotNull() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        realVO.setOffenceReferenceData(new ArrayList<>());
        when(referenceDataQueryService.retrieveOffenceData(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).withLocationRequired("Y").build()));
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(MOCK_OFFENCE_CODE, realVO, false);

        final Optional<Problem> problem = offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(realVO.getOffenceReferenceData().size(), is(1));
    }

    @Test
    void shouldReturnValidWhenVOAndServiceReturnEmpty() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(emptyList());
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(MOCK_OFFENCE_CODE, realVO, false);

        assertThat(offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }

    @Test
    void shouldReturnValidWhenLocationRequiredFlagIsNotY() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        realVO.setOffenceReferenceData(new ArrayList<>(Collections.singletonList(
                offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).withLocationRequired("N").build())));
        final DefendantWithReferenceData defendantWithReferenceData = buildDefendant(MOCK_OFFENCE_CODE, realVO, false);

        assertThat(offenceLocationValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService).isValid(), is(true));
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String offenceCode) {
        final String DEFENDANT_ID = "1234243";
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .build();

        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID)

                .withOffences(Arrays.asList(offence))
                .withInitiationCode("C")
                .build();

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private DefendantWithReferenceData buildDefendant(final String offenceCode, final ReferenceDataVO referenceDataVO, final boolean isCivil) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withOffenceSequenceNumber(1)
                .build();
        final Defendant defendant = new Defendant.Builder().withId("D1").withInitiationCode("C")
                .withOffences(Arrays.asList(offence)).build();
        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails, false, false, false, isCivil);
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode).withLocationRequired("Y")
                .build()
        );
    }

}
