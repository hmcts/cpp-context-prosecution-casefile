package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.Lists;

@ServiceComponent(COMMAND_API)
public class SjpProsecutionUpdateOffenceCodeApi {

    public static final String OFFENCE_CODE = "offenceCode";
    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("prosecutioncasefile.sjp-prosecution-update-offence-code")
    public void updateOffenceCode(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final List<OffenceReferenceData> offencesRefData = referenceDataQueryService.retrieveOffenceDataList(Lists.newArrayList(payload.getString(OFFENCE_CODE)), Optional.empty());
        final OffenceReferenceData offenceReferenceData = offencesRefData.get(0);

        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("caseId", payload.getString("caseId"))
                .add(OFFENCE_CODE, payload.getString(OFFENCE_CODE))
                .add("offenceReferenceData", objectToJsonObjectConverter.convert(offenceReferenceData))
                .build();

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("prosecutioncasefile.command.sjp-prosecution-update-offence-code").build(),
                commandPayload));

    }
}
