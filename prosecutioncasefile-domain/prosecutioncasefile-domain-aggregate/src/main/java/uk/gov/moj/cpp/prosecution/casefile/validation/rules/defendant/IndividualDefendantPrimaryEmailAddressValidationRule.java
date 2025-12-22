package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Constants.EMAIL;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class IndividualDefendantPrimaryEmailAddressValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getContactDetails() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getContactDetails().getPrimaryEmail() == null) {
            return VALID;
        }

        final String primaryEmailAddress = defendantWithReferenceData.getDefendant().getIndividual().getPersonalInformation().getContactDetails().getPrimaryEmail();
        if (primaryEmailAddress == null) {
            return VALID;
        }
        return newValidationResult(EMAIL_REGEX.matcher(primaryEmailAddress).matches() ?
                empty() :
                of(newProblem(DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID, new ProblemValue(null, DEFENDANT_PRIMARY_EMAIL_ADDRESS.getValue(), primaryEmailAddress))));
    }
}
