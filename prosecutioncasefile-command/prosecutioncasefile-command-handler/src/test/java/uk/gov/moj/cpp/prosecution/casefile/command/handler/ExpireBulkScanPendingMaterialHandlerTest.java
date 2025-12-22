package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ExpireBulkScanPendingMaterial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialRejected;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public class ExpireBulkScanPendingMaterialHandlerTest {
    @Spy
    private Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    BulkscanMaterialRejected.class);
    @InjectMocks
    private ExpireBulkScanPendingMaterialHandler handler;

    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventSource eventSource;
    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Mock
    private Clock clock;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(fromString("a4391788-f829-4514-a344-61f1d5d9690c"))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(clock.now()).thenReturn(ZonedDateTime.now());
    }

    @Test
    public void shoudlHandleExpirePendingMaterial() throws EventStreamException {

        final ExpireBulkScanPendingMaterial expirePendingMaterial = readJson("json/prosecutioncasefile.command.expire-pending-material.json", ExpireBulkScanPendingMaterial.class);

        final Envelope<ExpireBulkScanPendingMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.expire-bulk-scan-pending-material"), expirePendingMaterial);

        final BulkscanMaterialRejected bulkscanMaterialRejected = BulkscanMaterialRejected.bulkscanMaterialRejected().withCaseId(fromString("a4391788-f829-4514-a344-61f1d5d9690c")).build();
        when(prosecutionCaseFile.expireBulkScanPendingMaterial(any(), any())).thenReturn(Stream.of(bulkscanMaterialRejected));

        handler.expirePendingMaterial(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName("prosecutioncasefile.events.bulkscan-material-rejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo("a4391788-f829-4514-a344-61f1d5d9690c"))
                                ))))));
    }


}
