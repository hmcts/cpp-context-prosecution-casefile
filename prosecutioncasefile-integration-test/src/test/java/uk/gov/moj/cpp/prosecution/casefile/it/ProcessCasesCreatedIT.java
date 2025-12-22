package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.helper.ProcessCasesCreatedHelper;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class ProcessCasesCreatedIT extends BaseIT {

    private static final String FIELD_CASE_ID = "caseId";
    private final String VALUE_CASE_ID = randomUUID().toString();

    private final JmsMessageProducerClient privateMessageProducerQueue = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME)
            .getMessageProducerClient();
    @Test
    public void sendMessageToSjpCaseCreatedTopic() {
        final JsonObject payload = createObjectBuilder()
                .add(FIELD_CASE_ID, VALUE_CASE_ID)
                .build();
        final String clientCorrelationId = randomUUID().toString();

        ProcessCasesCreatedHelper processCasesCreatedHelper = new ProcessCasesCreatedHelper(payload, clientCorrelationId);
        privateMessageProducerQueue.sendMessage(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY, envelopeFrom(caseCreatedSuccessfullyMetadata(clientCorrelationId), payload));
        processCasesCreatedHelper.verifyInActiveMQ();
    }

    private Metadata caseCreatedSuccessfullyMetadata(final String clientCorrelationId) {

        return metadataBuilder()
                .withId(randomUUID())
                .withName(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY)
                .withPosition(123)
                .withSessionId(randomUUID().toString())
                .withUserId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withClientCorrelationId(clientCorrelationId)
                .build();
    }

}
