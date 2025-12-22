package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingMaterial.expirePendingMaterial;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected.materialRejected;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingMaterial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ExpirePendingMaterialHandlerTest {

    private static final String EXPIRE_PENDING_MATERIAL_COMMAND = "prosecutioncasefile.command.expire-pending-material";

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(MaterialRejected.class);

    @InjectMocks
    private ExpirePendingMaterialHandler expirePendingMaterialHandler;

    private UUID caseId = randomUUID();
    private UUID fileStoreId = randomUUID();
    private MaterialRejected materialRejectedEvent = materialRejected()
            .withCaseId(caseId)
            .withMaterial(material()
                    .withFileStoreId(fileStoreId)
                    .withDocumentType("SJPN")
                    .build())
            .build();

    @Test
    public void shouldExpirePendingMaterial() throws EventStreamException {
        final ExpirePendingMaterial expirePendingMaterial = expirePendingMaterial()
                .withCaseId(caseId)
                .withFileStoreId(fileStoreId)
                .build();

        final Envelope<ExpirePendingMaterial> expirePendingMaterialCommand = envelopeFrom(metadataFor(EXPIRE_PENDING_MATERIAL_COMMAND), expirePendingMaterial);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(prosecutionCaseFile.expirePendingMaterial(fileStoreId, clock.now())).thenReturn(Stream.of(materialRejectedEvent));

        expirePendingMaterialHandler.expirePendingMaterial(expirePendingMaterialCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(expirePendingMaterialCommand.metadata()).withName("prosecutioncasefile.events.material-rejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(materialRejectedEvent.getCaseId().toString())),
                                        withJsonPath("$.material.fileStoreId", equalTo(materialRejectedEvent.getMaterial().getFileStoreId().toString())),
                                        withJsonPath("$.material.documentType", equalTo(materialRejectedEvent.getMaterial().getDocumentType()))
                                ))))));
    }

    @Test
    public void shouldHandleExpirePendingMaterialCommand() {
        assertThat(expirePendingMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("expirePendingMaterial").thatHandles(EXPIRE_PENDING_MATERIAL_COMMAND)));
    }
}
