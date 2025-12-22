package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidationResultTest {

    @Test
    public void shouldReturnValidIfHasEmptyOrNullProblems() {
        List<Problem> list = null;
        ValidationResult validationResult = ValidationResult.newValidationResult(list);
        assertThat(validationResult.isValid(), is(true));

        validationResult = ValidationResult.newValidationResult(emptyList());
        assertThat(validationResult.isValid(), is(true));
    }

    @Test
    public void shouldReturnFirstProblem() {

        Problem firstProblem = newProblem(OFFENCE_CODE_IS_INVALID,
                new ProblemValue(null, "offenceCode", "NO SUCH CODE"),
                new ProblemValue(null, "offenceSequenceNo", "2")

        );

        ValidationResult validationResult = newValidationResult(asList(
                firstProblem,
                newProblem(OFFENCE_CODE_IS_INVALID,
                        new ProblemValue(null, "offenceCode", "NO SUCH CODE2"),
                        new ProblemValue(null, "offenceSequenceNo", "3"))
        ));


        assertThat(validationResult.problems().get(0), equalTo(firstProblem));
    }


}
