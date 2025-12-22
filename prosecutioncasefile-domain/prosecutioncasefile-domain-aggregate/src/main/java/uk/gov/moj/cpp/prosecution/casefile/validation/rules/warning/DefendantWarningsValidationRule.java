package uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning;

import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.getProblemFromDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.matchDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.ArrayList;
import java.util.List;

public class DefendantWarningsValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    public static final DefendantWarningsValidationRule INSTANCE = new DefendantWarningsValidationRule();

    private DefendantWarningsValidationRule() {
    }

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService context) {
        final List<Defendant> matchedDefendants = new ArrayList<>();
        final DefendantSubject defendantSubject = caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject();

        matchDefendants(caseDocumentWithReferenceData, defendantSubject, matchedDefendants);
        final List<Problem> problems = new ArrayList<>();

        matchedDefendants.forEach( defendant -> problems.add(getProblemFromDefendants(defendant, caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject())));
        return newValidationResult(problems);
    }

}
