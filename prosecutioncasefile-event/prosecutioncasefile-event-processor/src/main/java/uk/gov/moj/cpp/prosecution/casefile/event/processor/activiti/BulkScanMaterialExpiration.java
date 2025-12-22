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
public class BulkScanMaterialExpiration {

    private static final Logger LOGGER = getLogger(BulkScanMaterialExpiration.class);
    private static final String BULK_SCAN_MATERIAL_EXPIRATION_PROCESS = "bulkScanMaterialExpiration";

    private Duration materialExpirationDuration;
    private RuntimeService runtimeService;

    @Inject
    public BulkScanMaterialExpiration(final RuntimeService runtimeService, @Value(key = "bulkScan.material.expiration", defaultValue = "P5D") String materialExpiration) {
        this.runtimeService = runtimeService;
        materialExpirationDuration = Duration.parse(materialExpiration);
    }

    public ProcessInstance startMaterialTimer(final UUID fileStoreId, final UUID caseId, final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put("expirationPeriod", materialExpirationDuration.toString());
        params.put("caseId", caseId);
        params.put("metadata", metadataToString(metadata));

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(BULK_SCAN_MATERIAL_EXPIRATION_PROCESS, fileStoreId.toString(), params);

        LOGGER.info("BulkScan material expiration timeout started for material with fileStoreId {}", fileStoreId);

        return processInstance;
    }

    public void cancelMaterialTimer(final UUID fileStoreId) {
        runtimeService
                .createExecutionQuery()
                .processDefinitionKey(BULK_SCAN_MATERIAL_EXPIRATION_PROCESS)
                .processInstanceBusinessKey(fileStoreId.toString())
                .list()
                .forEach(processInstance -> runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "Timeout cancelled"));

        LOGGER.info("BulkScan material expiration timeout cancelled for material with fileStoreId  {}", fileStoreId);
    }
}
