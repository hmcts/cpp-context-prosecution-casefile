package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.DefendantIsOver18YearsOldValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.ImprisonableOffenceValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceNotSummaryValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceInEffectOnOffenceCommittedDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceOutOfTimeValidationRule;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class SjpProsecutionWarningRuleProvider {

    private static final List<ValidationRule<Defendant, ReferenceDataValidationContext>> WARNING_RULES =
            ImmutableList.of(
                    DefendantIsOver18YearsOldValidationRule.INSTANCE,
                    OffenceOutOfTimeValidationRule.INSTANCE,
                    OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE,
                    ImprisonableOffenceValidationRule.INSTANCE,
                    OffenceNotSummaryValidationRule.INSTANCE);

    private SjpProsecutionWarningRuleProvider() {
    }

    public static List<ValidationRule<Defendant, ReferenceDataValidationContext>> getWarningRules() {
        return WARNING_RULES;
    }
}
