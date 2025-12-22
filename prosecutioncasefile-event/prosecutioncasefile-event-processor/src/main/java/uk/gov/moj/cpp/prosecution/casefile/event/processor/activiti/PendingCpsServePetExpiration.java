package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.messaging.Metadata;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;

@Named
public class PendingCpsServePetExpiration {

    private static final Logger LOGGER = getLogger(PendingCpsServePetExpiration.class);
    private static final String PENDING_CPS_SERVE_PET_EXPIRATION_PROCESS_NAME = "pendingCpsServePetExpiration";

    private Duration servePet;
    private RuntimeService runtimeService;

    @Inject
    public PendingCpsServePetExpiration(final RuntimeService runtimeService, @Value(key = "cps.serve.pet.expiration", defaultValue = "P28D") String servePetExpiration) {
        this.runtimeService = runtimeService;
        servePet = Duration.parse(servePetExpiration);
    }

    public ProcessInstance startCpsServePetTimer(final UUID timerUUID, final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put("expirationPeriod", servePet.toString());
        params.put("metadata", metadataToString(metadata));

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PENDING_CPS_SERVE_PET_EXPIRATION_PROCESS_NAME, timerUUID.toString(), params);

        LOGGER.info("Pending cps serve pet expiration timeout started for cps serve pet with timerUUID {}", timerUUID);

        return processInstance;
    }

    public void cancelCpsServePetTimer(final UUID timerUUID) {
        final List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processDefinitionKey(PENDING_CPS_SERVE_PET_EXPIRATION_PROCESS_NAME)
                .processInstanceBusinessKey(timerUUID.toString())
                .list();

        if (isNotEmpty(executions)) {
            executions.forEach(processInstance ->
                    runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "Timeout cancelled"));
        }

        LOGGER.info("Pending cps serve pet expiration timeout cancelled for cps serve pet with timerUUID  {}", timerUUID);
    }
}
