package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationAggregate;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.PocaCourtApplication;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PocaApplicationUpdateHandlerTest {

    public static final String PROSECUTIONCASEFILE_COMMAND_UPDATE_APPLICATION_STATUS = "prosecutioncasefile.command.update-application-status";

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private PocaApplicationUpdateHandler pocaApplicationUpdateHandler;

    private ApplicationAggregate applicationAggregate;

    @BeforeEach
    public void setup() {

        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
    }

    @Test
    public void shouldProcessPocaApplicationUpdateStatusCommand() throws Exception {
        //given
        PocaCourtApplication pocaCourtApplication = PocaCourtApplication.pocaCourtApplication().withCourtApplication(
                        courtApplication()
                        .withId(randomUUID())
                        .build())
                .build();
        final Metadata metadata = Envelope.metadataBuilder().withName(PROSECUTIONCASEFILE_COMMAND_UPDATE_APPLICATION_STATUS)
                .withId(randomUUID())
                .build();
        final Envelope<PocaCourtApplication> envelope = envelopeFrom(metadata, pocaCourtApplication);

        //when
        pocaApplicationUpdateHandler.pocaApplicationUpdateStatus(envelope);

        //then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream.collect(toList()), hasSize(0));
    }
}