package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationRuleExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationRuleExecutor.class);

    private ValidationRuleExecutor() {
    }

    public static <T, S> List<Problem> validate(final T input, final S context, final List<ValidationRule<T, S>> validationRules) {
        return validationRules.stream()
                .map(validationRule -> {
                    final ValidationResult result = validationRule.validate(input, context);
                    if (!result.isValid()) {
                        LOGGER.info("RULE_FIRED rule={} codes={}", validationRule.getClass().getSimpleName(),
                                result.problems().stream().map(Problem::getCode).collect(toList()));
                    }
                    return result;
                })
                .flatMap(validationResult -> validationResult.problems().stream())
                .collect(toList());
    }

}
