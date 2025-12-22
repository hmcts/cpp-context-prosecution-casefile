package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.isNull;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationPayment;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.Optional;

@SuppressWarnings("squid:S3516")
public class CourtFeeDetailsValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public CourtFeeDetailsValidationRule() {
        super(ValidationError.COURT_PAYMENT_NOT_FOUND);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {

        final CourtApplicationPayment courtApplicationPayment = submitApplication.getCourtApplication().getCourtApplicationPayment();

        if (isNull(courtApplicationPayment)) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
