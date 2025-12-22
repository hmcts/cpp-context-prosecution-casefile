package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.NON_CIVIL_OFFENCES;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MojOffences;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class CivilCaseOffencesValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData prosecutionWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        String initialFeeStatus = prosecutionWithReferenceData.getCaseDetails().getFeeStatus();
        if (StringUtils.isEmpty(initialFeeStatus)) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        if (StringUtils.isNotEmpty(initialFeeStatus)) {
            final List<String> civilOffences = referenceDataQueryService.retrieveOffencesByType("VP").stream().map(MojOffences::getCjsOffenceCode).collect(Collectors.toList());
            prosecutionWithReferenceData.getDefendant().getOffences().forEach(o -> {
                if (!civilOffences.contains(o.getOffenceCode())) {
                    problemValues.add(new ProblemValue(o.getOffenceId().toString(), OFFENCE_CODE.getValue(), o.getOffenceCode()));
                }
            });
        }

        if (problemValues.isEmpty()) {
            return VALID;
        } else {
            return newValidationResult(of(newProblem(NON_CIVIL_OFFENCES, problemValues)));
        }
    }
}
