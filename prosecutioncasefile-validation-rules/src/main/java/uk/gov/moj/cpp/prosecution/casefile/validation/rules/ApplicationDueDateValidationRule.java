package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.time.LocalDate;
import java.util.Optional;

public class ApplicationDueDateValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public ApplicationDueDateValidationRule() {
        super(ValidationError.APPLICATION_DUE_DATE_INVALID);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {

        if (submitApplication.getBoxHearingRequest().getApplicationDueDate().isBefore(LocalDate.now())) {
            return Optional.of(getValidationError());
        }
        return Optional.empty();
    }
}
