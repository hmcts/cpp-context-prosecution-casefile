package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.regex.Pattern;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Constants.EMAIL;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class ParentGuardianPrimaryEmailAddressValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL.getValue());

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null || !doesParentGuardianPrimaryEmailExist(defendantWithReferenceData.getDefendant().getIndividual())) {
            return VALID;
        }

        final String primaryEmailAddress = defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getPersonalInformation().getContactDetails().getPrimaryEmail();
        return EMAIL_REGEX.matcher(primaryEmailAddress).matches() ?
                VALID :
                newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID, new ProblemValue(null, PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS.getValue(), primaryEmailAddress))));
    }

    private boolean doesParentGuardianPrimaryEmailExist(final Individual individual) {
        return  individual.getParentGuardianInformation() != null &&
                individual.getParentGuardianInformation().getPersonalInformation() != null &&
                individual.getParentGuardianInformation().getPersonalInformation().getContactDetails() != null &&
                individual.getParentGuardianInformation().getPersonalInformation().getContactDetails().getPrimaryEmail() != null;
    }
}
