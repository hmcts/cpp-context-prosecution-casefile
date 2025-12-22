package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static java.util.UUID.randomUUID;
import static org.activiti.engine.impl.test.JobTestHelper.waitForJobExecutorToProcessAllJobs;
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
public class BulkScanMaterialExpirationTest {

    private static final String TIMEOUT_PROCESS_PATH = "processes/bulkScanMaterialExpired.bpmn20.xml";

    private UUID caseId, fileStoreId;
    private BulkScanMaterialExpiration bulkScanMaterialExpiration;
    private JavaDelegate bulkScanPendingMaterialExpiredDelegate;
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
        bulkScanMaterialExpiration = new BulkScanMaterialExpiration(rule.getRuntimeService(), Duration.ofSeconds(1).toString());
        bulkScanPendingMaterialExpiredDelegate = (JavaDelegate) configuration.getBeans().get("bulkScanPendingMaterialExpiredDelegate");
        reset(bulkScanPendingMaterialExpiredDelegate);
    }

    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldTriggerTimeoutTaskAfterTimeout() throws Exception {
        final ProcessInstance processInstance = bulkScanMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);

        waitForAllJobs();

        verify(bulkScanPendingMaterialExpiredDelegate).execute(delegateExecutionCaptor.capture());

        final DelegateExecution delegateExecution = delegateExecutionCaptor.getValue();

        assertThat(processInstance.getBusinessKey(), is(fileStoreId.toString()));
        assertThat(delegateExecution.getVariable("metadata"), is(MetadataHelper.metadataToString(metadata)));
        assertThat(delegateExecution.getVariable("caseId"), is(caseId));
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }


    @Test
    @Deployment(resources = {TIMEOUT_PROCESS_PATH})
    public void shouldCancelAssignmentTimer() {
        final ProcessInstance processInstance = bulkScanMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);

        verifyNoInteractions(bulkScanPendingMaterialExpiredDelegate);
        assertThat(isProcessFinished(processInstance), equalTo(true));
    }

    private boolean isProcessFinished(final ProcessInstance processInstance) {
        final ProcessInstance refreshedProcessInstance = rule.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .singleResult();
        return refreshedProcessInstance == null;
    }

    private void waitForAllJobs() {
        waitForJobExecutorToProcessAllJobs(rule, 10000, 500);
    }
}
