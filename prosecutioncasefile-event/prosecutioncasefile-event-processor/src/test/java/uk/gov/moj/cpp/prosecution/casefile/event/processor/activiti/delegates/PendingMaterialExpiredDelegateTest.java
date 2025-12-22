package uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.delegates;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataToString;

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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PendingMaterialExpiredDelegateTest {

    @Mock
    private Sender sender;

    @Mock
    private DelegateExecution delegateExecution;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @InjectMocks
    private PendingMaterialExpiredDelegate pendingMaterialExpiredDelegate;

    @Test
    public void sendsExpirePendingMaterialCommand() {
        final UUID caseId = randomUUID();
        final UUID fileStoreId = randomUUID();
        final Metadata originalMetadata = metadataWithRandomUUIDAndName().build();

        when(delegateExecution.getProcessBusinessKey()).thenReturn(fileStoreId.toString());
        when(delegateExecution.getVariable("caseId", UUID.class)).thenReturn(caseId);
        when(delegateExecution.getVariable("metadata", String.class)).thenReturn(metadataToString(originalMetadata));

        pendingMaterialExpiredDelegate.execute(delegateExecution);

        verify(sender).sendAsAdmin(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(jsonEnvelope.metadata().name(), is("prosecutioncasefile.command.expire-pending-material"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("fileStoreId"), is(fileStoreId.toString()));
    }

}
