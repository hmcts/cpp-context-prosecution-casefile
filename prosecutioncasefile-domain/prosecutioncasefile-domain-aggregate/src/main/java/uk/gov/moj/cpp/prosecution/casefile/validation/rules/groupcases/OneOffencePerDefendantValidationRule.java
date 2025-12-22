package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.Optional.of;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.List;
import java.util.stream.Collectors;

public class OneOffencePerDefendantValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService context) {

        final List<String> defendantIds = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .flatMap(groupProsecutionWithReferenceData -> groupProsecutionWithReferenceData.getGroupProsecution().getDefendants().stream())
                .filter(defendant -> defendant.getOffences().size() > 1)
                .map(Defendant::getId)
                .collect(Collectors.toList());

        if (isNotEmpty(defendantIds)) {
            return newValidationResult(of(newProblem(ProblemCode.MORE_THAN_ONE_OFFENCE_PER_DEFENDANT, problemValue()
                    .withKey("more.than.one.offence.per.defendant")
                    .withValue(String.join(", ", defendantIds))
                    .build())));
        }

        return ValidationResult.VALID;

    }

}
