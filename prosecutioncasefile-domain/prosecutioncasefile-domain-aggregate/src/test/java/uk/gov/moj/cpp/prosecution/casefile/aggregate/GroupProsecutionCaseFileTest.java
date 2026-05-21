package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_CIVIL_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_FOR_SUMMONS;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class GroupProsecutionCaseFileTest {

    private static final String OFFENCE_CODE = "998A";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private GroupProsecutionCaseFile groupProsecutionCaseFile;

    private UUID groupId = randomUUID();

    @Test
    public void shouldRaiseGroupCasesParkedForApproval() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);
        List<SummonsCodeReferenceData> summonsCodeReferenceDataList = new ArrayList<SummonsCodeReferenceData>();
        summonsCodeReferenceDataList.add(SummonsCodeReferenceData.summonsCodeReferenceData().withSummonsCode("S02").build());
        when(referenceDataQueryService.retrieveSummonsCodes()).thenReturn(summonsCodeReferenceDataList);
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("S"));
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData1 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), true, "URN1");
        groupProsecutionWithReferenceData1.setReferenceDataVO(referenceDataVO);
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData2 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), false, "URN2");
        groupProsecutionWithReferenceData2.setReferenceDataVO(referenceDataVO);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData1);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData2);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        assertThat(eventStream.findFirst().get(), is(instanceOf(GroupCasesParkedForApproval.class)));

    }

    @Test
    public void shouldRaiseGroupCasesParkedForRejected() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("S"));
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData1 = buildGroupProsecutionWithReferenceDataWithouSummonCode(INITIATION_CODE_FOR_SUMMONS, true, "URN1");
        groupProsecutionWithReferenceData1.setReferenceDataVO(referenceDataVO);
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData2 = buildGroupProsecutionWithReferenceDataWithouSummonCode(INITIATION_CODE_FOR_SUMMONS, false, "URN2");
        groupProsecutionWithReferenceData2.setReferenceDataVO(referenceDataVO);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData1);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData2);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        assertThat(eventStream.findFirst().get(), is(instanceOf(GroupProsecutionRejected.class)));

    }

    @Test
    public void shouldRaiseGroupCasesReceived() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("O"));
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData1 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, randomUUID(), true, "URN1");
        groupProsecutionWithReferenceData1.setReferenceDataVO(referenceDataVO);
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData2 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, randomUUID(), false, "URN2");
        groupProsecutionWithReferenceData2.setReferenceDataVO(referenceDataVO);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData1);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData2);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);
        groupProsecutionList.setChannel(Channel.CIVIL);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        Object object = eventStream.findFirst().get();
        assertThat(object, is(instanceOf(GroupCasesReceived.class)));
        GroupCasesReceived groupCasesReceived = (GroupCasesReceived) object;
        assertThat(groupCasesReceived.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList(), Matchers.hasSize(2));
        assertThat(groupCasesReceived.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getCaseDetails().getInitiationCode(), is(INITIATION_CODE_CIVIL_CASE));
    }

    @Test
    public void shouldRaiseGroupProsecutionRejected() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("O"));
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, randomUUID(), true, "URN1");
        groupProsecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        assertThat(eventStream.findFirst().get(), is(instanceOf(GroupProsecutionRejected.class)));

    }

    private GroupProsecutionWithReferenceData buildGroupProsecutionWithReferenceDataWithouSummonCode(final String initiationCode, final Boolean isGroupMaster, final String prosecutorCaseReference){
        return new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withIsGroupMaster(isGroupMaster)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withInitiationCode(initiationCode)
                        .withProsecutorCaseReference(prosecutorCaseReference)
                        .build())
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.now().minusYears(10))
                                        .build())
                                .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                        .withDateOfBirth(LocalDate.now().minusYears(50))
                                        .build())
                                .build())
                        .withOffences(asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(OFFENCE_CODE)
                                .withArrestDate(LocalDate.now().minusDays(2))
                                .withChargeDate(LocalDate.now().minusDays(2))
                                .withOffenceLocation("London")
                                .withOffenceCommittedDate(LocalDate.now().minusDays(2))
                                .withStatementOfFacts("statements")
                                .build()))
                        .withInitialHearing(InitialHearing.initialHearing()
                                .withCourtHearingLocation("C55BN00")
                                .withDateOfHearing(LocalDate.now().plusDays(2).toString())
                                .withTimeOfHearing("09:05:01.001")
                                .build())
                        .build()))
                .build());
    }

    @Test
    public void shouldNotThrowRuntimeExceptionForDuplicateProsecutionCaseIds() {

        final UUID prosecutionCaseId = randomUUID();

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("O"));

        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData1 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, prosecutionCaseId, true, "URN1");
        groupProsecutionWithReferenceData1.setReferenceDataVO(referenceDataVO);

        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData2 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, prosecutionCaseId, false, "URN2");
        groupProsecutionWithReferenceData2.setReferenceDataVO(referenceDataVO);

        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData1);
        groupProsecutionWithReferenceDataList.add(groupProsecutionWithReferenceData2);

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);
        groupProsecutionList.setChannel(Channel.CIVIL);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        assertThat(eventStream.findFirst().get(), is(instanceOf(GroupCasesReceived.class)));
    }

    private GroupProsecutionWithReferenceData buildGroupProsecutionWithReferenceData(final String initiationCode, final UUID prosecutionCaseId, final Boolean isGroupMaster, final String prosecutorCaseReference){
        return new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMaster(isGroupMaster)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId)
                        .withInitiationCode(initiationCode)
                        .withSummonsCode("S02")
                        .withProsecutorCaseReference(prosecutorCaseReference)
                        .build())
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.now().minusYears(10))
                                        .build())
                                .withParentGuardianInformation(ParentGuardianInformation.parentGuardianInformation()
                                        .withDateOfBirth(LocalDate.now().minusYears(50))
                                        .build())
                                .build())
                        .withOffences(asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(OFFENCE_CODE)
                                .withArrestDate(LocalDate.now().minusDays(2))
                                .withChargeDate(LocalDate.now().minusDays(2))
                                .withOffenceLocation("London")
                                .withOffenceCommittedDate(LocalDate.now().minusDays(2))
                                .withStatementOfFacts("statements")
                                .build()))
                        .withInitialHearing(InitialHearing.initialHearing()
                                .withCourtHearingLocation("C55BN00")
                                .withDateOfHearing(LocalDate.now().plusDays(2).toString())
                                .withTimeOfHearing("09:05:01.001")
                                .build())
                        .build()))
                .build());
    }
}
