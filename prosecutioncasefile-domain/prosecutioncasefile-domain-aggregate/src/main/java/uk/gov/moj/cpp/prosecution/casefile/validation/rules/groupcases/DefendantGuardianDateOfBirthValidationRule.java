package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE;
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

public class DefendantGuardianDateOfBirthValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<Problem> problems = new ArrayList<>();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final List<ProblemValue> defendantGuardiansWithFutureDoB = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .filter(defendant ->
                        nonNull(defendant.getIndividual())
                                && nonNull(defendant.getIndividual().getParentGuardianInformation())
                                && nonNull(defendant.getIndividual().getParentGuardianInformation().getDateOfBirth())
                                && defendant.getIndividual().getParentGuardianInformation().getDateOfBirth().isAfter(now()))
                .map(defendant -> ProblemValue.problemValue()
                        .withId(defendant.getId())
                        .withKey("dateOfBirth")
                        .withValue(defendant.getIndividual().getParentGuardianInformation().getDateOfBirth().toString())
                        .build())
                .collect(toList());

        if (isNotEmpty(defendantGuardiansWithFutureDoB)) {

            final Problem problem = Problem.problem()
                    .withCode(DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE.toString())
                    .withValues(defendantGuardiansWithFutureDoB)
                    .build();

            problems.add(problem);

            return newValidationResult(problems);

        }

        return VALID;

    }

}
