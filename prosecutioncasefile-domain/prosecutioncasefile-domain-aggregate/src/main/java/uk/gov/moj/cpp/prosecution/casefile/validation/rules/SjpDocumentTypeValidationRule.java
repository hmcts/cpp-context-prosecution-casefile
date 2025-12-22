package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class SjpDocumentTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    private static final List<String> VALID_SJP_DOCUMENT_TYPES = asList("SJPN", "CITN", "PLEA", "FINANCIAL_MEANS", "DISQUALIFICATION_REPLY_SLIP");
    private static final String OTHER_DOCUMENT_TYPES = "OTHER-";

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        return newValidationResult(Optional.of(caseDocumentWithReferenceData)
                .filter(m -> !VALID_SJP_DOCUMENT_TYPES.contains(caseDocumentWithReferenceData.getMaterial().getDocumentType()))
                .filter(m -> !caseDocumentWithReferenceData.getMaterial().getDocumentType().startsWith(OTHER_DOCUMENT_TYPES))
                .map(m -> newProblem(INVALID_DOCUMENT_TYPE, "documentType", Optional.ofNullable(caseDocumentWithReferenceData.getMaterial().getDocumentType()).orElse(""))));
    }
}
