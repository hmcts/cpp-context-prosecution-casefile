package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.SUBJECT_REQUIRED;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

public class SubjectValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public SubjectValidationRule() {
        super(ValidationError.SUBJECT_INVALID);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        if (isRespondentsHasManyIsSubjectsTrue(submitApplication)) {
            return of(getValidationError());
        }
        final boolean isApplicantSubject = toBoolean(submitApplication.getCourtApplication().getApplicant().getIsSubject());
        final boolean isRespondentSubject = ofNullable(submitApplication.getCourtApplication().getRespondents())
                .orElse(emptyList()).stream().anyMatch(respondent -> toBoolean(respondent.getIsSubject()));

        if (isApplicantSubject && isRespondentSubject) {
            return of(getValidationError());
        }

        if (!isApplicantSubject && !isRespondentSubject) {
            return of(SUBJECT_REQUIRED);
        }

        if (!isValidApplicationTypeAndRespondent(submitApplication,
                additionalInformation,
                isApplicantSubject,
                isRespondentSubject)) {
            return of(getValidationError());
        }

        return empty();
    }

    private boolean isValidApplicationTypeAndRespondent(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation, final boolean isApplicantSubject, final boolean isRespondentSubject) {
        final boolean containsApplicationTypeOffenceActiveOrderOffence =
                containsApplicationTypeOffenceActiveOrderOffence(submitApplication, additionalInformation);

        if (isApplicantSubject && containsApplicationTypeOffenceActiveOrderOffence) {
            return false;
        }

        final boolean isRespondentDefendant = ofNullable(submitApplication.getCourtApplication().getRespondents())
                .orElse(emptyList()).stream().anyMatch(respondent -> toBoolean(respondent.getIsDefendantMatched()));

        return !isRespondentSubject || !containsApplicationTypeOffenceActiveOrderOffence || isRespondentDefendant;
    }

    private boolean isRespondentsHasManyIsSubjectsTrue(final SubmitApplication submitApplication) {
        return ofNullable(submitApplication.getCourtApplication().getRespondents())
                .orElse(emptyList()).stream().filter(respondent -> toBoolean(respondent.getIsSubject()))
                .count() > 1;
    }

    private boolean containsApplicationTypeOffenceActiveOrderOffence(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final List<CourtApplicationType> courtApplicationTypes =
                ofNullable(additionalInformation.getApplicationTypes()).orElse(emptyList());

        if (isEmpty(courtApplicationTypes)) {
            return false;
        }

        if (isNull(submitApplication.getCourtApplication().getCourtApplicationType())) {
            return false;
        }

        return courtApplicationTypes.stream()
                .filter(courtApplicationType -> nonNull(courtApplicationType.getCode()))
                .filter(courtApplicationType -> courtApplicationType.getCode()
                        .equals(submitApplication.getCourtApplication().getCourtApplicationType().getCode()))
                .anyMatch(courtApplicationType -> courtApplicationType.getOffenceActiveOrder().equals(OFFENCE));
    }
}
