package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.Arrays;
import java.util.List;

import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_FILE_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class SjpFileTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    private static final String UNRECOGNISED = "UNRECOGNISED";
    private static final List<String> VALID_SJPN_MIME_TYPES = Arrays.asList("application/pdf", "application/x-tika-msoffice", "application/x-tika-ooxml", "application/msword");
    private static final List<String> VALID_SJPN_DOCUMENT_TYPES = Arrays.asList("SJPN", "CITN");

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        return newValidationResult(of(caseDocumentWithReferenceData)
                .filter(m -> VALID_SJPN_DOCUMENT_TYPES.contains(caseDocumentWithReferenceData.getMaterial().getDocumentType()))
                .filter(m -> !VALID_SJPN_MIME_TYPES.contains(caseDocumentWithReferenceData.getMaterial().getFileType()))
                .map(m -> newProblem(INVALID_FILE_TYPE, "fileType", defaultIfBlank(caseDocumentWithReferenceData.getMaterial().getFileType(), UNRECOGNISED))));
    }
}
