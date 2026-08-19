package uk.gov.moj.cps.prosecutioncasefile;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails.summonsApplicationApprovedDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest.hearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ListDefendantRequest.listDefendantRequest;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData.prosecutorsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.APPLICATION_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.APPLICATION_ID_2;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.BIRTH_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CASE_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.COURT_HEARING_LOCATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CPS_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DATE_OF_HEARING;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.EXTERNAL_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FORENAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_CODE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_COST;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_ONE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_THREE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_TWO;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SECOND_DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SUMMONS_INITIATION_CODE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.THIRD_DEFENDANT_ID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ListDefendantRequest;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Aggregate-level cover for the magistrates' "find a hearing" summons route: the found hearing
 * must survive legal-adviser approval (FR-4 / AC-007) with its listDefendantRequests restricted to
 * the approved defendants (FR-5 / AC-008), and a pre-change event stream must still replay to
 * identical state (NFR-2 / AC-019).
 * See DD-43173
 */
class ProsecutionCaseFileFindAHearingSummonsTest {

    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID HEARING_TYPE_ID = randomUUID();
    private static final String BOOKED_SLOT_OU_CODE = "B10LY00";
    private static final ZonedDateTime EARLIEST_START = ZonedDateTime.parse("2050-10-03T09:00:00Z");
    private static final ZonedDateTime SLOT_START = ZonedDateTime.parse("2050-10-03T09:00:00Z");

    private ProsecutionCaseFile prosecutionCaseFile;

    @BeforeEach
    void setup() {
        prosecutionCaseFile = new ProsecutionCaseFile();
    }

    /**
     * DD-43173 AC-007: on approval the prosecution handed to the listing converter still carries the court
     * centre the user picked (including roomId), the hearing type, the booked slots and the timing.
     */
    @Test
    void shouldCarryTheFoundHearingThroughLegalAdviserApproval() {
        final List<Defendant> applicationDefendants = of(
                findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                findAHearingDefendant(SECOND_DEFENDANT_ID, "James", PROSECUTOR_DEFENDANT_REFERENCE_TWO));
        final HearingRequest foundHearing = foundHearing(DEFENDANT_ID, SECOND_DEFENDANT_ID);

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID, findAHearingProsecution(applicationDefendants, foundHearing), emptyList()));

        final List<Object> events = approve(APPLICATION_ID).collect(toList());

        assertThat(events, hasSize(1));
        final CcCaseReceived caseReceived = (CcCaseReceived) events.get(0);
        final HearingRequest carried = caseReceived.getProsecutionWithReferenceData().getProsecution().getListNewHearing();

        assertThat(carried, notNullValue());
        assertThat(carried.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(carried.getCourtCentre().getRoomId(), is(COURT_ROOM_ID));
        assertThat(carried.getCourtCentre().getName(), is("Lyminge Magistrates' Court"));
        assertThat(carried.getHearingType().getId(), is(HEARING_TYPE_ID));
        assertThat(carried.getHearingDateTimeType(), is(FIXED));
        assertThat(carried.getEarliestStartDateTime(), is(EARLIEST_START));
        assertThat(carried.getEstimatedMinutes(), is(60));
        assertThat(carried.getBookedSlots(), hasSize(1));
        assertThat(carried.getBookedSlots().get(0).getOucode(), is(BOOKED_SLOT_OU_CODE));
        assertThat(carried.getBookedSlots().get(0).getStartTime(), is(SLOT_START));
        assertThat(defendantIdsOn(carried), contains(fromString(DEFENDANT_ID), fromString(SECOND_DEFENDANT_ID)));
    }

    /**
     * DD-43173 AC-008: three defendants on the case, one rejected before the remaining application is
     * approved — the carried listDefendantRequests must contain exactly the two approved ids.
     * Reproduces the semantics an initialHearing already has, where the requests are derived from
     * the (already filtered) approved defendants rather than carried on the wire.
     */
    @Test
    void shouldRestrictListDefendantRequestsToTheApprovedDefendants() {
        final HearingRequest foundHearingForAllThree = foundHearing(DEFENDANT_ID, SECOND_DEFENDANT_ID, THIRD_DEFENDANT_ID);
        final List<Defendant> allThree = of(
                findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                findAHearingDefendant(SECOND_DEFENDANT_ID, "James", PROSECUTOR_DEFENDANT_REFERENCE_TWO),
                findAHearingDefendant(THIRD_DEFENDANT_ID, "Bob", PROSECUTOR_DEFENDANT_REFERENCE_THREE));

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID, findAHearingProsecution(allThree, foundHearingForAllThree), emptyList()));

        // the legal adviser rejects the second defendant
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID,
                newArrayList(fromString(SECOND_DEFENDANT_ID)),
                summonsRejectedOutcome().withReasons(of("Rejection Reason")).build()));

        // the remaining two are re-submitted as their own application, still carrying the case's
        // found hearing with all three requests on the wire
        final List<Defendant> remainingTwo = of(
                findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                findAHearingDefendant(THIRD_DEFENDANT_ID, "Bob", PROSECUTOR_DEFENDANT_REFERENCE_THREE));
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID_2, findAHearingProsecution(remainingTwo, foundHearingForAllThree), emptyList()));

        final List<Object> events = approve(APPLICATION_ID_2).collect(toList());

        assertThat(events, hasSize(1));
        final CcCaseReceived caseReceived = (CcCaseReceived) events.get(0);
        final HearingRequest carried = caseReceived.getProsecutionWithReferenceData().getProsecution().getListNewHearing();

        assertThat(carried, notNullValue());
        assertThat(carried.getBookedSlots().get(0).getOucode(), is(BOOKED_SLOT_OU_CODE));
        assertThat(defendantIdsOn(carried), contains(fromString(DEFENDANT_ID), fromString(THIRD_DEFENDANT_ID)));
    }

    /**
     * DD-43173 AC-009: a defendant added after the case exists is listed at the case's found hearing, with a
     * request for that defendant only.
     */
    @Test
    void shouldCarryTheFoundHearingWhenALaterApplicationAddsADefendantToAnExistingCase() {
        final HearingRequest foundHearingForAllThree = foundHearing(DEFENDANT_ID, SECOND_DEFENDANT_ID, THIRD_DEFENDANT_ID);
        final List<Defendant> firstApplication = of(
                findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE));
        final List<Defendant> secondApplication = of(
                findAHearingDefendant(THIRD_DEFENDANT_ID, "Bob", PROSECUTOR_DEFENDANT_REFERENCE_THREE));

        final ProsecutionWithReferenceData first = findAHearingProsecution(firstApplication, foundHearingForAllThree);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, first, emptyList()));
        prosecutionCaseFile.apply(new CcCaseReceived(first, summonsApprovedOutcome().withPersonalService(true).withProsecutorCost(PROSECUTOR_COST).withSummonsSuppressed(false).build(), randomUUID()));
        prosecutionCaseFile.apply(new CaseCreatedSuccessfully(CASE_ID, MCC, EXTERNAL_ID));
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID_2, findAHearingProsecution(secondApplication, foundHearingForAllThree), emptyList()));

        final List<Object> events = approve(APPLICATION_ID_2).collect(toList());

        assertThat(events, hasSize(1));
        final ProsecutionDefendantsAdded defendantsAdded = (ProsecutionDefendantsAdded) events.get(0);
        assertThat(defendantsAdded.getDefendants(), hasSize(1));
        assertThat(defendantsAdded.getDefendants().get(0).getId(), is(THIRD_DEFENDANT_ID));

        final HearingRequest carried = defendantsAdded.getListNewHearing();
        assertThat(carried, notNullValue());
        assertThat(carried.getCourtCentre().getRoomId(), is(COURT_ROOM_ID));
        assertThat(carried.getBookedSlots().get(0).getOucode(), is(BOOKED_SLOT_OU_CODE));
        assertThat(defendantIdsOn(carried), contains(fromString(THIRD_DEFENDANT_ID)));
    }

    /**
     * DD-43173 AC-021 / NFR-4: when none of the defendants being approved are on the found hearing, no
     * hearing is carried at all.
     * <p>
     * Reachable when a later application is parked on a case that already carries a found hearing:
     * the aggregate still holds the first application's hearing and it describes nobody in the
     * second. Carrying it with an empty listDefendantRequests would breach listHearingRequest.json
     * (minItems: 1) and the outbound Progression command would be rejected, rolling the event back
     * into a redelivery loop.
     */
    @Test
    void shouldNotCarryTheFoundHearingWhenNoneOfTheApprovedDefendantsAreOnIt() {
        final List<Defendant> applicationDefendants = of(
                findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE));
        // a hearing whose requests belong entirely to another application
        final HearingRequest foundHearing = foundHearing(SECOND_DEFENDANT_ID);

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID, findAHearingProsecution(applicationDefendants, foundHearing), emptyList()));

        final List<Object> events = approve(APPLICATION_ID).collect(toList());

        final CcCaseReceived caseReceived = (CcCaseReceived) events.get(0);

        assertThat(caseReceived.getProsecutionWithReferenceData().getProsecution().getListNewHearing(), nullValue());
    }

    /**
     * DD-43173 AC-019 / NFR-2: an event stream predating this change — a parked application whose prosecution
     * carries per-defendant initial hearings and no listNewHearing — replays to identical state, and
     * approval still emits a prosecution with a null listNewHearing.
     */
    @Test
    void shouldReplayAPreChangeEventStreamToIdenticalStateWithNoFoundHearing() {
        final List<Defendant> applicationDefendants = of(
                initialHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                initialHearingDefendant(SECOND_DEFENDANT_ID, "James", PROSECUTOR_DEFENDANT_REFERENCE_TWO));

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(
                APPLICATION_ID, findAHearingProsecution(applicationDefendants, null), emptyList()));

        final List<Object> events = approve(APPLICATION_ID).collect(toList());

        assertThat(events, hasSize(1));
        final CcCaseReceived caseReceived = (CcCaseReceived) events.get(0);
        assertThat(caseReceived.getProsecutionWithReferenceData().getProsecution().getListNewHearing(), nullValue());
        assertThat(caseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(2));
        assertThat(caseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getInitialHearing().getCourtHearingLocation(), is(COURT_HEARING_LOCATION));
    }

    /**
     * A later application parked without a found hearing must not erase the case's found hearing.
     */
    @Test
    void shouldNotEraseTheCaseFoundHearingWhenALaterApplicationCarriesNone() {
        final HearingRequest foundHearing = foundHearing(DEFENDANT_ID, SECOND_DEFENDANT_ID);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID,
                findAHearingProsecution(of(findAHearingDefendant(DEFENDANT_ID, FORENAME, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), foundHearing), emptyList()));
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID_2,
                findAHearingProsecution(of(findAHearingDefendant(SECOND_DEFENDANT_ID, "James", PROSECUTOR_DEFENDANT_REFERENCE_TWO)), null), emptyList()));

        final List<Object> events = approve(APPLICATION_ID_2).collect(toList());

        final CcCaseReceived caseReceived = (CcCaseReceived) events.get(0);
        final HearingRequest carried = caseReceived.getProsecutionWithReferenceData().getProsecution().getListNewHearing();
        assertThat(carried, notNullValue());
        assertThat(defendantIdsOn(carried), contains(fromString(SECOND_DEFENDANT_ID)));
    }

    private Stream<Object> approve(final UUID applicationId) {
        final SummonsApplicationApprovedDetails approvedDetails = summonsApplicationApprovedDetails()
                .withApplicationId(applicationId)
                .withCaseId(CASE_ID)
                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                        .withSummonsSuppressed(false)
                        .withPersonalService(true)
                        .withProsecutorCost(PROSECUTOR_COST)
                        .build())
                .build();
        return prosecutionCaseFile.approveCaseDefendants(approvedDetails, of(), of(), false);
    }

    private List<UUID> defendantIdsOn(final HearingRequest hearingRequest) {
        return hearingRequest.getListDefendantRequests().stream()
                .map(ListDefendantRequest::getDefendantId)
                .collect(toList());
    }

    private HearingRequest foundHearing(final String... defendantIds) {
        final List<ListDefendantRequest> listDefendantRequests = Stream.of(defendantIds)
                .map(defendantId -> listDefendantRequest()
                        .withDefendantId(fromString(defendantId))
                        .withDefendantOffences(singletonList(TestConstants.OFFENCE_ID))
                        .build())
                .collect(toList());

        return hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withEarliestStartDateTime(EARLIEST_START)
                .withEstimatedMinutes(60)
                .withHearingType(HearingType.hearingType()
                        .withId(HEARING_TYPE_ID)
                        .withDescription("First hearing")
                        .build())
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withName("Lyminge Magistrates' Court")
                        .withRoomId(COURT_ROOM_ID)
                        .withRoomName("Courtroom 01")
                        .build())
                .withBookedSlots(singletonList(RotaSlot.rotaSlot()
                        .withOucode(BOOKED_SLOT_OU_CODE)
                        .withStartTime(SLOT_START)
                        .build()))
                .withListDefendantRequests(listDefendantRequests)
                .build();
    }

    private ProsecutionWithReferenceData findAHearingProsecution(final List<Defendant> defendants, final HearingRequest listNewHearing) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData().withId(randomUUID()).build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(SUMMONS_INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .build())
                .withChannel(MCC)
                .withListNewHearing(listNewHearing)
                .withDefendants(defendants)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private Defendant findAHearingDefendant(final String defendantId, final String firstName, final String prosecutorDefendantReference) {
        return baseDefendant(defendantId, firstName, prosecutorDefendantReference).build();
    }

    private Defendant initialHearingDefendant(final String defendantId, final String firstName, final String prosecutorDefendantReference) {
        return baseDefendant(defendantId, firstName, prosecutorDefendantReference)
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    private Defendant.Builder baseDefendant(final String defendantId, final String firstName, final String prosecutorDefendantReference) {
        return defendant()
                .withId(defendantId)
                .withInitiationCode(SUMMONS_INITIATION_CODE)
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName("Bloggs").build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(BIRTH_DATE)
                                .build())
                        .build())
                .withOffences(singletonList(offence()
                        .withOffenceId(TestConstants.OFFENCE_ID)
                        .withOffenceSequenceNumber(1)
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceCommittedDate(TestConstants.OFFENCE_COMMITTED_DATE)
                        .withOffenceDateCode(2)
                        .build()));
    }
}
