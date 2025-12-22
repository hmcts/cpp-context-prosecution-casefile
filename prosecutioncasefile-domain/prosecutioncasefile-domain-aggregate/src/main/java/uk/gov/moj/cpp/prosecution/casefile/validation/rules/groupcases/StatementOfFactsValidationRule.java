package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.STATEMENT_OF_FACTS_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.ArrayList;
import java.util.List;

public class StatementOfFactsValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<Problem> problems = new ArrayList<>();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final List<ProblemValue> offencesWithStatementOfFacts = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> isNull(offence.getStatementOfFacts()))
                .map(offence -> ProblemValue.problemValue()
                        .withId(offence.getOffenceId().toString())
                        .withKey("statement_of_facts")
                        .withValue("is not present")
                        .build())
                .collect(toList());

        if (isNotEmpty(offencesWithStatementOfFacts)) {
            final Problem problem = Problem.problem()
                    .withCode(STATEMENT_OF_FACTS_REQUIRED.toString())
                    .withValues(offencesWithStatementOfFacts)
                    .build();

            problems.add(problem);
            return newValidationResult(problems);
        }

        return VALID;

    }
}
