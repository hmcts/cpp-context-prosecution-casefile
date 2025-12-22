package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    static final String REFERENCEDATA_GET_DOCUMENT_BUNDLE = "referencedata.query.parent-bundle-section";
    static final String UNBUNDLE_FLAG = "unbundleFlag";
    static final String CPS_BUNDLE_CODE = "cpsBundleCode";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public boolean isDocumentNeedsUnBundling(final int cmsMaterialType) {

        final JsonObject payload = createObjectBuilder().add(CPS_BUNDLE_CODE, String.valueOf(cmsMaterialType)).build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_DOCUMENT_BUNDLE),
                payload);

        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);
        final JsonObject responseJson = jsonResultEnvelope.payloadAsJsonObject();

        return nonNull(responseJson) && responseJson.getBoolean(UNBUNDLE_FLAG);
    }
}
