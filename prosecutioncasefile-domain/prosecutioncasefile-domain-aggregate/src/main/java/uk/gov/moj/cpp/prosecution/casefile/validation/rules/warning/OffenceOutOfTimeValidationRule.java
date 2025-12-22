package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;


import static java.lang.String.valueOf;
import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_OUT_OF_TIME;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;

public class OffenceOutOfTimeValidationRule implements ValidationRule<Defendant, ReferenceDataValidationContext> {

    public static final OffenceOutOfTimeValidationRule INSTANCE = new OffenceOutOfTimeValidationRule();

    private OffenceOutOfTimeValidationRule() {
    }

    @Override
    public ValidationResult validate(
            Defendant defendant,
            ReferenceDataValidationContext referenceDataValidationContext) {

        return newValidationResult(defendant.getOffences().stream()
                .map(offence -> validateOffence(offence, referenceDataValidationContext))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toList()));
    }

    private Optional<Problem> validateOffence(final Offence offence, final ReferenceDataValidationContext referenceDataValidationContext) {
        final LocalDate offenceCommittedDate = offence.getOffenceCommittedDate();
        final LocalDate offenceChargeDate = offence.getChargeDate();

        final Optional<Integer> prosecutionTimeLimitMonths = referenceDataValidationContext.getOffenceCodeReferenceData().stream()
                .filter(referenceData -> offence.getOffenceCode().equals(referenceData.getCjsOffenceCode()))
                .map(OffenceReferenceData::getProsecutionTimeLimit)
                .filter(NumberUtils::isNumber)
                .map(Integer::valueOf)
                .findFirst();

        return prosecutionTimeLimitMonths
                .map(timeLimitMonths -> DAYS.between(
                        offenceCommittedDate.plusMonths(timeLimitMonths),
                        offenceChargeDate))
                .filter(daysOverdue -> daysOverdue > 0)
                .map(daysOverdue -> newProblem(
                        OFFENCE_OUT_OF_TIME,
                        new ProblemValue(offence.getOffenceId().toString(), "daysOverdue",valueOf(daysOverdue)),
                        new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_CODE.getValue(), offence.getOffenceCode()),
                        new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(), offence.getOffenceSequenceNumber().toString())
                        ));
    }
}

