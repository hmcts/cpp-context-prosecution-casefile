package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.matchOrganisationsInDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ON_CP;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public class CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2 implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (!isBlank(caseDocumentWithReferenceData.getDocumentCategory())) {
            return getDefendantProblem(caseDocumentWithReferenceData);
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

        return getDefendantProblem(caseDocumentWithReferenceData);
    }

    private ValidationResult getDefendantProblem(final CaseDocumentWithReferenceData caseDocumentWithReferenceData) {
        if (!DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(caseDocumentWithReferenceData.getDocumentCategory())) {
            return VALID;
        }

        if(isDefendantNotOrganisation(caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject())){
            return VALID;
        }

        final DefendantSubject defendantSubject = caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject();

        return matchesOrganisationInformation(caseDocumentWithReferenceData, defendantSubject);
    }

    private ValidationResult matchesOrganisationInformation(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final DefendantSubject defendantSubject) {
        final List<Defendant> matchedDefendants = new ArrayList<>();
        final List<ProblemValue> problemValues = matchOrganisationsInDefendants(caseDocumentWithReferenceData, defendantSubject, matchedDefendants);
        if(! matchedDefendants.isEmpty()){
            return VALID;
        }else{
            return newValidationResult(Optional.of(newProblem(DEFENDANT_ON_CP, problemValues)));
        }
    }

    private boolean isDefendantNotOrganisation(final DefendantSubject defendantSubject) {
        return defendantSubject.getProsecutorOrganisationDefendantDetails() == null && defendantSubject.getCpsOrganisationDefendantDetails() == null;
    }

}
