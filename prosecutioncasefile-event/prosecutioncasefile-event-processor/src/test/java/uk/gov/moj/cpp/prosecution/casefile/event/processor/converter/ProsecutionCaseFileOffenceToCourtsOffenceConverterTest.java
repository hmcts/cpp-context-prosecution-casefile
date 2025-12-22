package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildOffences;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildReferenceDataIncludingDvlaCode;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildReferenceDataWithOffenceAndModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholLevelMethodReferenceData.alcoholLevelMethodReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.AlcoholRelatedOffence.alcoholRelatedOffence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;

import uk.gov.justice.core.courts.CivilOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ModeOfTrialReasonsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Verdict;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VerdictType;


@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileOffenceToCourtsOffenceConverterTest {

    private static final String EITHER_WAY = "Either Way";
    private static final String SUMMARY = "Summary";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private ProsecutionCaseFileOffenceToCourtsOffenceConverter converter;

    @Test
    public void shouldBuildAllocationDecisionWithSummaryOnlyWhenOffenceMoTIsNullAndChannelIsMCC() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Summary")
                        .build())
                        .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        final LocalDate custodyTimelineDefendant = LocalDate.now().minusDays(3);
        paramsVO.setCustodyTimelineDefendant(custodyTimelineDefendant);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getAllocationDecision().getOffenceId(), is(offenceId));

        final ModeOfTrialReasonsReferenceData modeOfTrialReasonsReferenceData = referenceDataVO.getModeOfTrialReasonsReferenceData().get(0);
        assertThat(offence.getAllocationDecision().getMotReasonId(), is(fromString(modeOfTrialReasonsReferenceData.getId())));
        assertThat(offence.getAllocationDecision().getMotReasonCode(), is(modeOfTrialReasonsReferenceData.getCode()));
        assertThat(offence.getAllocationDecision().getMotReasonDescription(), is(modeOfTrialReasonsReferenceData.getDescription()));
        assertThat(offence.getAllocationDecision().getSequenceNumber(), is(Integer.valueOf(modeOfTrialReasonsReferenceData.getSeqNum())));
        assertThat(offence.getCustodyTimeLimit().getTimeLimit(), is(custodyTimelineDefendant.toString()));

    }

    @Test
    public void shouldBuildAllocationDecisionWhenOffenceMoTIdInReferenceDataAndChannelIsMCC() {
        final UUID motReasonId = randomUUID();
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY, motReasonId.toString());
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withMotReasonId(motReasonId)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Either Way")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getAllocationDecision().getOffenceId(), is(offenceId));

        final ModeOfTrialReasonsReferenceData modeOfTrialReasonsReferenceData = referenceDataVO.getModeOfTrialReasonsReferenceData().get(1);
        assertThat(offence.getAllocationDecision().getMotReasonId(), is(fromString(modeOfTrialReasonsReferenceData.getId())));
        assertThat(offence.getAllocationDecision().getMotReasonCode(), is(modeOfTrialReasonsReferenceData.getCode()));
        assertThat(offence.getAllocationDecision().getMotReasonDescription(), is(modeOfTrialReasonsReferenceData.getDescription()));
        assertThat(offence.getAllocationDecision().getSequenceNumber(), is(Integer.valueOf(modeOfTrialReasonsReferenceData.getSeqNum())));
    }

    @Test
    public void shouldNotBuildAllocationDecisionWhenOffenceMoTIsOtherAndChannelIsMCC() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Indictable")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(nullValue()));
    }


    @Test
    public void shouldNotBuildAllocationDecisionWhenOffenceIsSummaryOnlyAndChannelIsMCC() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Summary")
                        .build())
                .withMaxPenalty("Max Penalty")
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.J.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(nullValue()));
    }

    @Test
    public void shouldNotBuildAllocationDecisionWhenOffenceIsSummaryOnlyAndChannelIsNotMCC() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(SUMMARY);
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Indictable")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(SPI);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);
        assertThat(coreOffences.get(0).getAllocationDecision(), is(nullValue()));
    }

    @Test
    public void convertToCourtsOffenceWithDVLACode() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataIncludingDvlaCode();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(buildOffences(), paramsVO);

        assertThat(coreOffences.get(0).getDvlaOffenceCode(), is("dvlaCode"));
    }

    @Test
    public void convertToCourtsOffenceWithNoDVLACode() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(buildOffences(), paramsVO);

        assertNull(coreOffences.get(0).getDvlaOffenceCode());
    }


    @Test
    public void convertToCourtsOffenceWithMaxPenalty() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataIncludingDvlaCode();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(buildOffences(), paramsVO);

        assertThat(coreOffences.get(0).getMaxPenalty(), is("Max Penalty"));
    }

    @Test
    public void shouldConvertOffences() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate arrestDate = LocalDate.now().minusDays(3);
        final LocalDate chargeDate = LocalDate.now().minusDays(2);
        final String offenceCode = "TVL-ABC";
        final int offenceSequenceNumber = 2;
        final LocalDate committedDate = LocalDate.now();

        final LocalDate offenceCommittedEndDate = LocalDate.now().plusDays(2);
        final LocalDate laidDate = LocalDate.now().plusDays(1);
        final String offenceWording = "offence wording";
        final String offenceWordingWelsh = "offence wording welsh";
        final Integer offenceDateCode = 3;
        final int alcoholLevelAmount = 5;
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(2);
        final UUID guiltyVerdictId = randomUUID();
        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withArrestDate(arrestDate)
                .withChargeDate(chargeDate)
                .withOffenceCode(offenceCode)
                .withVehicleRelatedOffence(new VehicleRelatedOffence("OTHER", "L"))
                .withAlcoholRelatedOffence(alcoholRelatedOffence()
                        .withAlcoholLevelMethod("A")
                        .withAlcoholLevelAmount(alcoholLevelAmount).build())
                .withOffenceSequenceNumber(offenceSequenceNumber)
                .withOffenceCommittedDate(committedDate)
                .withOffenceCommittedEndDate(offenceCommittedEndDate)
                .withLaidDate(laidDate)
                .withOffenceWording(offenceWording)
                .withOffenceWordingWelsh(offenceWordingWelsh)
                .withOffenceDateCode(offenceDateCode)
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("GUILTY")
                        .build())
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Guilty")
                                .withCategoryType("GUILTY")
                                .withDescription("Description")
                                .build())
                        .build())
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrialDerived("Summary")
                        .build())
                .withMaxPenalty("Max Penalty")
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(true).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);

        when(referenceDataQueryService.retrieveAlcoholLevelMethods()).thenReturn(asList(alcoholLevelMethodReferenceData().withMethodCode("A").withMethodDescription("Blood").build(),
                alcoholLevelMethodReferenceData().withMethodCode("B").withMethodDescription("Breath").build()));


        List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        verify(referenceDataQueryService, never()).retrieveOrganisationUnits(anyString());

        assertThat(coreOffences.size(), is(1));
        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getArrestDate(), is(arrestDate.toString()));
        assertThat(offence.getChargeDate(), is(chargeDate.toString()));
        assertThat(offence.getCount(), is(0));
        assertThat(offence.getOffenceCode(), is(offenceCode));
        assertThat(offence.getOffenceDefinitionId(), is(fromString("d8c63737-3c60-496b-94bb-30faa761f00a")));
        assertThat(offence.getOffenceFacts().getAlcoholReadingAmount(), is(alcoholLevelAmount));
        assertThat(offence.getOffenceFacts().getAlcoholReadingMethodCode(), is("A"));
        assertThat(offence.getOffenceFacts().getAlcoholReadingMethodDescription(), is("Blood"));
        assertThat(offence.getOffenceFacts().getVehicleRegistration(), is("L"));
        assertThat(offence.getOffenceTitle(), is("Offence Tittle"));
        assertThat(offence.getOffenceTitleWelsh(), is("Offence Tittle Welsh") );
        assertThat(offence.getOrderIndex(), is(offenceSequenceNumber));
        assertThat(offence.getStartDate(), is(committedDate.toString()));
        assertThat(offence.getEndDate(), is(offenceCommittedEndDate.toString()));
        assertThat(offence.getLaidDate(), is(laidDate.toString()));
        assertThat(offence.getWording(), is(offenceWording));
        assertThat(offence.getWordingWelsh(), is(offenceWordingWelsh));
        assertThat(offence.getModeOfTrial(), is("Either Way"));
        assertThat(offence.getOffenceLegislation(), is("offenceLegalisation"));
        assertThat(offence.getOffenceLegislationWelsh(), is("offenceLegalisationWelsh"));
        assertThat(offence.getOffenceDateCode(), is(offenceDateCode));
        assertNull(offence.getCommittingCourt());

        assertThat(offence.getPlea().getOffenceId(), is(offence.getId()));
        assertThat(offence.getPlea().getPleaDate(), is(pleaDate.toString()));
        assertThat(offence.getPlea().getPleaValue(), is("GUILTY"));
        assertThat(offence.getVerdict().getOffenceId(), is(offence.getId()));
        assertThat(offence.getVerdict().getVerdictDate(), is(verdictDate.toString()));
        assertThat(offence.getVerdict().getVerdictType().getId(), is(guiltyVerdictId));
        assertThat(offence.getVerdict().getVerdictType().getCategory(), is("Guilty"));
        assertThat(offence.getVerdict().getVerdictType().getCategoryType(), is("GUILTY"));
        assertThat(offence.getVerdict().getVerdictType().getDescription(), is("Description"));
        assertThat(offence.getMaxPenalty(), is("Max Penalty"));
        assertNotNull(offence.getCivilOffence());
        assertThat(offence.getCivilOffence().getIsExParte(), is(true));
    }

    @Test
    public void shouldConvertOffencesWithCommittingCourtWhenCaseIsMccAndTrialAndCourtLocationExists() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final String courtReceivedFromCode = "B01KR00";
        final String courtCentreId = randomUUID().toString();
        final UUID offenceId = randomUUID();
        final String offenceCode = "TVL-ABC";
        final String oucodeL1Code = "B";
        final String oucodeL3Code = "KR";
        final String oucodeL3Name = "Barkingside Magistrates' Court";
        final LocalDate committedDate = LocalDate.now();

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(committedDate)
                .withReferenceData(offenceReferenceData()
                        .withModeOfTrial("Summary only")
                        .build())
                        .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setReceivedFromCourtOUCode(courtReceivedFromCode);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        when(referenceDataQueryService.retrieveOrganisationUnits(eq(courtReceivedFromCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCentreId)
                        .withOucodeL1Code(oucodeL1Code)
                        .withOucodeL3Code(oucodeL3Code)
                        .withOucodeL3Name(oucodeL3Name)
                        .build()));
        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        verify(referenceDataQueryService).retrieveOrganisationUnits(courtReceivedFromCode);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));

        assertThat(offence.getCommittingCourt().getCourtCentreId(), is(fromString(courtCentreId)));
        assertThat(offence.getCommittingCourt().getCourtHouseType(), is(JurisdictionType.MAGISTRATES));
        assertThat(offence.getCommittingCourt().getCourtHouseCode(), is(oucodeL3Code));
        assertThat(offence.getCommittingCourt().getCourtHouseName(), is(oucodeL3Name));
        assertThat(offence.getCommittingCourt().getCourtHouseShortName(), is(oucodeL3Name));
    }

    @Test
    public void shouldSetConvictionDateAndConvictingCourtBasedOnGuiltyPleaDate() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(null);
        final UUID offenceId = randomUUID();
        final UUID courtCenterId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final String convictingCourtCode = "COURT100";
        when(referenceDataQueryService.retrieveOrganisationUnits(eq(convictingCourtCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCenterId.toString())
                        .withOucodeL1Code(convictingCourtCode).build()));

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("GUILTY")
                        .build())
                        .withConvictingCourtCode(convictingCourtCode)
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
        assertThat(offence.getConvictingCourt().getId(), is(courtCenterId));
        assertNull(offence.getConvictingCourt().getCourtLocationCode());
    }

    @Test
    public void shouldSetCourtLocationCodeBasedOnConvictingCourt() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(null);
        final UUID offenceId = randomUUID();
        final UUID courtCenterId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final String convictingCourtCode = "COURT100";
        when(referenceDataQueryService.retrieveOrganisationUnits(eq(convictingCourtCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCenterId.toString())
                        .withOucodeL1Code(convictingCourtCode)
                        .withCourtLocationCode("1234")
                        .build()));

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("GUILTY")
                        .build())
                .withConvictingCourtCode(convictingCourtCode)
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
        assertThat(offence.getConvictingCourt().getId(), is(courtCenterId));
        assertThat(offence.getConvictingCourt().getCourtLocationCode(), is("1234"));
    }

    @Test
    public void shouldSetLjaDetailsBasedOnConvictingCourt() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(null);
        final UUID offenceId = randomUUID();
        final UUID courtCenterId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final String convictingCourtCode = "COURT100";
        when(referenceDataQueryService.retrieveOrganisationUnits(eq(convictingCourtCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCenterId.toString())
                        .withOucodeL1Code(convictingCourtCode)
                        .withLja("2800")
                        .build()));

        when(referenceDataQueryService.getLjaDetails("2800", courtCenterId.toString()))
                .thenReturn(Optional.of(LjaDetails.ljaDetails()
                        .withLjaCode("2800")
                        .withLjaName("South Yorkshire Magistrates' Court")
                        .build()));

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("GUILTY")
                        .build())
                .withConvictingCourtCode(convictingCourtCode)
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
        assertThat(offence.getConvictingCourt().getId(), is(courtCenterId));
        assertThat(offence.getConvictingCourt().getLja().getLjaCode(), is("2800"));
        assertThat(offence.getConvictingCourt().getLja().getLjaName(), is("South Yorkshire Magistrates' Court"));
    }

    @Test
    public void shouldSetConvictionDateBasedOnIndicatedGuiltyPleaDate() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("INDICATED_GUILTY")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
    }

    @Test
    public void shouldSetConvictionDateBasedOnGuiltyVerdictDate() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";
        final UUID guiltyVerdictId = randomUUID();

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Guilty")
                                .withCategoryType("GUILTY")
                                .build())
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(verdictDate.toString()));
    }

    @Test
    public void shouldNotSetConvictionDateWhenNotGuiltyPleaAndVerdict() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";
        final UUID guiltyVerdictId = randomUUID();

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("NOT_GUILTY")
                        .build())
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Not Guilty")
                                .withCategoryType("NOT_GUILTY")
                                .build())
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setCustodyTimelineDefendant(LocalDate.now());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
        assertNotNull(offence.getCustodyTimeLimit());
    }

    @Test
    public void shouldNotSetConvictionDateWhenNonMCCChannel() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("INDICATED_GUILTY")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(SPI);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
    }

    @Test
    public void shouldNotSetConvictionDateWhenNotAnOtherInitiationType() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("INDICATED_GUILTY")
                        .build())
                .withCivilOffence(CivilOffence.civilOffence().withIsExParte(false).build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.S.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getConvictionDate());
    }

    @Test
    public void shouldSetCourtLocationCodeForConvictingCourtWhenVerdictTypeIsGuilty() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(null);
        final UUID offenceId = randomUUID();
        final UUID courtCenterId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";
        final UUID guiltyVerdictId = randomUUID();

        final String convictingCourtCode = "COURT100";
        when(referenceDataQueryService.retrieveOrganisationUnits(eq(convictingCourtCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCenterId.toString())
                        .withOucodeL1Code(convictingCourtCode)
                        .withCourtLocationCode("1234")
                        .build()));

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("NOT GUILTY")
                        .build())
                .withConvictingCourtCode(convictingCourtCode)
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Guilty")
                                .withCategoryType("GUILTY")
                                .build())
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
        assertThat(offence.getConvictingCourt().getId(), is(courtCenterId));
        assertThat(offence.getConvictingCourt().getCourtLocationCode(), is("1234"));
    }

    @Test
    public void shouldSetLjaDetailsForConvictingCourtWhenVerdictTypeIsGuilty() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(null);
        final UUID offenceId = randomUUID();
        final UUID courtCenterId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final UUID guiltyVerdictId = randomUUID();

        final String convictingCourtCode = "COURT100";
        when(referenceDataQueryService.retrieveOrganisationUnits(eq(convictingCourtCode)))
                .thenReturn(List.of(OrganisationUnitReferenceData.organisationUnitReferenceData()
                        .withId(courtCenterId.toString())
                        .withOucodeL1Code(convictingCourtCode)
                        .withLja("2800")
                        .build()));

        String ljaName = "South Yorkshire Magistrates' Court";
        when(referenceDataQueryService.getLjaDetails("2800", courtCenterId.toString()))
                .thenReturn(Optional.of(LjaDetails.ljaDetails()
                        .withLjaCode("2800")
                        .withLjaName(ljaName)
                        .build()));

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("GUILTY")
                        .build())
                .withConvictingCourtCode(convictingCourtCode)
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Guilty")
                                .withCategoryType("GUILTY")
                                .build())
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setCustodyTimelineDefendant(LocalDate.now());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertThat(offence.getConvictionDate(), is(pleaDate.toString()));
        assertThat(offence.getConvictingCourt().getId(), is(courtCenterId));
        assertThat(offence.getConvictingCourt().getLja().getLjaCode(), is("2800"));
        assertThat(offence.getConvictingCourt().getLja().getLjaName(), is(ljaName));
        assertNull(offence.getCustodyTimeLimit());
    }

    @Test
    public void shouldNotSetCustodyLimitWhenPleaNotGuiltyVerdictGuilty() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();
        final LocalDate pleaDate = LocalDate.now().minusDays(1);
        final LocalDate verdictDate = LocalDate.now().minusDays(1);
        final String offenceCode = "TVL-ABC";
        final UUID guiltyVerdictId = randomUUID();

        final List<Offence> offences = ImmutableList.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .withPlea(Plea.plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue("NOT_GUILTY")
                        .build())
                .withVerdict(Verdict.verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(VerdictType.verdictType()
                                .withId(guiltyVerdictId)
                                .withCategory("Guilty")
                                .withCategoryType("GUILTY")
                                .build())
                        .build())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setCustodyTimelineDefendant(LocalDate.now());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNull(offence.getCustodyTimeLimit());
    }

    @Test
    public void shouldNotSetCustodyLimitWhenPleaAndverdictNull() {
        final ReferenceDataVO referenceDataVO = buildReferenceDataWithOffenceAndModeOfTrial(EITHER_WAY);
        final UUID offenceId = randomUUID();

        final String offenceCode = "TVL-ABC";

        final List<Offence> offences = List.of(offence()
                .withOffenceId(offenceId)
                .withOffenceCode(offenceCode)
                .withOffenceCommittedDate(LocalDate.now())
                .build());

        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setReferenceDataVO(referenceDataVO);
        paramsVO.setChannel(MCC);
        paramsVO.setInitiationCode(InitiationCode.O.name());
        paramsVO.setCustodyTimelineDefendant(LocalDate.now());

        final List<uk.gov.justice.core.courts.Offence> coreOffences = converter.convert(offences, paramsVO);

        final uk.gov.justice.core.courts.Offence offence = coreOffences.get(0);
        assertThat(offence.getId(), is(offenceId));
        assertNotNull(offence.getCustodyTimeLimit());
    }

}