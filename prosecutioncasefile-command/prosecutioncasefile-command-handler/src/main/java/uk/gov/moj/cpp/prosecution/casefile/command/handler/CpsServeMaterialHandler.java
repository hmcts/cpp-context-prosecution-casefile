package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.nameUUIDFromBytes;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.SubmissionStatus.PENDING;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.SubmissionStatus.SUCCESS;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.repository.jdbc.exception.InvalidStreamIdException;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServePtph;
import uk.gov.moj.cpp.cps.prosecutioncasefile.command.handler.staging.PtphValidationData;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.CpsServeMaterialAggregate;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.DefenceService;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CpsFormValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CpsRejectBcmForTimerExpire;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CpsRejectPetForTimerExpire;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeBcm;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeCotr;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServePet;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsUpdateCotr;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ValidationData;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"pmd:NullAssignment"})
@ServiceComponent(COMMAND_HANDLER)
public class CpsServeMaterialHandler extends BaseCpsServeMaterialHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsServeMaterialHandler.class);
    private static final String CASE_ID = "caseId";

    @Inject
    ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefenceService defenceService;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    @Inject
    private CpsFormValidator cpsFormValidator;


    @Handles("prosecutioncasefile.command.process-received-cps-serve-pet")
    public void cpsServePetReceived(final Envelope<ProcessReceivedCpsServePet> envelope) throws EventStreamException {
        final ProcessReceivedCpsServePet processReceivedCpsServePet = envelope.payload();
        final String caseUrn = envelope.payload().getProsecutionCaseSubject().getUrn();
        final EventStream eventStream = eventSource.getStreamById(nameUUIDFromBytes(caseUrn.getBytes(UTF_8)));
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        final Optional<JsonObject> prosecutionCase = isCaseCreated(jsonEnvelope, caseUrn);
        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);
        final ValidationData validationData = processReceivedCpsServePet.getValidationData();
        final EventStream eventCaseStream = eventSource.getStreamById(caseId);
        ProsecutionCaseFile prosecutionCaseFile = new ProsecutionCaseFile();
        if(Objects.nonNull(eventCaseStream)){
            try {
                prosecutionCaseFile = aggregateService.get(eventCaseStream, ProsecutionCaseFile.class);
            }catch(InvalidStreamIdException e){
                LOGGER.error("No stream found for caseID {} {}",caseId,e);
            }
        }
        eventStream.append(aggregate
                .cpsReceivePet(objectToJsonObjectConverter.convert(processReceivedCpsServePet),
                        (prosecutionCase.isPresent() ? SUCCESS : PENDING).name(),
                        caseId,
                        prosecutionCase,
                        cpsFormValidator,
                        validationData.getValidOffences(),
                        convertDefendantIdsToJsonArray(validationData),
                        jsonObjectToObjectConverter,prosecutionCaseFile,
                        progressionService,
                        defenceService)
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("prosecutioncasefile.command.cps-reject-pet-for-timer-expire")
    public void cpsServePetTimerExpired(final Envelope<CpsRejectPetForTimerExpire> envelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(envelope.payload().getTimerUUID());
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(aggregate
                .cpsRejectPetForTimerExpire()
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("prosecutioncasefile.command.process-received-cps-serve-bcm")
    public void cpsServeBcmReceived(final Envelope<ProcessReceivedCpsServeBcm> envelope) throws EventStreamException {
        final ProcessReceivedCpsServeBcm processReceivedCpsServeBcm = envelope.payload();
        final String caseUrn = processReceivedCpsServeBcm.getProsecutionCaseSubject().getUrn();
        final EventStream eventStream = eventSource.getStreamById(UUID.nameUUIDFromBytes(caseUrn.getBytes(StandardCharsets.UTF_8)));
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        final Optional<JsonObject> prosecutionCase = isCaseCreated(jsonEnvelope, caseUrn);
        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);
        final ValidationData validationData = processReceivedCpsServeBcm.getValidationData();
        eventStream.append(aggregate
                .cpsReceiveBcm(objectToJsonObjectConverter.convert(processReceivedCpsServeBcm),
                        (prosecutionCase.isPresent() ? SUCCESS : PENDING).name(),
                        caseId,
                        prosecutionCase,
                        cpsFormValidator,
                        validationData.getValidOffences(),
                        convertDefendantIdsToJsonArray(validationData))
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("prosecutioncasefile.command.process-received-cps-serve-ptph")
    public void cpsServePtphReceived(final Envelope<ProcessReceivedCpsServePtph> envelope) throws EventStreamException {
        final ProcessReceivedCpsServePtph processReceivedCpsServePtph = envelope.payload();
        final String caseUrn = processReceivedCpsServePtph.getProsecutionCaseSubject().getUrn();
        final EventStream eventStream = eventSource.getStreamById(nameUUIDFromBytes(caseUrn.getBytes(UTF_8)));
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        final Optional<JsonObject> prosecutionCase = isCaseCreated(jsonEnvelope, caseUrn);
        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);
        final PtphValidationData validationData = processReceivedCpsServePtph.getPtphValidationData();
        final Optional<OrganisationUnitReferenceData> organisationUnit = fetchOrganisationUnit(processReceivedCpsServePtph);

        eventStream.append(aggregate
                .cpsReceivePtph(objectToJsonObjectConverter.convert(processReceivedCpsServePtph),
                        (prosecutionCase.isPresent() ? SUCCESS : PENDING).name(),
                        caseId,
                        prosecutionCase,
                        cpsFormValidator,
                        convertPtphDefendantIdsToJsonArray(validationData),
                        organisationUnit,
                        jsonObjectToObjectConverter,
                        objectToJsonObjectConverter)
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("prosecutioncasefile.command.cps-reject-bcm-for-timer-expire")
    public void cpsServeBcmTimerExpired(final Envelope<CpsRejectBcmForTimerExpire> envelope) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(envelope.payload().getTimerUUID());
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        eventStream.append(aggregate
                .cpsRejectBcmForTimerExpire()
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    public Optional<JsonObject> isCaseCreated(final JsonEnvelope jsonEnvelope, final String caseUrn) {
        final JsonObject prosecutionCase = prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(jsonEnvelope, caseUrn);
        return Optional.ofNullable(prosecutionCase);
    }

    private JsonArray convertDefendantIdsToJsonArray(final ValidationData validationData) {
        final JsonArrayBuilder cpsDefendantIdsAndDefendantIdsListBuilder = createArrayBuilder();
        if(nonNull(validationData) && validationData.getDefendantIds() != null) {
                validationData.getDefendantIds().forEach(defendantId -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    if (nonNull(defendantId.getCpsDefendantId())) {
                        builder.add(CPS_DEFENDANT_ID, defendantId.getCpsDefendantId());
                    }
                    if (nonNull(defendantId.getDefendantId())) {
                        builder.add(DEFENDANT_ID, defendantId.getDefendantId().toString());
                    }
                    cpsDefendantIdsAndDefendantIdsListBuilder.add(builder.build());
                });
            }
        return cpsDefendantIdsAndDefendantIdsListBuilder.build();
    }

    private JsonArray convertPtphDefendantIdsToJsonArray(final PtphValidationData ptphValidationData) {
        final JsonArrayBuilder cpsDefendantIdsListBuilder = createArrayBuilder();

        if (ptphValidationData.getDefendantIds() != null) {
            ptphValidationData.getDefendantIds().forEach(defendantId -> cpsDefendantIdsListBuilder.add(createObjectBuilder()
                    .add(CPS_DEFENDANT_ID, defendantId.getCpsDefendantId())
                    .add(DEFENDANT_ID, defendantId.getDefendantId().toString())));
        }

        return cpsDefendantIdsListBuilder.build();
    }

    @SuppressWarnings("squid:S2629")
    private Optional<OrganisationUnitReferenceData> fetchOrganisationUnit(ProcessReceivedCpsServePtph processReceivedCpsServePtph) {
        final String businessUnitCode = processReceivedCpsServePtph.getCpsOffice();
        final List<OrganisationUnitReferenceData> organisationUnits = referenceDataQueryService.retrieveOrganisationUnitsByOuCode(businessUnitCode);

        if (organisationUnits != null && organisationUnits.size() != 1) {
            LOGGER.error(String.format("Expected to get one organisation unit from reference data for given oucode: %s, but found %d", businessUnitCode, organisationUnits.size()));
        }

        return organisationUnits != null && !organisationUnits.isEmpty() ? Optional.of(organisationUnits.get(0)) : Optional.empty();
    }

    @Handles("prosecutioncasefile.command.process-received-cps-serve-cotr")
    public void cpsServeCotrReceived(final Envelope<ProcessReceivedCpsServeCotr> envelope) throws EventStreamException {
        final ProcessReceivedCpsServeCotr processReceivedCpsServeCotr = envelope.payload();
        final String caseUrn = processReceivedCpsServeCotr.getProsecutionCaseSubject().getUrn();
        final EventStream eventStream = eventSource.getStreamById(UUID.nameUUIDFromBytes(caseUrn.getBytes(StandardCharsets.UTF_8)));
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        final Optional<JsonObject> prosecutionCase = isCaseCreated(jsonEnvelope, caseUrn);
        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);
        final ValidationData validationData = processReceivedCpsServeCotr.getValidationData();
        LOGGER.info("cpsServeCotrReceived validationData for Defendant Ids {}", nonNull(validationData) ? validationData.getDefendantIds() : null);
        
        eventStream.append(aggregate
                .cpsReceiveCotr(objectToJsonObjectConverter.convert(processReceivedCpsServeCotr),
                        (prosecutionCase.isPresent() ? SUCCESS : PENDING).name(),
                        caseId,
                        prosecutionCase,
                        cpsFormValidator,
                        nonNull(validationData) ? convertDefendantIdsToJsonArray(validationData) : null)
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }

    @Handles("prosecutioncasefile.command.process-received-cps-update-cotr")
    public void cpsUpdateCotrReceived(final Envelope<ProcessReceivedCpsUpdateCotr> envelope) throws EventStreamException {
        final ProcessReceivedCpsUpdateCotr processReceivedCpsUpdateCotr = envelope.payload();
        final String caseUrn = processReceivedCpsUpdateCotr.getProsecutionCaseSubject().getUrn();
        final EventStream eventStream = eventSource.getStreamById(UUID.nameUUIDFromBytes(caseUrn.getBytes(StandardCharsets.UTF_8)));
        final CpsServeMaterialAggregate aggregate = aggregateService.get(eventStream, CpsServeMaterialAggregate.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), NULL);
        final Optional<JsonObject> prosecutionCase = isCaseCreated(jsonEnvelope, caseUrn);
        final UUID caseId = prosecutionCase.map(jsonObject -> fromString((jsonObject.getString(CASE_ID)))).orElse(null);
        eventStream.append(aggregate
                .cpsUpdateCotr(objectToJsonObjectConverter.convert(processReceivedCpsUpdateCotr),
                        (prosecutionCase.isPresent() ? SUCCESS : PENDING).name(),
                        caseId)
                .map(toEnvelopeWithMetadataFrom(jsonEnvelope)));

    }

}
