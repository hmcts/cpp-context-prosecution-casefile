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
import uk.gov.moj.cpp.prosecutioncasefile.command.handler.RecordReferProsecutionCasesToCourtAccepted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseReferredToCourtRecorded;

import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecordReferProsecutionCasesToCourtAcceptedHandlerTest {
    private static final UUID CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");
    private static final UUID REFERRAL_REASON_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf223");

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    CaseReferredToCourtRecorded.class);

    @InjectMocks
    private RecordReferProsecutionCasesToCourtAcceptedHandler recordReferProsecutionCasesToCourtAcceptedHandler;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    @Test
    public void shouldHaveAHandlerForAcceptCourtReferralCommand() {
        assertThat(recordReferProsecutionCasesToCourtAcceptedHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleRecordReferProsecutionCasesToCourt")
                        .thatHandles("prosecutioncasefile.command.record-refer-prosecution-cases-to-court-accepted")
                ));
    }

    @Test
    public void shouldHandleTheAcceptCourtReferralWhenTheCaseIsReferredToCourt() throws EventStreamException {
        // given
        final RecordReferProsecutionCasesToCourtAccepted recordReferProsecutionCasesToCourtAccepted = RecordReferProsecutionCasesToCourtAccepted
                .recordReferProsecutionCasesToCourtAccepted()
                .withCaseId(CASE_ID)
                .withReferralReasonId(REFERRAL_REASON_ID)
                .build();

        final Envelope<RecordReferProsecutionCasesToCourtAccepted> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.record-refer-prosecution-cases-to-court-accepted"), recordReferProsecutionCasesToCourtAccepted);

        // when
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        recordReferProsecutionCasesToCourtAcceptedHandler.handleRecordReferProsecutionCasesToCourt(envelope);

        // then
        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.case-referred-to-court-recorded",
                () -> readJson("json/caseReferredToCourtRecorded.json", JsonValue.class));
        assertThat(aggregate.isCaseReferredToCourt(), is(true));
    }

}
