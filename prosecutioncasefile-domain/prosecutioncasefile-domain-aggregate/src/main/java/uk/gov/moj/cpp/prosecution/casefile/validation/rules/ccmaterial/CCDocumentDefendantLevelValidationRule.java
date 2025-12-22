package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ID_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ID_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

public class CCDocumentDefendantLevelValidationRule implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
            return getProsecutorDefendantProblem(caseDocumentWithReferenceData, caseDocumentWithReferenceData.getProsecutorDefendantId());
        }

        final List<DocumentTypeAccessReferenceData> documentMetadataReferenceDataList = referenceDataQueryService.retrieveDocumentsTypeAccess();
        final Optional<DocumentTypeAccessReferenceData> matchedDocumentMetadata = documentMetadataReferenceDataList
                .stream()
                .filter(documentMetadataReferenceData ->
                        documentMetadataReferenceData.getSection()
                                .equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentType())).findFirst();

        matchedDocumentMetadata.ifPresent(caseDocumentWithReferenceData::setDocumentTypeAccessReferenceData);

        if (!matchedDocumentMetadata.isPresent()) {
            return VALID;
        }

        return getProsecutorDefendantProblem(caseDocumentWithReferenceData, caseDocumentWithReferenceData.getProsecutorDefendantId());
    }

    private ValidationResult getProsecutorDefendantProblem(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final String prosecutorDefendantId) {
        if (!DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory())) {
            return VALID;
        }
        final ValidationResult missingIdProblem = getProsecutorDefendantIdMissingProblem(prosecutorDefendantId);
        if (!missingIdProblem.isValid()) {
            return missingIdProblem;
        } else {
            Optional<Defendant> matchedDefendant = getDefendantFromProsecutorDefendantId(caseDocumentWithReferenceData.getDefendants(), caseDocumentWithReferenceData.getProsecutorDefendantId());
            if (!matchedDefendant.isPresent()) {
                matchedDefendant = getDefendantFromASN(caseDocumentWithReferenceData.getDefendants(), caseDocumentWithReferenceData.getProsecutorDefendantId());
            }
            matchedDefendant.ifPresent(defendant -> caseDocumentWithReferenceData.setDefendantId(fromString(defendant.getId())));
            return matchedDefendant.isPresent() ? VALID : newValidationResult(of(newProblem(DEFENDANT_ID_INVALID, PROSECUTOR_DEFENDANT_ID.getValue(), caseDocumentWithReferenceData.getProsecutorDefendantId())));
        }
    }

    private ValidationResult getProsecutorDefendantIdMissingProblem(final String prosecutorDefendantId) {
        return isEmpty(prosecutorDefendantId) ?
                newValidationResult(of(newProblem(DEFENDANT_ID_REQUIRED, PROSECUTOR_DEFENDANT_ID.getValue(), ""))) :
                VALID;
    }


    private Optional<Defendant> getDefendantFromProsecutorDefendantId(final List<Defendant> defendants, final String prosecutorDefendantId) {
        return defendants.stream()
                .filter(defendant -> prosecutorDefendantId.equalsIgnoreCase(defendant.getProsecutorDefendantReference()))
                .findFirst();
    }

    private Optional<Defendant> getDefendantFromASN(final List<Defendant> defendants, final String prosecutorDefendantId) {
        return defendants.stream()
                .filter(defendant -> prosecutorDefendantId.equalsIgnoreCase(defendant.getAsn()))
                .findFirst();
    }
}
