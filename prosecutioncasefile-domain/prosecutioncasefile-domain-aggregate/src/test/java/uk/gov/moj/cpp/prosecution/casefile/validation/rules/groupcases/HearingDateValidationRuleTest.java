package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class HearingDateValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private HearingDateValidationRule hearingDateValidationRule;

    @Test
    public void shouldNotReturnAnyValidationErrorsWhenTheHearingDateInTheFuture() {

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withInitialHearing(InitialHearing.initialHearing()
                                .withDateOfHearing(LocalDate.now().plusDays(1).toString())
                                .withTimeOfHearing("09:05:01.001")
                                .build())
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult problem = hearingDateValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        assertThat(problem.isValid(), is(true));

    }

    @Test
    public void shouldReturnValidationErrorsWhenTheHearingDateInThePast() {

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withInitialHearing(InitialHearing.initialHearing()
                                .withDateOfHearing(LocalDate.now().minusDays(1).toString())
                                .withTimeOfHearing("09:05:01")
                                .build())
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = hearingDateValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        Problem problem = result.problems().stream().findFirst().orElse(null);

        assertThat(problem.getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));
        assertThat(problem.getValues().size(), is(1));

    }

    @Test
    public void shouldReturnValidationErrorsWhenTheHearingDateIsNotAvailable() {

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        final GroupProsecution groupProsecution1 = GroupProsecution.groupProsecution()
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .build()))
                .build();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(groupProsecution1));

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = hearingDateValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        Problem problem = result.problems().stream().findFirst().orElse(null);

        assertThat(problem.getCode(), is("DATE_OF_HEARING_NOT_AVAILABLE"));
        assertThat(problem.getValues().size(), is(1));

    }


}