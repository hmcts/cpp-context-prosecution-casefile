package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.CHARGE_DATE_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CHARGE_DATE;
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
import java.util.Objects;
import java.util.stream.Collectors;

public class ChargeDateValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final List<Offence> offenceList = defendantWithReferenceData.getDefendant().getOffences().stream().filter(Objects::nonNull).filter(offence -> offence.getChargeDate() == null).collect(Collectors.toList());
        offenceList.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_CHARGE_DATE.getValue(), "Charge date not provided")));

        final List<Offence> chargeDateIsGreaterThanCurrentDate = defendantWithReferenceData.getDefendant().getOffences().stream().filter(offence -> offence.getChargeDate() != null && offence.getChargeDate().isAfter(LocalDate.now(ZoneId.of("Europe/London")))).collect(Collectors.toList());

        chargeDateIsGreaterThanCurrentDate.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_CHARGE_DATE.getValue(),
                offence.getChargeDate().toString())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(CHARGE_DATE_IN_FUTURE, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
