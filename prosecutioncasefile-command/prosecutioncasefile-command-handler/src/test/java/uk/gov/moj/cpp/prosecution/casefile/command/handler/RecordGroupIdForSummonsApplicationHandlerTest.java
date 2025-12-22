package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordGroupIdForSummonsApplication;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupIdRecordedForSummonsApplication;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;

@ExtendWith(MockitoExtension.class)
public class RecordGroupIdForSummonsApplicationHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    GroupIdRecordedForSummonsApplication.class);

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private RecordGroupIdForSummonsApplicationHandler recordGroupIdForSummonsApplicationHandler;

    private ProsecutionCaseFile aggregate;

    @BeforeEach
    public void setup(){
        aggregate = new ProsecutionCaseFile();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
    }

    @Test
    public void handleProsecutionCaseFiltered() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID groupId = randomUUID();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName("prosecutioncasefile.command.record-group-id-for-summons-application")
                .build();

        final RecordGroupIdForSummonsApplication payload = RecordGroupIdForSummonsApplication.recordGroupIdForSummonsApplication()
                .withCaseId(caseId)
                .withGroupId(groupId)
                .build();

        final Envelope<RecordGroupIdForSummonsApplication> commandEnvelope = envelopeFrom(metadata, payload);

        recordGroupIdForSummonsApplicationHandler.handleProsecutionCaseFiltered(commandEnvelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.group-id-recorded-for-summons-application",
                () -> createObjectBuilder().add("caseId", caseId.toString()).add("groupId", groupId.toString()).build());
    }
}