package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
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


public class ChargeDateValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<Problem> problems = new ArrayList<>();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final List<ProblemValue> offencesWithChargeDateInFuture = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getChargeDate()) && offence.getChargeDate().isAfter(now()))
                .map(offence -> ProblemValue.problemValue()
                        .withId(offence.getOffenceId().toString())
                        .withKey("offenceChargeDate")
                        .withValue(offence.getChargeDate().toString())
                        .build())
                .collect(toList());

        if (isNotEmpty(offencesWithChargeDateInFuture)) {

            final Problem problem = Problem.problem()
                    .withCode(ProblemCode.CHARGE_DATE_IN_FUTURE.toString())
                    .withValues(offencesWithChargeDateInFuture)
                    .build();
            problems.add(problem);

        }

        if (isNotEmpty(problems)) {
            return newValidationResult(problems);
        }
        return VALID;

    }

}
