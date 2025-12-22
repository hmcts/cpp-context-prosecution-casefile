package uk.gov.moj.cpp.prosecution.casefile.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DefenceServiceImpl implements DefenceService {

    private static final String DEFENCE_QUERY_ASSOCIATED_ORGANISATION = "defence.query.associated-organisation";
    private static final String DEFENDANT_ID = "defendantId";

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    @Override
    public JsonObject getAssociatedOrganisation(final UUID defendantId) {
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(DEFENCE_QUERY_ASSOCIATED_ORGANISATION),
                createObjectBuilder().add(DEFENDANT_ID, defendantId.toString()));

        return requester.requestAsAdmin(envelope, JsonObject.class).payload();
    }
}
