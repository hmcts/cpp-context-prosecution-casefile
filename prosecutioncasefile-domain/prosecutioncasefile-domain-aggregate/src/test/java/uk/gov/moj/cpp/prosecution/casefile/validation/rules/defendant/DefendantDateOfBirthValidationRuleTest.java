package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_DOB_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_DOB;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDateOfBirthValidationRuleTest {
    @InjectMocks
    private DefendantDateOfBirthValidationRule defendantDateOfBirthValidationRule;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ReferenceDataVO referenceDataVO;

    private static final String DEFENDANT_ID = "1234243";

    @Test
    public void shouldReturnProblemWhenDefendantDateOfBirthIsInFuture() {
        final LocalDate defendantDateOfBirth = LocalDate.now().plusDays(1);
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(defendantDateOfBirth);

        final Optional<Problem> problem = defendantDateOfBirthValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(problem, equalTo(Optional.of(newProblem(DEFENDANT_DOB_IN_FUTURE, DEFENDANT_DOB.getValue(), defendantDateOfBirth))));
    }

    @Test
    public void shouldNotReturnProblemWhenDefendantDateOfBirthIsNotInFuture() {
        final LocalDate defendantDateOfBirth = LocalDate.now().minusDays(1);
        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(defendantDateOfBirth);

        final Optional<Problem> actualProblem = defendantDateOfBirthValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }

    @Test
    public void shouldNotReturnProblemWhenDefendantDateOfBirthIsNotPresent() {

        final DefendantWithReferenceData defendantWithReferenceData = getMockDefendantWithReferenceData(null);

        final Optional<Problem> actualProblem = defendantDateOfBirthValidationRule.validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(actualProblem, is(Optional.empty()));
    }


    private DefendantWithReferenceData getMockDefendantWithReferenceData(final LocalDate defendantDateOfBirth) {

        final SelfDefinedInformation selfDefinedInformation = SelfDefinedInformation.selfDefinedInformation().withDateOfBirth(defendantDateOfBirth).build();
        final Individual individual = Individual.individual().withSelfDefinedInformation(selfDefinedInformation).build();
        final Defendant defendant = new Defendant.Builder().withId(DEFENDANT_ID).withIndividual(individual).build();

        return new DefendantWithReferenceData(defendant, referenceDataVO, null);
    }
}
