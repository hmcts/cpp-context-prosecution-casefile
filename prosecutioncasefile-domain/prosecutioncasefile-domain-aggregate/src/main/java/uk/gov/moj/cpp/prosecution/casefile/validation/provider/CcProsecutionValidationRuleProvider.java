package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.CHARGE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.OTHER;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.REQUISITION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.SJP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.CaseType.SUMMONS;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CaseInitiationValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CaseMarkersValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CaseNewhearingListDefendantsValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.PoliceForceCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ProsecutorReferenceDataValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SummonsCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.AdditionalNationalityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.BailConditionsValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CorporateDefendantPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CorporateDefendantSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CourtHearingLocationValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CourtReceivedFromCodeCourtValidationRules;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CourtReceivedToCodeCourtValidationRules;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CroNumberValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.CustodyStatusValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DateOfHearingPastDateValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DateOfHearingValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DefendantDateOfBirthValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DefendantInitiationCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.DefendantPerceivedBirthYearValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.HearingTypeCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.IndividualDefendantPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.IndividualDefendantSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.LaidDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.NationalityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ObservedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.OffenderCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ParentGuardianDateOfBirthValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ParentGuardianObservedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ParentGuardianPrimaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ParentGuardianSecondaryEmailAddressValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdSpiValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PncIdValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.PostCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.SelfDefinedEthnicityValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ArrestDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ArrestDateValidationRuleForCivil;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.ChargeDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceAlcoholLevelValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceBackDutyValidationRuleAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceDrugLevelAmountValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceDrugLevelMethodValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceGenericValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.OffenceLocationValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.StatementOfFactsValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.StatementOfFactsWelshValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence.VehicleCodeValidationAndEnricherRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.CaseCountValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.DuplicateProsecutionReferenceValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.HearingDateValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.OneDefendantPerProsecutionCaseValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.OneOffencePerDefendantValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases.SameOffenceCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor.ProsecutorAOCPValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.prosecutor.ProsecutorSJPValidationRule;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("java:S4738")
public class CcProsecutionValidationRuleProvider {

    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> SUMMONS_CASE_RULE_SET = unmodifiableList(asList(
            new SummonsCodeValidationRule()
    ));
    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> SJP_CASE_RULE_SET = unmodifiableList(asList(
            new CaseInitiationValidationRule(),
            new SummonsCodeValidationRule(),
            new ProsecutorReferenceDataValidationRule(),
            new ProsecutorSJPValidationRule(),
            new ProsecutorAOCPValidationRule()
    ));
    private static final List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> COMMON_CASE_RULE_SET = unmodifiableList(asList(
            new CaseInitiationValidationRule(),
            new ProsecutorReferenceDataValidationRule(),
            new CaseMarkersValidationAndEnricherRule(),
            new PoliceForceCodeValidationRule(),
            new CaseNewhearingListDefendantsValidationRule()
    ));
    private static final Map<String, List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>>> caseValidationMap = of(
            SUMMONS.getCode(), Stream.of(SUMMONS_CASE_RULE_SET, COMMON_CASE_RULE_SET).flatMap(Collection::stream).toList(),
            CHARGE.getCode(), COMMON_CASE_RULE_SET,
            REQUISITION.getCode(), COMMON_CASE_RULE_SET,
            SJP.getCode(), SJP_CASE_RULE_SET);
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SJP_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new NationalityValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new ChargeDateValidationRule(),
            new DefendantDateOfBirthValidationRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE = unmodifiableList(asList(
            new NationalityValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new ChargeDateValidationRule(),
            new DefendantDateOfBirthValidationRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule(),
            new DefendantInitiationCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> CHARGE_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new BailConditionsValidationAndEnricherRule(),
            new ArrestDateValidationRule(),
            new ChargeDateValidationRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new CustodyStatusValidationAndEnricherRule()

    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> CHARGE_DEFENDANT_RULE_SET_CIVIL = unmodifiableList(asList(
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new BailConditionsValidationAndEnricherRule(),
            new ArrestDateValidationRuleForCivil(),
            new AdditionalNationalityValidationAndEnricherRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> DEFAULT_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new ArrestDateValidationRule(),
            new ChargeDateValidationRule(),
            new CustodyStatusValidationAndEnricherRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> OTHER_DEFENDANT_RULE_SET = unmodifiableList(Collections.singletonList(
            new CustodyStatusValidationAndEnricherRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SUMMONS_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new StatementOfFactsValidationRule(),
            new StatementOfFactsWelshValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SUMMONS_DEFENDANT_RULE_MCC_SET = unmodifiableList(asList(
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new AdditionalNationalityValidationAndEnricherRule(),
            new StatementOfFactsWelshValidationRule()
    ));

    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> REQUISITION_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new DateOfHearingPastDateValidationAndEnricherRule(),
            new ChargeDateValidationRule(),
            new AdditionalNationalityValidationAndEnricherRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> COMMON_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new CourtHearingLocationValidationRule(),
            new DateOfHearingValidationAndEnricherRule(),
            new IndividualDefendantPrimaryEmailAddressValidationRule(),
            new IndividualDefendantSecondaryEmailAddressValidationRule(),
            new CorporateDefendantPrimaryEmailAddressValidationRule(),
            new CorporateDefendantSecondaryEmailAddressValidationRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new ParentGuardianObservedEthnicityValidationAndEnricherRule(),
            new ParentGuardianSelfDefinedEthnicityValidationAndEnricherRule(),
            new ParentGuardianPrimaryEmailAddressValidationRule(),
            new ParentGuardianSecondaryEmailAddressValidationRule(),
            new OffenderCodeValidationAndEnricherRule(),
            new SelfDefinedEthnicityValidationAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new ObservedEthnicityValidationAndEnricherRule(),
            new OffenceAlcoholLevelValidationAndEnricherRule(),
            new DefendantDateOfBirthValidationRule(),
            new NationalityValidationAndEnricherRule(),
            new VehicleCodeValidationAndEnricherRule(),
            new DefendantPerceivedBirthYearValidationRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new OffenceDrugLevelMethodValidationAndEnricherRule(),
            new OffenceDrugLevelAmountValidationAndEnricherRule(),
            new OffenceBackDutyValidationRuleAndEnricherRule(),
            new CourtReceivedFromCodeCourtValidationRules(),
            new CourtReceivedToCodeCourtValidationRules(),
            new HearingTypeCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> SPI_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new PncIdSpiValidationRule(),
            new CroNumberSpiValidationRule(),
            new OffenceGenericValidationAndEnricherRule(),
            new PostCodeValidationRule(),
            new DefendantInitiationCodeValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> NON_POLICE_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new PncIdValidationRule(),
            new CroNumberValidationRule()
    ));
    private static final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> GROUP_CIVIL_DEFENDANT_RULE_SET = unmodifiableList(asList(
            new ArrestDateValidationRuleForCivil(),
            new DefendantDateOfBirthValidationRule(),
            new ParentGuardianDateOfBirthValidationRule(),
            new CourtHearingLocationValidationRule(),
            new OffenderCodeValidationAndEnricherRule(),
            new OffenceLocationValidationAndEnricherRule(),
            new NationalityValidationAndEnricherRule(),
            new VehicleCodeValidationAndEnricherRule(),
            new OffenceCodeValidationAndEnricherRule(),
            new CourtReceivedFromCodeCourtValidationRules(),
            new CourtReceivedToCodeCourtValidationRules(),
            new HearingTypeCodeValidationRule(),
            new LaidDateValidationRule()
    ));

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMap = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            SJP.getCode(), SJP_DEFENDANT_RULE_SET);
    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapMCC = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_MCC_SET).flatMap(Collection::stream).toList(),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            SJP.getCode(), SJP_DEFENDANT_RULE_SET);
    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapSpi = of(
            SUMMONS.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            CHARGE.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            REQUISITION.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, REQUISITION_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            OTHER.getCode(), Stream.of(COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, OTHER_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            SJP.getCode(), SPI_DEFENDANT_RULE_SET_FOR_INITIATION_CODE);
    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapForGroupCivilCases = of(
            SUMMONS.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList(),
            OTHER.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList());

    private static final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMapMCCCivil = of(
            SUMMONS.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET,COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, SUMMONS_DEFENDANT_RULE_MCC_SET).flatMap(Collection::stream).toList(),
            OTHER.getCode(), Stream.of(GROUP_CIVIL_DEFENDANT_RULE_SET,COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, CHARGE_DEFENDANT_RULE_SET_CIVIL).flatMap(Collection::stream).toList());


    private CcProsecutionValidationRuleProvider() {
    }

    public static List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> getCaseValidationRules(final String caseInitiationCode) {

        if (caseValidationMap.containsKey(caseInitiationCode)) {
            return caseValidationMap.get(caseInitiationCode);
        } else {
            return COMMON_CASE_RULE_SET;
        }

    }

    public static List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> getDefendantValidationRules(final String defendantInitiationCode,
                                                                                                                          final Channel channel,
                                                                                                                          final Boolean isCivil) {
        if (nonNull(channel) && CIVIL.equals(channel) && nonNull(isCivil) && (isCivil)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapForGroupCivilCases);
        } else if (nonNull(channel) && MCC.equals(channel) && nonNull(isCivil) && (isCivil)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapMCCCivil);
        } else if (nonNull(channel) && SPI.equals(channel)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapSpi);
        } else if (nonNull(channel) && MCC.equals(channel)) {
            return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, SPI_DEFENDANT_RULE_SET, defendantValidationMapMCC);
        }
        return getValidationRules(defendantInitiationCode, COMMON_DEFENDANT_RULE_SET, NON_POLICE_DEFENDANT_RULE_SET, defendantValidationMap);
    }

    public static List<ValidationRule<GroupProsecutionList, ReferenceDataQueryService>> getGroupCasesValidationRules() {
        return unmodifiableList(asList(
                new CaseCountValidationRule(),
                new OneDefendantPerProsecutionCaseValidationRule(),
                new OneOffencePerDefendantValidationRule(),
                new SameOffenceCodeValidationRule(),
                new HearingDateValidationRule(),
                new DuplicateProsecutionReferenceValidationRule()
        ));
    }

    public static List<ValidationRule<ProsecutionWithReferenceData, ReferenceDataQueryService>> getCaseValidationRulesForCivil(final String initiationCode) {
        if(SUMMONS.getCode().equalsIgnoreCase(initiationCode)) {
            return unmodifiableList(asList(
                    new CaseInitiationValidationRule(),
                    new ProsecutorReferenceDataValidationRule(),
                    new SummonsCodeValidationRule()
            ));
        }
        return unmodifiableList(asList(
                new CaseInitiationValidationRule(),
                new ProsecutorReferenceDataValidationRule()
        ));
    }

    private static List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> getValidationRules(
            final String defendantInitiationCode,
            final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> commonDefendantRules,
            final List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>> spiDefendantRules,
            final Map<String, List<ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService>>> defendantValidationMap) {

        if (defendantValidationMap.containsKey(defendantInitiationCode)) {
            return defendantValidationMap.get(defendantInitiationCode);
        } else {
            return Stream.of(commonDefendantRules, spiDefendantRules, DEFAULT_DEFENDANT_RULE_SET).flatMap(Collection::stream).toList();
        }
    }
}