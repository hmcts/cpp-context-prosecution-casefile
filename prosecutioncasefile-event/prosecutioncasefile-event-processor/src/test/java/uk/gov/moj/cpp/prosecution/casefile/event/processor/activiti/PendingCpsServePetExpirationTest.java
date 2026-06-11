package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static java.util.UUID.randomUUID;
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
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PendingCpsServePetExpirationTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/pendingCpsServePetExpired.bpmn20.xml";

    private UUID timerUUID;
    private PendingCpsServePetExpiration pendingCpsServePetExpiration;
    private JavaDelegate pendingCpsServePetExpiredDelegate;
    private Metadata metadata;

    @RegisterExtension
    public final ActivitiJUnit5Extension activitiExtension = new ActivitiJUnit5Extension();

    @Captor
    private ArgumentCaptor<DelegateExecution> delegateExecutionCaptor;

    @BeforeEach
    public void init() {
        timerUUID = randomUUID();
        metadata = metadataWithRandomUUID("test").build();
        pendingCpsServePetExpiration = new PendingCpsServePetExpiration(activitiExtension.getRuntimeService(), Duration.ofSeconds(1).toString());
        pendingCpsServePetExpiredDelegate = (JavaDelegate) activitiExtension.getProcessEngineConfiguration().getBeans().get("pendingCpsServePetExpiredDelegate");
        reset(pendingCpsServePetExpiredDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {
        final ProcessInstance processInstance = pendingCpsServePetExpiration.startCpsServePetTimer(timerUUID, metadata);

        waitForAllJobs();

        verify(pendingCpsServePetExpiredDelegate).execute(delegateExecutionCaptor.capture());

        final DelegateExecution delegateExecution = delegateExecutionCaptor.getValue();

        assertThat(processInstance.getBusinessKey(), is(timerUUID.toString()));
        assertThat(delegateExecution.getVariable("metadata"), is(MetadataHelper.metadataToString(metadata)));
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    private void waitForAllJobs() {
        activitiExtension.waitForJobExecutorToProcessAllJobs(10000, 500);
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        final ProcessInstance refreshedProcessInstance = activitiExtension.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();
        return refreshedProcessInstance == null;
    }
}
