package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionUpdateOffenceCodeApiTest {

    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private OffenceReferenceData offenceReferenceData;

    @InjectMocks
    private SjpProsecutionUpdateOffenceCodeApi sjpProsecutionUpdateOffenceCodeApi;

    @Test
    public void shouldSendCommandWhenUpdatingOffenceCode() {
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any()))
                .thenReturn(singletonList(offenceReferenceData));
        when(objectToJsonObjectConverter.convert(offenceReferenceData))
                .thenReturn(createObjectBuilder().build());

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.sjp-prosecution-update-offence-code").build(),
                createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .add("offenceCode", "TH68001")
                        .build());

        sjpProsecutionUpdateOffenceCodeApi.updateOffenceCode(envelope);

        verify(sender).send(any(Envelope.class));
    }
}