package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OffenceInEffectOnOffenceCommittedDateValidationRule implements ValidationRule<Defendant, ReferenceDataValidationContext> {

    public static final OffenceInEffectOnOffenceCommittedDateValidationRule INSTANCE = new OffenceInEffectOnOffenceCommittedDateValidationRule();
    public static final String NOT_SET = "NOT_SET";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String PROBLEM_VALUE = "Offence code: %s, Offence committed on: %s, offence in effect from: %s to %s";

    private OffenceInEffectOnOffenceCommittedDateValidationRule() {
    }

    @Override
    public ValidationResult validate(final Defendant defendant, final ReferenceDataValidationContext referenceDataValidationContext) {
        final List<Problem> problems = new ArrayList<>();
        defendant
                .getOffences()
                .forEach(offence -> referenceDataValidationContext
                        .getOffenceCodeReferenceData()
                        .stream()
                        .filter(referenceData -> offence.getOffenceCode().equals(referenceData.getCjsOffenceCode()))
                        .findFirst()
                        .filter(offenceReferenceData -> !offenceInEffectOnCommittedDate(offenceReferenceData, offence.getOffenceCommittedDate()))
                        .ifPresent(offenceReferenceData ->
                                problems.add(newProblem(OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE,
                                        new ProblemValue(offence.getOffenceId().toString(), "offenceInEffectDates", format(PROBLEM_VALUE,
                                                offenceReferenceData.getCjsOffenceCode(),
                                                offence.getOffenceCommittedDate().toString(),
                                                ofNullable(offenceReferenceData.getOffenceStartDate()).orElse(NOT_SET),
                                                ofNullable(offenceReferenceData.getOffenceEndDate()).orElse(NOT_SET))),
                                        new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_CODE.getValue(), offence.getOffenceCode()),
                                        new ProblemValue(offence.getOffenceId().toString(), FieldName.OFFENCE_SEQUENCE_NO.getValue(), String.valueOf(offence.getOffenceSequenceNumber()))))));

        return problems.isEmpty() ? VALID: newValidationResult(problems);
    }

    private boolean offenceInEffectOnCommittedDate(final OffenceReferenceData offenceReferenceData, final LocalDate offenceCommittedDate) {
        final LocalDate offenceStartDate = LocalDate.parse(
                ofNullable(offenceReferenceData.getOffenceStartDate())
                        .orElse(offenceCommittedDate.minusDays(1).format(FORMATTER)), FORMATTER);
        final LocalDate offenceEndDate = LocalDate.parse(
                ofNullable(offenceReferenceData.getOffenceEndDate())
                        .orElse(offenceCommittedDate.plusDays(1).format(FORMATTER)), FORMATTER);
        return offenceCommittedDate.compareTo(offenceStartDate) >= 0 && offenceCommittedDate.compareTo(offenceEndDate) <= 0;
    }
}
