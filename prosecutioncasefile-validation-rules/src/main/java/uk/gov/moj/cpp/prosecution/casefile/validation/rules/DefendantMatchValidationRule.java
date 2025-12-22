package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefendantMatchValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public DefendantMatchValidationRule() {
        super(ValidationError.DEFENDANT_DETAILS_NOT_FOUND);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final List<Respondent> respondents = submitApplication.getCourtApplication().getRespondents();

        if (nonNull(respondents)) {
            final List<Respondent> multipleDefendantMatchedList = respondents.stream().filter(Objects::nonNull)
                    .filter(respondent -> toBoolean(respondent.getIsMultipleDefendantMatched())).collect(Collectors.toList());

            if (isNotEmpty(multipleDefendantMatchedList)) {
                return of(getValidationError());
            }
        }

        return empty();
    }
}
