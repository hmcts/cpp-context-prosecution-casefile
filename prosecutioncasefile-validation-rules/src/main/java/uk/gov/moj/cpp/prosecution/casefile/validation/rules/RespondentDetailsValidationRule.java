package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.helper.ValidationRuleHelper.isValidNameAndAddress;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

public class RespondentDetailsValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public RespondentDetailsValidationRule() {
        super(ValidationError.RESPONDENT_DETAILS_REQUIRED);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {

        final List<Respondent> respondents = submitApplication.getCourtApplication().getRespondents();
        if (nonNull(respondents) && !isEmpty(respondents) && !validRespondents(respondents)) {
            return of(getValidationError());
        }

        return empty();
    }

    private boolean validRespondents(final List<Respondent> respondents) {
        for (final Respondent respondent : respondents) {
            if (!isValidNameAndAddress(respondent.getPersonDetails(), respondent.getOrganisation())) {
                return false;
            }
        }
        return true;
    }

}
