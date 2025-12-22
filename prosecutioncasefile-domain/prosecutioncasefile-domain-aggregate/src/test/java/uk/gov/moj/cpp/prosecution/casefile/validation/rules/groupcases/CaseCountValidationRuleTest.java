package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class CaseCountValidationRuleTest {

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private CaseCountValidationRule caseCountValidationRule;

    @Test
    public void shouldReturnValidationErrorsWhenRequestContainsOneCase() {

        // list contains one group case
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution().build()));
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = caseCountValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        final Problem problem = result.problems().stream().findFirst().orElse(null);

        assert problem != null;
        assertThat(problem.getCode(), is("GROUP_PROSECUTION_CASE_COUNT_INVALID"));
        assertThat(problem.getValues().size(), is(1));

    }

    @Test
    public void shouldReturnValidationErrorsWhenRequestContainsMoreThanThousandCases() {

        // list contains 1001 group case
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        for (int i = 1; i <= 1001; i++) {
            groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution().build()));
        }
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = caseCountValidationRule.validate(groupProsecutionList, referenceDataQueryService);

        final Problem problem = result.problems().stream().findFirst().orElse(null);

        assert problem != null;
        assertThat(problem.getCode(), is("GROUP_PROSECUTION_CASE_COUNT_INVALID"));
        assertThat(problem.getValues().size(), is(1));

    }

    @Test
    public void shouldNotReturnValidationErrorsWhenRequestContainsMoreThanOneCaseAndLessThanThousandOneCases() {

        // list contains 1000 group case
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            groupProsecutionWithReferenceDataList.add(new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution().build()));
        }
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList);

        final ValidationResult result = caseCountValidationRule.validate(groupProsecutionList, referenceDataQueryService);
        assertThat(result.isValid(), is(true));

    }

}