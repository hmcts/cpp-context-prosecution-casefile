package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;

public class ValidationRuleExecutor {

    private ValidationRuleExecutor() {
    }

    public static <T, S> List<Problem> validate(final T input, final S context, final List<ValidationRule<T, S>> validationRules) {
        return validationRules.stream()
                .map(validationRule -> validationRule.validate(input, context))
                .flatMap(validationResult -> validationResult.problems().stream())
                .collect(toList());
    }

}
