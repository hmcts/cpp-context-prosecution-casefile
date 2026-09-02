package uk.gov.moj.cpp.prosecution.casefile.validation.provider;


import static java.util.Collections.emptyList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.ImprisonableOffenceValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceInEffectOnOffenceCommittedDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceOutOfTimeValidationRule;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CcProsecutionWarningRuleProvider {

    private static final List<ValidationRule<Defendant, ReferenceDataValidationContext>> WARNING_RULES =
            ImmutableList.of(
                    OffenceOutOfTimeValidationRule.INSTANCE,
                    OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE);

    private static final List<ValidationRule<Defendant, ReferenceDataValidationContext>> CIVIL_WARNING_RULES =
            ImmutableList.of(
                    OffenceOutOfTimeValidationRule.INSTANCE,
                    OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE,
                    ImprisonableOffenceValidationRule.INSTANCE);

    private CcProsecutionWarningRuleProvider() {
    }

    public static List<ValidationRule<Defendant, ReferenceDataValidationContext>> getWarningRules(final String caseInitiationCode, final Channel channel, final boolean isCivil) {
        // A civil offence submitted on the CPPI channel (isCivil=true) needs the same warnings as the
        // dedicated CIVIL channel; MCC-channel civil offences deliberately stay warning-free here.
        final boolean applyCivilWarningRules = CIVIL.equals(channel) || (CPPI.equals(channel) && isCivil);
        if (applyCivilWarningRules && ("O".equals(caseInitiationCode) || "S".equals(caseInitiationCode))) {
            return CIVIL_WARNING_RULES;
        } else if ("C".equals(caseInitiationCode) || "Q".equals(caseInitiationCode)) {
            return WARNING_RULES;
        } else {
            return emptyList();
        }
    }
}
