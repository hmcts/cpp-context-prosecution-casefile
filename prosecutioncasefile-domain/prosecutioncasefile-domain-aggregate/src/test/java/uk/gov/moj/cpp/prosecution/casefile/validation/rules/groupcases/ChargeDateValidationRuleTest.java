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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ChargeDateValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private ChargeDateValidationRule chargeDateValidationRule;

    @Test
    public void shouldNotReturnAnyValidationErrorsWhenChargeDateIsPresentAndInThePast() {

        final UUID groupId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String offenceCode = "OFFCODE";

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId1)
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId1.toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(offenceId1)
                                .withOffenceCode(offenceCode)
                                .withOffenceLocation("Location1")
                                .withChargeDate(LocalDate.now().minusDays(20))
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution2 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId2)
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId2.toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(offenceId2)
                                .withOffenceCode(offenceCode)
                                .withOffenceLocation("Location2")
                                .withChargeDate(LocalDate.now().minusDays(20))
                                .build()))
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution2));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult problem = chargeDateValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        assertThat(problem.isValid(), is(true));

    }

    @Test
    public void shouldReturnValidationErrorWhenChargeDateIsInFuture() {

        final UUID groupId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String offenceCode1 = "OFFCODE1";

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId1)
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId1.toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(offenceId1)
                                .withOffenceCode(offenceCode1)
                                .withChargeDate(LocalDate.now().plusDays(20))
                                .build()))
                        .build()))
                .build();
        final GroupProsecution groupProsecution2 = GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(prosecutionCaseId2)
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId2.toString())
                        .withOffences(Arrays.asList(Offence.offence()
                                .withOffenceId(offenceId2)
                                .withOffenceCode(offenceCode1)
                                .withChargeDate(LocalDate.now().plusDays(20))
                                .build()))
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution2));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = chargeDateValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        Problem problem = result.problems().stream().findFirst().orElse(null);

        assertThat(problem.getCode(), is("CHARGE_DATE_IN_FUTURE"));
        assertThat(problem.getValues().size(), is(2));
        assertThat(problem.getValues().get(0).getKey(), is("offenceChargeDate"));
        assertThat(problem.getValues().get(1).getKey(), is("offenceChargeDate"));

    }

}