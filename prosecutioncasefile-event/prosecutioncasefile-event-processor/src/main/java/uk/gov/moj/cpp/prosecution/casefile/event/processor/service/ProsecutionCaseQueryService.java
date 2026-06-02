package uk.gov.moj.cpp.prosecution.casefile.event.processor.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProsecutionCaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQueryService.class);

    public static final String CASE_URN = "prosecutionCaseReference";

    private static final String QUERY_PROSECUTION_CASES_BY_CASE_URN = "prosecutioncasefile.query.case-by-prosecutionCaseReference";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getProsecutionCaseByCaseUrn(final JsonEnvelope envelope, final String caseUrn) {
        JsonObject result = null;
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_URN, caseUrn)
                .build();

        LOGGER.info("Get prosecution case detail request for caseUrn {}", caseUrn);

        final JsonEnvelope prosecutionCase = envelopeFrom(metadataFrom(envelope.metadata())
                .withName(QUERY_PROSECUTION_CASES_BY_CASE_URN), requestParameter);

        final Envelope<JsonObject> response = requester.requestAsAdmin(prosecutionCase, JsonObject.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseUrn {} prosecution case detail payload {}", caseUrn, prosecutionCase.toObfuscatedDebugString());
        }

        if (null != response.payload() && JsonValue.NULL != response.payload()) {
            result = response.payload();
        }

        return result;
    }

}
