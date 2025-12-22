package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ReferenceDataService.CPS_BUNDLE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ReferenceDataService.REFERENCEDATA_GET_DOCUMENT_BUNDLE;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ReferenceDataService.UNBUNDLE_FLAG;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonObject responsePayload;

    @Captor
    private ArgumentCaptor<JsonEnvelope> requestJsonEnvelope;

    private Integer materialType = 1;

    @Test
    public void shouldReturnTrueWhenUnbundlingNeededForDocument() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);
        when(responsePayload.getBoolean(UNBUNDLE_FLAG)).thenReturn(true);

        boolean response = referenceDataService.isDocumentNeedsUnBundling(materialType);

        assertThat(response, is(true));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString(CPS_BUNDLE_CODE), is(String.valueOf(materialType)));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_DOCUMENT_BUNDLE));
    }

    @Test
    public void shouldReturnFalseWhenUnbundlingNotNeededForDocument() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(responsePayload);
        when(responsePayload.getBoolean(UNBUNDLE_FLAG)).thenReturn(false);

        boolean response = referenceDataService.isDocumentNeedsUnBundling(materialType);

        assertThat(response, is(false));
        assertThat(requestJsonEnvelope.getValue().payloadAsJsonObject().getString(CPS_BUNDLE_CODE), is(String.valueOf(materialType)));
        assertThat(requestJsonEnvelope.getValue().metadata().id(), notNullValue());
        assertThat(requestJsonEnvelope.getValue().metadata().name(), is(REFERENCEDATA_GET_DOCUMENT_BUNDLE));
    }

    @Test
    public void shouldReturnFalseWhenResponsePayloadIsNull() {

        when(requester.requestAsAdmin(requestJsonEnvelope.capture())).thenReturn(jsonEnvelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(null);

        boolean response = referenceDataService.isDocumentNeedsUnBundling(materialType);

        assertThat(response, is(false));
    }

}