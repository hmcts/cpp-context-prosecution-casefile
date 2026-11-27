package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssignCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssociateEnterpriseId;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.UnassignCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.json.JsonValue;

import org.apache.commons.collections.iterators.ArrayListIterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionHandlerTest {

    private static final UUID CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");
    private static final String ENTERPRISE_ID = UUID.randomUUID().toString();

    private static final String INITIATE_SJP_PROSECUTION_WITH_REFERENCE_DATA_COMMAND = "prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data";
    private static final String ASSOCIATE_ENTERPRISE_ID_COMMAND = "prosecutioncasefile.command.associate-enterprise-id";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private Stream<Object> newEvents;

    @Mock
    private Stream<Object> mappedNewEvents;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = createEnveloperWithEvents(ProsecutionCaseUnsupported.class, SjpProsecutionReceived.class, SjpProsecutionInitiated.class, SjpProsecutionRejected.class);

    @InjectMocks
    private SjpProsecutionHandler sjpProsecutionHandler;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptorStream;

    @Mock
    Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Test
    public void shouldInitiateSjpProsecutionCommand() {
        assertThat(sjpProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleInitiateSjpProsecutionWithReferenceData").thatHandles("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data"))
                .with(method("handleSjpProsecutionAssociateEnterpriseId").thatHandles("prosecutioncasefile.command.associate-enterprise-id"))
        );
    }

    @Test
    public void shouldInitiateSjpProsecutionCase() throws Exception {
        final ProsecutionWithReferenceData prosecutionWithReferenceDataMock = getMockProsecutionWithReferenceData();
        final Envelope<ProsecutionWithReferenceData> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(INITIATE_SJP_PROSECUTION_WITH_REFERENCE_DATA_COMMAND),
                prosecutionWithReferenceDataMock
        );
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(defendantRefDataEnrichers.iterator()).thenReturn(new ArrayListIterator());
        when(caseRefDataEnrichers.iterator()).thenReturn(new ArrayListIterator());
        when(prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceDataMock, newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), referenceDataQueryService)).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        sjpProsecutionHandler.handleInitiateSjpProsecutionWithReferenceData(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private ProsecutionWithReferenceData getMockProsecutionWithReferenceData() {
        return new ProsecutionWithReferenceData(Prosecution.prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .build())
                .build());
    }

    @Test
    public void shouldAssociateEnterpriseId() throws Exception {
        final AssociateEnterpriseId associateEnterpriseId = getMockAssociateEnterpriseId();
        final Envelope<AssociateEnterpriseId> envelope = Envelope.envelopeFrom(
                metadataWithRandomUUID(ASSOCIATE_ENTERPRISE_ID_COMMAND),
                associateEnterpriseId
        );
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(prosecutionCaseFile.associateEnterpriseId(associateEnterpriseId.getEnterpriseId())).thenReturn(newEvents);
        when(newEvents.map(any())).thenReturn(mappedNewEvents);

        sjpProsecutionHandler.handleSjpProsecutionAssociateEnterpriseId(envelope);

        checkEventsAppendedAreMappedNewEvents();
    }

    private AssociateEnterpriseId getMockAssociateEnterpriseId() {
        return AssociateEnterpriseId.associateEnterpriseId()
                .withCaseId(CASE_ID)
                .withEnterpriseId(ENTERPRISE_ID)
                .build();
    }

    @Test
    public void shouldHandleInitiateSjpProsecution() throws Exception {

        // GIVEN
        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                readJson("json/initiateSjpProsecutionWithReferenceData.json", ProsecutionWithReferenceData.class);

        final Envelope<ProsecutionWithReferenceData> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data"),
                        prosecutionWithReferenceData);

        // WHEN
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());

        sjpProsecutionHandler.handleInitiateSjpProsecutionWithReferenceData(envelope);

        // THEN
        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.sjp-prosecution-received",
                () -> readJson("json/sjpProsecutionReceived.json", JsonValue.class));
    }

    @Test
    public void shouldHandleSjpProsecutionAssociateEnterpriseId() throws Exception {
        // GIVEN
        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                readJson("json/initiateSjpProsecutionWithReferenceData.json", ProsecutionWithReferenceData.class);

        aggregate.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<CaseRefDataEnricher>(), new ArrayList<DefendantRefDataEnricher>(), referenceDataQueryService);

        final AssociateEnterpriseId associateEnterpriseId = readJson("json/associateEnterpriseId.json", AssociateEnterpriseId.class);

        final Envelope<AssociateEnterpriseId> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.associate-enterprise-id"), associateEnterpriseId);

        // WHEN
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        sjpProsecutionHandler.handleSjpProsecutionAssociateEnterpriseId(envelope);

        // THEN
        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.sjp-prosecution-initiated",
                () -> readJson("json/sjpProsecutionInitiated.json", JsonValue.class));
    }

    @Test
    public void shouldReturnProsecutionUnsupportedWhenInitiateSjpProsecutionHasMultipleDefendants() throws Exception {
        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                readJson("json/initiateSjpProsecutionWithReferenceDataAndMultipleDefendants.json", ProsecutionWithReferenceData.class);
        final Envelope<ProsecutionWithReferenceData> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data"),
                        prosecutionWithReferenceData);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        sjpProsecutionHandler.handleInitiateSjpProsecutionWithReferenceData(envelope);
        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.prosecution-case-unsupported",
                () -> readJson("json/prosecutionCaseUnsupported.json", JsonValue.class));
    }

    @Test
    public void shouldReturnProsecutionUnsupportedWhenDuplicatedSjpProsecutionMessageReceived() throws Exception {
        final Field field = ProsecutionCaseFile.class.getDeclaredField("prosecutionReceived");
        field.setAccessible(true);
        field.set(aggregate, true);
        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                readJson("json/initiateSjpProsecutionWithReferenceData.json", ProsecutionWithReferenceData.class);
        final Envelope<ProsecutionWithReferenceData> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data"),
                        prosecutionWithReferenceData);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        sjpProsecutionHandler.handleInitiateSjpProsecutionWithReferenceData(envelope);
        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.prosecution-case-unsupported",
                () -> readJson("json/prosecutionCaseUnsupported.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAssignCase() throws EventStreamException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        final AssignCase assignCaseData = readJson("json/acceptSjpCase.json", AssignCase.class);
        Envelope<AssignCase> envelope = Envelope.envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.assign-case"),assignCaseData);
        sjpProsecutionHandler.handleAssignCase(envelope);
    }

    @Test
    public void shouldHandleUnAssignCase() throws EventStreamException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        final UnassignCase unAssignCaseData = readJson("json/acceptSjpCase.json", UnassignCase.class);
        Envelope<UnassignCase> envelope = Envelope.envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.unassign-case"),unAssignCaseData);
        sjpProsecutionHandler.handleUnAssignCase(envelope);
    }

    private void checkEventsAppendedAreMappedNewEvents() throws EventStreamException {
        verify(eventStream).append(argumentCaptorStream.capture());
        final Stream<JsonEnvelope> stream = argumentCaptorStream.getValue();
        assertThat(stream, is(mappedNewEvents));
    }

}
