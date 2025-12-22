package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.BACKDUTY_FROMDATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.BACKDUTY_FROMDATE_TODATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.BACKDUTY_TODATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.BACKDUTY_VALUE;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 *  \section Integration Tests
 *
 */

@ExtendWith(MockitoExtension.class)
public class OffenceBackDutyValidationRuleAndEnricherRuleTest {

    private static final String MOCK_OFFENCE_CODE = "OFFENCE_CODE";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @InjectMocks
    private OffenceBackDutyValidationRuleAndEnricherRule offenceBackDutyValidationRuleAndEnricherRule;

    @Test
    public void shouldValidateOffencesAreNull() {
        //given
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);

        //when
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        final Optional<Problem> problem = validateResult.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(false));
    }

    @Test
    public void shouldValidateOffenceBackDutyValueIsNull() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        final Optional<Problem> problem = validateResult.problems().stream().filter(
                p -> p.getCode().equals(ProblemCode.BACK_DUTY_AMOUNT_MISSING.name())).findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getValues().get(0).getValue(), is(notNullValue()));

    }


    @Test
    public void shouldValidationOffenceGivesOffenceCodeAndOffenceSequenceNo() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        final Optional<Problem> problem = validateResult.problems().stream().findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getCode(), is(ProblemCode.BACK_DUTY_AMOUNT_MISSING.name()));
        assertThat(problem.get().getValues().get(0).getKey(), is(FieldName.OFFENCE_CODE.getValue()));
        assertThat(problem.get().getValues().get(1).getKey(), is(FieldName.OFFENCE_SEQUENCE_NO.getValue()));
        assertThat(problem.get().getValues().get(2).getKey(), is(BACKDUTY_VALUE.getValue()));

    }
    @Test
    public void shouldValidateOffenceBackDutyFromAndToAreNull() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withBackDuty(new BigDecimal("0.00"))
                .withOffenceSequenceNumber(1)
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        assertThat(validateResult.isValid(), is(true));
    }

    @Test
    public void shouldValidateOffenceBackDutyFrom() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .withBackDuty(new BigDecimal("0.00"))
                .withBackDutyDateFrom(LocalDate.now())
                .build();
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //When
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //then
        final Optional<Problem> problem = validateResult.problems().stream().filter(
                p -> p.getCode().equals(ProblemCode.BACK_DUTY_DATE_RANGE_INVALID.name())).findFirst();

        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getValues().get(2).getKey(), is(BACKDUTY_TODATE.getValue()));
        assertThat(problem.get().getValues().get(2).getValue(), is(notNullValue()));
    }

    @Test
    public void shouldValidateOffenceBackDutyTo() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withBackDuty(new BigDecimal("0.00"))
                .withOffenceSequenceNumber(1)
                .withBackDutyDateTo(LocalDate.now())
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        final Optional<Problem> problem = validateResult.problems().stream().filter(
                p -> p.getCode().equals(ProblemCode.BACK_DUTY_DATE_RANGE_INVALID.name())).findFirst();
        assertThat(problem.isPresent(), is(true));
        assertThat(problem.get().getValues().get(2).getKey(), is(BACKDUTY_FROMDATE.getValue()));
        assertThat(problem.get().getValues().get(2).getValue(), is(notNullValue()));
    }

    @Test
    public void shouldDetermineOffenceBackDutyFromDateEqualToToDateIsValid() {
        //given
        LocalDate backDutyDate = LocalDate.now();
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .withBackDuty(new BigDecimal("0.00"))
                .withBackDutyDateFrom(backDutyDate)
                .withBackDutyDateTo(backDutyDate)
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        assertThat("No validation errors", validateResult.isValid());
    }

    @Test
    public void shouldValidateOffenceBackDutyFromDateAfterToDate() {
        //given
        final Offence offence = Offence.offence()
                .withOffenceId(UUID.randomUUID())
                .withOffenceCode(MOCK_OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .withBackDuty(new BigDecimal("0.00"))
                .withBackDutyDateFrom(LocalDate.now())
                .withBackDutyDateTo(LocalDate.now().minusDays(1))
                .build();

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(offence);

        //when
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(getMockOffenceCodesReferenceData(MOCK_OFFENCE_CODE));
        final ValidationResult validateResult = offenceBackDutyValidationRuleAndEnricherRule.validate(defendantWithReferenceData, referenceDataQueryService);

        //Then
        final Optional<Problem> problem = validateResult.problems().stream().filter(
                p -> p.getCode().equals(ProblemCode.BACK_DUTY_DATE_RANGE_INVALID.name())).findFirst();
        assertThat(problem.get().getCode(), is(ProblemCode.BACK_DUTY_DATE_RANGE_INVALID.name()));
        assertThat(problem.get().getValues().get(2).getKey(), is(BACKDUTY_FROMDATE_TODATE.getValue()));
        assertThat(problem.get().getValues().get(2).getValue(), is(notNullValue()));
    }

    private List<OffenceReferenceData> getMockOffenceCodesReferenceData(final String offenceCode) {
        return Arrays.asList(offenceReferenceData().withCjsOffenceCode(offenceCode)
                .withBackDuty(true)
                .build()
        );
    }

    private DefendantWithReferenceData getMockDefendantWithReferenceData(final Offence... offences) {
        final String DEFENDANT_ID = "1234243";
        final CaseDetails caseDetails = CaseDetails.caseDetails().withInitiationCode("S").build();
        List<Offence> offenceList = offences == null ? Collections.emptyList() : Arrays.asList(offences);
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID)
                .withOffences(offenceList)
                .withInitiationCode("C")
                .build();

        return new DefendantWithReferenceData(defendant, referenceDataVO, caseDetails);
    }
}
