package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_OUT_OF_TIME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext.withOffenceCodeReferenceDataOnly;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class OffenceOutOfTimeValidationRuleTest {

    private static final String OFFENCE_CODE = "OFCODE12";
    private static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2019, 01, 01);
    private static final LocalDate OFFENCE_CHARGE_DATE = LocalDate.of(2019, 05, 04);

    @Test
    public void shouldNotReturnProblemWhenProsecutionTimeLimitNotPresent() {
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                null,
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE, id);
    }

    @Test
    public void shouldNotReturnProblemOrCrashWhenProsecutionTimeLimitInDifferentFormat() {
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                "Months 6",
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE, id);
    }

    @Test
    public void shouldNotReturnProblemWhenWithinProsecutionTimeLimit() {
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                "6",
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE, id);
    }

    @Test
    public void shouldNotReturnProblemWhenExactlyAtProsecutionTimeLimit() {
        final LocalDate offenceCommittedDate = OFFENCE_CHARGE_DATE.minusMonths(36);
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                "36",
                is(Optional.empty()),
                offenceCommittedDate, id);
    }

    @Test
    public void shouldReturnProblemWhenOffenceOver36MonthTimeLimit() {
        final LocalDate offenceCommittedDate = OFFENCE_CHARGE_DATE.minusMonths(37);
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                "36",
                is(Optional.of(newProblem(OFFENCE_OUT_OF_TIME,
                        new ProblemValue(id.toString(),"daysOverdue", "30"),
                        new ProblemValue(id.toString(), FieldName.OFFENCE_CODE.getValue(), OFFENCE_CODE),
                        new ProblemValue(id.toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(1))
                ))),
                offenceCommittedDate, id);
    }


    @Test
    public void shouldReturnProblemWhenOffenceOverdue() {
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                "4",
                is(Optional.of(newProblem(OFFENCE_OUT_OF_TIME,
                        new ProblemValue(id.toString(),"daysOverdue", "3"),
                        new ProblemValue(id.toString(), FieldName.OFFENCE_CODE.getValue(), OFFENCE_CODE),
                        new ProblemValue(id.toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(1))
                ))),
                OFFENCE_COMMITTED_DATE, id);
    }

    private void validateOffenceAndCheckResult(
            final String prosecutionTimeLimit,
            final Matcher<Optional<Problem>> resultMatcher,
            final LocalDate offenceCommittedDate, UUID id) {

        final Optional<Problem> result = OffenceOutOfTimeValidationRule.INSTANCE.validate(
                Defendant.defendant()
                        .withOffences(singletonList(Offence.offence()
                                .withOffenceCode(OFFENCE_CODE)
                                .withOffenceCommittedDate(offenceCommittedDate)
                                .withChargeDate(OFFENCE_CHARGE_DATE)
                                .withOffenceId(id)
                                .withOffenceSequenceNumber(1)
                                .build()))
                        .build(),
                withOffenceCodeReferenceDataOnly(
                        singletonList(OffenceReferenceData.offenceReferenceData()
                                .withCjsOffenceCode(OFFENCE_CODE)
                                .withProsecutionTimeLimit(prosecutionTimeLimit)
                                .build())))
                .problems().stream().findFirst();

        assertThat(result, resultMatcher);
    }
}
