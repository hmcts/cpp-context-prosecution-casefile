package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class DuplicateProsecutionReferenceValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private DuplicateProsecutionReferenceValidationRule duplicateProsecutionReferenceValidationRule;

    @Test
    public void shouldNotReturnAnyValidationErrorsWhenThereAreNoDuplicateGroupProsecutionCaseReferences() {

        final UUID groupId = randomUUID();
        final String offenceCode = "OFFCODE";

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA1")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode)
                                .withOffenceLocation("Location1")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution2 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA2")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution2));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult problem = duplicateProsecutionReferenceValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        assertThat(problem.isValid(), is(true));

    }

    @Test
    public void shouldReturnValidationErrorForOneDuplicateProsecutionCaseReference() {

        final UUID groupId = randomUUID();
        final String offenceCode1 = "OFFCODE1";
        final String offenceCode2 = "OFFCODE2";

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA1")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode1)
                                .withOffenceLocation("Location1")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution2 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA1")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode2)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution3 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA2")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode2)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution2));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution3));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = duplicateProsecutionReferenceValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        Problem problem = result.problems().stream().findFirst().orElse(null);

        assertThat(problem.getCode(), is("DUPLICATED_PROSECUTION"));
        assertThat(problem.getValues().size(), is(1));
        assertThat(problem.getValues().get(0).getKey(), is("AAAAAA1"));
        assertThat(problem.getValues().get(0).getValue(), is("2"));

    }

    @Test
    public void shouldReturnValidationErrorForTwoDuplicateProsecutionCaseReferences() {

        final UUID groupId = randomUUID();
        final String offenceCode1 = "OFFCODE1";
        final String offenceCode2 = "OFFCODE2";

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA1")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode1)
                                .withOffenceLocation("Location1")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution2 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA1")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode2)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution3 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA2")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode2)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution4 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("AAAAAA2")
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode(offenceCode2)
                                .withOffenceLocation("Location2")
                                .build()))
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution2));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution3));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution4));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = duplicateProsecutionReferenceValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        Problem problem = result.problems().stream().findFirst().orElse(null);

        assertThat(problem.getCode(), is("DUPLICATED_PROSECUTION"));
        assertThat(problem.getValues().size(), is(2));
        assertThat(problem.getValues().get(0).getKey(), is("AAAAAA2"));
        assertThat(problem.getValues().get(0).getValue(), is("2"));
        assertThat(problem.getValues().get(1).getKey(), is("AAAAAA1"));
        assertThat(problem.getValues().get(1).getValue(), is("2"));

    }

}