package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static org.apache.commons.lang3.StringUtils.removeEnd;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.CreateGroupCaseRequest;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls case management (service-cp-crime-caseadmin-case-management) to create the group_case row
 * for a received group (CAD-1850, contract POST /groupcases).
 *
 * <p>The caller is PCF's context system user, sent as CJSCPPUID; case management only lets members
 * of "System Users" create a group case. 200 (created) and 409 (already exists, i.e. a redelivery)
 * are success. Anything else, or no response, throws {@link CaseManagementException} so the event is
 * redelivered and then dead-lettered.
 *
 * <p>Logs only the group id and HTTP status: the request and response contain personal data.
 */
@ApplicationScoped
public class CaseManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseManagementService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    static final String GROUP_CASES_PATH = "/groupcases";
    static final String CJSCPPUID = "CJSCPPUID";
    static final String CPPCLIENTCORRELATIONID = "CPPCLIENTCORRELATIONID";
    private static final int OK = 200;
    private static final int CONFLICT = 409;

    @Inject
    @Value(key = "casemanagement.base.url", defaultValue = "http://localhost:8080/casemanagement")
    String baseUrl;

    @Inject
    private CaseManagementHttpClientProvider httpClientProvider;

    @Inject
    private SystemUserProvider systemUserProvider;

    public void createGroupCase(final CreateGroupCaseRequest request, final Metadata metadata) {
        final UUID groupId = request.getGroupId();
        final UUID systemUserId = systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new CaseManagementException(
                        "No context system user to call case management for group " + groupId, CaseManagementException.NO_STATUS));

        final HttpPost post = new HttpPost(removeEnd(baseUrl, "/") + GROUP_CASES_PATH);
        post.addHeader(CJSCPPUID, systemUserId.toString());
        metadata.clientCorrelationId().ifPresent(correlationId -> post.addHeader(CPPCLIENTCORRELATIONID, correlationId));

        final int status;
        try {
            post.setEntity(new StringEntity(OBJECT_MAPPER.writeValueAsString(request), ContentType.APPLICATION_JSON));
            final HttpResponse response = httpClientProvider.httpClient().execute(post);
            status = response.getStatusLine().getStatusCode();
            // Always consume the body, even unread: an unconsumed body keeps the pooled connection
            // leased, and a run of error responses would then exhaust the pool.
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (final IOException e) {
            throw new CaseManagementException("Case management call failed for group " + groupId, e);
        }

        if (status == OK) {
            LOGGER.info("Case management created group case for group {}", groupId);
        } else if (status == CONFLICT) {
            LOGGER.info("Case management already has group case for group {} (409), treating as created", groupId);
        } else {
            throw new CaseManagementException("Case management returned " + status + " creating group case for group " + groupId, status);
        }
    }
}
