package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_DOCUMENT_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DOCUMENT_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class CCDocumentTypeValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
            return VALID;
        }
        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = referenceDataQueryService.retrieveDocumentsTypeAccess();
        final Optional<DocumentTypeAccessReferenceData> matchedDocumentMetadata = documentMetadataReferenceDataList
                .stream()
                .filter(documentMetadataReferenceData -> documentMetadataReferenceData.getSection().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentType()))
                .findFirst();

        matchedDocumentMetadata.ifPresent(documentMetadata -> {
            if(caseDocumentWithReferenceData.getCourtApplicationSubject() != null){
                caseDocumentWithReferenceData.setDocumentTypeAccessReferenceData(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData()
                        .withDocumentCategory(DocumentCategory.APPLICATIONS.toString())
                        .withId(documentMetadata.getId())
                        .withSection(documentMetadata.getSection())
                        .withActionRequired(documentMetadata.getActionRequired())
                        .withCourtDocumentTypeRBAC(documentMetadata.getCourtDocumentTypeRBAC())
                        .withValidFrom(documentMetadata.getValidFrom())
                        .withValidTo(documentMetadata.getValidTo())
                        .withSectionCode(documentMetadata.getSectionCode())
                        .build());
                caseDocumentWithReferenceData.setDocumentCategory(DocumentCategory.APPLICATIONS.toString());
            }else{
                caseDocumentWithReferenceData.setDocumentTypeAccessReferenceData(documentMetadata);
                caseDocumentWithReferenceData.setDocumentCategory(documentMetadata.getDocumentCategory());
            }
            caseDocumentWithReferenceData.setDocumentType(documentMetadata.getSection());

        });
        if(!matchedDocumentMetadata.isPresent()) {
            return newValidationResult(of(newProblem(INVALID_DOCUMENT_TYPE, DOCUMENT_TYPE.getValue(), Optional.ofNullable(caseDocumentWithReferenceData.getDocumentType()).orElse(""))));
        }
        return VALID;
    }
}
