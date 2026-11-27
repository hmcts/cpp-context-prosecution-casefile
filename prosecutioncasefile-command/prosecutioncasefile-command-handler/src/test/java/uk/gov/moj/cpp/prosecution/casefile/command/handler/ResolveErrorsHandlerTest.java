package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ValidationCompleted;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.json.JsonObject;

import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class ResolveErrorsHandlerTest {
    private static final UUID CASE_ID = UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");

    @InjectMocks
    private ResolveErrorsHandler resolveErrorsHandler;

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(UUID.fromString("ba9ad3ae-239d-4bde-91b4-94b04769109f"))).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
    }

    @Test
    public void shouldResolveErrors() throws EventStreamException {
        final JsonObject obj = readJson("json/resolveErrors.json", JsonObject.class);
        JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.handler.resolve-errors"),obj);
        final ValidationCompleted validationCompleted = ValidationCompleted.validationCompleted().withCaseId(UUID.fromString("ba9ad3ae-239d-4bde-91b4-94b04769109f")).build();
        when(prosecutionCaseFile.receiveErrorCorrections(any(), any(),any(), any(),any(), any())).thenReturn(Stream.of(validationCompleted));
        resolveErrorsHandler.resolveErrors(envelope);
    }
}
