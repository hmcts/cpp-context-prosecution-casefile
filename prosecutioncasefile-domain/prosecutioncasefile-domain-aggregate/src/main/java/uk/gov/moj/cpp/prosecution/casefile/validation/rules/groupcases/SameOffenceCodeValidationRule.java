package uk.gov.moj.cpp.prosecution.casefile.validation.rules.groupcases;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import java.util.List;
import java.util.Set;


public class SameOffenceCodeValidationRule implements ValidationRule<GroupProsecutionList, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final GroupProsecutionList groupProsecutionList, final ReferenceDataQueryService referenceDataQueryService) {

        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList();

        final Set<String> offenceCodes = groupProsecutionWithReferenceDataList.stream()
                .flatMap(group -> group.getGroupProsecution().getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(Offence::getOffenceCode)
                .collect(toSet());

        if (offenceCodes.size() > 1) {

            return newValidationResult(of(newProblem(ProblemCode.DIFFERENT_TYPE_CIVIL_OFFENCE_CODES_ARE_PRESENT, problemValue()
                    .withKey("more.than.one.different.offence.code")
                    .withValue(String.join(", ", offenceCodes))
                    .build())));

        }

        return VALID;

    }

}
