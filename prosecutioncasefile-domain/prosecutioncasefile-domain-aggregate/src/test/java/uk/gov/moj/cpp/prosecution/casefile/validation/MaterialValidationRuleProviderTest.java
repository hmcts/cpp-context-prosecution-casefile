package uk.gov.moj.cpp.prosecution.casefile.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import uk.gov.moj.cpp.prosecution.casefile.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.provider.MaterialValidationRuleProvider;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.CourtReferralCreatedValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpCaseInSessionValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpDocumentTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.SjpFileTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentDefendantLevelValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CCDocumentTypeValidationRule;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ccmaterial.CaseEjectValidationRule;

import java.util.List;

import org.junit.jupiter.api.Test;

public class MaterialValidationRuleProviderTest {

    @Test
    public void shouldProvideAllTheRequiredValidationRulesWhenTheCaseTypeIsSJP() {
        List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> validationRuleList = MaterialValidationRuleProvider.getRejectionRules(CaseType.SJP);

        assertThat(validationRuleList.size(), is(4));
        assertThat(validationRuleList, contains(instanceOf(SjpFileTypeValidationRule.class)
                , instanceOf(SjpDocumentTypeValidationRule.class)
                , instanceOf(CourtReferralCreatedValidationRule.class),
                instanceOf(SjpCaseInSessionValidationRule.class)));
    }

    @Test
    public void shouldProvideAllTheRequiredValidationRulesWhenTheCaseTypeIsCC() {
        List<ValidationRule<CaseDocumentWithReferenceData, ReferenceDataQueryService>> validationRuleList = MaterialValidationRuleProvider.getRejectionRules(CaseType.CC);

        assertThat(validationRuleList.size(), is(3));
        assertThat(validationRuleList, contains(instanceOf(CCDocumentTypeValidationRule.class)
                , instanceOf(CCDocumentDefendantLevelValidationRule.class),
                instanceOf(CaseEjectValidationRule.class)));
    }
}