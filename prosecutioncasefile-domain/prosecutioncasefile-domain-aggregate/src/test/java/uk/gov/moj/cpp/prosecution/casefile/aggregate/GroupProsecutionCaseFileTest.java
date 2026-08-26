package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSummonsApplicationRejected;
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
    private static final String URN_1 = "URN1";
    private static final String URN_2 = "URN2";

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
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(Arrays.asList("S"));
        // "A" is the only valid civil summons code (SummonsCodeValidationRule) — both cases are
        // civil (withIsCivil(true)), so both must use it for this scenario to actually park.
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData1 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), true, "URN1", "A");
        groupProsecutionWithReferenceData1.setReferenceDataVO(referenceDataVO);
        final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData2 = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), false, "URN2", "A");
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

    @Test
    public void shouldRaiseGroupProsecutionRejectedWithCaseErrorsGroupedPerCaseReference() {

        final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData =
                Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());

        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom("C55BN00")).thenReturn(optionalOrganisationUnitWithCourtroomReferenceData);

        // master case is initiated as "O" but only "S" is a known initiation type for it -> one case-level problem
        final ReferenceDataVO masterReferenceDataVO = new ReferenceDataVO();
        masterReferenceDataVO.setInitiationTypes(Arrays.asList(INITIATION_CODE_FOR_SUMMONS));
        final GroupProsecutionWithReferenceData masterCase = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, randomUUID(), true, URN_1);
        masterCase.setReferenceDataVO(masterReferenceDataVO);

        // second case is initiated as "S" but only "O" is a known initiation type for it, and its summons code is not
        // a recognised one -> two case-level problems, distinct from the master case's
        final ReferenceDataVO secondReferenceDataVO = new ReferenceDataVO();
        secondReferenceDataVO.setInitiationTypes(Arrays.asList(INITIATION_CODE_CIVIL_CASE));
        final GroupProsecutionWithReferenceData secondCase = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), false, URN_2);
        secondCase.setReferenceDataVO(secondReferenceDataVO);

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        groupProsecutionWithReferenceDataList.add(masterCase);
        groupProsecutionWithReferenceDataList.add(secondCase);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);
        groupProsecutionList.setChannel(Channel.CIVIL);

        final Stream<Object> eventStream = groupProsecutionCaseFile.receiveGroupProsecution(groupProsecutionList, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final Object event = eventStream.findFirst().get();
        assertThat(event, is(instanceOf(GroupProsecutionRejected.class)));
        final GroupProsecutionRejected groupProsecutionRejected = (GroupProsecutionRejected) event;

        final List<CaseProblem> caseErrors = groupProsecutionRejected.getCaseErrors();
        assertThat(caseErrors, Matchers.hasSize(2));

        final CaseProblem masterCaseProblem = caseProblemFor(caseErrors, URN_1);
        assertThat(masterCaseProblem.getProblems(), Matchers.hasSize(1));
        assertThat(masterCaseProblem.getProblems().get(0).getCode(), is(ProblemCode.CASE_INITIATION_CODE_INVALID.name()));
        assertThat(masterCaseProblem.getProblems().get(0).getValues().get(0).getValue(), is(INITIATION_CODE_CIVIL_CASE));

        final CaseProblem secondCaseProblem = caseProblemFor(caseErrors, URN_2);
        assertThat(secondCaseProblem.getProblems(), Matchers.hasSize(2));
        assertThat(secondCaseProblem.getProblems().get(0).getCode(), is(ProblemCode.CASE_INITIATION_CODE_INVALID.name()));
        assertThat(secondCaseProblem.getProblems().get(0).getValues().get(0).getValue(), is(INITIATION_CODE_FOR_SUMMONS));
        assertThat(secondCaseProblem.getProblems().get(1).getCode(), is(ProblemCode.SUMMONS_CODE_INVALID.name()));

        // case-level problems must not leak into the group-level errors
        assertThat(groupProsecutionRejected.getGroupCaseErrors(), Matchers.empty());
    }

    @Test
    void shouldWrapProblemsIntoSingleCaseProblemWithNullReferenceOnRejectGroupProsecution() {
        seedAggregateWithMasterCase();

        final Problem problem1 = Problem.problem().withCode("DUPLICATED_PROSECUTION").build();
        final Problem problem2 = Problem.problem().withCode("ANOTHER_PROBLEM").build();

        final Stream<Object> eventStream = groupProsecutionCaseFile.rejectGroupProsecution(asList(problem1, problem2));
        final Object event = eventStream.findFirst().get();
        assertThat(event, is(instanceOf(GroupProsecutionRejected.class)));

        final GroupProsecutionRejected rejected = (GroupProsecutionRejected) event;
        assertThat(rejected.getGroupCaseErrors(), Matchers.hasSize(1));

        final CaseProblem wrapper = rejected.getGroupCaseErrors().get(0);
        assertThat(wrapper.getProsecutorCaseReference(), is(nullValue()));
        assertThat(wrapper.getProblems(), Matchers.contains(problem1, problem2));
    }

    @Test
    void shouldOmitGroupCaseErrorsWhenRejectGroupProsecutionCalledWithNoProblems() {
        seedAggregateWithMasterCase();

        final Stream<Object> eventStream = groupProsecutionCaseFile.rejectGroupProsecution();
        final GroupProsecutionRejected rejected = (GroupProsecutionRejected) eventStream.findFirst().get();

        assertThat(rejected.getGroupCaseErrors(), is(nullValue()));
    }

    @Test
    void shouldAlsoRaiseGroupSummonsApplicationRejectedWhenRejectGroupProsecutionCalledWithNoArgs() {
        final UUID externalId = randomUUID();
        final GroupProsecutionWithReferenceData masterCase = buildGroupProsecutionWithReferenceData(INITIATION_CODE_FOR_SUMMONS, randomUUID(), true, URN_1);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(asList(masterCase), externalId, Channel.CIVIL);
        groupProsecutionCaseFile.apply(GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(groupProsecutionList)
                .build());

        final List<Object> events = groupProsecutionCaseFile.rejectGroupProsecution().collect(java.util.stream.Collectors.toList());

        assertThat(events, Matchers.hasSize(2));
        assertThat(events.get(0), is(instanceOf(GroupProsecutionRejected.class)));
        assertThat(events.get(1), is(instanceOf(GroupSummonsApplicationRejected.class)));

        final GroupSummonsApplicationRejected groupSummonsApplicationRejected = (GroupSummonsApplicationRejected) events.get(1);
        assertThat(groupSummonsApplicationRejected.getGroupId(), is(groupId));
        assertThat(groupSummonsApplicationRejected.getChannel(), is(Channel.CIVIL));
        assertThat(groupSummonsApplicationRejected.getExternalId(), is(externalId));
    }

    @Test
    void shouldNotRaiseGroupSummonsApplicationRejectedWhenRejectGroupProsecutionCalledWithProblems() {
        seedAggregateWithMasterCase();

        final Problem problem = Problem.problem().withCode("DUPLICATED_PROSECUTION").build();
        final List<Object> events = groupProsecutionCaseFile.rejectGroupProsecution(asList(problem)).collect(java.util.stream.Collectors.toList());

        assertThat(events, Matchers.hasSize(1));
        assertThat(events.get(0), is(instanceOf(GroupProsecutionRejected.class)));
    }

    private void seedAggregateWithMasterCase() {
        final GroupProsecutionWithReferenceData masterCase = buildGroupProsecutionWithReferenceData(INITIATION_CODE_CIVIL_CASE, randomUUID(), true, URN_1);
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(asList(masterCase));
        groupProsecutionCaseFile.apply(GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(groupProsecutionList)
                .build());
    }

    private CaseProblem caseProblemFor(final List<CaseProblem> caseErrors, final String prosecutorCaseReference) {
        return caseErrors.stream()
                .filter(caseProblem -> prosecutorCaseReference.equals(caseProblem.getProsecutorCaseReference()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No CaseProblem found for prosecutorCaseReference " + prosecutorCaseReference));
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
        return buildGroupProsecutionWithReferenceData(initiationCode, prosecutionCaseId, isGroupMaster, prosecutorCaseReference, "S02");
    }

    private GroupProsecutionWithReferenceData buildGroupProsecutionWithReferenceData(final String initiationCode, final UUID prosecutionCaseId, final Boolean isGroupMaster, final String prosecutorCaseReference, final String summonsCode){
        return new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMaster(isGroupMaster)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId)
                        .withInitiationCode(initiationCode)
                        .withSummonsCode(summonsCode)
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
