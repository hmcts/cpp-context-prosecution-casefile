package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.ENFORCEMENT_OFFENCE_NOT_FOUND;
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
import java.util.Set;
import java.util.stream.Collectors;

public class EnforcementOffencesValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData,
                                     final ReferenceDataQueryService referenceDataQueryService) {

        final Set<String> allowedCodes = referenceDataQueryService.retrieveAllActiveMojOffences().stream()
                .map(MojOffences::getCjsOffenceCode)
                .collect(Collectors.toSet());

        final List<ProblemValue> notFound = new ArrayList<>();

        defendantWithReferenceData.getDefendant().getOffences().forEach(offence -> {
            if (!allowedCodes.contains(offence.getOffenceCode())) {
                final String offenceId = offence.getOffenceId() != null ? offence.getOffenceId().toString() : offence.getOffenceCode();
                notFound.add(new ProblemValue(offenceId, OFFENCE_CODE.getValue(), offence.getOffenceCode()));
            }
        });

        return notFound.isEmpty() ? VALID : newValidationResult(of(newProblem(ENFORCEMENT_OFFENCE_NOT_FOUND, notFound)));
    }
}
