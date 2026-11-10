package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.CreateGroupCaseRequest;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.LeadCase;
import uk.gov.moj.cpp.prosecution.casefile.casemanagement.model.ResultingIntent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseManagementServiceTest {

    private static final String BASE_URL = "http://casemanagement.example/base";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Mock
    private CaseManagementHttpClientProvider httpClientProvider;

    @Mock
    private HttpClient httpClient;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Captor
    private ArgumentCaptor<HttpPost> postCaptor;

    @InjectMocks
    private CaseManagementService caseManagementService;

    private final UUID systemUserId = randomUUID();
    private final UUID groupId = randomUUID();
    private RecordingInputStream responseBody;

    @BeforeEach
    void setUp() {
        setField(caseManagementService, "baseUrl", BASE_URL);
    }

    @Nested
    @DisplayName("request")
    class Request {

        @Test
        void createGroupCase_should_postJsonToGroupCasesWithSystemUserAndCorrelationHeaders() throws Exception {
            givenSystemUser();
            givenResponse(200);

            caseManagementService.createGroupCase(request(null), metadata("example-correlation-id"));

            verify(httpClient).execute(postCaptor.capture());
            final HttpPost post = postCaptor.getValue();
            assertThat(post.getURI().toString(), is(BASE_URL + "/groupcases"));
            assertThat(post.getFirstHeader("CJSCPPUID").getValue(), is(systemUserId.toString()));
            assertThat(post.getFirstHeader("CPPCLIENTCORRELATIONID").getValue(), is("example-correlation-id"));
            assertThat(post.getEntity().getContentType().getValue().startsWith("application/json"), is(true));

            final JsonNode body = OBJECT_MAPPER.readTree(EntityUtils.toString(post.getEntity()));
            assertThat(body.get("groupId").asText(), is(groupId.toString()));
            assertThat(body.get("dateOfHearing").asText(), is("2050-02-04"));
            assertThat(body.get("caseCount").asInt(), is(3));
            assertThat(body.get("resultingIntent").asText(), is("GRANT"));
            assertThat(body.get("leadCase").get("caseUrn").asText(), is("EXAMPLEURN1"));
            assertThat(body.has("courtroomId"), is(false));
            assertThat(body.has("parentGroupId"), is(false));
        }

        @Test
        void createGroupCase_baseUrlWithTrailingSlash_should_notDoubleTheSlash() throws Exception {
            setField(caseManagementService, "baseUrl", BASE_URL + "/");
            givenSystemUser();
            givenResponse(200);

            caseManagementService.createGroupCase(request(null), metadata(null));

            verify(httpClient).execute(postCaptor.capture());
            assertThat(postCaptor.getValue().getURI().toString(), is(BASE_URL + "/groupcases"));
        }

        @Test
        void createGroupCase_noCorrelationId_should_omitTheHeader() throws Exception {
            givenSystemUser();
            givenResponse(200);

            caseManagementService.createGroupCase(request(randomUUID()), metadata(null));

            verify(httpClient).execute(postCaptor.capture());
            assertThat(postCaptor.getValue().getFirstHeader("CPPCLIENTCORRELATIONID"), is(nullValue()));
        }

        @Test
        void createGroupCase_noContextSystemUser_should_throwWithoutCalling() throws Exception {
            when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

            final CreateGroupCaseRequest request = request(null);
            final Metadata metadata = metadata(null);
            assertThrows(CaseManagementException.class, () -> caseManagementService.createGroupCase(request, metadata));

            verify(httpClient, never()).execute(any(HttpPost.class));
        }
    }

    @Nested
    @DisplayName("response")
    class Response {

        @ParameterizedTest
        @ValueSource(ints = {200, 409})
        void createGroupCase_200Or409_should_succeedAndConsumeTheBody(final int status) throws Exception {
            givenSystemUser();
            givenResponse(status);

            caseManagementService.createGroupCase(request(null), metadata(null));

            assertThat(responseBody.closed, is(true));
        }

        @ParameterizedTest
        @ValueSource(ints = {201, 400, 403, 404, 500, 503})
        void createGroupCase_anyOtherStatus_should_throwWithTheStatusAndConsumeTheBody(final int status) throws Exception {
            givenSystemUser();
            givenResponse(status);

            final CreateGroupCaseRequest request = request(null);
            final Metadata metadata = metadata(null);
            final CaseManagementException exception = assertThrows(CaseManagementException.class,
                    () -> caseManagementService.createGroupCase(request, metadata));

            assertThat(exception.getStatusCode(), is(status));
            assertThat(responseBody.closed, is(true));
        }

        @Test
        void createGroupCase_ioFailure_should_throwCaseManagementException() throws Exception {
            givenSystemUser();
            when(httpClientProvider.httpClient()).thenReturn(httpClient);
            when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("connection refused"));

            final CreateGroupCaseRequest request = request(null);
            final Metadata metadata = metadata(null);
            final CaseManagementException exception = assertThrows(CaseManagementException.class,
                    () -> caseManagementService.createGroupCase(request, metadata));

            assertThat(exception.getStatusCode(), is(CaseManagementException.NO_STATUS));
        }
    }

    private void givenSystemUser() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
    }

    private void givenResponse(final int status) throws IOException {
        responseBody = new RecordingInputStream("{}");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(responseBody);
        final HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, status, "example");
        response.setEntity(entity);
        when(httpClientProvider.httpClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);
    }

    private CreateGroupCaseRequest request(final UUID courtroomId) {
        return new CreateGroupCaseRequest()
                .groupId(groupId)
                .leadCase(new LeadCase().leadCaseId(randomUUID()).caseUrn("EXAMPLEURN1").defendantName("Alex Example"))
                .dateOfHearing(LocalDate.of(2050, 2, 4))
                .caseCount(3)
                .offenceType("Example offence title")
                .courtHearingLocation("B01LY00")
                .courtCentreId(randomUUID())
                .courtroomId(courtroomId)
                .prosecutingAuthority("EXAMPLE01")
                .prosecutingAuthorityName("Example Borough Council")
                .resultingIntent(ResultingIntent.GRANT);
    }

    private static Metadata metadata(final String clientCorrelationId) {
        final MetadataBuilder builder = metadataBuilder()
                .withId(randomUUID())
                .withName("prosecutioncasefile.events.group-cases-received");
        if (clientCorrelationId != null) {
            builder.withClientCorrelationId(clientCorrelationId);
        }
        return builder.build();
    }

    private static final class RecordingInputStream extends InputStream {
        private final InputStream delegate;
        private boolean closed;

        RecordingInputStream(final String content) {
            this.delegate = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }
}
