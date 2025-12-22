package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.function.Function;
import java.util.stream.Stream;
import javax.json.JsonValue;
import uk.gov.justice.core.courts.AddCaseCourtDocument;
import uk.gov.justice.core.courts.AddApplicationCourtDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.DocumentDetails;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddApplicationMaterialV2;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddCpsMaterial;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterial;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterialV2;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterials;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;


@ServiceComponent(COMMAND_HANDLER)
public class AddMaterialHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    ReferenceDataQueryService referenceDataQueryService;

    @Inject
    ProgressionService progressionService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final String DEFENDANT = "defendant";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_URN = "caseUrn";
    private static final String MATERIAL_TYPE = "materialType";
    private static final String FILE_SERVICE_ID = "fileServiceId";
    private static final String CASE_ID = "caseId";
    private static final String DOB = "dob";
    private static final String FORENAMES = "forenames";
    private static final String OUCODE = "oucode";
    private static final String SURNAME = "surname";

    @Handles("prosecutioncasefile.command.add-material")
    public void addMaterial(final Envelope<AddMaterial> command) throws EventStreamException {

        final AddMaterial addMaterial = command.payload();
        final UUID caseId = addMaterial.getCaseId();

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
                prosecutionCaseFile.addMaterial(
                        caseId,
                        addMaterial.getProsecutingAuthority(),
                        addMaterial.getProsecutorDefendantId(),
                        addMaterial.getMaterial(),
                        referenceDataQueryService,
                        addMaterial.getIsCpsCase(),
                        addMaterial.getReceivedDateTime()));
    }

    @Handles("prosecutioncasefile.command.add-materials")
    public void addMaterials(final Envelope<AddMaterials> command) throws EventStreamException {

        final AddMaterials addMaterials = command.payload();
        final UUID caseId = addMaterials.getCaseId();

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
                prosecutionCaseFile.addMaterials(
                        caseId,
                        addMaterials.getProsecutingAuthority(),
                        addMaterials.getProsecutorDefendantId(),
                        addMaterials.getMaterials(),
                        referenceDataQueryService,
                        addMaterials.getIsCpsCase(),
                        addMaterials.getReceivedDateTime()));
    }

    @Handles("prosecutioncasefile.command.add-material-v2")
    public void addMaterialV2(final Envelope<AddMaterialV2> command) throws EventStreamException {

        final AddMaterialV2 addMaterialV2 = command.payload();
        final UUID caseId = addMaterialV2.getCaseId();

        final AddMaterialCommonV2 addMaterialCommonV2 = new AddMaterialCommonV2.Builder()
                .withSubmissionId(addMaterialV2.getSubmissionId())
                .withCaseId(addMaterialV2.getCaseId())
                .withCaseType(addMaterialV2.getCaseType())
                .withMaterial(addMaterialV2.getMaterial())
                .withMaterialContentType(addMaterialV2.getMaterialContentType())
                .withMaterialType(addMaterialV2.getMaterialType())
                .withMaterialName(addMaterialV2.getMaterialName())
                .withFileName(addMaterialV2.getFileName())
                .withDocumentTypeId(addMaterialV2.getDocumentTypeId())
                .withDocumentType(addMaterialV2.getDocumentType())
                .withDocumentCategory(addMaterialV2.getDocumentCategory())
                .withSectionOrderSequence(addMaterialV2.getSectionOrderSequence())
                .withCaseSubFolderName(addMaterialV2.getCaseSubFolderName())
                .withExhibit(addMaterialV2.getExhibit())
                .withWitnessStatement(addMaterialV2.getWitnessStatement())
                .withTag(addMaterialV2.getTag())
                .withProsecutionCaseSubject(addMaterialV2.getProsecutionCaseSubject())
                .withReceivedDateTime(addMaterialV2.getReceivedDateTime())
                .withIsCpsCase(addMaterialV2.getIsCpsCase())
                .build();

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        final Stream<Object> events = prosecutionCaseFile.addMaterialV2(addMaterialCommonV2, referenceDataQueryService);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(command.metadata(), JsonValue.NULL);

        final Stream<JsonEnvelope> mappedEvents = mapNewEventsToEnvelope(events, jsonEnvelope);

        eventStream.append(mappedEvents);
    }


    @Handles("prosecutioncasefile.command.add-application-material-v2")
    public void addApplicationMaterialV2(final Envelope<AddApplicationMaterialV2> command) throws EventStreamException {

        final AddApplicationMaterialV2 addMaterialV2 = command.payload();
        final UUID applicationId = addMaterialV2.getApplicationId();

        final AddMaterialCommonV2 addMaterialCommonV2 = new AddMaterialCommonV2.Builder()
                .withSubmissionId(addMaterialV2.getSubmissionId())
                .withMaterial(addMaterialV2.getMaterial())
                .withMaterialContentType(addMaterialV2.getMaterialContentType())
                .withMaterialType(addMaterialV2.getMaterialType())
                .withMaterialName(addMaterialV2.getMaterialName())
                .withFileName(addMaterialV2.getFileName())
                .withDocumentTypeId(addMaterialV2.getDocumentTypeId())
                .withDocumentType(addMaterialV2.getDocumentType())
                .withDocumentCategory(addMaterialV2.getDocumentCategory())
                .withSectionOrderSequence(addMaterialV2.getSectionOrderSequence())
                .withCaseSubFolderName(addMaterialV2.getCaseSubFolderName())
                .withExhibit(addMaterialV2.getExhibit())
                .withWitnessStatement(addMaterialV2.getWitnessStatement())
                .withTag(addMaterialV2.getTag())
                .withReceivedDateTime(addMaterialV2.getReceivedDateTime())
                .withCourtApplicationSubject(addMaterialV2.getCourtApplicationSubject())
                .build();

        appendApplicationEventsToStream(applicationId, command, applicationFile ->
                applicationFile.addMaterialV2(addMaterialCommonV2, referenceDataQueryService, progressionService));
    }

    @Handles("prosecutioncasefile.command.handler.add-cps-material")
    public void addCpsMaterial(final Envelope<AddCpsMaterial> command) throws EventStreamException {

        final AddCpsMaterial addMaterial = command.payload();
        final UUID caseId = addMaterial.getCaseId();
        final Metadata metadata = command.metadata();
        final Integer materialType = command.payload().getCmsDocumentIdentifier().getMaterialType();

        String cmsDocumentId = null;

        if (nonNull(addMaterial.getCmsDocumentIdentifier())) {
            cmsDocumentId = addMaterial.getCmsDocumentIdentifier().getDocumentId();
        }
        final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = getDocumentTypeAccessRefData(metadata, materialType.toString());

        final DocumentDetails documentDetails = new DocumentDetails(cmsDocumentId, materialType,
                documentTypeAccessReferenceData != null ? documentTypeAccessReferenceData.getSectionCode() : addMaterial.getMaterial().getDocumentType());

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
        {
            final Material material = addMaterial.getMaterial();
            final Material updatedMaterial = Material.material()
                    .withDocumentType(documentTypeAccessReferenceData != null ? documentTypeAccessReferenceData.getSection() : addMaterial.getMaterial().getDocumentType())
                    .withFileStoreId(material.getFileStoreId())
                    .withFileType(material.getFileType())
                    .build();
            return prosecutionCaseFile.addCpsMaterial(
                    caseId,
                    addMaterial.getProsecutingAuthority(),
                    addMaterial.getProsecutorDefendantId(),
                    updatedMaterial,
                    referenceDataQueryService,
                    addMaterial.getReceivedDateTime(),
                    documentDetails);
        });
    }


    private DocumentTypeAccessReferenceData getDocumentTypeAccessRefData(final Metadata metadata, final String materialType) {
        final ParentBundleSectionReferenceData parentBundleSectionByCpsBundleCode =
                referenceDataQueryService.getParentBundleSectionByCpsBundleCode(metadata, materialType);

        if(parentBundleSectionByCpsBundleCode != null && parentBundleSectionByCpsBundleCode.getTargetSectionCode()!=null){
            final DocumentTypeAccessReferenceData documentTypeAccessReferenceData = referenceDataQueryService.getDocumentTypeAccessBySectionCode(metadata, parentBundleSectionByCpsBundleCode.getTargetSectionCode());
            if(documentTypeAccessReferenceData!=null){
                return documentTypeAccessReferenceData;
            }
        }
        return null;
    }


    @Handles("prosecutioncasefile.handler.add-idpc-material")
    public void addIdpcMaterial(final JsonEnvelope jsonEnvelope) throws EventStreamException {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final JsonObject defendantJson = payload.getJsonObject(DEFENDANT);
        final String caseUrn = payload.getString(CASE_URN);
        final UUID fileServiceId = fromString(payload.getString(FILE_SERVICE_ID));
        final String materialType = payload.getString(MATERIAL_TYPE);
        final UUID caseId = fromString(payload.getString(CASE_ID));


        appendEventsToStream(caseId, jsonEnvelope, prosecutionCaseFile ->
                prosecutionCaseFile.populateIdpcMaterialReceived(caseId,
                        caseUrn,
                        fileServiceId,
                        materialType,
                        mapDefendant(defendantJson)));

        appendEventsToStream(caseId, jsonEnvelope, prosecutionCaseFile ->
                prosecutionCaseFile.addIdpcCaseMaterial(caseId,
                        caseUrn,
                        fileServiceId,
                        materialType,
                        mapDefendant(defendantJson)));
    }

    @Handles("prosecutioncasefile.handler.case-updated-initiate-idpc-match")
    public void spiCcCaseUpdated(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        final UUID caseId = fromString(payload.getString(CASE_ID));
        appendEventsToStream(caseId, jsonEnvelope, ProsecutionCaseFile::caseUpdated);
    }

    @SuppressWarnings("squid:S1612")
    @Handles("prosecutioncasefile.command.add-case-court-document")
    public void addCaseCourtDocument(final Envelope<AddCaseCourtDocument> command) throws EventStreamException {
        final DefendantDocument defendantDocument = command.payload().getCourtDocument().getDocumentCategory().getDefendantDocument();
        final CaseDocument caseDocument = command.payload().getCourtDocument().getDocumentCategory().getCaseDocument();

        final UUID caseId = ofNullable(defendantDocument).map(DefendantDocument::getProsecutionCaseId).orElseGet(()->caseDocument.getProsecutionCaseId());

        appendEventsToStream(caseId, command, prosecutionCaseFile ->
                prosecutionCaseFile.addCourtDocument(command.payload().getCourtDocument(), command.payload().getMaterialId(), command.payload().getFileStoreId()));

    }

    @Handles("prosecutioncasefile.command.add-application-court-document")
    public void addApplicationCourtDocument(final Envelope<AddApplicationCourtDocument> command) throws EventStreamException {
        final UUID applicationId = command.payload().getApplicationId();

        appendApplicationEventsToStream(applicationId, command, applicationFile ->
                applicationFile.addCourtDocument(command.payload().getCourtDocument(), command.payload().getMaterialId(), command.payload().getFileStoreId()));
    }

    private Defendant mapDefendant(final JsonObject defendantJson) {
        final String dob = defendantJson.getString(DOB, null);
        final String forenames = defendantJson.getString(FORENAMES, null);
        final String ouCode = defendantJson.getString(OUCODE, null);
        final String defendantId = defendantJson.getString(DEFENDANT_ID, null);
        final String surname = defendantJson.getString(SURNAME);
        return new Defendant.Builder()
                .withDefendantId(defendantId)
                .withDob(dob)
                .withForenames(forenames)
                .withOucode(ouCode)
                .withSurname(surname)
                .build();
    }

    private Stream<JsonEnvelope> mapNewEventsToEnvelope(final Stream<Object> events, final JsonEnvelope commandEnvelope) {
        return events.map(event -> mapNewEventsToEnvelope(commandEnvelope , event));
    }

    private JsonEnvelope mapNewEventsToEnvelope(final JsonEnvelope commandEnvelope, final Object event) {
        final Function<Object, JsonEnvelope> payloadToEnvelope;

        if (event instanceof MaterialAddedV2) {
            final MaterialAddedV2 materialAdded = (MaterialAddedV2) event;

            final JsonEnvelope eventEnvelope = replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(commandEnvelope, materialAdded);
            payloadToEnvelope = toEnvelopeWithMetadataFrom(eventEnvelope);
        } else {
            payloadToEnvelope = toEnvelopeWithMetadataFrom(commandEnvelope);
        }

        return payloadToEnvelope.apply(event);
    }

    private JsonEnvelope replaceCaseSubmissionIdWithMaterialSubmissionIdFromOriginalEvent(final JsonEnvelope jsonEnvelope, final MaterialAddedV2 materialAddedV2) {
        final JsonObject commandMetadata = jsonEnvelope.metadata().asJsonObject();
        final JsonObject commandMetadataWithEventSubmission = createObjectBuilder(commandMetadata).add("submissionId", materialAddedV2.getSubmissionId().toString()).build();
        final Metadata eventMetadata = metadataFrom(commandMetadataWithEventSubmission).build();
        return envelopeFrom(eventMetadata, objectToJsonObjectConverter.convert(materialAddedV2));
    }
}
