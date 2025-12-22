package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import javax.json.JsonObject;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

public class SjpService {

    public static final String INITIATION_CODE = "J";
    public static final String URN = "urn";
    public static final String POSTCODE = "postcode";
    public static final String SJP_FIND_CASE_BY_URN_POSTCODE_QUERY = "sjp.query.case-by-urn-postcode";

    public JsonObject findCase(final Envelope<?> envelope, final Requester requester, final String caseUrn, final String postcode) {
        final JsonObject findCaseByUrnAndPostcodeRequest = createObjectBuilder()
                .add(URN, caseUrn)
                .add(POSTCODE, postcode)
                .build();
        final Envelope<JsonObject> requestEnvelope = envelop(findCaseByUrnAndPostcodeRequest)
                .withName(SJP_FIND_CASE_BY_URN_POSTCODE_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.request(requestEnvelope, JsonObject.class);
        return response.payload();
    }

}
