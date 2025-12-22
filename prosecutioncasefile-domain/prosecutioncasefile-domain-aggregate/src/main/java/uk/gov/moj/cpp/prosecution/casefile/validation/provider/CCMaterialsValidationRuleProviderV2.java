package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.ApplicationValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelValidationRuleForPendingV2;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelValidationRuleV2;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelWithOrganisationValidationRuleV2;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.FileTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.OuCodeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.ProsecutorValidationRule;

import java.util.List;

public class CCMaterialsValidationRuleProviderV2 {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> REJECTION_RULES = unmodifiableList(asList(
            new CCDocumentTypeValidationRule(),
            new CCDocumentDefendantLevelValidationRuleV2(),
            new CCDocumentDefendantLevelWithOrganisationValidationRuleV2(),
            new FileTypeValidationRule(),
            new ProsecutorValidationRule(),
            new OuCodeValidationRule(),
            new ApplicationValidationRule()
    ));

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> PENDING_RULES = unmodifiableList(asList(
            new CCDocumentDefendantLevelValidationRuleForPendingV2(),
            new CCDocumentDefendantLevelWithOrganisationValidationRuleForPendingV2()
    ));

    private CCMaterialsValidationRuleProviderV2() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules() {
        return REJECTION_RULES;
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getPendingRules() {
        return PENDING_RULES;
    }
}
