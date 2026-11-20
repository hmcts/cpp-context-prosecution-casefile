package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
public class PendingMaterialExpirationTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/pendingMaterialExpired.bpmn20.xml";

    private UUID caseId, fileStoreId;
    private PendingMaterialExpiration pendingMaterialExpiration;
    private JavaDelegate pendingMaterialExpiredDelegate;
    private Metadata metadata;

    @RegisterExtension
    public final ActivitiJUnit5Extension activitiExtension = new ActivitiJUnit5Extension();

    @Captor
    private ArgumentCaptor<DelegateExecution> delegateExecutionCaptor;

    @BeforeEach
    public void init() {
        caseId = randomUUID();
        fileStoreId = randomUUID();
        metadata = metadataWithRandomUUID("test").build();
        pendingMaterialExpiration = new PendingMaterialExpiration(activitiExtension.getRuntimeService(), Duration.ofSeconds(1).toString());
        pendingMaterialExpiredDelegate = (JavaDelegate) activitiExtension.getProcessEngineConfiguration().getBeans().get("pendingMaterialExpiredDelegate");
        reset(pendingMaterialExpiredDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {
        final ProcessInstance processInstance = pendingMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);

        waitForAllJobs();

        verify(pendingMaterialExpiredDelegate).execute(delegateExecutionCaptor.capture());

        final DelegateExecution delegateExecution = delegateExecutionCaptor.getValue();

        assertThat(processInstance.getBusinessKey(), is(fileStoreId.toString()));
        assertThat(delegateExecution.getVariable("metadata"), is(MetadataHelper.metadataToString(metadata)));
        assertThat(delegateExecution.getVariable("caseId"), is(caseId));
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldCancelAssignmentTimer() {
        final ProcessInstance processInstance = pendingMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);

        verifyNoInteractions(pendingMaterialExpiredDelegate);
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    private void waitForAllJobs() {
        activitiExtension.waitForJobExecutorToProcessAllJobs(10000, 500);
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        return activitiExtension.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult() == null;
    }
}
