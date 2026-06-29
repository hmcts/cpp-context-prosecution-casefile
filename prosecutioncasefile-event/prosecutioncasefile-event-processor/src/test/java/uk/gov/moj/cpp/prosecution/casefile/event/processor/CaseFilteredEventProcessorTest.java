package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataWithIdpcProcessId;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.BulkScanMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseFiltered;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseFilteredEventProcessorTest {

    @Mock
    private PendingMaterialExpiration pendingMaterialExpiration;

    @Mock
    private BulkScanMaterialExpiration bulkScanMaterialExpiration;

    @InjectMocks
    CaseFilteredEventProcessor caseFilteredEventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> captor;


    @Test
    public void shouldHandleFilterProsecutionCase() {
        final UUID caseId = randomUUID();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.stagingprosecutorsspi.event.prosecution-case-filtered").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        caseFilteredEventProcessor.handleProsecutionCaseFiltered(envelope);

        verify(sender, times(1)).send(captor.capture());

        final Envelope envelopeToSender = captor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.filter-prosecution-case"));
        assertThat(metadata.clientCorrelationId(), is(envelope.metadata().clientCorrelationId()));

        assertThat(envelopeToSender.payload(), is(notNullValue()));
        final String payload = envelopeToSender.payload().toString();
        assertThat(payload.contains(caseId.toString()), equalTo(true));
    }

    @Test
    public void handleFilteredCase() {

        final Envelope<CaseFiltered> event = createStopPendingMaterialsEvent();
        final CaseFiltered payload = event.payload();

        caseFilteredEventProcessor.handleFilteredCase(event);

        payload.getMaterials().stream().forEach(material -> {
            verify(pendingMaterialExpiration).cancelMaterialTimer(material.getFileStoreId());
            verify(bulkScanMaterialExpiration).cancelMaterialTimer(material.getFileStoreId());

        });
    }

    private static Envelope<CaseFiltered> createStopPendingMaterialsEvent() {

        List<Material> materials = new ArrayList<Material>();
        materials.add(randomMaterial(null));
        materials.add(randomMaterial(null));
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.event.stop-timer-for-pending-materials"),
                CaseFiltered.caseFiltered()
                        .withCaseId(randomUUID())
                        .withMaterials(materials)
                        .build());
    }

    private static Material randomMaterial(String documentType) {
        return material()
                .withFileStoreId(randomUUID())
                .withDocumentType(nonNull(documentType) ? documentType : randomAlphanumeric(5))
                .withFileType(randomAlphanumeric(5))
                .build();
    }
}