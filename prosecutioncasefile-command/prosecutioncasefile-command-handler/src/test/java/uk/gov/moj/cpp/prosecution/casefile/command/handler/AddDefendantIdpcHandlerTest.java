package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.builder.AddDefendantIdpcCommandBuilder.anAddDefendantIdpcCommand;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_ID_STR;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_MATERIAL_ID_STR;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_DOCUMENT_TYPE_IDPC;
import static uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData.CASE_ID_STR;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.command.handler.util.DefaultTestData;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentAdditionFailed;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class AddDefendantIdpcHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            DefendantIdpcAdded.class,
            CaseDocumentAdditionFailed.class
    );
    @Spy
    private final Clock clock = new UtcClock();
    private ProsecutionCaseFile prosecutionCaseFile;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AddDefendantIdpcHandler addCaseDocumentHandler;

    @BeforeEach
    public void setUp() {
        prosecutionCaseFile = new ProsecutionCaseFile();
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(prosecutionCaseFile);
    }

    @AfterEach
    public void tearDown() {
        verify(eventSource).getStreamById(eq(DefaultTestData.CASE_ID));
        verify(aggregateService).get(eq(eventStream), eq(ProsecutionCaseFile.class));
        verifyNoMoreInteractions(eventStream, eventSource, aggregateService);
    }


    @Test
    public void testAddDefendantIdpc_triggersDefendantIdpcAddedEvent() throws Exception {
        final JsonEnvelope addCaseDocumentCommand = anAddDefendantIdpcCommand()
                .build();

        addCaseDocumentHandler.addDefendantIdpc(addCaseDocumentCommand);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(addCaseDocumentCommand)
                                        .withName("prosecutioncasefile.events.defendant-idpc-added"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", is(CASE_ID_STR)),
                                        withJsonPath("$.caseDocument.id", is(CASE_DOCUMENT_ID_STR)),
                                        withJsonPath("$.caseDocument.materialId", is(CASE_DOCUMENT_MATERIAL_ID_STR)),
                                        withJsonPath("$.caseDocument.documentType", is(CASE_DOCUMENT_TYPE_IDPC))
                                ))))));
    }
}
