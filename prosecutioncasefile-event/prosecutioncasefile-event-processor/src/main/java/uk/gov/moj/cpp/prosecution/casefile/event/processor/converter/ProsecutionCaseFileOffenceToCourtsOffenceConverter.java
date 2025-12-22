package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.lang.Integer.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.core.courts.CommittingCourt.committingCourt;
import static uk.gov.justice.core.courts.InitiationCode.O;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;

import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.VehicleCodeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

public class ProsecutionCaseFileOffenceToCourtsOffenceConverter implements ParameterisedConverter<List<Offence>, List<uk.gov.justice.core.courts.Offence>, ParamsVO> {

    private static final Logger LOGGER = getLogger(ProsecutionCaseFileOffenceToCourtsOffenceConverter.class);

    private static final String MAGISTRATES_COURT_HOUSE_TYPE = "B";
    private static final String SUMMARY_ONLY_MODE_OF_TRIAL = "Summary";
    private static final String EITHER_WAY = "Either Way";
    private static final String INDICATED_GUILTY = "INDICATED_GUILTY";
    private static final String GUILTY = "GUILTY";

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Override
    public List<uk.gov.justice.core.courts.Offence> convert(final List<Offence> source, final ParamsVO param) {
        return source.stream().map(offence -> buildOffence(offence, param)).collect(toList());
    }

    private uk.gov.justice.core.courts.Offence buildOffence(final Offence offence, final ParamsVO paramsVO) {
        final ReferenceDataVO referenceDataVO = paramsVO.getReferenceDataVO();
        final String modeOfTrialDerived = getModeOfTrialDerived(offence.getOffenceCode(), referenceDataVO);

        final CustodyTimeLimit custodyTimeLimit = Optional.ofNullable(paramsVO.getCustodyTimelineDefendant()).map(e-> CustodyTimeLimit.custodyTimeLimit().withTimeLimit(e.toString()).build()).orElse(null);
        CourtCentre convictingCourt = getConvictingCourt(offence, paramsVO);
        return offence()
                .withId(offence.getOffenceId())
                .withArrestDate(getDate(offence.getArrestDate()))
                .withChargeDate(getDate(offence.getChargeDate()))
                .withCount(0)
                .withOffenceCode(offence.getOffenceCode())
                .withOffenceDefinitionId(getOffenceId(offence.getOffenceCode(), referenceDataVO))
                .withOffenceFacts(buildOffenceFacts(offence))
                .withOffenceTitle(getOffenceTitle(offence.getOffenceCode(), referenceDataVO))
                .withOffenceTitleWelsh(getOffenceWelshTitle(offence.getOffenceCode(), referenceDataVO))
                .withOrderIndex(offence.getOffenceSequenceNumber())
                .withStartDate(offence.getOffenceCommittedDate().toString())
                .withEndDate(getDate(offence.getOffenceCommittedEndDate()))
                .withLaidDate(getDate(offence.getLaidDate()))
                .withWording(offence.getOffenceWording())
                .withWordingWelsh(offence.getOffenceWordingWelsh())
                .withModeOfTrial(modeOfTrialDerived)
                .withOffenceLegislation(getOffenceLegislation(offence.getOffenceCode(), referenceDataVO))
                .withOffenceLegislationWelsh(getOffenceLegislationWelsh(offence.getOffenceCode(), referenceDataVO))
                .withOffenceDateCode(offence.getOffenceDateCode())
                .withCommittingCourt(getCommittingCourtFromReferenceData(paramsVO))
                .withPlea(convertPlea(offence))
                .withVerdict(convertVerdict(offence))
                .withConvictionDate(calculateConvictionDate(offence, paramsVO))
                .withAllocationDecision(buildAllocationDecision(offence, paramsVO))
                .withDvlaOffenceCode(getDvlaCode(offence.getOffenceCode(), referenceDataVO))
                .withMaxPenalty(getMaxPenalty(offence.getOffenceCode(), referenceDataVO))
                .withConvictingCourt(convictingCourt)
                .withCustodyTimeLimit(isCustodyLimitTobeSet(offence, paramsVO) ? custodyTimeLimit : null)
                .withCivilOffence(offence.getCivilOffence())
                .build();
    }

    private boolean isCustodyLimitTobeSet(final Offence offence, final ParamsVO paramsVO) {
        boolean guiltyPlea = hasGuiltyPlea(offence);
        final boolean guiltyVerdict = hasGuiltyVerdict(offence);
        boolean convictingCourtIsNull = isNull(offence.getConvictingCourtCode());
        final boolean isMCC = MCC.equals(paramsVO.getChannel())
                && O.name().equalsIgnoreCase(paramsVO.getInitiationCode());
        return isMCC && !guiltyPlea && !guiltyVerdict && convictingCourtIsNull;

    }

    private CourtCentre getConvictingCourt(final Offence offence, final ParamsVO paramsVO) {
        final boolean guiltyPlea = hasGuiltyPlea(offence);
        final boolean guiltyVerdict = hasGuiltyVerdict(offence);
        final boolean isMCC = MCC.equals(paramsVO.getChannel())
                && O.name().equalsIgnoreCase(paramsVO.getInitiationCode());

        if (isMCC && guiltyPlea && Objects.nonNull(offence.getConvictingCourtCode()))  {
            return getCourtCentre(offence.getConvictingCourtCode());
        } else if (isMCC && guiltyVerdict && Objects.nonNull(offence.getConvictingCourtCode())) {
            return getCourtCentre(offence.getConvictingCourtCode());
        }

        return null;
    }

    private String getDvlaCode(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getDvlaCode).orElse(null);
    }

    private String getMaxPenalty(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getMaxPenalty).orElse(null);
    }

    /**
     * Regarding whether motReasonId exists in offence It returns matching motreasonId with
     * referenceData or  returns a modeOfTrialReason which description is Summary-only offence
     *
     * @param offence - the offence to use motReasonId
     * @return ModeOfTrialReasonsReferenceData
     * @param paramsVO  - which includes Reference Data with all ModeOfTrialReasons
     **/
    private ModeOfTrialReasonsReferenceData retrieveModeOfTrialReason(final Offence offence, final ParamsVO paramsVO) {
        return paramsVO.getReferenceDataVO().getModeOfTrialReasonsReferenceData().stream()
                .filter(mot -> (nonNull(offence.getMotReasonId()) && mot.getId().equals(offence.getMotReasonId().toString())) ||
                        (isNull(offence.getMotReasonId()) && "Summary-only offence".equals(mot.getDescription())))
                .findFirst()
                .orElse(null);
    }

    /**
     * checks whether case created by manually and InitiationCode is trial and mode of trial is
     * valid for allocation decision
     */
    private boolean isValidForAllocationDecision(final ParamsVO paramsVO, final Offence offence) {
        return O.name().equals(paramsVO.getInitiationCode())
                && MCC.equals(paramsVO.getChannel())
                && isValidModeOfTrialForAllocationDecision(offence);
    }

    /**
     * checks whether mode of trial is Summary or either way with motReasonId
     */
    private boolean isValidModeOfTrialForAllocationDecision(final Offence offence) {
        return nonNull(offence.getReferenceData())
                && (SUMMARY_ONLY_MODE_OF_TRIAL.equals(offence.getReferenceData().getModeOfTrialDerived())
                || (EITHER_WAY.equals(offence.getReferenceData().getModeOfTrialDerived()) && nonNull(offence.getMotReasonId())));
    }

    private AllocationDecision buildAllocationDecision(final Offence offence, final ParamsVO paramsVO) {
        final ModeOfTrialReasonsReferenceData modeOfTrialReason = retrieveModeOfTrialReason(offence, paramsVO);

        AllocationDecision allocationDecision = null;
        if (nonNull(modeOfTrialReason) && isValidForAllocationDecision(paramsVO, offence)) {
            LOGGER.debug("Setting allocation decision to ModeOfTrialReasonId: {} : {}",
                    modeOfTrialReason.getId(),
                    nonNull(offence.getReferenceData()) ? offence.getReferenceData().getModeOfTrialDerived() : null);
            allocationDecision = AllocationDecision.allocationDecision()
                    .withOffenceId(offence.getOffenceId())
                    .withMotReasonId(fromString(modeOfTrialReason.getId()))
                    .withMotReasonCode(modeOfTrialReason.getCode())
                    .withMotReasonDescription(modeOfTrialReason.getDescription())
                    .withSequenceNumber(valueOf(modeOfTrialReason.getSeqNum()))
                    .build();
        }
        return allocationDecision;
    }


    private String getDate(final LocalDate date) {
        return nonNull(date) ? date.toString() : null;
    }

    private String getOffenceLegislation(String offenceCode, ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getLegislation).orElse(null);
    }

    private String getOffenceLegislationWelsh(String offenceCode, ReferenceDataVO referenceDataVO) {
        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getLegislationWelsh).orElse(null);
    }

    private OffenceFacts buildOffenceFacts(final Offence offence) {
        final VehicleCode vehicleCode = getVehicleCode(offence);
        final String vehicleRegistration = buildVehicleRegistration(offence);
        final String alcoholReadingMethodCode = offence.getAlcoholRelatedOffence() != null ? offence.getAlcoholRelatedOffence().getAlcoholLevelMethod() : null;
        final Integer alcoholReadingAmount = getAlcoholReadingAmount(offence);

        if (vehicleCode == null && isEmpty(vehicleRegistration) && isEmpty(alcoholReadingMethodCode) && alcoholReadingAmount == null) {
            return null;
        }

        return OffenceFacts.offenceFacts()
                .withVehicleCode(vehicleCode)
                .withVehicleRegistration(vehicleRegistration)
                .withAlcoholReadingMethodCode(alcoholReadingMethodCode)
                .withAlcoholReadingMethodDescription(getAlcoholLevelMethodDescription(alcoholReadingMethodCode))
                .withAlcoholReadingAmount(alcoholReadingAmount)
                .build();
    }

    private String getAlcoholLevelMethodDescription(final String alcoholMethodCode) {
        final List<AlcoholLevelMethodReferenceData> alcoholLevelMethodReferenceData = referenceDataQueryService.retrieveAlcoholLevelMethods();
        return alcoholLevelMethodReferenceData.stream().filter(am -> am.getMethodCode().equals(alcoholMethodCode))
                .map(AlcoholLevelMethodReferenceData::getMethodDescription)
                .findFirst()
                .orElse(null);
    }

    private Integer getAlcoholReadingAmount(final Offence offence) {
        return (offence.getAlcoholRelatedOffence() != null && offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() != null) ? offence.getAlcoholRelatedOffence().getAlcoholLevelAmount() : null;
    }

    private String buildVehicleRegistration(final Offence offence) {
        return (offence.getVehicleRelatedOffence() != null && offence.getVehicleRelatedOffence().getVehicleRegistrationMark() != null) ? offence.getVehicleRelatedOffence().getVehicleRegistrationMark() : null;
    }

    private VehicleCode getVehicleCode(final Offence offence) {

        if (null != offence.getVehicleRelatedOffence()) {
            final Optional<VehicleCode> vehicleCodeForCC = VehicleCodeType.valueFor(offence.getVehicleRelatedOffence().getVehicleCode());
            if (vehicleCodeForCC.isPresent()) {
                return vehicleCodeForCC.get();
            }

        }
        return null;
    }

    private UUID getOffenceId(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getOffenceId).orElse(null);
    }

    private String getModeOfTrialDerived(final String offenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(offenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getModeOfTrialDerived).orElse(null);
    }

    private String getOffenceTitle(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getTitle).orElse(null);
    }

    private String getOffenceWelshTitle(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {

        final Optional<OffenceReferenceData> offenceReferenceData = getOffenceReferenceData(cjsOffenceCode, referenceDataVO);
        return offenceReferenceData.map(OffenceReferenceData::getTitleWelsh).orElse(null);
    }

    private Optional<OffenceReferenceData> getOffenceReferenceData(final String cjsOffenceCode, final ReferenceDataVO referenceDataVO) {
        return referenceDataVO.getOffenceReferenceData().stream().
                filter(offenceReferenceData1 -> offenceReferenceData1.getCjsOffenceCode().equals(cjsOffenceCode)).findFirst();
    }

    private String calculateConvictionDate(final Offence offence, final ParamsVO paramsVO) {
        String convictionDate = null;

        if(nonNull(paramsVO.getCivil()) && paramsVO.getCivil()){
            return convictionDate;
        }

        final boolean guiltyPlea = hasGuiltyPlea(offence);
        final boolean guiltyVerdict = hasGuiltyVerdict(offence);

        final boolean isMCCCrownCourt = MCC.equals(paramsVO.getChannel())
                && O.name().equalsIgnoreCase(paramsVO.getInitiationCode());

        if (isMCCCrownCourt && guiltyPlea) {
            convictionDate = offence.getPlea().getPleaDate().toString();
        } else if (isMCCCrownCourt && guiltyVerdict) {
            convictionDate = offence.getVerdict().getVerdictDate().toString();
        }
        LOGGER.debug("Conviction date of '{}' is set for offence id '{}'", convictionDate, offence.getOffenceId());

        return convictionDate;
    }

    private boolean hasGuiltyVerdict(final Offence offence) {
        return offence.getVerdict() != null && offence.getVerdict().getVerdictType() != null
                && GUILTY.equalsIgnoreCase(offence.getVerdict().getVerdictType().getCategory());
    }

    private boolean hasGuiltyPlea(final Offence offence) {
        return offence.getPlea() != null && (INDICATED_GUILTY.equalsIgnoreCase(offence.getPlea().getPleaValue()) || GUILTY.equalsIgnoreCase(offence.getPlea().getPleaValue()));
    }

    private Verdict convertVerdict(final Offence offence) {
        Verdict convertedVerdict = null;

        if (offence.getVerdict() != null) {
            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Verdict verdict = offence.getVerdict();

            convertedVerdict = Verdict.verdict()
                    .withOffenceId(offence.getOffenceId())
                    .withVerdictDate(verdict.getVerdictDate().toString())
                    .withVerdictType(VerdictType.verdictType()
                            .withId(verdict.getVerdictType().getId())
                            .withCategory(verdict.getVerdictType().getCategory())
                            .withCategoryType(verdict.getVerdictType().getCategoryType())
                            .withDescription(verdict.getVerdictType().getDescription())
                            .build())
                    .build();
        }

        return convertedVerdict;
    }

    private Plea convertPlea(final Offence offence) {
        Plea convertedPlea = null;

        if (offence.getPlea() != null) {
            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea plea = offence.getPlea();

            convertedPlea = Plea.plea()
                    .withOffenceId(offence.getOffenceId())
                    .withPleaDate(plea.getPleaDate().toString())
                    .withPleaValue(plea.getPleaValue())
                    .build();
        }

        return convertedPlea;
    }

    /**
     * firstly checks whether initiation type is Trial or Committal for sentence and channel is MCC
     * then return committingCourt regarding query from referencedata.query.organisationunits
     *
     * @param paramsVO which includes channel, initiationType and receivedFromCourtOUCode
     * @return committingCourt
     */
    private CommittingCourt getCommittingCourtFromReferenceData(final ParamsVO paramsVO) {
        if (isNotEmpty(paramsVO.getReceivedFromCourtOUCode()) && O.name().equals(paramsVO.getInitiationCode()) && MCC.equals(paramsVO.getChannel())) {
            final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnits(paramsVO.getReceivedFromCourtOUCode());

            if (isNotEmpty(organisationUnits)) {
                final OrganisationUnitReferenceData organisationUnit = organisationUnits.get(0);
                return committingCourt()
                        .withCourtCentreId(fromString(organisationUnit.getId()))
                        .withCourtHouseType(MAGISTRATES_COURT_HOUSE_TYPE.equalsIgnoreCase(organisationUnit.getOucodeL1Code()) ? MAGISTRATES : CROWN)
                        .withCourtHouseCode(organisationUnit.getOucodeL3Code())
                        .withCourtHouseName(organisationUnit.getOucodeL3Name())
                        .withCourtHouseShortName(organisationUnit.getOucodeL3Name())
                        .build();
            }
        }
        return null;
    }

    private CourtCentre getCourtCentre(String convictingCourtCode) {
        final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnits(convictingCourtCode);
        if (isNotEmpty(organisationUnits)) {
            final OrganisationUnitReferenceData organisationUnitReferenceData = organisationUnits.get(0);
            final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
            courtCentreBuilder.withId(fromString(organisationUnitReferenceData.getId()));
            courtCentreBuilder.withName(organisationUnitReferenceData.getOucodeL3Name());
            courtCentreBuilder.withWelshName(organisationUnitReferenceData.getOucodeL3WelshName());
            if (nonNull(organisationUnitReferenceData.getCourtLocationCode())) {
                courtCentreBuilder.withCourtLocationCode(organisationUnitReferenceData.getCourtLocationCode());
            } else {
                final Optional<LjaDetails> ljaDetailsOptional = referenceDataQueryService.getLjaDetails(organisationUnitReferenceData.getLja(), organisationUnitReferenceData.getId());
                ljaDetailsOptional.ifPresent(courtCentreBuilder::withLja);
            }
            return courtCentreBuilder.build();
        }
        return null;
    }
}
