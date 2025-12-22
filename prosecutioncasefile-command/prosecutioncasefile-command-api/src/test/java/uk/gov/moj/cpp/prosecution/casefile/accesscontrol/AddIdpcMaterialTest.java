package uk.gov.moj.cpp.prosecution.casefile.accesscontrol;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.prosecution.casefile.command.api.accesscontrol.RuleConstants;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class AddIdpcMaterialTest extends BaseDroolsAccessControlTest {

    private static final String PROSECUTIONCASEFILE_COMMAND_ADD_IDPC_MATERIAL = "prosecutioncasefile.add-idpc-material";
    private static final List<String> ALLOWED_USER_GROUPS = RuleConstants.getAddMaterialIdpcActionGroups();

    private Action action;

    public AddIdpcMaterialTest() {
        super("COMMAND_API_SESSION");
    }

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() {
        action = createActionFor(PROSECUTIONCASEFILE_COMMAND_ADD_IDPC_MATERIAL);
    }

    @AfterEach
    public void tearDown() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS);
    }

    @Test
    public void whenUserIsAMemberOfAllowedUserGroups_thenSuccessfull() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, ALLOWED_USER_GROUPS)).thenReturn(true);

        ExecutionResults executionResults = executeRulesWith(action);

        assertSuccessfulOutcome(executionResults);
    }

    @Test
    public void whenUserIsNotAMemberOfAllowedUserGroups_thenFailure() {
        when(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action,
                ALLOWED_USER_GROUPS)).thenReturn(false);

        ExecutionResults executionResults = executeRulesWith(action);

        assertFailureOutcome(executionResults);
    }
}
