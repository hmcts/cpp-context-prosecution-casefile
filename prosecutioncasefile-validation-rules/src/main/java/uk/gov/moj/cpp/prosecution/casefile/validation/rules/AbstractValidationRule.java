package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

public class AbstractValidationRule {
    private final ValidationError validationError;

    public AbstractValidationRule(final ValidationError validationError) {
        this.validationError = validationError;
    }
    public String getErrorCode() {
        return validationError.getCode();
    }

    public ValidationError getValidationError() {
        return validationError;
    }
}
