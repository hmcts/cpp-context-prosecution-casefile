package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATED_PROSECUTION;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectGroupCases;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CcProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
public class RejectGroupCaseHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(GroupProsecutionRejected.class, CcProsecutionRejected.class);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream groupEventStream;

    @Mock
    private EventStream caseEventStream;

    @Mock
    private AggregateService aggregateService;

    private GroupProsecutionCaseFile groupAggregate;
    private ProsecutionCaseFile caseAggregate;

    @InjectMocks
    private RejectGroupCaseHandler rejectGroupCaseHandler;

    private static final UUID GROUP_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final String CASE_URN = randomAlphanumeric(8);

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleRejectGroupCases() {
        assertThat(rejectGroupCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleRejectGroupCases")
                        .thatHandles("prosecutioncasefile.command.handler.reject-group-cases")
                ));
    }

    @Test
    public void shouldHandle_WhenMemberGroupRejected() throws Exception {
        groupAggregate = new GroupProsecutionCaseFile();
        when(eventSource.getStreamById(GROUP_ID)).thenReturn(groupEventStream);
        when(aggregateService.get(groupEventStream, GroupProsecutionCaseFile.class)).thenReturn(groupAggregate);

        rejectGroupCaseHandler.handleRejectGroupCases(createRejectGroupCases(GROUP_ID, null, CASE_URN));

        ArgumentCaptor<Stream> groupArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(groupEventStream, times(1)).append(groupArgumentCaptor.capture());

        final JsonEnvelope envelope = (JsonEnvelope) groupArgumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        GroupProsecutionRejected groupProsecutionRejected = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), GroupProsecutionRejected.class);

        assertThat(groupProsecutionRejected.getGroupCaseErrors().get(0).getCode(), is(DUPLICATED_PROSECUTION.toString()));
        assertThat(groupProsecutionRejected.getGroupCaseErrors().get(0).getValues().get(0).getKey(), is("urn"));
        assertThat(groupProsecutionRejected.getGroupCaseErrors().get(0).getValues().get(0).getValue(), is(CASE_URN));
    }

    @Test
    public void shouldHandle_WhenMemberCaseRejected() throws Exception {
        caseAggregate = new ProsecutionCaseFile();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(caseEventStream);
        when(aggregateService.get(caseEventStream, ProsecutionCaseFile.class)).thenReturn(caseAggregate);

        rejectGroupCaseHandler.handleRejectGroupCases(createRejectGroupCases(null, CASE_ID, CASE_URN));

        ArgumentCaptor<Stream> caseArgumentCaptor = ArgumentCaptor.forClass(Stream.class);
        Mockito.verify(caseEventStream, times(1)).append(caseArgumentCaptor.capture());

        final JsonEnvelope envelope = (JsonEnvelope) caseArgumentCaptor.getAllValues().get(0).findFirst().orElse(null);
        CcProsecutionRejected ccProsecutionRejected = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), CcProsecutionRejected.class);

        assertThat(ccProsecutionRejected.getCaseErrors().get(0).getCode(), is(DUPLICATED_PROSECUTION.toString()));
        assertThat(ccProsecutionRejected.getCaseErrors().get(0).getValues().get(0).getKey(), is("urn"));
        assertThat(ccProsecutionRejected.getCaseErrors().get(0).getValues().get(0).getValue(), is(CASE_URN));

    }

    private Envelope<RejectGroupCases> createRejectGroupCases(final UUID groupId, final UUID caseId, final String caseUrn) {
        return Enveloper
                .envelop(RejectGroupCases.rejectGroupCases()
                        .withGroupId(groupId)
                        .withCaseId(caseId)
                        .withCaseUrn(caseUrn)
                        .build())
                .withName("prosecutioncasefile.command.handler.reject-group-cases")
                .withMetadataFrom(JsonEnvelope
                        .envelopeFrom(metadataWithRandomUUID(randomUUID().toString()),
                                createObjectBuilder().build()));
    }
}