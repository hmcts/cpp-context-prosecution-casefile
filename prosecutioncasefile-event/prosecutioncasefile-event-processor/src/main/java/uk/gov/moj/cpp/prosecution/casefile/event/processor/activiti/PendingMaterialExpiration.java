package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.messaging.Metadata;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;

@Named
public class PendingMaterialExpiration {

    private static final Logger LOGGER = getLogger(PendingMaterialExpiration.class);
    private static final String PENDING_MATERIAL_EXPIRATION_PROCESS_NAME = "pendingMaterialExpiration";

    private Duration materialExpirationDuration;
    private RuntimeService runtimeService;

    @Inject
    public PendingMaterialExpiration(final RuntimeService runtimeService, @Value(key = "material.expiration", defaultValue = "P28D") String materialExpiration) {
        this.runtimeService = runtimeService;
        materialExpirationDuration = Duration.parse(materialExpiration);
    }

    public ProcessInstance startMaterialTimer(final UUID fileStoreId, final UUID caseId, final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put("expirationPeriod", materialExpirationDuration.toString());
        params.put("caseId", caseId);
        params.put("metadata", metadataToString(metadata));

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, fileStoreId.toString(), params);

        LOGGER.info("Pending material expiration timeout started for material with fileStoreId {}", fileStoreId);

        return processInstance;
    }

    public void cancelMaterialTimer(final UUID fileStoreId) {
        runtimeService
                .createExecutionQuery()
                .processDefinitionKey(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME)
                .processInstanceBusinessKey(fileStoreId.toString())
                .list()
                .forEach(processInstance -> runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "Timeout cancelled"));

        LOGGER.info("Pending material expiration timeout cancelled for material with fileStoreId  {}", fileStoreId);
    }
}
