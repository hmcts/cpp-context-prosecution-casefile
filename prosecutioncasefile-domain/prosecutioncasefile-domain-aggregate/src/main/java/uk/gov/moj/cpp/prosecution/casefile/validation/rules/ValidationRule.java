package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

@FunctionalInterface
public interface ValidationRule<T, S> {

    ValidationResult validate(final T input, final S context);

}
