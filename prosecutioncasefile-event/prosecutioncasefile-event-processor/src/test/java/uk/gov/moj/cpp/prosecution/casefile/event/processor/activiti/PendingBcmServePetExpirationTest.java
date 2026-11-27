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
public class PendingBcmServePetExpirationTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/pendingCpsServeBcmExpired.bpmn20.xml";

    private UUID timerUUID;
    private PendingCpsServeBcmExpiration pendingCpsServeBcmExpiration;
    private JavaDelegate pendingCpsServeBcmExpiredDelegate;
    private Metadata metadata;

    @Rule
    public ActivitiRule rule = new ActivitiRule();

    @Captor
    private ArgumentCaptor<DelegateExecution> delegateExecutionCaptor;

    @Before
    public void init() {
        final StandaloneProcessEngineConfiguration configuration = (StandaloneProcessEngineConfiguration) rule.getProcessEngine().getProcessEngineConfiguration();
        timerUUID = randomUUID();
        metadata = metadataWithRandomUUID("test").build();
        pendingCpsServeBcmExpiration = new PendingCpsServeBcmExpiration(rule.getRuntimeService(), Duration.ofSeconds(1).toString());
        pendingCpsServeBcmExpiredDelegate = (JavaDelegate) configuration.getBeans().get("pendingCpsServeBcmExpiredDelegate");
        reset(pendingCpsServeBcmExpiredDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {
        final ProcessInstance processInstance = pendingCpsServeBcmExpiration.startCpsServeBcmTimer(timerUUID,metadata);

        waitForAllJobs();

        verify(pendingCpsServeBcmExpiredDelegate).execute(delegateExecutionCaptor.capture());

        final DelegateExecution delegateExecution = delegateExecutionCaptor.getValue();

        assertThat(processInstance.getBusinessKey(), is(timerUUID.toString()));
        assertThat(delegateExecution.getVariable("metadata"), is(MetadataHelper.metadataToString(metadata)));
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
