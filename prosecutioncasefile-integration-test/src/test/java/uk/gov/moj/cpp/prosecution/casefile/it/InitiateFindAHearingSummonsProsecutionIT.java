package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper.FIND_A_HEARING_BOOKED_SLOT_OU_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper.FIND_A_HEARING_EARLIEST_START;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeForGroupCases;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;

import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;

import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The magistrates' "find a hearing" journey for Summons cases: the case is created and parked for
 * legal-adviser approval, the box hearing is raised at the found hearing's court, and on approval
 * the case is listed at the slot the user picked.
 *
 * <p>Covers DD-43173 AC-001, AC-002, AC-003, AC-007, AC-008.
 */
@SuppressWarnings("java:S2699")
@ExtendWith(MockitoExtension.class)
class InitiateFindAHearingSummonsProsecutionIT extends BaseIT {

    private static final String COMMAND_PAYLOAD_FOR_MCC_FIND_A_HEARING_SUMMONS =
            "command-json/prosecutioncasefile.command.initiate-mcc-find-a-hearing-summons-prosecution.json";

    @BeforeAll
    static void setup() {
        stubOffencesForOffenceCodeForGroupCases();
        stubGetOrganisationUnits();
    }

    /**
     * DD-43173 AC-001: the command is accepted and the application is parked instead of the case being
     * created outright — no NullPointerException on the box-hearing conversion.
     * <p>
     * DD-43173 AC-002 / AC-003: the box hearing carries the reference-data-enriched court centre for the
     * booked slot's OU code — postal address and courtHearingLocation included, not the thin court
     * centre the UI supplies — and an application due date of the found hearing minus 14 days.
     */
    @Test
    void shouldParkAnMccFindAHearingSummonsAndRaiseTheBoxHearingAtTheFoundHearingCourt() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndCaptureApplication(MCC, COMMAND_PAYLOAD_FOR_MCC_FIND_A_HEARING_SUMMONS);

        helper.awaitSummonsApplicationInitiated();

        final JsonObject boxHearing = helper.getLastSummonsApplicationPayload().getJsonObject("boxHearing");
        assertThat(boxHearing, notNullValue());
        assertThat(boxHearing.getString("applicationDueDate"),
                is(ZonedDateTime.parse(FIND_A_HEARING_EARLIEST_START).minusDays(14).toLocalDate().toString()));

        final JsonObject courtCentre = boxHearing.getJsonObject("courtCentre");
        assertThat(courtCentre.getString("courtHearingLocation"), is(FIND_A_HEARING_BOOKED_SLOT_OU_CODE));
        assertThat(courtCentre.getString("code"), is(FIND_A_HEARING_BOOKED_SLOT_OU_CODE));
        assertThat(courtCentre.getJsonObject("address"), notNullValue());
        assertThat(courtCentre.getJsonObject("address").getString("postcode"), notNullValue());
    }

    /**
     * DD-43173 AC-007 / AC-008: the parked application is approved by the legal adviser and the case is
     * listed at the hearing the user originally picked — same court centre (with roomId), hearing
     * type, booked slot and timing — with a listDefendantRequest for each approved defendant and
     * no others.
     */
    @Test
    void shouldListTheCaseAtTheFoundHearingOnceTheLegalAdviserApproves() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.initiateSummonsCaseForChannelAndCaptureApplication(MCC, COMMAND_PAYLOAD_FOR_MCC_FIND_A_HEARING_SUMMONS);
        helper.awaitSummonsApplicationInitiated();

        helper.whenSummonsApplicationIsApprovedForDefendants();

        helper.thenEventsShouldBeRaised(new String[]{EVENT_SELECTOR_CC_PROSECUTION_RECEIVED});
        helper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED});

        helper.awaitCourtProceedingsInitiated();

        final JsonObject courtReferral = helper.getLastCourtProceedingsPayload().getJsonObject("initiateCourtProceedings");
        assertThat(courtReferral.getJsonArray("listHearingRequests"), hasSize(1));

        final JsonObject listHearingRequest = courtReferral.getJsonArray("listHearingRequests").getJsonObject(0);
        assertThat(listHearingRequest.getJsonObject("courtCentre").getString("id"), is("89592405-c29b-3706-b1d3-b1dd3a08b227"));
        assertThat(listHearingRequest.getJsonObject("courtCentre").getString("roomId"), is("60853c27-8a9d-349a-aeb5-7f5049a774dd"));
        assertThat(listHearingRequest.getJsonObject("hearingType").getString("id"), is("bf8155e1-90b9-4080-b133-bfbad895d6e4"));
        assertThat(listHearingRequest.getJsonArray("bookedSlots"), hasSize(1));
        assertThat(listHearingRequest.getJsonArray("bookedSlots").getJsonObject(0).getString("oucode"), is(FIND_A_HEARING_BOOKED_SLOT_OU_CODE));
        assertThat(listHearingRequest.getInt("estimateMinutes"), is(60));

        final List<String> listedDefendantIds = listHearingRequest.getJsonArray("listDefendantRequests").stream()
                .map(value -> ((JsonObject) value).getString("defendantId"))
                .collect(toList());

        assertThat(listedDefendantIds, hasSize(helper.getDefendantIds().size()));
        assertThat(listedDefendantIds, containsInAnyOrder(helper.getDefendantIds().toArray(new String[0])));
    }
}
