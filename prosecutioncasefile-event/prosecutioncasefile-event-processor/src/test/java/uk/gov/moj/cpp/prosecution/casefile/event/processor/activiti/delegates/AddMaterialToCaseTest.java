package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;

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
public class AddMaterialToCaseTest {

    @InjectMocks
    private AddMaterialToCase addMaterialToCaseTask;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private Sender sender;

    @Spy
    private MetadataHelper metadataHelper;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void addsDocumentToACaseWhenMaterialAdded() {
        final Metadata originalMetadata = metadataBuilder().withId(randomUUID()).withName("name").build();
        final String originalMetadataString = metadataHelper.metadataToString(originalMetadata);
        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(originalMetadataString);

        final UUID caseId = randomUUID();
        when(delegateExecution.getVariable("caseId", String.class)).thenReturn(caseId.toString());

        final UUID documentReference = randomUUID();
        when(delegateExecution.getVariable("documentReference", String.class)).thenReturn(documentReference.toString());

        final String documentType = "PLEA";
        when(delegateExecution.getVariable("documentType", String.class)).thenReturn(documentType);

        final UUID materialId = randomUUID();
        when(delegateExecution.getVariable("materialId", String.class)).thenReturn(materialId.toString());

        final UUID defendantId = randomUUID();
        when(delegateExecution.getVariable("defendantId", String.class)).thenReturn(defendantId.toString());

        addMaterialToCaseTask.execute(delegateExecution);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope capturedAddDocumentCommandEnvelope = envelopeCaptor.getValue();
        assertThat(capturedAddDocumentCommandEnvelope, jsonEnvelope(
                metadata()
                        .withName("prosecutioncasefile.command.add-defendant-idpc"),
                payloadIsJson(allOf(
                        withJsonPath("$.id", is(documentReference.toString())),
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.materialId", is(materialId.toString())),
                        withJsonPath("$.documentType", is(documentType)),
                        withJsonPath("$.defendantId", is(defendantId.toString()))
                ))
        ));

    }
}
