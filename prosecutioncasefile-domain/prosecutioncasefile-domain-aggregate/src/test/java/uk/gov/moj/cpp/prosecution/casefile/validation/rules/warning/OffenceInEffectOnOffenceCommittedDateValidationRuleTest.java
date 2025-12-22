package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext.withOffenceCodeReferenceDataOnly;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class OffenceInEffectOnOffenceCommittedDateValidationRuleTest {
    public static final String NOT_SET = "NOT_SET";
    private static final String OFFENCE_CODE = "FOO";
    private static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.now();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String PROBLEM_VALUE = "Offence code: %s, Offence committed on: %s, offence in effect from: %s to %s";

    @Test
    public void shouldNotReturnProblemWhenOffenceInEffectOnOffenceCommittedDate() {

        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                OFFENCE_COMMITTED_DATE.minusYears(1).format(FORMATTER),
                OFFENCE_COMMITTED_DATE.plusYears(1).format(FORMATTER));
    }

    @Test
    public void shouldNotReturnProblemWhenOffenceInEffectTodayAndCommittedToday() {

        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                OFFENCE_COMMITTED_DATE.format(FORMATTER),
                OFFENCE_COMMITTED_DATE.plusYears(1).format(FORMATTER));
    }

    @Test
    public void shouldNotReturnProblemWhenOffenceWillNotBeInEffectTomorrowAndCommittedToday() {

        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                OFFENCE_COMMITTED_DATE.minusYears(1).format(FORMATTER),
                OFFENCE_COMMITTED_DATE.format(FORMATTER));
    }

    @Test
    public void shouldNotReturnProblemWhenOffenceHasNoStartDateAndCommittedBeforeOffenceInEffectEndDate() {

        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                null,
                OFFENCE_COMMITTED_DATE.plusYears(1).format(FORMATTER));
    }

    @Test
    public void shouldNotReturnProblemWhenOffenceHasNoEndDateAndCommittedAfterOffenceInEffectDate() {

        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                OFFENCE_COMMITTED_DATE.minusYears(1).format(FORMATTER),
                null);
    }

    @Test
    public void shouldNotReturnProblemWhenOffenceHasNoEndDateAndStartDate() {
        validateOffenceAndCheckResult(
                UUID.randomUUID(),
                is(Optional.empty()),
                OFFENCE_COMMITTED_DATE,
                null,
                null);
    }

    @Test
    public void shouldReturnProblemWhenOffenceCommittedBeforeOffenceIsInEffect() {
        final String offenceStartDate = OFFENCE_COMMITTED_DATE.plusDays(1).format(FORMATTER);
        final String offenceEndDate = OFFENCE_COMMITTED_DATE.format(FORMATTER);
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(
                id,
                getMatcher(id, offenceStartDate, offenceEndDate),
                OFFENCE_COMMITTED_DATE,
                offenceStartDate,
                offenceEndDate);
    }

    @Test
    public void shouldReturnProblemWhenOffenceCommittedAfterOffenceIsNotInEffect() {
        final String offenceStartDate = OFFENCE_COMMITTED_DATE.format(FORMATTER);
        final String offenceEndDate = OFFENCE_COMMITTED_DATE.minusDays(1).format(FORMATTER);
        final UUID id = UUID.randomUUID();
        validateOffenceAndCheckResult(id,
                getMatcher(id, offenceStartDate, offenceEndDate),
                OFFENCE_COMMITTED_DATE,
                offenceStartDate,
                offenceEndDate);
    }

    private Matcher<Optional<Problem>> getMatcher(UUID id, final String offenceStartDate, final String offenceEndDate) {
        return is(Optional.of(newProblem(OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE,
                new ProblemValue(id.toString(), "offenceInEffectDates", format(PROBLEM_VALUE,
                        OFFENCE_CODE,
                        OFFENCE_COMMITTED_DATE.toString(),
                        ofNullable(offenceStartDate).orElse(NOT_SET),
                        ofNullable(offenceEndDate).orElse(NOT_SET)
                )),
                new ProblemValue(id.toString(), FieldName.OFFENCE_CODE.getValue(), OFFENCE_CODE),
                new ProblemValue(id.toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(1))
                )));
    }

    private void validateOffenceAndCheckResult(
            final UUID offenceId,
            final Matcher<Optional<Problem>> resultMatcher,
            final LocalDate offenceCommittedDate,
            final String offenceStartDate,
            final String offenceEndDate) {

        final Offence offence = offence()
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceSequenceNumber(1)
                .withOffenceCommittedDate(offenceCommittedDate)
                .withOffenceId(offenceId)
                .build();

        final Defendant defendant = Defendant.defendant().withOffences(singletonList(offence)).build();

        final Optional<Problem> result = OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE.validate(
                defendant,
                withOffenceCodeReferenceDataOnly(
                        singletonList(offenceReferenceData()
                                .withCjsOffenceCode(OFFENCE_CODE)
                                .withOffenceStartDate(offenceStartDate)
                                .withOffenceEndDate(offenceEndDate)
                                .build())))
                .problems().stream().findFirst();

        assertThat(result, resultMatcher);
    }
}
