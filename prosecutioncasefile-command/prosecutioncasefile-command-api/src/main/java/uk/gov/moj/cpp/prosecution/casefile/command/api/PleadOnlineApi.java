package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.service.AddressService.normalizePostcodeInAddress;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode.J;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.command.api.validator.PleadOnlineValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnlinePcqVisited;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    private static final String ADDRESS = "address";
    private static final String PERSONAL_DETAILS = "personalDetails";
    private static final String EMPLOYER = "employer";
    private static final String CASE_ID = "caseId";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASE = "progression.query.prosecutioncase";

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private PleadOnlineValidator pleadOnlineValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("prosecutioncasefile.plead-online")
    public void pleadOnline(final Envelope<PleadOnline> envelope) {
        final PleadOnline pleadOnline = envelope.payload();
        final JsonObject payload = objectToJsonObjectConverter.convert(envelope.payload());

        validatePleaPayload(pleadOnline);
        validateCaseForPlea(envelope);

        final JsonObjectBuilder pleaOnlineObjectBuilder = createObjectBuilderWithFilter(payload, field -> !asList(PERSONAL_DETAILS, EMPLOYER).contains(field));

        if (payload.containsKey(PERSONAL_DETAILS)) {
            pleaOnlineObjectBuilder.add(PERSONAL_DETAILS, replacePostcodeInPayload(payload, PERSONAL_DETAILS));
        }

        if (payload.containsKey(EMPLOYER)) {
            pleaOnlineObjectBuilder.add(EMPLOYER, replacePostcodeInPayload(payload, EMPLOYER));
        }

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("prosecutioncasefile.command.plead-online").build(),
                pleaOnlineObjectBuilder.build()));
    }

    @Handles("prosecutioncasefile.plead-online-pcq-visited")
    public void pleadOnlinePCQVisited(final Envelope<PleadOnlinePcqVisited> envelope) {
        final JsonObject payload = objectToJsonObjectConverter.convert(envelope.payload());

        final JsonObjectBuilder pleaOnlineObjectBuilder = createObjectBuilder(payload);

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("prosecutioncasefile.command.plead-online-pcq-visited").build(),
                pleaOnlineObjectBuilder.build()));
    }

    private void validateCaseForPlea(final Envelope<PleadOnline> envelope) {
        checkValidationErrors(validateCase(envelope));
    }

    private void validatePleaPayload(final PleadOnline pleadOnline) {
        checkValidationErrors(pleadOnlineValidator.validate(pleadOnline));
    }

    private Map<String, List<String>> validateCase(final Envelope<PleadOnline> envelope) {
        final PleadOnline pleadOnline = envelope.payload();
        if (isSjpCase(pleadOnline)) {
            final JsonObject caseDetail = getCaseDetailFromSjp(envelope);
            return pleadOnlineValidator.validate(caseDetail);
        } else {//CC case
            return pleadOnlineValidator.validate(getCaseDetailFromProgression(envelope));
        }
    }

    private boolean isSjpCase(final PleadOnline pleadOnline) {
        return J == pleadOnline.getInitiationCode();
    }

    private void checkValidationErrors(Map<String, List<String>> validationErrors) {
        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }
    }


    private JsonObject getCaseDetailFromSjp(final Envelope<PleadOnline> envelope) {
        final JsonObject queryCasePayload = JsonObjects.createObjectBuilder()
                .add(CASE_ID, envelope.payload().getCaseId().toString())
                .build();
        final Envelope<JsonObject> requestEnvelope = envelop(queryCasePayload)
                .withName("sjp.query.case").withMetadataFrom(envelope);
        return requester.requestAsAdmin(requestEnvelope, JsonObject.class).payload();
    }

    private JsonObjectBuilder replacePostcodeInPayload(final JsonObject payload, final String objectToUpdate) {
        final JsonObjectBuilder objectToUpdateBuilder = createObjectBuilderWithFilter(payload.getJsonObject(objectToUpdate),
                field -> !field.contains(ADDRESS));

        objectToUpdateBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(objectToUpdate).getJsonObject(ADDRESS)));

        return objectToUpdateBuilder;
    }

    public ProsecutionCase getCaseDetailFromProgression(final Envelope<PleadOnline> envelope) {

        final Envelope<JsonObject> queryEnvelope = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PROGRESSION_QUERY_PROSECUTION_CASE)
                        .build(),
                createObjectBuilder().add(CASE_ID, envelope.payload().getCaseId().toString()).build());

        final JsonObject response = requester.requestAsAdmin(queryEnvelope, JsonObject.class).payload();
        ProsecutionCase prosecutionCase = null;
        if (nonNull(response) && response.containsKey("prosecutionCase")) {
            prosecutionCase = jsonObjectToObjectConverter.convert(response.getJsonObject("prosecutionCase"), ProsecutionCase.class);
        }
        return prosecutionCase;
    }

}
