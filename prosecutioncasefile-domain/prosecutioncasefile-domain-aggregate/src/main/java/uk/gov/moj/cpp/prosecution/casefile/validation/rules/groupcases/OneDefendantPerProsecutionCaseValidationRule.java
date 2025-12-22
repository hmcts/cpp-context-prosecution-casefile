package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.List;
import java.util.stream.Collectors;

public class OneDefendantPerProsecutionCaseValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService context) {

        final List<String> prosecutionCaseIds = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .filter(groupProsecutionWithReferenceData -> groupProsecutionWithReferenceData.getGroupProsecution().getDefendants().size() > 1)
                .map(groupProsecutionWithReferenceData -> groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails().getCaseId().toString())
                .collect(Collectors.toList());

        if (isNotEmpty(prosecutionCaseIds)) {
            return newValidationResult(of(newProblem(ProblemCode.MORE_THAN_ONE_DEFENDANT_PER_PROSECUTION_CASE, problemValue()
                    .withKey("more.than.one.defendant.per.prosecution.case")
                    .withValue(String.join(", ", prosecutionCaseIds))
                    .build())));
        }

        return ValidationResult.VALID;

    }

}
