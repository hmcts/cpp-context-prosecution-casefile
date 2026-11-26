package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.exception.DocumentGenerationException;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.exception.FileUploadException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentGeneratorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);

    public static final String GROUP_CASES_DEFENDANT_LIST_TEMPLATE_NAME = "RespondentList";
    public static final String GROUP_CASES_DEFENDANT_LIST_DOCUMENT_ORDER = "RespondentList";
    public static final String RESPONDENT_LIST_FILENAME = "RespondentList.pdf";

    @Inject
    private FileStorer fileStorer;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private MaterialService materialService;

    @Transactional(REQUIRES_NEW)
    public String generateGroupCasesSummonsDocument(final Envelope originatingEnvelope, final JsonObject payload, final UUID materialId) {
        try {
            LOGGER.info("Calling {} with materialId {}", GROUP_CASES_DEFENDANT_LIST_DOCUMENT_ORDER, materialId);
            final DocumentGeneratorClient documentGeneratorClient = this.documentGeneratorClientProducer.documentGeneratorClient();
            final byte[] resultOrderAsByteArray = documentGeneratorClient.generatePdfDocument(payload, GROUP_CASES_DEFENDANT_LIST_TEMPLATE_NAME, getSystemUserUuid());
            addDocumentToMaterial(originatingEnvelope, RESPONDENT_LIST_FILENAME, new ByteArrayInputStream(resultOrderAsByteArray), materialId);
            return RESPONDENT_LIST_FILENAME;
        } catch (final IOException | RuntimeException e) {
            throw new DocumentGenerationException(e);
        }
    }

    private void addDocumentToMaterial(final Envelope originatingEnvelope, final String filename, final InputStream fileContent, final UUID materialId) {
        try {
            final UUID fileId = storeFile(fileContent, filename);
            LOGGER.info("Stored material {} in file store {}", materialId, fileId);
            this.materialService.uploadMaterial(fileId, materialId, originatingEnvelope);
        } catch (final FileServiceException e) {
            LOGGER.error("Error while uploading file {}", filename);
            throw new FileUploadException(e);
        }
    }

    private UUID getSystemUserUuid() {
        return this.systemUserProvider.getContextSystemUserId().orElseThrow(() -> new RuntimeException("Could not find systemId "));
    }

    private UUID storeFile(final InputStream fileContent, final String fileName) throws FileServiceException {
        final JsonObject metadata = createObjectBuilder().add("fileName", fileName).build();
        return this.fileStorer.store(metadata, fileContent);
    }
}
