package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CpsDefendantIdValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public CpsDefendantIdValidationRule() {
        super(ValidationError.DEFENDANT_DETAILS_NOT_FOUND);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final List<Respondent> respondents = submitApplication.getCourtApplication().getRespondents();

        if (nonNull(respondents)) {
            final boolean isDefendantUnMatched = respondents.stream().filter(Objects::nonNull)
                    .anyMatch(respondent -> !toBoolean(respondent.getIsDefendantMatched()) && !isNullOrEmpty(respondent.getCpsDefendantId()));

            if (isDefendantUnMatched) {
                return of(getValidationError());
            }
        }
        return empty();
    }
}
