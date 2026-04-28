package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.query.api.service.SjpService.SJP_FIND_CASE_BY_URN_POSTCODE_QUERY;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpServiceTest {
    @Mock
    private Requester requester;

    @InjectMocks
    private SjpService sjpService;

    private final String caseUrn = randomAlphanumeric(10);
    private final String postcode = randomAlphanumeric(6);

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;
    @Mock
    private Envelope<JsonObject> requestEnvelope;

    @Test
    public void shouldFindCaseByUrnAndPostcode() {
        final Metadata metadata = metadataBuilder().withName(SJP_FIND_CASE_BY_URN_POSTCODE_QUERY).withId(randomUUID()).withUserId(randomUUID().toString()).build();
        final Envelope envelope = envelopeFrom(metadata, createObjectBuilder().build());

        when(requester.request(envelopeCaptor.capture(), any())).thenReturn(envelope);

        final JsonObject caseResponse = sjpService.findCase(envelope, requester, caseUrn, postcode);

        assertThat(caseResponse, is(notNullValue()));
        assertThat(envelopeCaptor.getValue().metadata().name(), is(SJP_FIND_CASE_BY_URN_POSTCODE_QUERY));
        assertThat(envelopeCaptor.getValue().payload().getString("urn"), is(caseUrn));
        assertThat(envelopeCaptor.getValue().payload().getString("postcode"), is(postcode));
    }

}