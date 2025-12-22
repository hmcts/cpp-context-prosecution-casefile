package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;

import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.IMPRISONABLE_OFFENCE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.*;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;


public class ImprisonableOffenceValidationRule implements ValidationRule<Defendant, ReferenceDataValidationContext> {

    private static List<String> nonImprisonableModeOfTrials = asList("STRAFF", "SNONIMP");
    public static final ImprisonableOffenceValidationRule INSTANCE = new ImprisonableOffenceValidationRule();

    @Override
    public ValidationResult validate(final Defendant defendant, final ReferenceDataValidationContext referenceDataValidationContext) {

        return newValidationResult(defendant
                .getOffences()
                .stream()
                .filter(offence -> isImprisonable(referenceDataValidationContext, offence))
                .map(offence -> newProblem(IMPRISONABLE_OFFENCE,
                        new ProblemValue(offence.getOffenceId().toString(), "offenceCode", offence.getOffenceCode()),
                        new ProblemValue(offence.getOffenceId().toString(), "offenceSequenceNo", offence.getOffenceSequenceNumber().toString())))
                .collect(toList()));

    }

    private static boolean isImprisonable(final ReferenceDataValidationContext referenceDataValidationContext, final Offence offence) {
        return referenceDataValidationContext
                .getReferenceDataByOffenceCode(offence.getOffenceCode())
                .map(offenceReferenceData -> isImprisonable(offenceReferenceData.getModeOfTrial()))
                .orElse(FALSE);

    }

    private static boolean isImprisonable(final String modeOfTrial) {
        return !nonImprisonableModeOfTrials.contains(modeOfTrial);
    }
}
