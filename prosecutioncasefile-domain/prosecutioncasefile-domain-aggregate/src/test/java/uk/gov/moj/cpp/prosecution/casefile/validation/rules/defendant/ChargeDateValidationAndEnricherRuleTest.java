package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CHARGE_DATE_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CHARGE_DATE;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ChargeDateValidationRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChargeDateValidationAndEnricherRuleTest {

    private static final UUID OFFENCE_ID = randomUUID();
    private static final BigDecimal APPLIED_COMPENSATION = new BigDecimal("30.00");
    private static final BigDecimal BACK_DUTY = new BigDecimal("150.10");
    private static final LocalDate BACK_DUTY_DATE_FROM = LocalDate.of(2011, 1, 1);
    private static final LocalDate BACK_DUTY_DATE_TO = LocalDate.of(2015, 1, 1);
    private static final LocalDate ARREST_DATE = LocalDate.of(2017, 11, 8);
    private static final UUID MOT_REASON_ID = randomUUID();
    private static final String OFFENCE_CODE = "OFCODE12";
    private static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2017, 6, 1);
    private static final LocalDate OFFENCE_COMMITTED_END_DATE = LocalDate.of(2017, 6, 20);
    private static final Integer OFFENCE_DATE_CODE = 15;
    private static final String OFFENCE_LOCATION = "London";
    private static final Integer OFFENCE_SEQUENCE_NUMBER = 3;
    private static final String OFFENCE_TITLE = "Offence Title";
    private static final String OFFENCE_TITLE_WELSH = "Offence Title (Welsh)";
    private static final String OFFENCE_WORDING = "TV Licence not paid";
    private static final String OFFENCE_WORDING_WELSH = "TV Licence not paid (Welsh)";
    private static final String STATEMENT_OF_FACTS = "Prosecution charge wording";
    private static final String STATEMENT_OF_FACTS_WELSH = "Prosecution charge wording (Welsh)";
    public static final String CHARGE_DATE_NOT_PROVIDED = "Charge date not provided";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenNoOffencesAndCaseInitiationNotMandatory() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(null);
        final Optional<Problem> optionalProblem = new ChargeDateValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyListWhenChargeDatePresentForOtherCaseTypes() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getMockOffences(now().minusMonths(5)));
        final Optional<Problem> optionalProblem = new ChargeDateValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnViolationWhenNoChargeDateAndCaseInitiationMandatory() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getMockOffences(null));

        final Optional<Problem> optionalProblem = new ChargeDateValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(CHARGE_DATE_IN_FUTURE.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(OFFENCE_CHARGE_DATE.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(CHARGE_DATE_NOT_PROVIDED));
    }

    @Test
    public void shouldReturnEmptyListWhenChargeDateInThePast() {
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getMockOffences(now().minusMonths(5)));
        final Optional<Problem> optionalProblem = new ChargeDateValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(false));
    }

    @Test
    public void shouldReturnViolationWhenChargeDateInTheFuture() {
        final LocalDate chargeDate = now().plusMonths(5);
        when(defendantWithReferenceData.getDefendant().getOffences()).thenReturn(getMockOffences(chargeDate));
        final Optional<Problem> optionalProblem = new ChargeDateValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();
        assertThat(optionalProblem.isPresent(), is(true));
        assertThat(optionalProblem.get().getCode(), is(CHARGE_DATE_IN_FUTURE.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is(OFFENCE_CHARGE_DATE.getValue()));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(chargeDate.toString()));
    }


    private List<Offence> getMockOffences(final LocalDate chargeDate) {
        Offence offence = Offence.offence()
                .withAppliedCompensation(APPLIED_COMPENSATION)
                .withArrestDate(ARREST_DATE)
                .withBackDuty(BACK_DUTY)
                .withBackDutyDateFrom(BACK_DUTY_DATE_FROM)
                .withBackDutyDateTo(BACK_DUTY_DATE_TO)
                .withChargeDate(chargeDate)
                .withMotReasonId(MOT_REASON_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE)
                .withOffenceDateCode(OFFENCE_DATE_CODE)
                .withOffenceId(OFFENCE_ID)
                .withOffenceLocation(OFFENCE_LOCATION)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                .withOffenceWording(OFFENCE_WORDING)
                .withOffenceWordingWelsh(OFFENCE_WORDING_WELSH)
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .build();

        return Collections.singletonList(offence);
    }

}
