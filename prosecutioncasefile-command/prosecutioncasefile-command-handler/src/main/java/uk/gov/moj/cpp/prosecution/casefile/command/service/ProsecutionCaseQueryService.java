package uk.gov.moj.cpp.prosecution.casefile.command.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class ProsecutionCaseQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseQueryService.class);

    public static final String CASE_ID = "caseId";
    public static final String CASE_URN = "prosecutionCaseReference";

    private static final String QUERY_PROSECUTION_CASES_BY_CASE_URN = "prosecutioncasefile.query.case-by-prosecutionCaseReference";
    private static final String QUERY_PROSECUTION_CASES_BY_CASE_ID = "prosecutioncasefile.query.case";

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    public JsonObject getProsecutionCaseByCaseUrn(final JsonEnvelope envelope, final String caseUrn) {
        JsonObject result = null;
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_URN, caseUrn)
                .build();

        LOGGER.info("caseId {} , Get prosecution case detail request {}", caseUrn, requestParameter);

        final JsonEnvelope prosecutionCase = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(QUERY_PROSECUTION_CASES_BY_CASE_URN), requestParameter);

        final Envelope<JsonObject> response = requester.requestAsAdmin(prosecutionCase, JsonObject.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseUrn, prosecutionCase.toObfuscatedDebugString());
        }

        if (null != response.payload() && JsonValue.NULL != response.payload()) {
            result = response.payload();
        }

        return result;
    }

    public JsonObject getProsecutionCaseByCaseId(final JsonEnvelope envelope, final String caseId) {
        JsonObject result = null;
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} , Get prosecution case detail request {}", caseId, requestParameter);

        final JsonEnvelope prosecutionCase = envelopeFrom(metadataFrom(envelope.metadata())
                .withName(QUERY_PROSECUTION_CASES_BY_CASE_ID), requestParameter);

        final Envelope<JsonObject> response = requester.requestAsAdmin(prosecutionCase, JsonObject.class);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutionCase.toObfuscatedDebugString());
        }

        if (null != response.payload() && JsonValue.NULL != response.payload()) {
            result = response.payload();
        }
        return result;
    }

}
