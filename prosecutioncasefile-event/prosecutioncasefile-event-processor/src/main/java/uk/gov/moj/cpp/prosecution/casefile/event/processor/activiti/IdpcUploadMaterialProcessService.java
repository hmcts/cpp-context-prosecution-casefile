package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti;

import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
public class IdpcUploadMaterialProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpcUploadMaterialProcessService.class);

    private static final String UPLOAD_FILE_PROCESS_NAME = "upload-file";
    private static final String WAIT_MATERIAL_ADDED_EVENT_NAME = "wait-for-material-added";

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private MetadataHelper metadataHelper;

    public void startUploadFileProcess(final Envelope<IdpcDefendantMatched> idpcMaterialMatched) {
        final IdpcDefendantMatched payload = idpcMaterialMatched.payload();
        final Map<String, Object> processVariables = new HashMap<>();
        final UUID documentReference = payload.getFileServiceId();
        final UUID caseId = payload.getCaseId();
        final String documentType = payload.getMaterialType();
        processVariables.put("metadata", metadataToString(idpcMaterialMatched.metadata()));
        processVariables.put("caseId", caseId.toString());
        processVariables.put("documentReference", documentReference.toString());
        processVariables.put("documentType", documentType);
        processVariables.put("defendantId", payload.getDefendantId());

        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(UPLOAD_FILE_PROCESS_NAME, processVariables);

        LOGGER.info("{} process started with id {} for case {} and document type {}",
                UPLOAD_FILE_PROCESS_NAME, processInstance.getId(), caseId, documentType);
    }

    public String signalUploadFileProcess(final JsonEnvelope idpcMaterialAdded, final String processId, final UUID materialId) {
        final Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processId)
                .activityId(WAIT_MATERIAL_ADDED_EVENT_NAME)
                .singleResult();

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("metadata", metadataToString(idpcMaterialAdded.metadata()));
        processVariables.put("materialId", materialId.toString());

        final String fileServiceId = runtimeService.getVariable(execution.getId(), "documentReference").toString();

        runtimeService.signal(execution.getId(), processVariables);
        LOGGER.info("{} process with id {} signaled with event {} with materialId {}",
                UPLOAD_FILE_PROCESS_NAME, execution.getId(), WAIT_MATERIAL_ADDED_EVENT_NAME, materialId);

        return fileServiceId;
    }

}
