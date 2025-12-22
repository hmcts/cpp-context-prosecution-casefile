package uk.gov.moj.cpp.prosecution.casefile.accesscontrol;

import static org.mockito.BDDMockito.given;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.accesscontrol.RuleConstants.getInitiateSjpProsecutionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class InitiateSjpProsecutionTest extends BaseDroolsAccessControlTest {

    private static final String PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION = "prosecutioncasefile.command.initiate-sjp-prosecution";

    public InitiateSjpProsecutionTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Test
    public void shouldAllowUserInAuthorisedGroupToInitiateSjpProsecution() {
        final Action action = createActionFor(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getInitiateSjpProsecutionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUserNotInAuthorisedGroupToInitiateSjpProsecution() {
        final Action action = createActionFor(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION);
        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
