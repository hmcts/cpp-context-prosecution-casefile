package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom.courtRoom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution.groupProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData.prosecutorsReferenceData;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.CreateGroupCaseRequest;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.ResultingIntent;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * All values are example data only.
 */
class GroupCasesReceivedToCreateGroupCaseRequestConverterTest {

    private static final String OFFENCE_CODE = "CT96001";
    private static final String OFFENCE_TITLE = "Example offence title";
    private static final String COURT_OU_CODE = "B01LY00";
    private static final String PROSECUTOR_OU_CODE = "EXAMPLE01";
    private static final String PROSECUTOR_FULL_NAME = "Example Borough Council";
    private static final String DATE_OF_HEARING = "2050-02-04";

    private final GroupCasesReceivedToCreateGroupCaseRequestConverter converter = new GroupCasesReceivedToCreateGroupCaseRequestConverter();

    private final UUID groupId = randomUUID();
    private final UUID leadCaseId = randomUUID();
    private final UUID courtCentreId = randomUUID();
    private final UUID refDataCourtroomId = randomUUID();

    @Nested
    @DisplayName("lead case mapping")
    class LeadCaseMapping {

        @Test
        void convert_individualLeadDefendant_should_mapEveryField() {
            final GroupCasesReceived event = event(
                    leadCase(individualDefendant(initialHearingWithoutRoom())),
                    memberCase(),
                    memberCase());

            final CreateGroupCaseRequest request = converter.convert(event).orElseThrow();

            assertThat(request.getGroupId(), is(groupId));
            assertThat(request.getLeadCase().getLeadCaseId(), is(leadCaseId));
            assertThat(request.getLeadCase().getCaseUrn(), is("EXAMPLEURN1"));
            assertThat(request.getLeadCase().getDefendantName(), is("Alex Example"));
            assertThat(request.getLeadCase().getDefendantDoB(), is(nullValue()));
            assertThat(request.getDateOfHearing(), is(LocalDate.of(2050, 2, 4)));
            assertThat(request.getCourtHearingLocation(), is(COURT_OU_CODE));
            assertThat(request.getCourtCentreId(), is(courtCentreId));
            assertThat(request.getCourtroomId(), is(refDataCourtroomId));
            assertThat(request.getProsecutingAuthority(), is(PROSECUTOR_OU_CODE));
            assertThat(request.getProsecutingAuthorityName(), is(PROSECUTOR_FULL_NAME));
            assertThat(request.getOffenceType(), is(OFFENCE_TITLE));
            assertThat(request.getCaseCount(), is(3));
            assertThat(request.getResultingIntent(), is(ResultingIntent.GRANT));
            assertThat(request.getParentGroupId(), is(nullValue()));
        }

        @Test
        void convert_organisationLeadDefendant_should_useOrganisationName() {
            final Defendant organisation = defendant()
                    .withOrganisationName("Example Trading Ltd")
                    .withInitialHearing(initialHearingWithoutRoom())
                    .withOffences(singletonList(offence().withOffenceCode(OFFENCE_CODE).build()))
                    .build();

            final CreateGroupCaseRequest request = converter.convert(event(leadCase(organisation))).orElseThrow();

            assertThat(request.getLeadCase().getDefendantName(), is("Example Trading Ltd"));
        }

        @Test
        void convert_leadCaseNotFirstInList_should_useTheGroupMaster() {
            final GroupCasesReceived event = event(memberCase(), leadCase(individualDefendant(initialHearingWithoutRoom())));

            final CreateGroupCaseRequest request = converter.convert(event).orElseThrow();

            assertThat(request.getLeadCase().getLeadCaseId(), is(leadCaseId));
            assertThat(request.getCaseCount(), is(2));
        }

        @Test
        void convert_noGroupMaster_should_returnEmpty() {
            final Optional<CreateGroupCaseRequest> request = converter.convert(event(memberCase(), memberCase()));

            assertThat(request.isPresent(), is(false));
        }
    }

    @Nested
    @DisplayName("courtroom")
    class Courtroom {

        @Test
        void convert_initialHearingHasRoomId_should_preferItOverReferenceData() {
            final UUID submittedRoomId = randomUUID();
            final InitialHearing hearing = initialHearing()
                    .withCourtHearingLocation(COURT_OU_CODE)
                    .withDateOfHearing(DATE_OF_HEARING)
                    .withRoomId(submittedRoomId.toString())
                    .build();

            final CreateGroupCaseRequest request = converter.convert(event(leadCase(individualDefendant(hearing)))).orElseThrow();

            assertThat(request.getCourtroomId(), is(submittedRoomId));
        }

        @Test
        void convert_noRoomIdAndNoReferenceDataCourtroom_should_leaveCourtroomOut() {
            final GroupProsecutionWithReferenceData lead = leadCase(individualDefendant(initialHearingWithoutRoom()));
            lead.getReferenceDataVO().setOrganisationUnitWithCourtroomReferenceData(Optional.of(courtCentre(null)));

            final CreateGroupCaseRequest request = converter.convert(event(lead)).orElseThrow();

            assertThat(request.getCourtroomId(), is(nullValue()));
            assertThat(request.getCourtCentreId(), is(courtCentreId));
        }
    }

    @Nested
    @DisplayName("offence type")
    class OffenceType {

        @Test
        void convert_noReferenceDataForOffenceCode_should_fallBackToTheCode() {
            final GroupProsecutionWithReferenceData lead = leadCase(individualDefendant(initialHearingWithoutRoom()));
            lead.getReferenceDataVO().setOffenceReferenceData(singletonList(
                    offenceReferenceData().withCjsOffenceCode("OTHER01").withTitle("Another offence").build()));

            final CreateGroupCaseRequest request = converter.convert(event(lead)).orElseThrow();

            assertThat(request.getOffenceType(), is(OFFENCE_CODE));
        }

        @Test
        void convert_offenceTitleLongerThan250_should_truncateTo250() {
            final String longTitle = "x".repeat(300);
            final GroupProsecutionWithReferenceData lead = leadCase(individualDefendant(initialHearingWithoutRoom()));
            lead.getReferenceDataVO().setOffenceReferenceData(singletonList(
                    offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withTitle(longTitle).build()));

            final CreateGroupCaseRequest request = converter.convert(event(lead)).orElseThrow();

            assertThat(request.getOffenceType(), is("x".repeat(250)));
        }
    }

    @Nested
    @DisplayName("missing reference data")
    class MissingReferenceData {

        @Test
        void convert_noCourtCentreReferenceData_should_throw() {
            final GroupProsecutionWithReferenceData lead = leadCase(individualDefendant(initialHearingWithoutRoom()));
            lead.getReferenceDataVO().setOrganisationUnitWithCourtroomReferenceData(Optional.empty());

            final GroupCasesReceived event = event(lead);
            assertThrows(IllegalStateException.class, () -> converter.convert(event));
        }

        @Test
        void convert_noProsecutorReferenceData_should_throw() {
            final GroupProsecutionWithReferenceData lead = leadCase(individualDefendant(initialHearingWithoutRoom()));
            lead.getReferenceDataVO().setProsecutorsReferenceData(null);

            final GroupCasesReceived event = event(lead);
            assertThrows(IllegalStateException.class, () -> converter.convert(event));
        }
    }

    private GroupCasesReceived event(final GroupProsecutionWithReferenceData... cases) {
        return GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(new GroupProsecutionList(new ArrayList<>(asList(cases)), randomUUID()))
                .build();
    }

    private GroupProsecutionWithReferenceData leadCase(final Defendant leadDefendant) {
        final GroupProsecution lead = groupProsecution()
                .withGroupId(groupId)
                .withIsGroupMaster(true)
                .withIsGroupMember(true)
                .withCaseDetails(caseDetails().withCaseId(leadCaseId).withProsecutorCaseReference("EXAMPLEURN1").build())
                .withDefendants(singletonList(leadDefendant))
                .build();
        final GroupProsecutionWithReferenceData withReferenceData = new GroupProsecutionWithReferenceData(lead);
        withReferenceData.setReferenceDataVO(referenceData());
        return withReferenceData;
    }

    private GroupProsecutionWithReferenceData memberCase() {
        final GroupProsecution member = groupProsecution()
                .withGroupId(groupId)
                .withIsGroupMaster(false)
                .withIsGroupMember(true)
                .withCaseDetails(caseDetails().withCaseId(randomUUID()).withProsecutorCaseReference("EXAMPLEURN2").build())
                .withDefendants(singletonList(individualDefendant(initialHearingWithoutRoom())))
                .build();
        final GroupProsecutionWithReferenceData withReferenceData = new GroupProsecutionWithReferenceData(member);
        withReferenceData.setReferenceDataVO(referenceData());
        return withReferenceData;
    }

    private static Defendant individualDefendant(final InitialHearing initialHearing) {
        return defendant()
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation().withFirstName("Alex").withLastName("Example").build())
                        .build())
                .withInitialHearing(initialHearing)
                .withOffences(singletonList(offence().withOffenceCode(OFFENCE_CODE).build()))
                .build();
    }

    private static InitialHearing initialHearingWithoutRoom() {
        return initialHearing()
                .withCourtHearingLocation(COURT_OU_CODE)
                .withDateOfHearing(DATE_OF_HEARING)
                .build();
    }

    private ReferenceDataVO referenceData() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(Optional.of(
                courtCentre(courtRoom().withId(refDataCourtroomId.toString()).build())));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .withOucode(PROSECUTOR_OU_CODE)
                .withShortName("EXSHORT")
                .withFullName(PROSECUTOR_FULL_NAME)
                .build());
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData> offences = new ArrayList<>();
        offences.add(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withTitle(OFFENCE_TITLE).build());
        referenceDataVO.setOffenceReferenceData(offences);
        return referenceDataVO;
    }

    private OrganisationUnitWithCourtroomReferenceData courtCentre(final CourtRoom courtRoom) {
        return organisationUnitWithCourtroomReferenceData()
                .withId(courtCentreId.toString())
                .withCourtRoom(courtRoom)
                .build();
    }
}
