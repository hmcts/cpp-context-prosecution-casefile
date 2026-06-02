package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.BulkScanMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseFiltered;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseFilteredEventProcessor {

    @Inject
    private BulkScanMaterialExpiration bulkScanMaterialExpiration;

    @Inject
    private PendingMaterialExpiration pendingMaterialExpiration;

    @Inject
    private Sender sender;

    @Handles("public.stagingprosecutorsspi.event.prosecution-case-filtered")
    public void handleProsecutionCaseFiltered(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.filter-prosecution-case")
                .build();

        sender.send(envelopeFrom(metadata, createObjectBuilder()
                .add("caseId", payload.getString("caseId"))
                .build()));
    }

    @Handles("prosecutioncasefile.events.case-filtered")
    public void handleFilteredCase(final Envelope<CaseFiltered> pendingMaterialsEnvelope) {
        final CaseFiltered caseFiltered = pendingMaterialsEnvelope.payload();

        final List<Material> materials = caseFiltered.getMaterials();

        materials.forEach(material -> {
            pendingMaterialExpiration.cancelMaterialTimer(material.getFileStoreId());
            bulkScanMaterialExpiration.cancelMaterialTimer(material.getFileStoreId());
        });
    }

}
