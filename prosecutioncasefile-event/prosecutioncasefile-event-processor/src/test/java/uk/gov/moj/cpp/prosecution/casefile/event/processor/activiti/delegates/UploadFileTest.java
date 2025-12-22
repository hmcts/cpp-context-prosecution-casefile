package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.UuidStringMatcher.isAUuid;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;

import java.util.Optional;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UploadFileTest {

    @InjectMocks
    private UploadFile uploadFileTask;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private Sender sender;

    @Spy
    private MetadataHelper metadataHelper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void sendsMaterialAndRaisesPublicEvent() {
        final Metadata originalMetadata = metadataWithRandomUUIDAndName().build();
        final UUID documentReference = randomUUID();
        final UUID caseId = randomUUID();
        final String originalMetadataString = metadataToString(originalMetadata);
        final String executionId = "executionId";

        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(originalMetadataString);
        when(delegateExecution.getVariable("documentReference", String.class)).thenReturn(documentReference.toString());
        when(delegateExecution.getId()).thenReturn(executionId);

        uploadFileTask.execute(delegateExecution);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope capturedAddMaterialCommand = envelopeCaptor.getAllValues().get(0);
        assertThat(capturedAddMaterialCommand, jsonEnvelope(
                metadata()
                        .withName("material.command.upload-file"),
                payloadIsJson(allOf(
                        withJsonPath("$.fileServiceId", is(documentReference.toString())),
                        withJsonPath("$.materialId", isAUuid())
                ))
        ));
        final Optional<String> idpcProcessId = metadataHelper.getIdpcProcessId(capturedAddMaterialCommand);
        assertTrue(idpcProcessId.isPresent());
        assertThat(idpcProcessId.get(), is(executionId));
    }

}