package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.FilterProsecutionCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseFiltered;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class FilterProsecutionCaseHandlerTest {

    private static final String PROSECUTION_CASE_FILTERED_COMMAND = "prosecutioncasefile.command.filter-prosecution-case";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(CaseFiltered.class);

    @InjectMocks
    private FilterProsecutionCaseHandler filterProsecutionCaseHandler;

    private List<Material> materialList = generateMaterials();
    private static final UUID  CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");
    private CaseFiltered caseFilteredEvent = CaseFiltered.caseFiltered()
            .withCaseId(CASE_ID)
            .withMaterials(materialList)
            .build();

    @Test
    public void shouldHandleFilterProsecutionCaseCommand() {
        assertThat(filterProsecutionCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleProsecutionCaseFiltered").thatHandles(PROSECUTION_CASE_FILTERED_COMMAND)));
    }

    @Test
    public void shouldFilterProsecutionCaseEvent() throws EventStreamException {
        final FilterProsecutionCase filterProsecutionCase = FilterProsecutionCase.filterProsecutionCase()
                .withCaseId(CASE_ID)
                .build();

        final Envelope<FilterProsecutionCase> filterProsecutionCaseCommand= envelopeFrom(metadataFor(PROSECUTION_CASE_FILTERED_COMMAND), filterProsecutionCase);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(prosecutionCaseFile.filterCase(CASE_ID)).thenReturn(Stream.of(caseFilteredEvent));

        filterProsecutionCaseHandler.handleProsecutionCaseFiltered(filterProsecutionCaseCommand);
        verify(eventSource).getStreamById(CASE_ID);
        verify(aggregateService).get(eventStream, ProsecutionCaseFile.class);
        verify(prosecutionCaseFile).filterCase(CASE_ID);
        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(filterProsecutionCaseCommand.metadata()).withName("prosecutioncasefile.events.case-filtered"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseFilteredEvent.getCaseId().toString())),
                                        withJsonPath("$.materials", hasSize(2)),
                                        withJsonPath("$.materials[0].fileStoreId", equalTo(caseFilteredEvent.getMaterials().get(0).getFileStoreId().toString())),
                                        withJsonPath("$.materials[1].fileStoreId", equalTo(caseFilteredEvent.getMaterials().get(1).getFileStoreId().toString()))
                                ))))));
    }

    private List<Material> generateMaterials() {
        List<Material> list = new ArrayList<>();
        list.add(randomMaterial(null));
        list.add(randomMaterial(null));
        return list;
    }

    private static Material randomMaterial(String documentType) {
        return material()
                .withFileStoreId(randomUUID())
                .withDocumentType(nonNull(documentType) ? documentType : randomAlphanumeric(5))
                .withFileType(randomAlphanumeric(5))
                .build();
    }
}
