package uk.gov.moj.cpp.prosecution.casefile.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.provider.SjpProsecutionWarningRuleProvider;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.DefendantIsOver18YearsOldValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.ImprisonableOffenceValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceNotSummaryValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceInEffectOnOffenceCommittedDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceOutOfTimeValidationRule;

import java.util.List;

import org.junit.jupiter.api.Test;

public class SjpProsecutionWarningRuleProviderTest {

    @Test
    public void shouldProvideAllWarningRules() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> warningRules = SjpProsecutionWarningRuleProvider.getWarningRules();
        assertThat(warningRules,
                contains(
                        DefendantIsOver18YearsOldValidationRule.INSTANCE,
                        OffenceOutOfTimeValidationRule.INSTANCE,
                        OffenceInEffectOnOffenceCommittedDateValidationRule.INSTANCE,
                        ImprisonableOffenceValidationRule.INSTANCE,
                        OffenceNotSummaryValidationRule.INSTANCE));
    }
}