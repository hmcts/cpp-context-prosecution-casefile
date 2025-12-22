package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;

public class DefendantIsOver18YearsOldValidationRule implements ValidationRule<Defendant, ReferenceDataValidationContext> {

    public static final DefendantIsOver18YearsOldValidationRule INSTANCE = new DefendantIsOver18YearsOldValidationRule();

    private DefendantIsOver18YearsOldValidationRule() {
    }

    @Override
    public ValidationResult validate(
            final Defendant defendant,
            final ReferenceDataValidationContext referenceDataValidationContext) {
        if (defendant.getIndividual() == null || defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth() == null) {
            return VALID;
        }

        final LocalDate dateOfBirth = defendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();
        final LocalDate chargeDate = defendant.getOffences().get(0).getChargeDate();

        return newValidationResult(ofNullable(dateOfBirth)
                .filter(dob -> nonNull(chargeDate) && chargeDate.isBefore(dob.plusYears(18)))
                .map(dob -> newProblem(DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE,
                        new ProblemValue(null, "dateOfBirth", dob.toString()),
                        new ProblemValue(null, "chargeDate", chargeDate.toString())
                )));
    }
}
