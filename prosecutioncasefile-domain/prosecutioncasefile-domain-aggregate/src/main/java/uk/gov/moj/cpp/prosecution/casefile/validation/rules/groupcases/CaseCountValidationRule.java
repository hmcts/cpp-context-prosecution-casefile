package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class CaseCountValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService context) {
        final Integer caseCount = groupProsecutionList.getGroupProsecutionWithReferenceDataList().size();

        if(caseCount > 1 && caseCount <= 1000){
            return ValidationResult.VALID;
        }

        return newValidationResult(of(newProblem(ProblemCode.GROUP_PROSECUTION_CASE_COUNT_INVALID, problemValue()
                .withKey("groupProsecutions.size")
                .withValue(caseCount.toString())
                .build())));
    }

}
