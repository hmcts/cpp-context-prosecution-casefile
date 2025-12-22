package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import uk.gov.moj.cpp.prosecution.casefile.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Collections;
import java.util.List;

import static uk.gov.moj.cpp.prosecution.casefile.CaseType.CC;
import static uk.gov.moj.cpp.prosecution.casefile.CaseType.SJP;

public class MaterialValidationRuleProvider {

    private MaterialValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules(final CaseType caseType) {
        if (caseType == SJP) {
            return SjpMaterialsValidationRuleProvider.getRejectionRules();
        } else if (caseType == CC) {
            return CCMaterialsValidationRuleProvider.getRejectionRules();
        } else {
            return Collections.emptyList();
        }
    }
}
