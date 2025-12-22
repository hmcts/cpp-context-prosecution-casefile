package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_NOT_SUMMARY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;

public class OffenceNotSummaryValidationRule implements ValidationRule<Defendant, ReferenceDataValidationContext> {

    public static final OffenceNotSummaryValidationRule INSTANCE = new OffenceNotSummaryValidationRule();
    private static final List<String> nonSummaryCaseModeOfTrialsDerived = asList("Either Way", "Indictable");


    private OffenceNotSummaryValidationRule() {
    }

    @Override
    public ValidationResult validate(
            final Defendant defendant,
            final ReferenceDataValidationContext referenceDataValidationContext) {

        return newValidationResult(defendant.getOffences()
                .stream()
                .filter(offence -> isNonSummaryOffence(referenceDataValidationContext, offence))
                .map(offence -> newProblem(OFFENCE_NOT_SUMMARY,
                        new ProblemValue(offence.getOffenceId().toString(), "offenceCode", offence.getOffenceCode()),
                        new ProblemValue(offence.getOffenceId().toString(), "offenceSequenceNo", offence.getOffenceSequenceNumber().toString())))
                .collect(toList()));
    }

    private static boolean isNonSummaryOffence(final ReferenceDataValidationContext referenceDataValidationContext, final Offence offence) {
        return referenceDataValidationContext
                .getReferenceDataByOffenceCode(offence.getOffenceCode())
                .map(offenceReferenceData -> isNonSummaryOffence(offenceReferenceData.getModeOfTrialDerived()))
                .orElse(FALSE);

    }

    private static boolean isNonSummaryOffence(final String modeOfTrialDerived) {
        return nonSummaryCaseModeOfTrialsDerived.contains(modeOfTrialDerived);
    }



}
