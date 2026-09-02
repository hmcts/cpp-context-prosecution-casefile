package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.ImprisonableOffenceValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceInEffectOnOffenceCommittedDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.OffenceOutOfTimeValidationRule;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class CcProsecutionWarningRuleProviderTest {

    private static final String INITIATION_CODE_FOR_OTHER = "O";
    private static final String INITIATION_CODE_FOR_SUMMONS = "S";
    private static final String INITIATION_CODE_CHARGE = "C";
    private static final String INITIATION_CODE_REQUISITION = "Q";

    @Test
    public void shouldIncludeAllThreeWarningRulesForCivilChannelOtherInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, false);

        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceOutOfTimeValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceInEffectOnOffenceCommittedDateValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(ImprisonableOffenceValidationRule.class)));
    }

    @Test
    public void shouldIncludeAllThreeWarningRulesForCivilChannelSummonsInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_FOR_SUMMONS, Channel.CIVIL, false);

        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceOutOfTimeValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceInEffectOnOffenceCommittedDateValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(ImprisonableOffenceValidationRule.class)));
    }

    @Test
    public void shouldIncludeAllThreeWarningRulesForCivilOffenceOnCppiChannelSummonsInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_FOR_SUMMONS, Channel.CPPI, true);

        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceOutOfTimeValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceInEffectOnOffenceCommittedDateValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(ImprisonableOffenceValidationRule.class)));
    }

    @Test
    public void shouldReturnEmptyListForCivilOffenceOnMccChannelOtherInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_FOR_OTHER, Channel.MCC, true);

        assertTrue(rules.isEmpty());
    }

    @Test
    public void shouldNotIncludeImprisonableOffenceRuleForMccChannelOtherInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_FOR_OTHER, Channel.MCC, false);

        assertTrue(rules.isEmpty());
    }

    @Test
    public void shouldNotIncludeImprisonableOffenceRuleForCppiChannelChargeInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_CHARGE, Channel.CPPI, false);

        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceOutOfTimeValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceInEffectOnOffenceCommittedDateValidationRule.class)));
        assertFalse(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(ImprisonableOffenceValidationRule.class)));
    }

    @Test
    public void shouldNotIncludeImprisonableOffenceRuleForCivilChannelRequisitionInitiationCode() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules(INITIATION_CODE_REQUISITION, Channel.CIVIL, false);

        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceOutOfTimeValidationRule.class)));
        assertTrue(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(OffenceInEffectOnOffenceCommittedDateValidationRule.class)));
        assertFalse(rules.stream().map(warningRuleClass()).anyMatch(s -> s.equals(ImprisonableOffenceValidationRule.class)));
    }

    @Test
    public void shouldReturnEmptyListForUnmappedInitiationCodeAndChannel() {
        final List<ValidationRule<Defendant, ReferenceDataValidationContext>> rules = CcProsecutionWarningRuleProvider
                .getWarningRules("J", Channel.SPI, false);

        assertTrue(rules.isEmpty());
    }

    private Function<ValidationRule<Defendant, ReferenceDataValidationContext>, ? extends Class<? extends ValidationRule>> warningRuleClass() {
        return ValidationRule::getClass;
    }
}
