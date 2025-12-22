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
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingIdpcMaterial.expirePendingIdpcMaterial;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected.idpcMaterialRejected;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpirePendingIdpcMaterial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ExpirePendingIdpcMaterialHandlerTest {

    private static final String EXPIRE_PENDING_IDPC_MATERIAL_COMMAND = "prosecutioncasefile.command.expire-pending-idpc-material";

    @Spy
    private final Clock clock = new StoppedClock(now(UTC));
    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(IdpcMaterialRejected.class);
    private final UUID caseId = randomUUID();
    private final UUID fileStoreId = randomUUID();
    private final IdpcMaterialRejected idpcMaterialRejectedEvent = idpcMaterialRejected()
            .withCaseId(caseId)
            .withFileServiceId(fileStoreId)
            .build();

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;
    @InjectMocks
    private ExpirePendingIdpcMaterialHandler expirePendingIdpcMaterialHandler;

    @Test
    public void shouldExpirePendingIdpcMaterial() throws EventStreamException {
        final ExpirePendingIdpcMaterial expirePendingIdpcMaterial = expirePendingIdpcMaterial()
                .withCaseId(caseId)
                .withFileServiceId(fileStoreId)
                .build();

        final Envelope<ExpirePendingIdpcMaterial> expirePendingMaterialCommand = envelopeFrom(metadataFor(EXPIRE_PENDING_IDPC_MATERIAL_COMMAND), expirePendingIdpcMaterial);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(prosecutionCaseFile.expirePendingIdpcMaterial(fileStoreId, clock.now())).thenReturn(Stream.of(idpcMaterialRejectedEvent));

        expirePendingIdpcMaterialHandler.expirePendingIdpcMaterial(expirePendingMaterialCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(expirePendingMaterialCommand.metadata()).withName("prosecutioncasefile.events.idpc-material-rejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(idpcMaterialRejectedEvent.getCaseId().toString())),
                                        withJsonPath("$.fileServiceId", equalTo(idpcMaterialRejectedEvent.getFileServiceId().toString()))
                                ))))));
    }

    @Test
    public void shouldHandleExpirePendingMaterialCommand() {
        assertThat(expirePendingIdpcMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("expirePendingIdpcMaterial").thatHandles(EXPIRE_PENDING_IDPC_MATERIAL_COMMAND)));
    }
}
