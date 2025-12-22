package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

public class ApplicationTypeValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public ApplicationTypeValidationRule() {
        super(ValidationError.APPLICATION_TYPE_NOT_FOUND);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {

        final CourtApplicationType courtApplicationType = submitApplication.getCourtApplication().getCourtApplicationType();

        if (isNull(courtApplicationType) || isNull(courtApplicationType.getCode()) || isInvalidCourtApplicationType(submitApplication, additionalInformation.getApplicationTypes())) {
            return Optional.of(getValidationError());
        }
        return empty();
    }

    private boolean isInvalidCourtApplicationType(final SubmitApplication submitApplication, final List<uk.gov.justice.core.courts.CourtApplicationType> applicationTypes) {
        return !enrichedCourtApplicationType(submitApplication, applicationTypes).isPresent();
    }

    private Optional<uk.gov.justice.core.courts.CourtApplicationType> enrichedCourtApplicationType(final SubmitApplication sourceSubmitApplication, final List<uk.gov.justice.core.courts.CourtApplicationType> applicationTypes) {

        if (isNull(applicationTypes)) {
            return empty();
        }
        return applicationTypes.stream()
                .filter(courtApplicationType -> nonNull(courtApplicationType.getCode()))
                .filter(applicationType -> nonNull(sourceSubmitApplication.getCourtApplication().getCourtApplicationType()) && applicationType.getCode().equals(sourceSubmitApplication.getCourtApplication().getCourtApplicationType().getCode())).findFirst();
    }
}
