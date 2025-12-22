package uk.gov.moj.cpp.prosecution.casefile.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.accesscontrol.RuleConstants.getInitiateCCProsecutionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

class InitiateCcProsecutionTest extends BaseDroolsAccessControlTest {

    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION = "prosecutioncasefile.command.initiate-cc-prosecution";

    public InitiateCcProsecutionTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    void shouldAllowUserInAuthorisedGroupToInitiateSjpProsecution() {
        final Action action = createActionFor(PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getInitiateCCProsecutionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    void shouldNotAllowUserNotInAuthorisedGroupToInitiateSjpProsecution() {
        final Action action = createActionFor(PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
