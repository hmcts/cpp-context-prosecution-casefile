package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.time.LocalDate.parse;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase.acceptCase;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.CaseDefendantChangedCommand.caseDefendantChangedCommand;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.EjectCase.ejectCase;

import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected;
import uk.gov.justice.progression.courts.CaseOrApplicationEjected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptGroupCases;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CaseDefendantChangedCommand;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.EjectCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectGroupCases;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class ProgressionPublicEventProcessor {

    public static final String APPLICATION_ID = "applicationId";
    private static final String FIELD_ID = "id";
    private static final String FIELD_GROUP_ID = "groupId";
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String FIELD_CASE_URN = "caseUrn";
    private static final String FIELD_REFERRAL_REASON_ID = "referralReasonId";
    private static final String COURT_APPLICATION = "courtApplication";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final Logger LOGGER = getLogger(ProgressionPublicEventProcessor.class);

    @Handles("public.progression.prosecution-case-created")
    public void handleProsecutionCaseCreated(final JsonEnvelope prosecutionCaseCreated) {

        final JsonObject prosecutionCase = prosecutionCaseCreated.payloadAsJsonObject().getJsonObject("prosecutionCase");
        final UUID caseId = fromString(prosecutionCase.getString(FIELD_ID));

        final List<UUID> defendantIds = prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class).stream()
                .map(defendant -> fromString(defendant.getString(FIELD_ID))).collect(toList());

        final boolean hasMigrationSourceSystem = prosecutionCase.containsKey("migrationSourceSystem")
                && prosecutionCase.getJsonObject("migrationSourceSystem") != null;
        if (!hasMigrationSourceSystem) {
            final AcceptCase acceptCase = acceptCase()
                    .withCaseId(caseId)
                    .withDefendantIds(defendantIds)
                    .build();

            final Metadata metadata = metadataFrom(prosecutionCaseCreated.metadata())
                    .withName("prosecutioncasefile.command.accept-case")
                    .build();

            this.sender.send(envelopeFrom(metadata, acceptCase));
        }
    }

    @Handles("public.progression.group-prosecution-cases-created")
    public void handleGroupProsecutionCasesCreated(final JsonEnvelope groupProsecutionCasesCreated) {
        LOGGER.info("public.progression.group-prosecution-cases-created event");

        final String groupIdStr = groupProsecutionCasesCreated.payloadAsJsonObject().getString(FIELD_GROUP_ID);
        final UUID groupId = fromString(groupIdStr);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cases created event for Group with id '{}'", groupId);
        }

        final AcceptGroupCases acceptGroupCases = AcceptGroupCases.acceptGroupCases()
                .withGroupId(groupId)
                .build();

        final Metadata metadata = metadataFrom(groupProsecutionCasesCreated.metadata())
                .withName("prosecutioncasefile.command.handler.accept-group-cases")
                .build();

        sender.send(envelopeFrom(metadata, acceptGroupCases));
    }

    @Handles("public.progression.events.case-or-application-ejected")
    public void handleCaseEjected(final Envelope<CaseOrApplicationEjected> ejectedEnvelope) {
        final Optional<EjectCase> ejectCase = ofNullable(ejectedEnvelope.payload().getProsecutionCaseId()).map(e ->
                ejectCase().withCaseId(e).build());
        ejectCase.ifPresent(eject -> {
            final Metadata metadata = metadataFrom(ejectedEnvelope.metadata())
                    .withName("prosecutioncasefile.command.eject-case")
                    .build();
            sender.send(envelopeFrom(metadata, eject));
        });

    }

    @Handles("public.progression.refer-prosecution-cases-to-court-accepted")
    public void handleReferProsecutionCaseToCourtAccepted(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.record-refer-prosecution-cases-to-court-accepted")
                .build();

        sender.send(envelopeFrom(metadata, createObjectBuilder()
                .add(FIELD_CASE_ID, payload.getString(FIELD_CASE_ID))
                .add(FIELD_REFERRAL_REASON_ID, payload.getString(FIELD_REFERRAL_REASON_ID))
                .build()));
    }

    @Handles("public.progression.court-application-summons-rejected")
    public void handleCourtApplicationSummonsRejected(final Envelope<PublicProgressionCourtApplicationSummonsRejected> envelope) {
        LOGGER.info("public.progression.court-application-summons-rejected event received for case: {}", envelope.payload().getProsecutionCaseId());

        final PublicProgressionCourtApplicationSummonsRejected eventPayload = envelope.payload();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.reject-case-defendants-as-summons-application-rejected")
                .build();

        final JsonObject commandPayload = createObjectBuilder()
                .add(FIELD_CASE_ID, eventPayload.getProsecutionCaseId().toString())
                .add(APPLICATION_ID, eventPayload.getId().toString())
                .add("summonsRejectedOutcome", objectToJsonObjectConverter.convert(eventPayload.getSummonsRejectedOutcome()))
                .build();
        sender.send(envelopeFrom(metadata, commandPayload));
    }

    @Handles("public.progression.court-application-summons-approved")
    public void handleCourtApplicationSummonsApproved(final Envelope<PublicProgressionCourtApplicationSummonsApproved> envelope) {
        LOGGER.info("public.progression.court-application-summons-approved event received for case: {}", envelope.payload().getProsecutionCaseId());
        final PublicProgressionCourtApplicationSummonsApproved eventPayload = envelope.payload();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved")
                .build();
        final JsonObject commandPayload = createObjectBuilder()
                .add(FIELD_CASE_ID, eventPayload.getProsecutionCaseId().toString())
                .add(APPLICATION_ID, eventPayload.getId().toString())
                .add("summonsApprovedOutcome", objectToJsonObjectConverter.convert(eventPayload.getSummonsApprovedOutcome()))
                .build();
        sender.send(envelopeFrom(metadata, commandPayload));
    }

    @Handles("public.progression.case-defendant-changed")
    public void handleCaseDefendantChanged(final JsonEnvelope envelope) {
        final JsonObject defendant = envelope.payloadAsJsonObject().getJsonObject("defendant");
        final UUID defendantId = fromString(defendant.getString("id"));
        final Optional<JsonObject> personDefendant = getJsonObject(defendant, "personDefendant");
        final Optional<JsonObject> legalEntityDefendant = getJsonObject(defendant, "legalEntityDefendant");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        final String caseId = defendant.getString(FIELD_PROSECUTION_CASE_ID);

        if (personDefendant.isPresent()) {
            final Optional<JsonObject> personDetails = getJsonObject(personDefendant.get(), "personDetails");
            if (personDetails.isPresent()) {
                final JsonObject person = personDetails.get();
                final PersonalInformation.Builder personalInformationBuilder = new PersonalInformation.Builder()
                        .withFirstName(getString(person, "firstName").orElse(null))
                        .withLastName(getString(person, "lastName").orElse(null))
                        .withTitle(getString(person, "title").orElse(null));
                if (nonNull(person.getJsonObject("contact"))) {
                    personalInformationBuilder.withContactDetails(jsonObjectToObjectConverter.convert(person.getJsonObject("contact"), ContactDetails.class));
                }
                if (nonNull(person.getJsonObject("address"))) {
                    personalInformationBuilder.withAddress(jsonObjectToObjectConverter.convert(person.getJsonObject("address"), Address.class));
                }

                final CaseDefendantChangedCommand.Builder caseDefendantChangedCommand = caseDefendantChangedCommand()
                        .withDefendantId(defendantId)
                        .withPersonDetails(personalInformationBuilder.build());

                final String dateOfBirth = getString(person, "dateOfBirth").orElse(null);
                if (nonNull(dateOfBirth)) {
                    caseDefendantChangedCommand.withDateOfBirth(parse(dateOfBirth));
                }

                final Metadata metadata = metadataFrom(envelope.metadata())
                        .withName("prosecutioncasefile.command.case-defendant-changed")
                        .build();

                sender.send(envelopeFrom(metadata, caseDefendantChangedCommand.build()));
                objectToJsonObjectConverter.convert(caseDefendantChangedCommand.build()).forEach(payloadBuilder::add);
                payloadBuilder.add(FIELD_CASE_ID, caseId);
                updateCaseWithDefendant(envelope, payloadBuilder);

            }
        } else if (legalEntityDefendant.isPresent()) {
            payloadBuilder.add("organisationName", legalEntityDefendant.get().getJsonObject("organisation").getString("name"));
            payloadBuilder.add("defendantId", defendantId.toString());
            payloadBuilder.add(FIELD_CASE_ID, caseId);
            updateCaseWithDefendant(envelope, payloadBuilder);
        } else {
            LOGGER.info("public.progression.case-defendant-changed : person defendant or legalEntityDefendant is not in the payload");
        }
    }

    private void updateCaseWithDefendant(final JsonEnvelope envelope, final JsonObjectBuilder payloadBuilder) {
        final Metadata metadata2 = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.update-case-with-defendant")
                .build();

        sender.send(envelopeFrom(metadata2, payloadBuilder.build()));
    }

    @Handles("public.progression.court-application-created")
    public void handleCourtApplicationCreated(final Envelope<CourtApplicationCreated> envelope) {
        LOGGER.info("public.progression.court-application-created");
        final CourtApplicationCreated eventPayload = envelope.payload();
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName("prosecutioncasefile.command.update-application-status")
                .build();
        final JsonObject commandPayload = createObjectBuilder()
                .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(eventPayload.getCourtApplication()))
                .build();
        sender.send(envelopeFrom(metadata, commandPayload));
    }

    @Handles("public.progression.events.civil-case-exists")
    public void handleCivilCaseExists(final JsonEnvelope civilCaseExists) {
        final JsonObject payload = civilCaseExists.payloadAsJsonObject();
        final UUID groupId = payload.containsKey(FIELD_GROUP_ID) ? fromString(payload.getString(FIELD_GROUP_ID)) : null;
        final UUID caseId = payload.containsKey(FIELD_PROSECUTION_CASE_ID) ? fromString(payload.getString(FIELD_PROSECUTION_CASE_ID)) : null;
        final String caseUrn = payload.containsKey(FIELD_CASE_URN) ? payload.getString(FIELD_CASE_URN) : null;

        LOGGER.info("public.progression.events.civil-case-exists with groupId: {}, caseId: {}, caseUrn: {}", groupId, caseId, caseUrn);
        final RejectGroupCases rejectGroupCases = RejectGroupCases.rejectGroupCases()
                .withGroupId(groupId)
                .withCaseId(caseId)
                .withCaseUrn(caseUrn)
                .build();

        final Metadata metadata = metadataFrom(civilCaseExists.metadata())
                .withName("prosecutioncasefile.command.handler.reject-group-cases")
                .build();

        sender.send(envelopeFrom(metadata, rejectGroupCases));
    }
}
