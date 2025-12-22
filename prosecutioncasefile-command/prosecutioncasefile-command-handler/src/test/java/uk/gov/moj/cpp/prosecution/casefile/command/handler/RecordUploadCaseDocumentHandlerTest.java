package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived.sjpProsecutionReceived;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RecordUploadCaseDocument;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.UploadCaseDocumentRecorded;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecordUploadCaseDocumentHandlerTest {

    private static final UUID CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(UploadCaseDocumentRecorded.class);

    @InjectMocks
    private RecordUploadCaseDocumentHandler recordUploadCaseDocumentHandler;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    private Defendant defendant = Defendant.defendant().withId(UUID.randomUUID().toString()).build();


    @Test
    public void shouldHandleRecordUploadCaseDocumentCommand() {
        assertThat(recordUploadCaseDocumentHandler, isHandler(COMMAND_HANDLER)
                .with(method("recordUploadCaseDocument")
                        .thatHandles("prosecutioncasefile.command.record-upload-case-document")
                ));
    }

    @Test
    public void shouldHandleRecordUploadCaseDocument() throws Exception {

        final RecordUploadCaseDocument recordUploadCaseDocument =
                readJson("json/recordUploadCaseDocument.json", RecordUploadCaseDocument.class);

        final Envelope<RecordUploadCaseDocument> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.record-upload-case-document"), recordUploadCaseDocument);

        aggregate.apply(sjpProsecutionReceived()
                .withProsecution(prosecution()
                        .withCaseDetails(caseDetails().build()).withDefendants(singletonList(defendant)).build()).build());

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        recordUploadCaseDocumentHandler.recordUploadCaseDocument(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.upload-case-document-recorded",
                () -> readJson("json/uploadCaseDocumentRecorded.json", JsonValue.class));
    }

}