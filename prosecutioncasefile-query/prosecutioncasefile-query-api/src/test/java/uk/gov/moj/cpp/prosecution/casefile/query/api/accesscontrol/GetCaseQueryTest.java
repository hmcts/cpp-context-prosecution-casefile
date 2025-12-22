package uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_COURT_ADMINISTRATORS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_COURT_ASSOCIATE;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_COURT_CLERKS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_CROWN_COURT_ADMIN;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_LEGAL_ADVISERS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_LISTING_OFFICERS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_ONLINE_PLEA_SYSTEM_USERS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.GROUP_SYSTEM_USERS;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.getQueryCaseActionGroups;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.getQueryCaseByProsecutionReferenceActionGroups;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.getQueryCaseErrorsActionGroups;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.getQueryCaseForCitizenActionGroups;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.accesscontrol.RuleConstants.getQueryCountsCasesErrorsActionGroups;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class GetCaseQueryTest extends BaseDroolsAccessControlTest {
    public static final String PROSECUTIONCASEFILE_QUERY_CASES_ERRORS = "prosecutioncasefile.query.cases.errors";
    private static final String CONTENT_TYPE = "prosecutioncasefile.query.case";
    private static final String PROSECUTIONCASEFILE_QUERY_CASES_WITH_ERRORS_COUNT = "prosecutioncasefile.query.counts-cases-errors";
    private static final String PROSECUTIONCASEFILE_QUERY_CASE_FOR_CITIZEN = "prosecutioncasefile.query.case-for-citizen";
    private Action action;


    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;



    public GetCaseQueryTest(){
        super("QUERY_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @BeforeEach
    public void setUp() {
        action = createActionFor(CONTENT_TYPE);
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCase() {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQueryCaseActionGroups())).willReturn(true);
        assertSuccessfulOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class
        ), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(singletonList("System Users").toArray()));

    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCaseByProsecutionReference() throws Exception {
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getQueryCaseByProsecutionReferenceActionGroups())).willReturn(true);
        assertSuccessfulOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class
        ), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(singletonList("System Users").toArray()));

    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCaseErrors() throws Exception {
        final Action newAction = createActionFor(PROSECUTIONCASEFILE_QUERY_CASES_ERRORS);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(newAction, getQueryCaseErrorsActionGroups())).willReturn(true);
        assertSuccessfulOutcome(executeRulesWith(newAction));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class
        ), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(Arrays.asList(GROUP_SYSTEM_USERS, GROUP_CROWN_COURT_ADMIN, GROUP_LISTING_OFFICERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS).toArray()));

    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCaseErrorsCount() {
        final Action queryCasesCountAction = createActionFor(PROSECUTIONCASEFILE_QUERY_CASES_WITH_ERRORS_COUNT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(queryCasesCountAction, getQueryCountsCasesErrorsActionGroups())).willReturn(true);
        final ExecutionResults executionResults = executeRulesWith(queryCasesCountAction);
        assertSuccessfulOutcome(executionResults);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(Arrays.asList(GROUP_CROWN_COURT_ADMIN, GROUP_LEGAL_ADVISERS, GROUP_COURT_ASSOCIATE, GROUP_COURT_CLERKS, GROUP_COURT_ADMINISTRATORS).toArray()));
    }

    @Test
    public void shouldNotAllowUserInAuthorisedGroupToQueryCaseErrorsCount() {
        final Action queryCasesCountAction = createActionFor(PROSECUTIONCASEFILE_QUERY_CASES_WITH_ERRORS_COUNT);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(queryCasesCountAction, getQueryCountsCasesErrorsActionGroups())).willReturn(false);
        final ExecutionResults executionResults = executeRulesWith(queryCasesCountAction);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class), listCaptor.capture());
        assertFailureOutcome(executionResults);
    }

    @Test
    public void shouldAllowUserInAuthorisedGroupToQueryCaseForCitizen() {
        final Action queryCasesCountAction = createActionFor(PROSECUTIONCASEFILE_QUERY_CASE_FOR_CITIZEN);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(queryCasesCountAction, getQueryCaseForCitizenActionGroups())).willReturn(true);
        final ExecutionResults executionResults = executeRulesWith(queryCasesCountAction);
        assertSuccessfulOutcome(executionResults);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(any(Action.class), listCaptor.capture());
        assertThat(listCaptor.getValue(), containsInAnyOrder(singletonList(GROUP_ONLINE_PLEA_SYSTEM_USERS).toArray()));
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(userAndGroupProvider);
    }
}

