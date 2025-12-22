package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.service.DocumentGeneratorService.GROUP_CASES_DEFENDANT_LIST_TEMPLATE_NAME;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorServiceTest {

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private MaterialService materialService;

    @Mock
    private SystemUserProvider systemUserProvider;

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private JsonEnvelope originatingEnvelope;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Captor
    ArgumentCaptor<JsonObject> fileStorerMetaDataCaptor;

    @Captor
    ArgumentCaptor<InputStream> fileStorerInputStreamCaptor;



    @Test
    public void shouldGenerateGroupCasesSummonsDocument() throws IOException, FileServiceException {
        final UUID materialId = randomUUID();
        final UUID systemUserId = randomUUID();
        final UUID fileId = randomUUID();
        final byte[] documentData = {34, 56, 78, 90};
        final JsonObject payload = createObjectBuilder().build();

        when(fileStorer.store(any(), any())).thenReturn(fileId);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(payload, GROUP_CASES_DEFENDANT_LIST_TEMPLATE_NAME, systemUserId))
                .thenReturn(documentData);

        documentGeneratorService.generateGroupCasesSummonsDocument(originatingEnvelope, payload, materialId);

        verify(fileStorer, times(1)).store(fileStorerMetaDataCaptor.capture(), fileStorerInputStreamCaptor.capture());

        byte[] dataSent = new byte[documentData.length];
        fileStorerInputStreamCaptor.getValue().read(dataSent, 0, documentData.length);
        assertThat(documentData, is(dataSent));
        verify(materialService, times(1)).uploadMaterial(fileId, materialId, originatingEnvelope);
    }
}