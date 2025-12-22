package uk.gov.moj.cpp.prosecution.casefile.validation;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SubmitApplicationValidationRule;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.StreamSupport.stream;

public class SubmitApplicationValidator {

    @Inject
    Instance<SubmitApplicationValidationRule> validatorRules;

    public List<Optional<ValidationError>> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {

        final Stream<SubmitApplicationValidationRule> validationRuleStream = stream(validatorRules.spliterator(), false);
        return validationRuleStream
                .sorted(Comparator.comparing(SubmitApplicationValidationRule::getErrorCode))
                .map(rule -> rule.validate(submitApplication, additionalInformation))
                .collect(Collectors.toList());
    }
}
