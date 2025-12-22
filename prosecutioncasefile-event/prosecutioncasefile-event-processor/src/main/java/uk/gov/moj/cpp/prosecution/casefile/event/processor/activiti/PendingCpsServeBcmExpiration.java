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
public class PendingCpsServeBcmExpiration {

    private static final Logger LOGGER = getLogger(PendingCpsServeBcmExpiration.class);
    private static final String PENDING_CPS_SERVE_BCM_EXPIRATION_PROCESS_NAME = "pendingCpsServeBcmExpiration";

    private Duration serveBcm;
    private RuntimeService runtimeService;

    @Inject
    public PendingCpsServeBcmExpiration(final RuntimeService runtimeService, @Value(key = "cps.serve.bcm.expiration", defaultValue = "P28D") String serveBcmExpiration) {
        this.runtimeService = runtimeService;
        serveBcm = Duration.parse(serveBcmExpiration);
    }

    public ProcessInstance startCpsServeBcmTimer(final UUID timerUUID, final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put("expirationPeriod", serveBcm.toString());
        params.put("metadata", metadataToString(metadata));

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PENDING_CPS_SERVE_BCM_EXPIRATION_PROCESS_NAME, timerUUID.toString(), params);

        LOGGER.info("Pending cps serve bcm expiration timeout started for cps serve bcm with timerUUID {}", timerUUID);

        return processInstance;
    }

    public void cancelCpsServeBcmTimer(final UUID timerUUID) {
        final List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processDefinitionKey(PENDING_CPS_SERVE_BCM_EXPIRATION_PROCESS_NAME)
                .processInstanceBusinessKey(timerUUID.toString())
                .list();

        if (isNotEmpty(executions)) {
            executions.forEach(processInstance ->
                    runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "Timeout cancelled"));
        }

        LOGGER.info("Pending cps serve bcm expiration timeout cancelled for cps serve bcm with timerUUID  {}", timerUUID);
    }
}
