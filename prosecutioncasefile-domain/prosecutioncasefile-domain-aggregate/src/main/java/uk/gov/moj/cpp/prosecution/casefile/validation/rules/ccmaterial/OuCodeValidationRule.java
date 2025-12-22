package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.HEADER_OUCODE_NOT_RECOGNISED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OU_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class OuCodeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (caseDocumentWithReferenceData.getProsecutionCaseSubject() == null) {
            return VALID;
        }

        return validateProsecutorOuCode(caseDocumentWithReferenceData, referenceDataQueryService);
    }

    private ValidationResult validateProsecutorOuCode(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        final String ouCode = caseDocumentWithReferenceData.getProsecutionCaseSubject().getOuCode();
        final ProsecutorsReferenceData prosecutorsByOuCode = referenceDataQueryService.getProsecutorsByOuCode(ouCode);

        if(isNull(prosecutorsByOuCode)){
            return newValidationResult(of(newProblem(HEADER_OUCODE_NOT_RECOGNISED, OU_CODE.getValue(), ouCode)));
        } else if(ofNullable(prosecutorsByOuCode.getCpsFlag()).orElse(false)) {
            caseDocumentWithReferenceData.setHeaderOuCodeCPS(true);
            return VALID;
        } else if (ouCode.equals(caseDocumentWithReferenceData.getProsecutionCaseSubject().getProsecutingAuthority())){
            return VALID;
        } else{
            return newValidationResult(of(newProblem(HEADER_OUCODE_NOT_RECOGNISED, OU_CODE.getValue(), ouCode)));
        }
    }
}
