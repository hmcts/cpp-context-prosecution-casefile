package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CaseEjectValidationRule;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public class CCMaterialsValidationRuleProvider {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> REJECTION_RULES = unmodifiableList(asList(
            new CCDocumentTypeValidationRule(),
            new CCDocumentDefendantLevelValidationRule(),
            new CaseEjectValidationRule()
    ));

    private CCMaterialsValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules() {
        return REJECTION_RULES;
    }

}
