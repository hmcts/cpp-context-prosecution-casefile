package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.LAID_DATE_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_LAID_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LaidDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        if (defendantWithReferenceData.getDefendant() == null || defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final List<Offence> laidDateIsGreaterThanCurrentDate = defendantWithReferenceData.getDefendant().getOffences().stream().filter(offence -> offence.getLaidDate() != null && offence.getLaidDate().isAfter(LocalDate.now(ZoneId.of("Europe/London")))).collect(Collectors.toList());

        laidDateIsGreaterThanCurrentDate.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_LAID_DATE.getValue(), offence.getLaidDate().toString())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(LAID_DATE_IN_FUTURE, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }


}
