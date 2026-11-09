package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import uk.gov.justice.services.common.configuration.Value;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the pooled HTTP client used to call case management. Exposed through this class rather than a
 * CDI producer of {@link HttpClient}, so it cannot clash with any other HttpClient in the deployment.
 * Timeouts and pool sizes are JNDI values, the same set Hearing uses for the results validator.
 */
@ApplicationScoped
public class CaseManagementHttpClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseManagementHttpClientProvider.class);

    @Inject
    @Value(key = "casemanagement.timeout.ms", defaultValue = "5000")
    String socketTimeoutMs;

    @Inject
    @Value(key = "casemanagement.timeout.connect.ms", defaultValue = "1000")
    String connectTimeoutMs;

    @Inject
    @Value(key = "casemanagement.timeout.connection.request.ms", defaultValue = "1000")
    String connectionRequestTimeoutMs;

    @Inject
    @Value(key = "casemanagement.pool.max.total", defaultValue = "50")
    String poolMaxTotal;

    @Inject
    @Value(key = "casemanagement.pool.max.per.route", defaultValue = "50")
    String poolMaxPerRoute;

    @Inject
    @Value(key = "casemanagement.evict.idle.seconds", defaultValue = "30")
    String evictIdleSeconds;

    private CloseableHttpClient client;

    @PostConstruct
    void createClient() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(Integer.parseInt(poolMaxTotal));
        connectionManager.setDefaultMaxPerRoute(Integer.parseInt(poolMaxPerRoute));

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Integer.parseInt(connectTimeoutMs))
                .setSocketTimeout(Integer.parseInt(socketTimeoutMs))
                .setConnectionRequestTimeout(Integer.parseInt(connectionRequestTimeoutMs))
                .build();

        client = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(Long.parseLong(evictIdleSeconds), TimeUnit.SECONDS)
                .evictExpiredConnections()
                .build();
    }

    public HttpClient httpClient() {
        return client;
    }

    @PreDestroy
    void close() {
        if (client != null) {
            try {
                client.close();
            } catch (final IOException e) {
                LOGGER.warn("Failed to close the case management HTTP client", e);
            }
        }
    }
}
