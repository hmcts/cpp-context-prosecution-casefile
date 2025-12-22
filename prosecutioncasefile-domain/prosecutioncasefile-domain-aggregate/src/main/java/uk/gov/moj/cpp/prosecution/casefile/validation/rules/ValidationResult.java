package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import com.google.common.collect.Iterators;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static java.util.Collections.emptyList;

public class ValidationResult {
    private List<Problem> problems;

    public static final ValidationResult VALID = new ValidationResult(emptyList());

    public List<Problem> problems() {
        return problems;
    }

    private ValidationResult(List<Problem> problems) {
        this.problems = problems;
    }

    public static ValidationResult newValidationResult(List<Problem> problems) {

        return new ValidationResult(ofNullable(problems).orElse(emptyList()));
    }

    public static ValidationResult newValidationResult(Optional<Problem> problem) {
        return new ValidationResult(problem.map(Arrays::asList).orElse(emptyList()));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ValidationResult)) {
            return false;
        }

        final ValidationResult that = (ValidationResult) obj;
        return Iterators.elementsEqual(problems.iterator(), that.problems.iterator());
    }

    @Override
    public int hashCode() {
        return problems().hashCode();
    }

    @Override
    public String toString() {
        return problems().toString();
    }

    public boolean isValid() {
        return problems().isEmpty();
    }
}
