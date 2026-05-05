package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CaseInitiationValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ProsecutorReferenceDataValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.BailConditionsValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DateOfHearingPastDateValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.LaidDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ArrestDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ArrestDateValidationRuleForCivil;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ChargeDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.CivilOffenceCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceGenericValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.StatementOfFactsValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.StatementOfFactsWelshValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor.ProsecutorSJPValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.CaseCountValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.HearingDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.OneDefendantPerProsecutionCaseValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.OneOffencePerDefendantValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.SameOffenceCodeValidationRule;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CcProsecutionValidationRuleProviderTest {

    private static final String INITIATION_CODE_CHARGE_CASE = "C";
    private static final String INITIATION_CODE_FOR_SUMMONS = "S";
    private static final String INITIATION_CODE_FOR_SJP = "J";
    private static final String INITIATION_CODE_FOR_OTHER = CaseType.OTHER.getCode();
    private static final String UNKNOWN_INITIATION_CODE = "X";

    private static boolean containsDefendantRule(
            final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> rules,
            final Class<? extends ValidationRule> ruleClass) {
        return rules.stream()
                .map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass)
                .anyMatch(ruleClass::equals);
    }

    private static boolean containsCaseRule(
            final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> rules,
            final Class<? extends ValidationRule> ruleClass) {
        return rules.stream()
                .map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass)
                .anyMatch(ruleClass::equals);
    }

    @Test
    void shouldValidateDefendantValidateSpiRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.SPI,Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OffenceGenericValidationAndEnricherRule.class)));
        assertFalse(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
    }


    @Test
    void shouldValidateSJPCaseCreationValidationRule() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>  validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_FOR_SJP);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorAOCPValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(SummonsCodeValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorSJPValidationRule.class)));


    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromCPPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.CPPI,Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromSPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.SPI,Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    void shouldValidateDefendantValidateCPPIRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.CPPI,Boolean.FALSE);

        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OffenceGenericValidationAndEnricherRule.class)));
    }

    @Test
    void shouldValidateDefendantValidateMCCRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.MCC,Boolean.FALSE);

        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OffenceGenericValidationAndEnricherRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PncIdValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).noneMatch(s -> s.equals(StatementOfFactsWelshValidationRule.class)));
        assertTrue(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
    }

    @Test
    void verifyGroupCasesValidationRules() {

        final List<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getGroupCasesValidationRules();

        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseCountValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OneDefendantPerProsecutionCaseValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OneOffencePerDefendantValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(SameOffenceCodeValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(HearingDateValidationRule.class)));

    }

    @Test
    void verifyCasesValidationRules() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil(null);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertFalse(containsCaseRule(validationRules, SummonsCodeValidationRule.class));

    }

    @Test
    void verifyGroupCasesValidationRulesWithSummonsCode() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil(CaseType.SUMMONS.getCode());

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(SummonsCodeValidationRule.class)));
    }

    @Test
    void shouldUseCivilGroupRulesForCivilChannelWithSummonsAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.CIVIL, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRuleForCivil.class));
        assertTrue(containsDefendantRule(validationRules, LaidDateValidationRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
    }

    @Test
    void shouldUseCivilGroupRulesForCivilChannelWithOtherAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRuleForCivil.class));
        assertTrue(containsDefendantRule(validationRules, LaidDateValidationRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
    }

    @Test
    void shouldFallBackToCivilDefendantRuleSetForCivilChannelWithUnknownCodeAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.CIVIL, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, OffenceGenericValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
        assertTrue(containsDefendantRule(validationRules, ChargeDateValidationRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
    }

    @Test
    void shouldUseMCCCivilSummonsRulesForMCCChannelWithSummonsAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.MCC, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRuleForCivil.class));
        assertTrue(containsDefendantRule(validationRules, LaidDateValidationRule.class));
        assertTrue(containsDefendantRule(validationRules, PncIdValidationRule.class));
        assertTrue(containsDefendantRule(validationRules, DateOfHearingPastDateValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, StatementOfFactsValidationRule.class));
    }

    @Test
    void shouldUseMCCCivilOtherRulesForMCCChannelWithOtherAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.MCC, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRuleForCivil.class));
        assertTrue(containsDefendantRule(validationRules, BailConditionsValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
    }

    @Test
    void shouldFallBackToCivilDefendantRuleSetForMCCChannelWithUnknownCodeAndIsCivilTrue() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.MCC, Boolean.TRUE);

        assertTrue(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, OffenceGenericValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
        assertTrue(containsDefendantRule(validationRules, ChargeDateValidationRule.class));
        assertFalse(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
    }

    @Test
    void shouldUseDefaultDefendantValidationMapWhenChannelIsNull() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, null, Boolean.FALSE);

        assertTrue(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertTrue(containsDefendantRule(validationRules, PncIdValidationRule.class));
        assertTrue(containsDefendantRule(validationRules, ArrestDateValidationRule.class));
        assertFalse(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
    }

    @Test
    void shouldUseDefaultDefendantValidationMapWhenIsCivilFalseEvenOnCivilChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.CIVIL, Boolean.FALSE);

        assertTrue(containsDefendantRule(validationRules, OffenceCodeValidationAndEnricherRule.class));
        assertFalse(containsDefendantRule(validationRules, CivilOffenceCodeValidationAndEnricherRule.class));
    }

    @Test
    void shouldReturnDefaultCivilCaseRulesForUnknownInitiationCode() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRulesForCivil(UNKNOWN_INITIATION_CODE);

        assertTrue(containsCaseRule(validationRules, CaseInitiationValidationRule.class));
        assertTrue(containsCaseRule(validationRules, ProsecutorReferenceDataValidationRule.class));
        assertFalse(containsCaseRule(validationRules, SummonsCodeValidationRule.class));
    }


}
