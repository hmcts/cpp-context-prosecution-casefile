package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

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
public class BulkScanPendingMaterialExpiredDelegateTest {

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private BulkScanPendingMaterialExpiredDelegate bulkScanPendingMaterialExpiredDelegate;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void sendsExpirePendingMaterialCommand() {
        final UUID caseId = randomUUID();
        final UUID fileStoreId = randomUUID();
        final Metadata originalMetadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(fileStoreId.toString());
        when(delegateExecution.getVariable("caseId", UUID.class)).thenReturn(caseId);
        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(metadataToString(originalMetadata));

        bulkScanPendingMaterialExpiredDelegate.execute(delegateExecution);

        verify(sender).sendAsAdmin(envelopeCaptor.capture());

        final JsonEnvelope capturedAddDocumentCommandEnvelope = envelopeCaptor.getValue();

        assertThat(capturedAddDocumentCommandEnvelope,
                jsonEnvelope(
                        metadata().withName("prosecutioncasefile.command.expire-bulk-scan-pending-material"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.fileStoreId", equalTo(fileStoreId.toString())))
                        )));
    }

}
