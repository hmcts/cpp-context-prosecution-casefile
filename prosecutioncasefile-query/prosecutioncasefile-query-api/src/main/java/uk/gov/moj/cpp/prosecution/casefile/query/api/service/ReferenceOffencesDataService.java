package uk.gov.moj.cpp.prosecution.casefile.query.api.service;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import javax.json.JsonValue;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

public class ReferenceOffencesDataService {

    private static final String REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST = "referencedataoffences.query.offences-list";
    private static final String CJS_OFFENCE_CODE = "cjsoffencecode";
    private static final String DATE = "date";
    private static final String OFFENCES = "offences";

    public JsonObject getOffenceReferenceData(final Envelope<?> originatingQuery, final Requester requester, final String offenceCode, final String date) {

        final Envelope<JsonObject> requestEnvelope = envelop(createObjectBuilder()
                .add(CJS_OFFENCE_CODE, offenceCode)
                .add(DATE, date)
                .build())
                .withName(REFERENCE_DATA_OFFENCE_QUERY_OFFENCE_LIST).withMetadataFrom(originatingQuery);

        final JsonEnvelope response = requester.request(requestEnvelope);
        return response.payload().equals(JsonValue.NULL)?  null :  response.payloadAsJsonObject().getJsonArray(OFFENCES).getJsonObject(0) ;
    }
}
