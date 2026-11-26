package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.fromString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CcProsecutionHandlerTest {

    @InjectMocks
    private CcProsecutionHandler ccProsecutionHandler;

    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private EventSource eventSource;
    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CcCaseReceived.class);

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(fromString("a4391788-f829-4514-a344-61f1d5d9690c"))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
    }

    @Test
    public void shouldHandleReceiveCCProsecution() throws EventStreamException {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getMockProsecutionWithReferenceData("FHG");
        final Envelope<ProsecutionWithReferenceData> envelope =
                Envelope.envelopeFrom(metadataFor("prosecutioncasefile.command.initiate-cc-prosecution-with-reference-data"), prosecutionWithReferenceData);

        final CcCaseReceived ccCaseReceived = CcCaseReceived.ccCaseReceived().withProsecutionWithReferenceData(prosecutionWithReferenceData).build();
        when(prosecutionCaseFile.receiveCCCase(any(), any(),any(), any())).thenReturn(Stream.of(ccCaseReceived));


        ccProsecutionHandler.initiateCCProsecutionWithReferenceData(envelope);

        assertThat(ccProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("initiateCCProsecutionWithReferenceData")
                        .thatHandles("prosecutioncasefile.command.initiate-cc-prosecution-with-reference-data")
                ));
    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData(final String hearingCode) {
        final Prosecution prosecution = Prosecution.prosecution()
                .withDefendants(getMockDefendantList(hearingCode))
                .withCaseDetails(CaseDetails.caseDetails()
                        .withSummonsCode("summonsCode")
                        .withCaseId(UUID.fromString("a4391788-f829-4514-a344-61f1d5d9690c")).build())
                .build();

        return new ProsecutionWithReferenceData(prosecution);
    }

    private List<Defendant> getMockDefendantList(final String hearingTypeCode){
        return Collections.singletonList(Defendant.defendant()
                .withInitialHearing(InitialHearing.initialHearing()
                        .withHearingTypeCode(hearingTypeCode)
                        .build())
                .build());
    }

}