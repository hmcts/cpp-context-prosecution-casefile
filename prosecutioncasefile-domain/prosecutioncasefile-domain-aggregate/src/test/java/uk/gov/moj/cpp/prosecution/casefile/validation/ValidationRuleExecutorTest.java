package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ValidationRuleExecutorTest {

    private static final String PROBLEM_CODE_1 = "PROBLEM_CODE_1";
    private static final String PROBLEM_CODE_2 = "PROBLEM_CODE_2";

    @Mock
    private ValidationRule<SjpProsecutionReceived, ProsecutionCaseFile> validationRule1, validationRule2;

    @Mock
    private SjpProsecutionReceived prosecution;

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Test
    public void shouldReturnEmptyProblemListsWhenAllRulesPassed() {
        shouldCollectProblemsFromAllValidationRules(null, null);
    }

    @Test
    public void shouldReturnListOffProblemsWhenFirstRuleFailed() {
        final Problem problem = new Problem(PROBLEM_CODE_1, asList(new ProblemValue(null,"dob", "12-10-2018")));

        shouldCollectProblemsFromAllValidationRules(problem, null);
    }

    @Test
    public void shouldReturnListOffProblemsWhenAllRulesFailed() {
        final Problem problem1 = new Problem(PROBLEM_CODE_1, asList(new ProblemValue(null,"key1", "value1"), new ProblemValue(null,"key2", "value2")));
        final Problem problem2 = new Problem(PROBLEM_CODE_2, asList(new ProblemValue(null,"dob", "12-11-2018")));

        shouldCollectProblemsFromAllValidationRules(problem1, problem2);
    }

    @Test
    public void shouldRethrowAnyExceptionThrownByAnyRule() {
        final Exception exception = new RuntimeException("Exception from rule");

        when(validationRule1.validate(prosecution, prosecutionCaseFile)).thenThrow(exception);

        try {
            ValidationRuleExecutor.validate(prosecution, prosecutionCaseFile, asList(validationRule1, validationRule2));
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e, equalTo(exception));
        }

        verify(validationRule1).validate(prosecution, prosecutionCaseFile);
        verify(validationRule2, never()).validate(prosecution, prosecutionCaseFile);
    }

    private void shouldCollectProblemsFromAllValidationRules(final Problem problem1, final Problem problem2) {
        when(validationRule1.validate(prosecution, prosecutionCaseFile)).thenReturn(ValidationResult.newValidationResult(Optional.ofNullable(problem1)));
        when(validationRule2.validate(prosecution, prosecutionCaseFile)).thenReturn(ValidationResult.newValidationResult(Optional.ofNullable(problem2)));

        final List<Problem> problems = ValidationRuleExecutor.validate(prosecution, prosecutionCaseFile, asList(validationRule1, validationRule2));

        assertThat(problems, containsInAnyOrder(Stream.of(problem1, problem2).filter(Objects::nonNull).toArray()));

        verify(validationRule1).validate(prosecution, prosecutionCaseFile);
        verify(validationRule2).validate(prosecution, prosecutionCaseFile);
    }
}
