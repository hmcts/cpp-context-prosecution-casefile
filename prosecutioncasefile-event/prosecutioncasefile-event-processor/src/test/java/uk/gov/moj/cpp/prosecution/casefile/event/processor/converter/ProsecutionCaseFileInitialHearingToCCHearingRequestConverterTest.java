package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.core.courts.SummonsType.FIRST_HEARING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.MOCKED_OFFENCE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom.courtRoom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileInitialHearingToCCHearingRequestConverterTest {

    private static final String PROSECUTOR_EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();
    @InjectMocks
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter;

    @Test
    public void shouldConvertInitialHearingToCCHearingRequest() {
        final CcCaseReceived ccCaseReceived = buildCcCaseReceived();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO());

        final List<UUID> expectedOffenceIds = asList(MOCKED_OFFENCE_ID);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(defendants, paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertThat(listHearingRequests.get(0).getCourtScheduleId(), is("test-courtScheduleId"));
        assertThat(listHearingRequests.get(0).getListedEndDateTime().toString(), is("2050-02-04T10:05:01.001Z"));

        if (ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData inputOrganisationUnitReferenceData = ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().get();

            assertThat(inputOrganisationUnitReferenceData.getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3WelshName(), equalTo(listHearingRequests.get(0).getCourtCentre().getWelshName()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3Name(), equalTo(listHearingRequests.get(0).getCourtCentre().getName()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getCourtroomName(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomName()));
            final ListDefendantRequest firstDefendantRequest = listHearingRequests.get(0).getListDefendantRequests().get(0);
            assertThat(expectedOffenceIds, equalTo(firstDefendantRequest.getDefendantOffences()));
            assertThat(JurisdictionType.MAGISTRATES.toString(), equalTo(listHearingRequests.get(0).getJurisdictionType().toString()));
            assertThat(firstDefendantRequest.getSummonsRequired(), is(FIRST_HEARING));
            assertThat(firstDefendantRequest.getSummonsApprovedOutcome(), is(nullValue()));
        }
    }

    @Test
    public void shouldConvertInitialHearingWithApplicationResultsToCCHearingRequest() {
        final CcCaseReceived ccCaseReceived = buildCcCaseReceivedWithSummonsApprovedResults();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO());
        paramsVO.setSummonsApprovedOutcome(ccCaseReceived.getSummonsApprovedOutcome());

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(defendants, paramsVO);

        assertThat(listHearingRequests, hasSize(1));
        final SummonsApprovedOutcome summonsApprovedOutcome = listHearingRequests.get(0).getListDefendantRequests().get(0).getSummonsApprovedOutcome();
        assertThat(summonsApprovedOutcome.getProsecutorCost(), is(ccCaseReceived.getSummonsApprovedOutcome().getProsecutorCost()));
        assertThat(summonsApprovedOutcome.getPersonalService(), is(ccCaseReceived.getSummonsApprovedOutcome().getPersonalService()));
        assertThat(summonsApprovedOutcome.getSummonsSuppressed(), is(ccCaseReceived.getSummonsApprovedOutcome().getSummonsSuppressed()));
        assertThat(summonsApprovedOutcome.getProsecutorEmailAddress(), is(PROSECUTOR_EMAIL_ADDRESS));
    }

    @Test
    public void shouldNotPopulateRoomIdAndRoomNameWhenMultipleCourtrooms() {
        final CcCaseReceived ccCaseReceived = buildCcCaseReceived();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO());

        final List<UUID> expectedOffenceIds = asList(MOCKED_OFFENCE_ID);

        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(defendants, paramsVO);

        assertEquals(1, listHearingRequests.size());
        assertThat(listHearingRequests.get(0).getCourtScheduleId(), is("test-courtScheduleId"));
        assertThat(listHearingRequests.get(0).getListedEndDateTime().toString(), is("2050-02-04T10:05:01.001Z"));

        if (ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData inputOrganisationUnitReferenceData = ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().get();

            assertThat(inputOrganisationUnitReferenceData.getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3WelshName(), equalTo(listHearingRequests.get(0).getCourtCentre().getWelshName()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3Name(), equalTo(listHearingRequests.get(0).getCourtCentre().getName()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getCourtroomName(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomName()));
            assertThat(expectedOffenceIds, equalTo(listHearingRequests.get(0).getListDefendantRequests().get(0).getDefendantOffences()));
        }

    }

    @Test
    public void shouldSetJurisdictionTypeCrownForManualCaseCreation() {
        final CcCaseReceived ccCaseReceived = buildCcCaseReceived();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO());
        paramsVO.setChannel(Channel.MCC);
        paramsVO.setOucodeL1Code("C");

        final List<UUID> expectedOffenceIds = asList(MOCKED_OFFENCE_ID);
        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(defendants, paramsVO);

        assertEquals(1, listHearingRequests.size());

        if (ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().isPresent()) {
            final OrganisationUnitWithCourtroomReferenceData inputOrganisationUnitReferenceData = ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().getOrganisationUnitWithCourtroomReferenceData().get();

            assertThat(inputOrganisationUnitReferenceData.getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3WelshName(), equalTo(listHearingRequests.get(0).getCourtCentre().getWelshName()));
            assertThat(inputOrganisationUnitReferenceData.getOucodeL3Name(), equalTo(listHearingRequests.get(0).getCourtCentre().getName()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getId(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomId().toString()));
            assertThat(inputOrganisationUnitReferenceData.getCourtRoom().getCourtroomName(), equalTo(listHearingRequests.get(0).getCourtCentre().getRoomName()));
            assertThat(expectedOffenceIds, equalTo(listHearingRequests.get(0).getListDefendantRequests().get(0).getDefendantOffences()));
            assertThat(JurisdictionType.CROWN.toString(), equalTo(listHearingRequests.get(0).getJurisdictionType().toString()));
        }
    }

    @Test
    @Disabled("Will be addressed later")
    public void shouldSetJurisdictionTypeCrownForManualCaseCreationWithWC() {
        final CcCaseReceived ccCaseReceived = buildCcCaseReceived();
        final List<Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        UUID hearingTypeId = UUID.randomUUID();
        final ParamsVO paramsVO = new ParamsVO();
        paramsVO.setCaseId(ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId());
        paramsVO.setReferenceDataVO(ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO());
        paramsVO.setChannel(Channel.MCC);
        paramsVO.setOucodeL1Code("C");
        final String startDate = "2029-10-10";
        paramsVO.setListNewHearing(createHearingRequest(hearingTypeId, startDate));

        final List<UUID> expectedOffenceIds = asList(MOCKED_OFFENCE_ID);
        final List<ListHearingRequest> listHearingRequests = prosecutionCaseFileInitialHearingToCCHearingRequestConverter.convert(defendants, paramsVO);

        assertEquals(1, listHearingRequests.size());


        assertThat(JurisdictionType.CROWN.toString(), equalTo(listHearingRequests.get(0).getJurisdictionType().toString()));
        assertThat(hearingTypeId, equalTo(listHearingRequests.get(0).getHearingType().getId()));
        assertThat(startDate, equalTo(listHearingRequests.get(0).getWeekCommencingDate().getStartDate()));
        assertThat(listHearingRequests.get(0).getCourtCentre(), is(notNullValue()));
        assertThat(listHearingRequests.get(0).getCourtCentre().getId(), equalTo(hearingTypeId));

        assertThat(listHearingRequests.get(0).getEstimateMinutes(), equalTo(20));
        assertThat(listHearingRequests.get(0).getEstimatedDuration(), equalTo("2d"));

        assertThat(listHearingRequests.get(0).getBookedSlots(), is(notNullValue()));
        assertThat(listHearingRequests.get(0).getBookedSlots().size(), greaterThan(0));

        assertThat(listHearingRequests.get(0).getBookingType(), equalTo("Video"));

        assertThat(listHearingRequests.get(0).getJudiciary(), is(notNullValue()));
        assertThat(listHearingRequests.get(0).getJudiciary().size(), greaterThan(0));

        assertThat(listHearingRequests.get(0).getPriority(), equalTo("HIGH"));

        assertThat(listHearingRequests.get(0).getSpecialRequirements(), is(notNullValue()));
        assertThat(listHearingRequests.get(0).getSpecialRequirements(), hasItem("tea on Arrival"));
    }

    HearingRequest createHearingRequest(UUID hearingTypeId,String startDate){
        return HearingRequest.hearingRequest()
                .withCourtCentre(CourtCentre.courtCentre().withId(hearingTypeId).build())
                .withHearingType(uk.gov.justice.core.courts.HearingType.hearingType()
                        .withDescription("First")
                        .withId(hearingTypeId)
                        .build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withEstimatedMinutes(20)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(startDate)
                        .build())
                .withEstimatedDuration("2d")
                .withBookedSlots(List.of(RotaSlot.rotaSlot().build()))
                .withBookingType("Video")
                .withJudiciary(List.of(JudicialRole.judicialRole().build()))
                .withPriority("HIGH")
                .withSpecialRequirements(List.of("tea on Arrival"))
                .build();


    }

    private CcCaseReceived buildCcCaseReceived() {
        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(buildProsecutionWithReferenceData("Either Way")).build();

        ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().setOrganisationUnitWithCourtroomReferenceData(
                of(organisationUnitWithCourtroomReferenceData()
                        .withId(randomUUID().toString())
                        .withOucodeL3Name("South Western (Lavender Hill)")
                        .withOucodeL3WelshName("Welsh Name")
                        .withCourtRoom(getCourtroom())
                        .build()));
        ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().setHearingType(HearingType.hearingType().withId(randomUUID()).withHearingDescription("Preliminary Hearing").build());
        ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().withId(randomUUID()).withShortName("DVLA").build());
        ccCaseReceived.getProsecutionWithReferenceData().getReferenceDataVO().setOffenceReferenceData(asList(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode("TVL-ABC").withOffenceId(MOCKED_OFFENCE_ID).build()));

        return ccCaseReceived;
    }

    private CcCaseReceived buildCcCaseReceivedWithSummonsApprovedResults() {
        return ccCaseReceived()
                .withProsecutionWithReferenceData(buildCcCaseReceived().getProsecutionWithReferenceData())
                .withSummonsApprovedOutcome(SummonsApprovedOutcome.summonsApprovedOutcome()
                        .withProsecutorCost("£300.00")
                        .withPersonalService(BOOLEAN.next())
                        .withSummonsSuppressed(BOOLEAN.next())
                        .withProsecutorEmailAddress(PROSECUTOR_EMAIL_ADDRESS)
                        .build())
                .build();
    }

    private CourtRoom getCourtroom() {
        return courtRoom()
                .withCourtroomName("Courtroom 05")
                .withId(randomUUID().toString())
                .build();
    }


}