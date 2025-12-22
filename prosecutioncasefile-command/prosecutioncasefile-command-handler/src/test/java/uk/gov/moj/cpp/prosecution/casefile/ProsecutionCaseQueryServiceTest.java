package uk.gov.moj.cpp.prosecution.casefile;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.prosecution.casefile.command.service.ProsecutionCaseQueryService;

import java.util.UUID;

import javax.json.JsonObject;

import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseQueryServiceTest {
    @InjectMocks
    private ProsecutionCaseQueryService prosecutionCaseQueryService ;

    @Mock
    private Requester requester;

    @Test
    public void shouldGetProsecutionCaseByCaseUrn(){
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName(),
                createObjectBuilder().build());
        final String caseURN = UUID.randomUUID().toString();
        final Envelope<JsonObject> result = Envelope.envelopeFrom(metadataBuilder().withName("prosecutioncasefile.query.case-by-prosecutionCaseReference")
                .withId(randomUUID()), createObjectBuilder().add("caseURN",caseURN)
                .build());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(result);
        final JsonObject response = prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(envelope, caseURN);
        assertThat(response.size(), is(1));
        assertThat(response.getString("caseURN"), is(caseURN));
    }

    @Test
    public void shouldGetProsecutionCaseByCaseId(){
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUIDAndName(),
                createObjectBuilder().build());
        final String caseId = UUID.randomUUID().toString();
        final Envelope<JsonObject> result = Envelope.envelopeFrom(metadataBuilder().withName("prosecutioncasefile.query.case-by-prosecutionCaseReference")
                .withId(randomUUID()), createObjectBuilder().add("caseId", caseId)
                .build());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(result);
        final JsonObject response = prosecutionCaseQueryService.getProsecutionCaseByCaseId(envelope, caseId);
        assertThat(response.size(), is(1));
        assertThat(response.getString("caseId"), is(caseId));
    }
}
