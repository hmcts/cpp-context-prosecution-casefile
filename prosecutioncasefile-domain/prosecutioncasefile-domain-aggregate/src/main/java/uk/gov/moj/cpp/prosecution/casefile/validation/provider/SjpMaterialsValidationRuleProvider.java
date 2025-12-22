package uk.gov.moj.cpp.prosecution.casefile.validation.provider;

import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CourtReferralCreatedValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpCaseInSessionValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpDocumentTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpFileTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public class SjpMaterialsValidationRuleProvider {

    private static final List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> REJECTION_RULES = unmodifiableList(asList(
            new SjpFileTypeValidationRule(),
            new SjpDocumentTypeValidationRule(),
            new CourtReferralCreatedValidationRule(),
            new SjpCaseInSessionValidationRule()));

    private SjpMaterialsValidationRuleProvider() {
    }

    public static List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> getRejectionRules() {
        return REJECTION_RULES;
    }

}
