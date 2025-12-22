package uk.gov.moj.cpp.prosecution.casefile.helper;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import io.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final long RETRIEVE_TIMEOUT = 20000;

    public static JsonPath retrieveMessage(final JmsMessageConsumerClient consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
    }

    private static Optional<JsonPath> retrieveMessage(final JmsMessageConsumerClient consumer, long customTimeOutInMillis) {
        return retrieveMessageAsString(consumer, customTimeOutInMillis)
                .map(JsonPath::new);
    }

    public static Optional<String> retrieveMessageAsString(final JmsMessageConsumerClient consumer) {
        return retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT);
    }

    private static Optional<String> retrieveMessageAsString(final JmsMessageConsumerClient consumer, long customTimeOutInMillis) {
        Optional<String> message = consumer.retrieveMessage(customTimeOutInMillis);

        if (message.isEmpty()) {
            LOGGER.error("No message retrieved using consumer");
            return Optional.empty();
        }
        return message;
    }

    public static JsonEnvelope getEventFromQueue(final JmsMessageConsumerClient consumer) {
        return retrieveMessageAsString(consumer)
                .map(new DefaultJsonObjectEnvelopeConverter()::asEnvelope)
                .orElse(null);
    }

}
