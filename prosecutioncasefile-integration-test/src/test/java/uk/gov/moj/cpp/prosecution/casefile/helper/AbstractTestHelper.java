package uk.gov.moj.cpp.prosecution.casefile.helper;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;

import io.restassured.path.json.JsonPath;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;
import com.jayway.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractTestHelper {
    public static final String CONTEXT_NAME = "prosecutioncasefile";

    public static final int POLL_INTERVAL_SECS = 1;
    public static final int POLL_DELAY_SECS = 1;
    public static final String USER_ID = randomUUID().toString();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestHelper.class);
    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    protected static final String BASE_URI = System.getProperty("baseUri", "http://" + HOST + ":8080");
    private static final String WRITE_BASE_URL = "/prosecutioncasefile-service/command/api/rest/prosecutioncasefile";
    private static final String READ_BASE_URL = "/prosecutioncasefile-service/query/api/rest/prosecutioncasefile";
    private static final long RETRIEVE_TIMEOUT = 20000;
    private static final int POLL_INTERVAL = 500;
    private static final int POLL_DELAY = 500;
    protected final RestClient restClient = new RestClient();

    private Map<String, JmsMessageConsumerClient> messageConsumerClientMap = new HashMap<>();

    private JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider()
            .getMessageProducerClient();

    protected JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    public static String getWriteUrl(String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    public static String getReadUrl(String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }

    protected void makePostCall(String url, String mediaType, String payload) {
        makePostCall(UUID.fromString(USER_ID), url, mediaType, payload);
    }

    protected void makePostCall(UUID userId, String url, String mediaType, String payload) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
    }

    protected void makePostCallWithBadRequest(String url, String mediaType, String payload) {
        UUID userId = UUID.fromString(USER_ID);
        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", url, mediaType, payload);
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(HeaderConstants.USER_ID, userId.toString());
        Response response = restClient.postCommand(url, mediaType, payload, map);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    public JmsMessageConsumerClient createPublicConsumer(final String eventSelector) {
        if (!messageConsumerClientMap.containsKey(eventSelector)) {
            messageConsumerClientMap.put(eventSelector, newPublicJmsMessageConsumerClientProvider()
                    .withEventNames(eventSelector)
                    .getMessageConsumerClient());
        }

        return messageConsumerClientMap.get(eventSelector);
    }

    public JmsMessageConsumerClient createPrivateConsumer(final String eventSelector) {
        if (!messageConsumerClientMap.containsKey(eventSelector)) {
            messageConsumerClientMap.put(eventSelector, newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                    .withEventNames(eventSelector)
                    .getMessageConsumerClient());
        }

        return messageConsumerClientMap.get(eventSelector);
    }

    public void createPrivateConsumerForMultipleSelectors(final String... eventNames) {
        Arrays.stream(eventNames).forEach(this::createPrivateConsumer);
    }

    public void createPublicConsumerForMultipleSelectors(final String... eventNames) {
        Arrays.stream(eventNames).forEach(this::createPublicConsumer);
    }

    public void clearMessages(final String eventName) {
        if (messageConsumerClientMap.containsKey(eventName)) {
            messageConsumerClientMap.get(eventName).clearMessages();
        }
    }

    public Optional<JsonEnvelope> retrieveEvent(final String eventName) {
        if (!messageConsumerClientMap.containsKey(eventName)) {
            return Optional.empty();
        }

        return messageConsumerClientMap.get(eventName).retrieveMessageAsJsonEnvelope(RETRIEVE_TIMEOUT);
    }

    public Optional<JsonPath> retrieveEventAsJsonPath(final String eventName) {
        if (!messageConsumerClientMap.containsKey(eventName)) {
            return Optional.empty();
        }

        return messageConsumerClientMap.get(eventName).retrieveMessageAsJsonPath(RETRIEVE_TIMEOUT);
    }

    public Optional<JsonEnvelope> retrieveMessageWithMatchers(final String eventName, final Matcher matchers) {
        if (!messageConsumerClientMap.containsKey(eventName)) {
            return Optional.empty();
        }

        return retrieveMessageWithMatchers(matchers, messageConsumerClientMap.get(eventName));
    }

    private Optional<JsonEnvelope> retrieveMessageWithMatchers(final Matcher matchers, JmsMessageConsumerClient messageConsumer) {

        final AtomicReference<String> message = new AtomicReference<>();
        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL, TimeUnit.MILLISECONDS)
                .pollDelay(POLL_DELAY, TimeUnit.MILLISECONDS)
                .until(
                        () -> {
                            Optional<String> msg = messageConsumer.retrieveMessage(RETRIEVE_TIMEOUT);

                            if (msg.isPresent()) {
                                message.set(msg.get());
                            }
                            return message.get();
                        }, (allOf(matchers)));

        return Optional.of(message.get())
                .map(new DefaultJsonObjectEnvelopeConverter()::asEnvelope);

    }


    public void sendMessage(final String name, final JsonEnvelope envelope) {
        publicMessageProducerClient.sendMessage(name, envelope);
    }

}

