package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant.offence;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.STATEMENT_OF_FACTS_WELSH_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_STATEMENT_OF_FACTS_WELSH;
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

public class StatementOfFactsWelshValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final String SUMMONS_CODE_SUMMCA = "M";
    private static final String WELSH_DOCUMENTATION_LANGUAGE = "W";

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.getDefendant() == null ||
                defendantWithReferenceData.getDefendant().getOffences() == null) {
            return VALID;
        }

        final String summonsCode = defendantWithReferenceData.getCaseDetails().getSummonsCode();

        if (!SUMMONS_CODE_SUMMCA.equals(summonsCode)) {
            return VALID;
        }

        if(null == defendantWithReferenceData.getDefendant().getDocumentationLanguage() || !WELSH_DOCUMENTATION_LANGUAGE.equals(defendantWithReferenceData.getDefendant().getDocumentationLanguage().toString())) {
            return VALID;
        }

        final List<ProblemValue> problemValues = new ArrayList<>();

        final List<Offence> offenceList = defendantWithReferenceData.getDefendant().getOffences().stream()
                .filter(Objects::nonNull)
                .filter(offence -> isEmpty(trim(offence.getStatementOfFactsWelsh())))
                .collect(Collectors.toList());

        offenceList.forEach(offence -> problemValues.add(new ProblemValue(offence.getOffenceId().toString(), OFFENCE_STATEMENT_OF_FACTS_WELSH.getValue(), offence.getStatementOfFactsWelsh() == null ? "" : offence.getStatementOfFactsWelsh())));

        if (problemValues.isEmpty()) {
            return VALID;
        }

        return newValidationResult(of(newProblem(STATEMENT_OF_FACTS_WELSH_REQUIRED, problemValues.toArray(new ProblemValue[problemValues.size()]))));
    }
}
