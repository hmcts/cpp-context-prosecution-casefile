package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordMigratedCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MigratedCaseRecorded;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordMigratedCaseHandlerTest {

    private static final UUID CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");
    private static final String COMMAND_NAME = "prosecutioncasefile.command.record-migrated-case";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(MigratedCaseRecorded.class);

    @InjectMocks
    private RecordMigratedCaseHandler recordMigratedCaseHandler;

    private final ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    @Test
    void shouldHandleRecordMigratedCaseCommand() {
        assertThat(recordMigratedCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleRecordMigratedCase").thatHandles(COMMAND_NAME)));
    }

    @Test
    void handleRecordMigratedCase_libraCase_should_appendMigratedCaseRecorded() throws EventStreamException {
        final RecordMigratedCase recordMigratedCase = readJson("json/recordMigratedCase.json", RecordMigratedCase.class);
        final Envelope<RecordMigratedCase> envelope = envelopeFrom(metadataFor(COMMAND_NAME), recordMigratedCase);

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        recordMigratedCaseHandler.handleRecordMigratedCase(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.migrated-case-recorded",
                () -> readJson("json/migratedCaseRecorded.json", JsonValue.class));
        assertThat(aggregate.isProsecutionAccepted(), is(true));
    }
}
