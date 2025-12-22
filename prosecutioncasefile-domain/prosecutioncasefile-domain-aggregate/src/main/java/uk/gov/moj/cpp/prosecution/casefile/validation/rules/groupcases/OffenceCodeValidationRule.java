package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class OffenceCodeValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<Problem> problems = new ArrayList<>();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final Set<String> offenceCodes = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(Offence::getOffenceCode)
                .collect(toSet());

        if (offenceCodes.size() > 1) {

            final Problem problem = Problem.problem()
                    .withCode("more.than.one.offence.code.available")
                    .withValues(Arrays.asList(ProblemValue.problemValue()
                            .withKey("offenceCode")
                            .withValue(String.join(", ", offenceCodes))
                            .build()))
                    .build();

            problems.add(problem);

        }

        final Set<Offence> offencesWithoutLocation = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> isNull(offence.getOffenceLocation()))
                .collect(toSet());

        if (isNotEmpty(offencesWithoutLocation)) {

            final List<ProblemValue> problemValues = offencesWithoutLocation.stream()
                    .map(offence -> ProblemValue.problemValue()
                            .withId(offence.getOffenceId().toString())
                            .withKey("offenceCode")
                            .withValue(offence.getOffenceCode())
                            .build())
                    .collect(toList());
            final Problem problem = Problem.problem()
                    .withCode("no.offence.location.available")
                    .withValues(problemValues)
                    .build();
            problems.add(problem);

        }

        if (isNotEmpty(problems)) {
            return newValidationResult(problems);
        }

        return VALID;

    }

}
