package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_FILE_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;

public class FileTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {


    private static final String UNRECOGNISED = "UNRECOGNISED";
    private static final List<String> VALID_FILE_TYPES = asList(
            "image/bmp",
            "image/jpeg",
            "image/png",
            "image/tiff",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/pdf");

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        return newValidationResult(of(caseDocumentWithReferenceData)
                .filter(m -> !VALID_FILE_TYPES.contains(caseDocumentWithReferenceData.getMaterialContentType()))
                .map(m -> newProblem(INVALID_FILE_TYPE, "materialContentType", defaultIfBlank(caseDocumentWithReferenceData.getMaterialContentType(), UNRECOGNISED))));
    }
}
