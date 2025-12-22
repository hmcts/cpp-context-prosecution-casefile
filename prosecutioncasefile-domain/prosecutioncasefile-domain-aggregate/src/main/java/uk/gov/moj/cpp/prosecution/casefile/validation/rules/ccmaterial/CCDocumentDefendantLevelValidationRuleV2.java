package uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.getCpsDefendantId;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.getDefendantId;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.getProsecutorDefendantId;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.matchDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ID_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_ID_REQUIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATE_DEFENDANT;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.PROSECUTOR_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


import java.util.ArrayList;
import java.util.UUID;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;
import java.util.Optional;

public class CCDocumentDefendantLevelValidationRuleV2 implements ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService> {

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
        if(caseDocumentWithReferenceData.getProsecutionCaseSubject() == null || caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject() == null){
            return newValidationResult(of(newProblem(DEFENDANT_ID_REQUIRED, PROSECUTOR_DEFENDANT_ID.getValue(), "")));
        }

        if(isDefendantOrganisation(caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject())){
            return VALID;
        }

        if(caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject().getAsn() != null){
            return getValidationResultForASN(caseDocumentWithReferenceData);
        }

        final ValidationResult missingIdProblem = getDefendantIdMissingProblem(caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject());
        if (!missingIdProblem.isValid()) {
            return missingIdProblem;
        }

        final String defendantId = getDefendantId(caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject());

        final DefendantSubject defendantSubject = caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject();
        if(defendantSubject.getCpsPersonDefendantDetails() == null && defendantSubject.getProsecutorPersonDefendantDetails() == null){
            final UUID caseDefendantId = caseDocumentWithReferenceData.getValidDefendantIds().get(defendantId);
            if(caseDefendantId != null) {
                caseDocumentWithReferenceData.setDefendantId(caseDefendantId);
            }
            return VALID;
        }else{
            return matchesPersonalInformation(caseDocumentWithReferenceData, defendantSubject, defendantId);
        }
    }

    private ValidationResult getValidationResultForASN(final CaseDocumentWithReferenceData caseDocumentWithReferenceData) {
        final Optional<Defendant> matchedDefendant = getDefendantFromASN(caseDocumentWithReferenceData.getDefendants(), caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject().getAsn());
        if (matchedDefendant.isPresent()){
            matchedDefendant.ifPresent(defendant -> caseDocumentWithReferenceData.setDefendantId(fromString(defendant.getId())));
        }else{
            return newValidationResult(of(newProblem(DEFENDANT_ID_INVALID, ASN.getValue(), caseDocumentWithReferenceData.getProsecutionCaseSubject().getDefendantSubject().getAsn())));
        }
        return VALID;
    }

    private boolean isDefendantOrganisation(final DefendantSubject defendantSubject) {
        return defendantSubject.getProsecutorOrganisationDefendantDetails() != null || defendantSubject.getCpsOrganisationDefendantDetails() != null;
    }

    private ValidationResult matchesPersonalInformation(final CaseDocumentWithReferenceData caseDocumentWithReferenceData, final DefendantSubject defendantSubject, final String defendantId) {
        final List<Defendant> matchedDefendants = new ArrayList<>();
        final List<ProblemValue> problemValues = matchDefendants(caseDocumentWithReferenceData, defendantSubject, matchedDefendants);
        if(matchedDefendants.size() >  1){
            problemValues.clear();
            return newValidationResult(of(newProblem(DUPLICATE_DEFENDANT, PROSECUTOR_DEFENDANT_ID.getValue(), defendantId)));
        }else if(matchedDefendants.size() ==  1){
            caseDocumentWithReferenceData.setDefendantId(fromString(matchedDefendants.get(0).getId()));
        }
        return VALID;
    }

    private Optional<Defendant> getDefendantFromASN(final List<Defendant> defendants, final String asn) {
        if (isBlank(asn)) {
            return empty();
        }
        return defendants.stream()
                .filter(defendant -> asn.equals(defendant.getAsn()))
                .findFirst();
    }

    private ValidationResult getDefendantIdMissingProblem(final DefendantSubject defendantSubject) {
        return !getProsecutorDefendantId(defendantSubject).isPresent() && !getCpsDefendantId(defendantSubject).isPresent() ?
                newValidationResult(of(newProblem(DEFENDANT_ID_REQUIRED, PROSECUTOR_DEFENDANT_ID.getValue(), ""))) :
                VALID;
    }



}
