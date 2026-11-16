package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.RotaSlot.rotaSlot;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded.prosecutionDefendantsAdded;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.getReferenceDataVO;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest.hearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ListDefendantRequest.listDefendantRequest;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cover for the add-defendant route on a magistrates' "find a hearing" summons case (FR-6 /
 * AC-009), and for the unchanged output of an existing non-find-a-hearing add-defendant case
 * (AC-020). The hearing converter is the real one, since the point of these tests is what the
 * ParamsVO makes it do.
 * See DD-43173
 */
@ExtendWith(MockitoExtension.class)
class ProsecutionToCCAddDefendantConverterFindAHearingTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = randomUUID();
    private static final UUID NEW_DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String BOOKED_SLOT_OU_CODE = "B10LY00";
    private static final ZonedDateTime EARLIEST_START = ZonedDateTime.parse("2050-10-03T09:00:00Z");

    @Mock
    private ProsecutionCaseFileDefendantToCCDefendantConverter prosecutionCaseFileDefendantToCCDefendantConverter;

    @Spy
    private ProsecutionCaseFileInitialHearingToCCHearingRequestConverter prosecutionCaseFileInitialHearingToCCHearingRequestConverter =
            new ProsecutionCaseFileInitialHearingToCCHearingRequestConverter();

    @InjectMocks
    private ProsecutionToCCAddDefendantConverter converter;

    /**
     * DD-43173 AC-009: the added defendant is listed at the case's found hearing, with a listDefendantRequest
     * for that defendant only, and the court centre and slot the user picked intact.
     */
    @Test
    void shouldListALaterAddedDefendantAtTheCaseFoundHearing() {
        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(emptyList());

        final ProsecutionDefendantsAdded source = prosecutionDefendantsAdded()
                .withCaseId(CASE_ID)
                .withChannel(MCC)
                .withDefendants(singletonList(findAHearingDefendant()))
                .withReferenceDataVO(getReferenceDataVO())
                .withListNewHearing(foundHearing())
                .build();

        final AddDefendantsToCourtProceedings result = converter.convert(source);

        assertThat(result.getListHearingRequests(), hasSize(1));
        final ListHearingRequest listHearingRequest = result.getListHearingRequests().get(0);
        assertThat(listHearingRequest.getCourtCentre(), notNullValue());
        assertThat(listHearingRequest.getCourtCentre().getId(), is(COURT_CENTRE_ID));
        assertThat(listHearingRequest.getCourtCentre().getRoomId(), is(COURT_ROOM_ID));
        assertThat(listHearingRequest.getEarliestStartDateTime(), is(EARLIEST_START));
        assertThat(listHearingRequest.getBookedSlots(), hasSize(1));
        assertThat(listHearingRequest.getBookedSlots().get(0).getOucode(), is(BOOKED_SLOT_OU_CODE));
        assertThat(defendantIdsOn(listHearingRequest), contains(NEW_DEFENDANT_ID));
    }

    /**
     * DD-43173 AC-020: an existing MCC add-defendant case with no found hearing keeps producing exactly the
     * standard listing request. Setting channel on the ParamsVO must stay inert: initiationCode and
     * oucodeL1Code are deliberately left unset, so the jurisdiction derivation still yields
     * MAGISTRATES and the MCC offence-conversion branches stay dormant.
     */
    @Test
    void shouldLeaveAnExistingAddDefendantCaseUnchangedWhenThereIsNoFoundHearing() {
        when(prosecutionCaseFileDefendantToCCDefendantConverter.convert(isA(List.class), isA(ParamsVO.class))).thenReturn(emptyList());

        final ProsecutionDefendantsAdded source = prosecutionDefendantsAdded()
                .withCaseId(CASE_ID)
                .withChannel(MCC)
                .withDefendants(singletonList(initialHearingDefendant()))
                .withReferenceDataVO(getReferenceDataVO())
                .build();

        final AddDefendantsToCourtProceedings result = converter.convert(source);

        assertThat(result.getListHearingRequests(), hasSize(1));
        final ListHearingRequest listHearingRequest = result.getListHearingRequests().get(0);
        assertThat(listHearingRequest.getJurisdictionType(), is(JurisdictionType.MAGISTRATES));
        assertThat(listHearingRequest.getBookedSlots(), nullValue());
        assertThat(defendantIdsOn(listHearingRequest), contains(NEW_DEFENDANT_ID));
    }

    private List<UUID> defendantIdsOn(final ListHearingRequest listHearingRequest) {
        return listHearingRequest.getListDefendantRequests().stream()
                .map(ListDefendantRequest::getDefendantId)
                .collect(toList());
    }

    private HearingRequest foundHearing() {
        return hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withEarliestStartDateTime(EARLIEST_START)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withName("Lyminge Magistrates' Court")
                        .withRoomId(COURT_ROOM_ID)
                        .withRoomName("Courtroom 01")
                        .build())
                .withBookedSlots(singletonList(rotaSlot()
                        .withOucode(BOOKED_SLOT_OU_CODE)
                        .withStartTime(EARLIEST_START)
                        .build()))
                .withListDefendantRequests(singletonList(listDefendantRequest()
                        .withDefendantId(NEW_DEFENDANT_ID)
                        .withDefendantOffences(singletonList(OFFENCE_ID))
                        .build()))
                .build();
    }

    private Defendant findAHearingDefendant() {
        return defendant()
                .withId(NEW_DEFENDANT_ID.toString())
                .withOffences(singletonList(offence().withOffenceId(OFFENCE_ID).build()))
                .build();
    }

    private Defendant initialHearingDefendant() {
        return defendant()
                .withId(NEW_DEFENDANT_ID.toString())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing("2050-10-03")
                        .withTimeOfHearing("09:05:01")
                        .withCourtHearingLocation("B016771")
                        .build())
                .withOffences(singletonList(offence().withOffenceId(OFFENCE_ID).build()))
                .build();
    }
}
