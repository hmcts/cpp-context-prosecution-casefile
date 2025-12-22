package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.Optional;

public interface ApplicationValidationRule<T> {

    Optional<ValidationError> validate(final T input, AdditionalInformation additionalInformation);

    String getErrorCode();
}
