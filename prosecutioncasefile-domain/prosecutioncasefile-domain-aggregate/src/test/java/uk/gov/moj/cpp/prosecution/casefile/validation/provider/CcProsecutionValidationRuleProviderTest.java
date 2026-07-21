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
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CaseMarkersValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ProsecutorReferenceDataValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DateOfHearingPastDateValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DefendantInitiationCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ObservedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PostCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ChargeDateValidationRule;
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
    private static final String INITIATION_CODE_FOR_OTHER = "O";

    @Test
    public void shouldValidateDefendantValidateSpiRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.SPI,Boolean.FALSE, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OffenceGenericValidationAndEnricherRule.class)));
    }


    @Test
    public void shouldValidateSJPCaseCreationValidationRule() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>  validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_FOR_SJP);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorAOCPValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(SummonsCodeValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorSJPValidationRule.class)));


    }

    @Test
    public void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromCPPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.CPPI,Boolean.FALSE, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    public void shouldValidateTheDefendantForStatementOfFactsWhenSummonsIsInitiationFromSPIChannel() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.SPI,Boolean.FALSE, Boolean.FALSE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(StatementOfFactsValidationRule.class)));
    }

    @Test
    public void shouldValidateDefendantValidateCPPIRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.CPPI,Boolean.FALSE, Boolean.FALSE);

        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberSpiValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdSpiValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CroNumberValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(PncIdValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OffenceGenericValidationAndEnricherRule.class)));
    }

    @Test
    public void shouldValidateDefendantValidateMCCRules() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_CHARGE_CASE, Channel.MCC,Boolean.FALSE, Boolean.FALSE);

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
    }

    @Test
    public void verifyGroupCasesValidationRules() {

        final List<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getGroupCasesValidationRules();

        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseCountValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OneDefendantPerProsecutionCaseValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(OneOffencePerDefendantValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(SameOffenceCodeValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<GroupProsecutionList, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(HearingDateValidationRule.class)));

    }

    @Test
    public void shouldIncludeDateOfHearingPastDateValidationRuleForOtherInitiationCodeWhenChannelIsCivilAndIsCivilIsTrueForGroupCase() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE, Boolean.TRUE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingPastDateValidationAndEnricherRule.class)));
    }

    @Test
    public void shouldIncludeDateOfHearingPastDateValidationRuleForSummonsInitiationCodeWhenChannelIsCivilAndIsCivilIsTrueForGroupCase() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_SUMMONS, Channel.CIVIL, Boolean.TRUE, Boolean.TRUE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingPastDateValidationAndEnricherRule.class)));
    }

    @Test
    public void shouldIncludeExtraDefendantRulesForSingleCivilCaseOnly() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> singleCaseRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE, Boolean.FALSE);
        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> groupCaseRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE, Boolean.TRUE);

        assertFalse(singleCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ChargeDateValidationRule.class)));
        assertTrue(singleCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ObservedEthnicityValidationAndEnricherRule.class)));
        assertTrue(singleCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DefendantInitiationCodeValidationRule.class)));
        assertTrue(singleCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PostCodeValidationRule.class)));

        assertFalse(groupCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ChargeDateValidationRule.class)));
        assertFalse(groupCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ObservedEthnicityValidationAndEnricherRule.class)));
        assertFalse(groupCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DefendantInitiationCodeValidationRule.class)));
        assertFalse(groupCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(PostCodeValidationRule.class)));

        assertTrue(groupCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingPastDateValidationAndEnricherRule.class)));
        assertFalse(singleCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingPastDateValidationAndEnricherRule.class)));
    }

    @Test
    public void shouldIncludeDateOfHearingMoreThan31DaysInPastRuleOnlyForSingleCivilCase() {

        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> singleCivilCaseRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE, Boolean.FALSE);
        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> groupCivilCaseRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.CIVIL, Boolean.TRUE, Boolean.TRUE);
        final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> mccCivilCaseRules = CcProsecutionValidationRuleProvider
                .getDefendantValidationRules(INITIATION_CODE_FOR_OTHER, Channel.MCC, Boolean.TRUE, Boolean.FALSE);

        assertTrue(singleCivilCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule.class)));
        assertFalse(groupCivilCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule.class)));
        assertFalse(mccCivilCaseRules.stream().map((Function<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule.class)));
    }

    @Test
    public void verifyCasesValidationRules() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil(null);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorReferenceDataValidationRule.class)));

    }

    @Test
    public void verifyGroupCasesValidationRulesWithSummonsCode() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil(CaseType.SUMMONS.getCode());

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s-> s.equals(SummonsCodeValidationRule.class)));
    }

    @Test
    public void shouldReturnCivilCaseValidationRulesWhenIsCivilTrue() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRules = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_CHARGE_CASE, Boolean.TRUE);

        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseInitiationValidationRule.class)));
        assertTrue(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(ProsecutorReferenceDataValidationRule.class)));
        assertFalse(validationRules.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseMarkersValidationAndEnricherRule.class)));
    }

    @Test
    public void shouldReturnStandardCaseValidationRulesWhenIsCivilFalseOrNull() {

        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRulesForFalse = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_CHARGE_CASE, Boolean.FALSE);
        final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> validationRulesForNull = CcProsecutionValidationRuleProvider
                .getCaseValidationRules(INITIATION_CODE_CHARGE_CASE, null);

        assertTrue(validationRulesForFalse.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseMarkersValidationAndEnricherRule.class)));
        assertTrue(validationRulesForNull.stream().map((Function<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>, ? extends Class<? extends ValidationRule>>) ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>::getClass).anyMatch(s -> s.equals(CaseMarkersValidationAndEnricherRule.class)));
    }

}
