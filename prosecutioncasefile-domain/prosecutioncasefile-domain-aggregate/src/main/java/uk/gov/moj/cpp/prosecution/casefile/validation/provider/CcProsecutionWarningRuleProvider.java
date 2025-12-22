package uk.gov.moj.cpp.prosecution.casefile.validation.provider;


import static java.util.Collections.emptyList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceInEffectOnOffenceCommittedDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceOutOfTimeValidationRule;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CcProsecutionWarningRuleProvider {

    private static final List<ValidationRule<Defendant, ReferenceDataValidationContext>> WARNING_RULES =
            ImmutableList.of(
                    OffenceOutOfTimeValidationRule.INSTANCE,
                    OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE);

    private CcProsecutionWarningRuleProvider() {
    }

    public static List<ValidationRule<Defendant, ReferenceDataValidationContext>> getWarningRules(final String caseInitiationCode) {
        if("C".equals(caseInitiationCode) || "Q".equals(caseInitiationCode)) {
            return WARNING_RULES;
        } else {
            return emptyList();
        }
    }
}
