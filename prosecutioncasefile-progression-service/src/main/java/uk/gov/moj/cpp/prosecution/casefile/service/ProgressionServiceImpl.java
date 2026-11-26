package uk.gov.moj.cpp.prosecution.casefile.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProgressionServiceImpl implements ProgressionService {

    private static final String PROGRESSION_QUERY_APPLICATION_ONLY = "progression.query.application-only";
    private static final String PROGRESSION_QUERY_CASE = "progression.query.prosecutioncase";

    private static final String APPLICATION_ID = "applicationId";
    private static final String CASE_ID = "caseId";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Override
    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public CourtApplication getApplicationOnly(final UUID applicationId) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(PROGRESSION_QUERY_APPLICATION_ONLY),
                createObjectBuilder().add(APPLICATION_ID, applicationId.toString()));
        final JsonObject app = requester.requestAsAdmin(envelope).asJsonObject();

        if (app.containsKey("courtApplication")) {
            return jsonObjectToObjectConverter.convert(app.getJsonObject("courtApplication"), CourtApplication.class);
        } else {
            return null;
        }
    }

    public JsonObject getProsecutionCase(final UUID caseId) {
        final JsonEnvelope envelope = envelopeFrom(getMetadataBuilder(PROGRESSION_QUERY_CASE),
                createObjectBuilder().add(CASE_ID, caseId.toString()));

        return requester.requestAsAdmin(envelope, JsonObject.class).payload();
    }


    private MetadataBuilder getMetadataBuilder(final String queryName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(queryName);
    }
}
