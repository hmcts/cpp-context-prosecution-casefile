package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.warning.DefendantWarningsValidationRule;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class CCMaterialsWarningRuleProvider {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> WARNING_RULES =
            ImmutableList.of(DefendantWarningsValidationRule.INSTANCE);

    private CCMaterialsWarningRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getWarningRules() {
        return WARNING_RULES;
    }
}
