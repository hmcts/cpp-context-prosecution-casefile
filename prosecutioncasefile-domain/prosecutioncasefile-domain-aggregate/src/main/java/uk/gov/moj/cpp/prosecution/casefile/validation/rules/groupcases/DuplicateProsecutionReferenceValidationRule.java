package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class DuplicateProsecutionReferenceValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<Problem> problems = new ArrayList<>();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final List<ProblemValue> problemValues = new ArrayList<>();

                groupProsecutionWithReferenceDataList.stream()
                .map(group -> group.getGroupProsecution().getCaseDetails().getProsecutorCaseReference())
                .collect(groupingBy(Function.identity(), counting()))
                .forEach((key, value) -> {
                    if (value > 1) {
                        problemValues.add(ProblemValue.problemValue()
                                .withId(key)
                                .withKey(key)
                                .withValue(value.toString())
                                .build());

                    }
                });

        if (isNotEmpty(problemValues)) {
            final Problem problem = Problem.problem()
                    .withCode(ProblemCode.DUPLICATED_PROSECUTION.toString())
                    .withValues(problemValues)
                    .build();
            problems.add(problem);
            return newValidationResult(problems);
        }

        return VALID;

    }

}
