package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_NOT_SUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_SEQUENCE_NO;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

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
class OffenceCodeValidationAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "MOCK CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;
    @InjectMocks
    private OffenceCodeValidationAndEnricherRule offenceCodeValidationAndEnricherRule;

    @Test
    void shouldNotInvalidateValidOffenceCode() {

        when(referenceDataQueryService.retrieveOffenceData(any(),any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(MOCK_OFFENCE_CODE);

        final ValidationResult problem = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(problem.isValid(), is(true));

    }

    @Test
    void shouldValidateInvalidOffenceCode() {
        final String offenceCode2 = "code2" ;
        when(referenceDataQueryService.retrieveOffenceData(any(),any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offenceCode2);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is( OFFENCE_CODE_IS_INVALID.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is( OFFENCE_CODE.getValue()));
        assertThat(problem.get().getValues().get(0).getValue(), is(offenceCode2));
    }

    @Test
    void shouldNotRaiseProblemWhenOffenceCodeIsGenericAltered() {
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData("998A");
        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);
        Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(false));
    }

    @Test
    void shouldReturnValidWhenDefendantIsNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(null, referenceDataVO, caseDetails);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    @Test
    void shouldReturnValidWhenOffencesAreNull() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant defendant = new Defendant.Builder().withId("D1").withInitiationCode("C").build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    @Test
    void shouldReturnValidWhenOffencesAreEmpty() {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Defendant defendant = new Defendant.Builder().withId("D1").withInitiationCode("C").withOffences(emptyList()).build();
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    @Test
    void shouldNotRaiseProblemWhenOffenceFoundInReferenceDataVO() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        realVO.setOffenceReferenceData(new ArrayList<>(Collections.singletonList(
                offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build())));
        final DefendantWithReferenceData defendantWithReferenceData = getDefendantWithReferenceData(MOCK_OFFENCE_CODE, realVO, false);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    @Test
    void shouldAddNewRefDataToExistingVOListWhenVOListNotNull() {
        final ReferenceDataVO realVO = new ReferenceDataVO();
        realVO.setOffenceReferenceData(new ArrayList<>());
        when(referenceDataQueryService.retrieveOffenceData(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build()));
        final DefendantWithReferenceData defendantWithReferenceData = getDefendantWithReferenceData(MOCK_OFFENCE_CODE, realVO, false);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
        assertThat(realVO.getOffenceReferenceData().size(), is(1));
        assertThat(realVO.getOffenceReferenceData().get(0).getCjsOffenceCode(), is(MOCK_OFFENCE_CODE));
    }

    @Test
    void shouldRaiseOffenceCodeNotSupportedForCivilCaseWhenRefDataNotFound() {
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(emptyList());
        final DefendantWithReferenceData defendantWithReferenceData = getDefendantWithReferenceData(MOCK_OFFENCE_CODE, new ReferenceDataVO(), true);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        final Optional<Problem> problem = result.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(OFFENCE_CODE_NOT_SUPPORTED.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is(OFFENCE_CODE.getValue()));
        assertThat(problem.get().getValues().get(0).getValue(), is(MOCK_OFFENCE_CODE));
        assertThat(problem.get().getValues().get(1).getKey(), is(OFFENCE_SEQUENCE_NO.getValue()));
    }

    @Test
    void shouldNotRaiseProblemForCivilCaseWhenRefDataFound() {
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any()))
                .thenReturn(Collections.singletonList(offenceReferenceData().withCjsOffenceCode(MOCK_OFFENCE_CODE).build()));
        final DefendantWithReferenceData defendantWithReferenceData = getDefendantWithReferenceData(MOCK_OFFENCE_CODE, new ReferenceDataVO(), true);

        final ValidationResult result = offenceCodeValidationAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        assertThat(result.isValid(), is(true));
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final String offenceCode) {
        final String DEFENDANT_ID = "1234243";
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Offence offence = Offence.offence()
                                .withOffenceId(UUID.randomUUID())
                                .withOffenceCode(offenceCode)
                                .withOffenceSequenceNumber(1)
                                .build();

        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID)

                .withOffences(Arrays.asList(offence))
                .withInitiationCode("C")
                .build();

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }

    private DefendantWithReferenceData getDefendantWithReferenceData(final String offenceCode, final ReferenceDataVO referenceDataVO, final boolean isCivil) {
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(offenceCode)
                .withOffenceSequenceNumber(1)
                .build();
        final Defendant defendant = new Defendant.Builder().withId("D1")
                .withOffences(Arrays.asList(offence))
                .withInitiationCode("C")
                .build();
        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails, false, false, false, isCivil);
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode)
                .build()
        );
    }

}
