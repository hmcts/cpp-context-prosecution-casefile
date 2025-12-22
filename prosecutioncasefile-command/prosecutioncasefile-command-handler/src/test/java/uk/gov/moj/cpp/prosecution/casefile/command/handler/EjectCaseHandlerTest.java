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
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseEjected;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.EjectCase;

import java.util.stream.Stream;


import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public class EjectCaseHandlerTest {


    @Spy
    private Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    CaseEjected.class);
    @InjectMocks
    EjectCaseHandler ejectCaseHandler;

    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventSource eventSource;
    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222"))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
    }

    @Test
    public void shoudlHandleEjectCase() throws EventStreamException {
        final EjectCase ejectCase = readJson("json/addMaterial.json", EjectCase.class);

        final Envelope<EjectCase> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material"), ejectCase);

        when(prosecutionCaseFile.ejectCase(any())).thenReturn(Stream.of(new CaseEjected(ejectCase.getCaseId())));

        ejectCaseHandler.handleEjectCase(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName("prosecutioncasefile.events.case-ejected"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo("51cac7fb-387c-4d19-9c80-8963fa8cf222"))
                                ))))));


    }
}