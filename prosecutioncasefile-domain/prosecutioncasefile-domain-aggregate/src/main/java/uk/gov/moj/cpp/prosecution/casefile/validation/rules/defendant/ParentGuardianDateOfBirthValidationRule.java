package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.time.LocalDate.now;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PARENT_GUARDIAN_DATE_OF_BIRTH;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.time.ZoneId;

public class ParentGuardianDateOfBirthValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant().getIndividual() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation() == null ||
                defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getDateOfBirth() == null) {
            return VALID;
        }

        final LocalDate dob = defendantWithReferenceData.getDefendant().getIndividual().getParentGuardianInformation().getDateOfBirth();

        if(dob.isAfter(now(ZoneId.of("Europe/London")))) {
            return newValidationResult(of(newProblem(DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE, new ProblemValue(null, PARENT_GUARDIAN_DATE_OF_BIRTH.getValue(), dob.toString()))));
        }

        return VALID;
    }
}
