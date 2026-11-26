package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static java.util.UUID.randomUUID;
import static org.activiti.engine.impl.test.JobTestHelper.waitForJobExecutorToProcessAllJobs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;

import java.time.Duration;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PendingIdpcMaterialExpirationTest {
    private static final String TIMEOUT_PROCESS_PATH = "processes/pendingIdpcMaterialExpired.bpmn20.xml";

    private UUID caseId, fileStoreId;
    private PendingIdpcMaterialExpiration pendingIdpcMaterialExpiration;
    private JavaDelegate pendingIdpcMaterialExpiredDelegate;
    private Metadata metadata;

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    @Captor
    private ArgumentCaptor<DelegateExecution> delegateExecutionCaptor;

    @Before
    public void init() {
        final StandaloneProcessEngineConfiguration configuration = (StandaloneProcessEngineConfiguration) rule.getProcessEngine().getProcessEngineConfiguration();
        caseId = randomUUID();
        fileStoreId = randomUUID();
        metadata = metadataWithRandomUUID("test").build();
        pendingIdpcMaterialExpiration = new PendingIdpcMaterialExpiration(rule.getRuntimeService(), Duration.ofSeconds(1).toString());
        pendingIdpcMaterialExpiredDelegate = (JavaDelegate) configuration.getBeans().get("pendingIdpcMaterialExpiredDelegate");
        reset(pendingIdpcMaterialExpiredDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {
        final ProcessInstance processInstance = pendingIdpcMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);

        waitForAllJobs();

        verify(pendingIdpcMaterialExpiredDelegate).execute(delegateExecutionCaptor.capture());

        final DelegateExecution delegateExecution = delegateExecutionCaptor.getValue();

        assertThat(processInstance.getBusinessKey(), is(fileStoreId.toString()));
        assertThat(delegateExecution.getVariable("metadata"), is(MetadataHelper.metadataToString(metadata)));
        assertThat(delegateExecution.getVariable("caseId"), is(caseId));
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    private void waitForAllJobs() {
        waitForJobExecutorToProcessAllJobs(rule, 10000, 500);
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        final ProcessInstance refreshedProcessInstance = rule.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();
        return refreshedProcessInstance == null;
    }

}
