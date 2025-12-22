package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.helper.ValidationRuleHelper.isValidNameAndAddress;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ThirdParty;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

public class ThirdPartyDetailsValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public ThirdPartyDetailsValidationRule() {
        super(ValidationError.THIRD_PARTY_DETAILS_REQUIRED);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final List<ThirdParty> thirdParties = submitApplication.getCourtApplication().getThirdParties();

        if (nonNull(thirdParties) && !isEmpty(thirdParties) && !validThirdParty(thirdParties)) {
                return of(getValidationError());
        }

        return empty();
    }

    private boolean validThirdParty(final List<ThirdParty> thirdParties) {
        for (final ThirdParty thirdParty : thirdParties) {
            if (!isValidNameAndAddress(thirdParty.getPersonDetails(), thirdParty.getOrganisation())) {
                return false;
            }
        }
        return true;
    }

}
