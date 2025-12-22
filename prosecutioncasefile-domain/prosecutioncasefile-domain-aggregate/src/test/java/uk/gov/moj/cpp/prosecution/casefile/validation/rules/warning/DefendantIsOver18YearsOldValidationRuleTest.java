package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext.withOffenceCodeReferenceDataOnly;

import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class DefendantIsOver18YearsOldValidationRuleTest {

    private final LocalDate offenceChargeDate = LocalDate.of(2018, 10, 15);

    private ProsecutionCaseFile prosecutionCaseFile;

    @Test
    public void shouldReturnProblemWhenDefendantIsUnder18AtChargeDate() {
        final LocalDate defendantDateOfBirth = offenceChargeDate.minusYears(18).plusDays(1);
        final Defendant defendant = getProsecution(defendantDateOfBirth);

        final Optional<Problem> actualProblem = DefendantIsOver18YearsOldValidationRule.INSTANCE.validate(
                defendant,
                withOffenceCodeReferenceDataOnly(emptyList()))
                .problems().stream().findFirst();

        final Problem expectedProblem = newProblem(DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE,
                new ProblemValue(null,"dateOfBirth", defendantDateOfBirth.toString()),
                new ProblemValue(null,"chargeDate", offenceChargeDate.toString()));

        assertThat(actualProblem, equalTo(Optional.of(expectedProblem)));
    }

    @Test
    public void shouldNotReturnProblemWhenDefendantIsOver18AtChargeDate() {
        final LocalDate defendantDateOfBirth = offenceChargeDate.minusYears(18).minusDays(1);
        final Defendant defendant = getProsecution(defendantDateOfBirth);

        final Optional<Problem> actualProblem = DefendantIsOver18YearsOldValidationRule.INSTANCE.validate(
                defendant,
                withOffenceCodeReferenceDataOnly(emptyList()))
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }

    @Test
    public void shouldNotReturnProblemWhenDefendantDateOfBirthIsNotPresent() {
        final Defendant defendant = getProsecution(null);

        final Optional<Problem> actualProblem = DefendantIsOver18YearsOldValidationRule.INSTANCE.validate(
                defendant,
                withOffenceCodeReferenceDataOnly(emptyList()))
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }

    private Defendant getProsecution(final LocalDate dateOfBirth) {
        final Offence.Builder offenceBuilder = Offence.offence();
        return defendant()
                        .withIndividual(individual()
                                .withSelfDefinedInformation(selfDefinedInformation()
                                        .withDateOfBirth(dateOfBirth)
                                        .build())
                                .build())
                        .withOffences(singletonList(offenceBuilder
                                .withChargeDate(offenceChargeDate)
                                .build()))
                .build();
    }

}