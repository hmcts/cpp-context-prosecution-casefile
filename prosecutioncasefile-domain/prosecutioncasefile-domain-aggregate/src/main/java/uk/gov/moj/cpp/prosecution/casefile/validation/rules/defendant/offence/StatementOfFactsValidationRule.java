package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.STATEMENT_OF_FACTS_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_STATEMENT_OF_FACTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatementOfFactsValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final String SUMMONS_CODE = "M";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }


        final String summonsCode = defendantWithReferenceData.getCaseDetails().getSummonsCode();

        if (!SUMMONS_CODE.equals(summonsCode)) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final List<Offence> offenceList = defendantWithReferenceData.getDefendant().getOffences().stream().filter(Objects::nonNull).filter(offence -> isEmpty(trim(offence.getStatementOfFacts()))).collect(Collectors.toList());
        offenceList.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_STATEMENT_OF_FACTS.getValue(), offence.getStatementOfFacts() == null ? "" : offence.getStatementOfFacts())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(STATEMENT_OF_FACTS_REQUIRED, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
