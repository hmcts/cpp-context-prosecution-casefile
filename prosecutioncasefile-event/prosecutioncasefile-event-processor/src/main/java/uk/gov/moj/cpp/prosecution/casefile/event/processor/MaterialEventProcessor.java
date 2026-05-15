package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentBundleArrivedForUnbundling.documentBundleArrivedForUnbundling;


import org.apache.commons.collections.CollectionUtils;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.BulkScanMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.IdpcUploadMaterialProcessService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingIdpcMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CmsDocumentIdentifier;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentBundleArrivedForUnbundling;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedWithWarnings;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class MaterialEventProcessor {

    private static final String PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED = "public.prosecutioncasefile.material-added";
    private static final String PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED_V2 = "public.prosecutioncasefile.material-added-v2";
    private static final String PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED_WITH_WARNINGS = "public.prosecutioncasefile.material-added-with-warnings";
    private static final String MATERIAL_COMMAND_UPLOAD_FILE = "material.command.upload-file";
    private static final String CASE_ID_FIELD = "caseId";
    private static final String APPLICATION_ID_FIELD = "applicationId";
    private static final String DEFENDANT_ID_FIELD = "defendantId";
    private static final String DOCUMENT_TYPE_ID_FIELD = "documentTypeId";
    private static final String DOCUMENT_TYPE_DESCRIPTION_FIELD = "documentTypeDescription";
    private static final String DOCUMENT_CATEGORY_FIELD = "documentCategory";
    private static final String MATERIAL_ID_FIELD = "materialId";
    private static final String IS_UNBUNDLED_DOCUMENT = "isUnbundledDocument";
    private static final String FILE_SERVICE_ID_FIELD = "fileServiceId";
    public static final String RECEIVED_DATE_TIME = "receivedDateTime";
    public static final String IDPC_SECTION_CODE = "IDPC";
    public static final String SECTION_CODE = "sectionCode";
    public static final String IS_CPS_CASE = "isCpsCase";
    static final String PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE = "public.prosecutioncasefile.document-bundle-arrived-for-unbundling";
    private static final String FILE_CLOUD_LOCATION = "fileCloudLocation";

    @Inject
    private Sender sender;
    @Inject
    private FileStorer fileStorer;
    @Inject
    private EnvelopeHelper envelopeHelper;
    @Inject
    private PendingMaterialExpiration pendingMaterialExpiration;
    @Inject
    private MetadataHelper metadataHelper;
    @Inject
    private IdpcUploadMaterialProcessService idpcUploadMaterialProcessService;
    @Inject
    private PendingIdpcMaterialExpiration idpcPendingMaterialExpiration;
    @Inject
    private BulkScanMaterialExpiration bulkScanMaterialExpiration;
    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("prosecutioncasefile.events.material-added")
    public void handleMaterialAdded(final Envelope<MaterialAdded> materialAddedEvent) {
        final MaterialAdded materialAdded = materialAddedEvent.payload();

        final UUID fileStoreId = materialAdded.getMaterial().getFileStoreId();

        if (CaseType.SJP.toString().equals(materialAddedEvent.payload().getCaseType())) {
            final JsonEnvelope envelope = getSJPUploadDocumentPayload(materialAddedEvent, fileStoreId);
            sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelope));
        } else {
            uploadToMaterialContext(materialAddedEvent, fileStoreId.toString(), 1);
        }

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);
        initiateMaterialForUnbundling(materialAddedEvent);
        sender.send(envelop(materialAdded)
                .withName(PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED)
                .withMetadataFrom(materialAddedEvent));

    }

    @Handles("prosecutioncasefile.events.material-added-v2")
    public void handleMaterialAddedV2(final Envelope<MaterialAddedV2> materialAddedEvent) {
        final MaterialAddedV2 materialAdded = materialAddedEvent.payload();

        final UUID fileStoreId = materialAdded.getMaterial();

        uploadToMaterialContextV2(materialAddedEvent, fileStoreId.toString(), 2);

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);

        sender.send(envelop(materialAdded)
                .withName(PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED_V2)
                .withMetadataFrom(materialAddedEvent));
    }

    @Handles("prosecutioncasefile.events.material-added-with-warnings")
    public void handleMaterialAddedWithWarnings(final Envelope<MaterialAddedWithWarnings> materialAddedEvent) {
        final MaterialAddedWithWarnings materialAdded = materialAddedEvent.payload();

        final UUID fileStoreId = materialAdded.getMaterial();

        uploadToMaterialContextWithWarnings(materialAddedEvent, fileStoreId.toString(), 2);

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);

        sender.send(envelop(materialAdded)
                .withName(PUBLIC_PROSECUTIONCASEFILE_MATERIAL_ADDED_WITH_WARNINGS)
                .withMetadataFrom(materialAddedEvent));
    }

    private void initiateMaterialForUnbundling(final Envelope<MaterialAdded> materialAddedEvent) {
        final MaterialAdded materialAdded = materialAddedEvent.payload();

        final CmsDocumentIdentifier documentIdentifier = materialAdded.getCmsDocumentIdentifier();
        if (nonNull(documentIdentifier) &&
                referenceDataService.isDocumentNeedsUnBundling(documentIdentifier.getMaterialType())) {

            final DocumentBundleArrivedForUnbundling documentBundleArrivedForUnbundling = documentBundleArrivedForUnbundling()
                    .withCaseId(materialAdded.getCaseId())
                    .withProsecutingAuthority(materialAdded.getProsecutingAuthority())
                    .withProsecutorDefendantId(materialAdded.getProsecutorDefendantId())
                    .withMaterial(materialAdded.getMaterial())
                    .withCmsDocumentIdentifier(materialAdded.getCmsDocumentIdentifier())
                    .withReceivedDateTime(materialAdded.getReceivedDateTime())
                    .withDefendantName(materialAdded.getDefendantName())
                    .build();
            sender.send(envelopeFrom(metadataFrom(materialAddedEvent.metadata())
                            .withName(PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE).build(),
                    objectToJsonObjectConverter.convert(documentBundleArrivedForUnbundling)));
        }
    }

    private void uploadToMaterialContext(final Envelope<MaterialAdded> materialAddedEvent, final String fileStoreId, final int version) {
        final MaterialAdded materialAdded = materialAddedEvent.payload();

        final JsonObjectBuilder ccMetadataBuilder = createObjectBuilder()
                .add(CASE_ID_FIELD, materialAdded.getCaseId().toString())
                .add(DOCUMENT_TYPE_ID_FIELD, materialAdded.getDocumentTypeId())
                .add(DOCUMENT_CATEGORY_FIELD, materialAdded.getDocumentCategory())
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, materialAdded.getDocumentType());
        if (nonNull(materialAdded.getIsCpsCase())) {
            ccMetadataBuilder.add(IS_CPS_CASE, materialAdded.getIsCpsCase());
        }

        if (nonNull(materialAdded.getReceivedDateTime())) {
            ccMetadataBuilder.add(RECEIVED_DATE_TIME, materialAdded.getReceivedDateTime().toOffsetDateTime().toString());
        }

        if (DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(materialAdded.getDocumentCategory())) {
            ccMetadataBuilder.add(DEFENDANT_ID_FIELD, materialAdded.getDefendantId().toString());
        }

        if (nonNull(materialAdded.getSectionCode())) {
            ccMetadataBuilder.add(SECTION_CODE, materialAdded.getSectionCode());
        }

        final JsonObject ccMetadata = ccMetadataBuilder.build();

        final JsonObjectBuilder uploadFilePayloadBuilder = createObjectBuilder()
                .add(MATERIAL_ID_FIELD, randomUUID().toString())
                .add(FILE_SERVICE_ID_FIELD, fileStoreId);

        if (nonNull(materialAdded.getMaterial().getIsUnbundledDocument()) && materialAdded.getMaterial().getIsUnbundledDocument()) {
            uploadFilePayloadBuilder.add(IS_UNBUNDLED_DOCUMENT, true);
        }

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(materialAddedEvent.metadata()).withName(MATERIAL_COMMAND_UPLOAD_FILE).build(),
                ccMetadata,
                uploadFilePayloadBuilder.build(), version == 1 ? null : fileStoreId));
    }

    private void uploadToMaterialContextV2(final Envelope<MaterialAddedV2> materialAddedEvent, final String fileStoreId, final int version) {
        final MaterialAddedV2 materialAdded = materialAddedEvent.payload();

        final JsonObjectBuilder ccMetadataBuilder = createObjectBuilder()
                .add(DOCUMENT_TYPE_ID_FIELD, materialAdded.getDocumentTypeId())
                .add(DOCUMENT_CATEGORY_FIELD, materialAdded.getDocumentCategory())
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, materialAdded.getMaterialType());

        ofNullable(materialAdded.getCaseId()).ifPresent(caseId -> ccMetadataBuilder.add(CASE_ID_FIELD, caseId.toString()));
        ofNullable(materialAdded.getCourtApplicationSubject()).ifPresent(courtApplicationSubject -> ccMetadataBuilder.add(APPLICATION_ID_FIELD, courtApplicationSubject.getCourtApplicationId().toString()));

        if (nonNull(materialAdded.getIsCpsCase())) {
            ccMetadataBuilder.add(IS_CPS_CASE, materialAdded.getIsCpsCase());
        }

        if (nonNull(materialAdded.getReceivedDateTime())) {
            ccMetadataBuilder.add(RECEIVED_DATE_TIME, materialAdded.getReceivedDateTime().toOffsetDateTime().toString());
        }

        if (DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(materialAdded.getDocumentCategory())) {
            ccMetadataBuilder.add(DEFENDANT_ID_FIELD, materialAdded.getDefendantId().toString());
        }

        final JsonObject ccMetadata = ccMetadataBuilder.build();

        final JsonObjectBuilder uploadFilePayloadBuilder = createObjectBuilder()
                .add(MATERIAL_ID_FIELD, randomUUID().toString())
                .add(FILE_SERVICE_ID_FIELD, fileStoreId);

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(materialAddedEvent.metadata()).withName(MATERIAL_COMMAND_UPLOAD_FILE).build(),
                ccMetadata,
                uploadFilePayloadBuilder.build(), version == 1 ? null : fileStoreId));
    }

    private void uploadToMaterialContextWithWarnings(final Envelope<MaterialAddedWithWarnings> materialAddedEvent, final String fileStoreId, final int version) {
        final MaterialAddedWithWarnings materialAdded = materialAddedEvent.payload();

        final JsonObjectBuilder ccMetadataBuilder = createObjectBuilder()
                .add(DOCUMENT_TYPE_ID_FIELD, materialAdded.getDocumentTypeId())
                .add(DOCUMENT_CATEGORY_FIELD, materialAdded.getDocumentCategory())
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, materialAdded.getMaterialType());
        ofNullable(materialAdded.getCaseId()).ifPresent(caseId -> ccMetadataBuilder.add(CASE_ID_FIELD, caseId.toString()));
        ofNullable(materialAdded.getCourtApplicationSubject()).ifPresent(courtApplicationSubject -> ccMetadataBuilder.add(APPLICATION_ID_FIELD, courtApplicationSubject.getCourtApplicationId().toString()));

        if (nonNull(materialAdded.getIsCpsCase())) {
            ccMetadataBuilder.add(IS_CPS_CASE, materialAdded.getIsCpsCase());
        }

        if (nonNull(materialAdded.getReceivedDateTime())) {
            ccMetadataBuilder.add(RECEIVED_DATE_TIME, materialAdded.getReceivedDateTime().toOffsetDateTime().toString());
        }

        if (DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(materialAdded.getDocumentCategory())) {
            ccMetadataBuilder.add(DEFENDANT_ID_FIELD, materialAdded.getDefendantId().toString());
        }

        final JsonObject ccMetadata = ccMetadataBuilder.build();

        final JsonObjectBuilder uploadFilePayloadBuilder = createObjectBuilder()
                .add(MATERIAL_ID_FIELD, randomUUID().toString())
                .add(FILE_SERVICE_ID_FIELD, fileStoreId);

        sender.send(metadataHelper.envelopeWithCustomMetadata(
                metadataFrom(materialAddedEvent.metadata()).withName(MATERIAL_COMMAND_UPLOAD_FILE).build(),
                ccMetadata,
                uploadFilePayloadBuilder.build(), version == 1 ? null : fileStoreId));
    }

    private JsonEnvelope getSJPUploadDocumentPayload(final Envelope<MaterialAdded> materialAddedEvent, final UUID fileStoreId) {
        final Metadata metadata = Envelope.metadataFrom(materialAddedEvent.metadata()).withName("sjp.upload-case-document").build();

        final JsonObject payload = createObjectBuilder().add(CASE_ID_FIELD, materialAddedEvent.payload().getCaseId().toString())
                .add("caseDocument", fileStoreId.toString())
                .add("caseDocumentType", materialAddedEvent.payload().getMaterial().getDocumentType())
                .build();

        return envelopeFrom(metadata, payload);
    }

    @Handles("prosecutioncasefile.events.material-rejected")
    public void handleMaterialRejected(final Envelope<MaterialRejected> materialRejectedEnvelope) {
        final MaterialRejected materialRejected = materialRejectedEnvelope.payload();
        final UUID fileStoreId = materialRejected.getMaterial().getFileStoreId();
        final JsonEnvelope materialRejectedJsonEnvelope = envelopeFrom(materialRejectedEnvelope.metadata(), NULL);

        sender.send(envelop(materialRejected)
                .withName("public.prosecutioncasefile.material-rejected")
                .withMetadataFrom(materialRejectedJsonEnvelope));

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);
    }

    @Handles("prosecutioncasefile.events.material-rejected-v2")
    public void handleMaterialRejectedV2(final Envelope<MaterialRejectedV2> materialRejectedEnvelope) {
        final MaterialRejectedV2 materialRejected = materialRejectedEnvelope.payload();
        final UUID fileStoreId = materialRejected.getMaterial();
        final JsonEnvelope materialRejectedJsonEnvelope = envelopeFrom(materialRejectedEnvelope.metadata(), NULL);

        sender.send(envelop(materialRejected)
                .withName("public.prosecutioncasefile.material-rejected-v2")
                .withMetadataFrom(materialRejectedJsonEnvelope));

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);
    }

    @Handles("prosecutioncasefile.events.material-rejected-with-warnings")
    public void handleMaterialRejectedWithWarnings(final Envelope<MaterialRejectedWithWarnings> materialRejectedEnvelope) {
        final MaterialRejectedWithWarnings materialRejectedWithWarnings = materialRejectedEnvelope.payload();
        final UUID fileStoreId = materialRejectedWithWarnings.getMaterial();
        final JsonEnvelope materialRejectedJsonEnvelope = envelopeFrom(materialRejectedEnvelope.metadata(), NULL);

        sender.send(envelop(materialRejectedWithWarnings)
                .withName("public.prosecutioncasefile.material-rejected-with-warnings")
                .withMetadataFrom(materialRejectedJsonEnvelope));

        pendingMaterialExpiration.cancelMaterialTimer(fileStoreId);
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);
    }

    @Handles("prosecutioncasefile.events.case-document-review-required")
    public void handleDocumentReviewRequired(final Envelope<CaseDocumentReviewRequired> materialPendingEnvelope) {

        final CaseDocumentReviewRequired caseDocumentReviewRequired = materialPendingEnvelope.payload();

        final JsonEnvelope materialPendingJsonEnvelope = envelopeFrom(materialPendingEnvelope.metadata(), NULL);

        final DocumentReviewRequired documentReviewRequired = DocumentReviewRequired.documentReviewRequired()
                .withCaseId(caseDocumentReviewRequired.getCaseId())
                .withCmsDocumentId(caseDocumentReviewRequired.getCmsDocumentId())
                .withDocumentType(caseDocumentReviewRequired.getDocumentType())
                .withErrorCodes(caseDocumentReviewRequired.getErrorCodes())
                .withFileStoreId(caseDocumentReviewRequired.getFileStoreId())
                .withReceivedDateTime(caseDocumentReviewRequired.getReceivedDateTime())
                .withSource(caseDocumentReviewRequired.getSource())
                .withProsecutingAuthority(caseDocumentReviewRequired.getProsecutingAuthority())
                .build();

        sender.send(envelop(documentReviewRequired)
                .withName("public.prosecutioncasefile.document-review-required")
                .withMetadataFrom(materialPendingJsonEnvelope));
    }

    @Handles("prosecutioncasefile.events.case-document-review-required-v2")
    public void handleDocumentReviewRequiredV2(final Envelope<CaseDocumentReviewRequired> materialPendingEnvelope) {

        final CaseDocumentReviewRequired caseDocumentReviewRequired = materialPendingEnvelope.payload();

        final JsonEnvelope materialPendingJsonEnvelope = envelopeFrom(materialPendingEnvelope.metadata(), NULL);

        final DocumentReviewRequired documentReviewRequired = DocumentReviewRequired.documentReviewRequired()
                .withCaseId(caseDocumentReviewRequired.getCaseId())
                .withCmsDocumentId(caseDocumentReviewRequired.getCmsDocumentId())
                .withDocumentType(caseDocumentReviewRequired.getDocumentType())
                .withErrorCodes(caseDocumentReviewRequired.getErrorCodes())
                .withFileStoreId(caseDocumentReviewRequired.getFileStoreId())
                .withReceivedDateTime(caseDocumentReviewRequired.getReceivedDateTime())
                .withSource(caseDocumentReviewRequired.getSource())
                .withProsecutingAuthority(caseDocumentReviewRequired.getProsecutingAuthority())
                .build();

        sender.send(envelop(documentReviewRequired)
                .withName("public.prosecutioncasefile.document-review-required-v2")
                .withMetadataFrom(materialPendingJsonEnvelope));
    }

    @Handles("prosecutioncasefile.events.bulkscan-material-rejected")
    public void handleBulkScanMaterialRejected(final Envelope<BulkscanMaterialRejected> materialRejectedEnvelope) {
        final BulkscanMaterialRejected materialRejected = materialRejectedEnvelope.payload();
        final UUID fileStoreId = materialRejected.getMaterial().getFileStoreId();
        final JsonEnvelope materialRejectedJsonEnvelope = envelopeFrom(materialRejectedEnvelope.metadata(), NULL);
        sender.send(envelop(materialRejected).withName("public.prosecutioncasefile.bulkscan-material-followup").withMetadataFrom(materialRejectedJsonEnvelope));
        bulkScanMaterialExpiration.cancelMaterialTimer(fileStoreId);
    }

    @Handles("prosecutioncasefile.events.idpc-material-rejected")
    public void handleIdpcMaterialRejected(final Envelope<IdpcMaterialRejected> idpcMaterialRejectedEnvelope) throws FileServiceException {
        final IdpcMaterialRejected idpcMaterialRejected = idpcMaterialRejectedEnvelope.payload();
        final UUID fileServiceId = idpcMaterialRejected.getFileServiceId();

        idpcPendingMaterialExpiration.cancelIdpcMaterialTimer(fileServiceId);
        fileStorer.delete(fileServiceId);
    }

    @Handles("prosecutioncasefile.events.material-pending")
    public void handleMaterialPending(final Envelope<MaterialPending> materialPendingEnvelope) {
        final MaterialPending materialPending = materialPendingEnvelope.payload();
        final UUID fileStoreId = materialPending.getMaterial().getFileStoreId();
        final UUID caseId = materialPending.getCaseId();
        final Metadata metadata = materialPendingEnvelope.metadata();

        pendingMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
        bulkScanMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
    }

    @Handles("prosecutioncasefile.events.material-pending-v2")
    public void handleMaterialPendingV2(final Envelope<MaterialPendingV2> materialPendingEnvelope) {
        final MaterialPendingV2 materialPendingV2 = materialPendingEnvelope.payload();
        final UUID fileStoreId = materialPendingV2.getMaterial();
        final UUID caseId = materialPendingV2.getCaseId();
        final Metadata metadata = materialPendingEnvelope.metadata();

        pendingMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
        bulkScanMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);

        if (CollectionUtils.isNotEmpty(materialPendingV2.getWarnings())) {
            sender.send(envelop(materialPendingV2)
                    .withName("public.prosecutioncasefile.material-pending-with-warnings")
                    .withMetadataFrom(materialPendingEnvelope));
        }
    }

    @Handles("prosecutioncasefile.events.idpc-defendant-matched")
    public void idpcDefendantMatch(final Envelope<IdpcDefendantMatched> idpcDefendantMatch) {

        idpcUploadMaterialProcessService.startUploadFileProcess(idpcDefendantMatch);
        idpcPendingMaterialExpiration.cancelIdpcMaterialTimer(idpcDefendantMatch.payload().getFileServiceId());
    }

    @Handles("prosecutioncasefile.events.idpc-defendant-match-pending")
    public void handleIdpcDefendantMatchPending(final Envelope<IdpcDefendantMatchPending> idpcNoCaseMatch) {
        final IdpcDefendantMatchPending idpcDefendantMatchPending = idpcNoCaseMatch.payload();
        final UUID fileStoreId = idpcDefendantMatchPending.getFileServiceId();
        final UUID caseId = idpcDefendantMatchPending.getCaseId();
        final Metadata metadata = idpcNoCaseMatch.metadata();

        idpcPendingMaterialExpiration.startMaterialTimer(fileStoreId, caseId, metadata);
    }


    @Handles("material.material-added")
    public void handleMaterialAddedFromMaterialContext(final JsonEnvelope materialAddedEvent) throws FileServiceException {
        final Optional<JsonObject> ccMetadata = metadataHelper.getCCMetadata(materialAddedEvent);
        final String fileStoreId = metadataHelper.getFileStoreId(materialAddedEvent);

        final UUID materialId = fromString(materialAddedEvent.payloadAsJsonObject().getString(MATERIAL_ID_FIELD));
        final String fileCloudLocationId = ccMetadata.map(c -> c.getString(FILE_CLOUD_LOCATION, null)).orElse(null);

        if (ccMetadata.isPresent() && isNull(fileCloudLocationId)) {
            handleDocumentUploadedEvent(materialAddedEvent, ccMetadata.get(), materialId.toString(), fileStoreId);
        } else {
            final Optional<String> idpcProcessId = MetadataHelper.getIdpcProcessId(materialAddedEvent);
            if (idpcProcessId.isPresent()) {
                final String fileServiceId = idpcUploadMaterialProcessService.signalUploadFileProcess(materialAddedEvent, idpcProcessId.get(), materialId);
                fileStorer.delete(UUID.fromString(fileServiceId));
            }
        }
    }

    @Handles("prosecutioncasefile.events.court-document-added")
    public void handleCourtDocumentAdded(final JsonEnvelope courtDocumentAddedEvent) {
        final JsonObjectBuilder progressionPayloadBuilder = createObjectBuilder();

        courtDocumentAddedEvent.asJsonObject()
                .forEach((key, value) -> {
                    if (!"fileStoreId".equals(key)) {
                        progressionPayloadBuilder.add(key, value);
                    }
                });

        final JsonObject progressionPayload = progressionPayloadBuilder.build();

        final Metadata metadata = Envelope.metadataFrom(courtDocumentAddedEvent.metadata()).withName("progression.add-court-document-v2").build();
        sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelopeFrom(metadata, progressionPayload)));
    }

    private void handleDocumentUploadedEvent(final JsonEnvelope materialAddedEvent, final JsonObject ccMetadata, final String materialId, final String fileStoreId) {
        final String caseId = ccMetadata.getString(CASE_ID_FIELD, null);
        final String applicationId = ccMetadata.getString(APPLICATION_ID_FIELD, null);
        final String documentTypeId = ccMetadata.getString(DOCUMENT_TYPE_ID_FIELD);
        final String documentTypeDescription = ccMetadata.getString(DOCUMENT_TYPE_DESCRIPTION_FIELD);
        final String documentCategory = ccMetadata.getString(DOCUMENT_CATEGORY_FIELD);
        final String receivedDateTime = ccMetadata.containsKey(RECEIVED_DATE_TIME) ? ccMetadata.getString(RECEIVED_DATE_TIME) : null;
        final String sectionCode = ccMetadata.containsKey(SECTION_CODE) ? ccMetadata.getString(SECTION_CODE) : null;
        final Boolean isCpsCase = ccMetadata.containsKey(IS_CPS_CASE) ? ccMetadata.getBoolean(IS_CPS_CASE) : null;

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add("courtDocumentId", randomUUID().toString())
                .add("name", materialAddedEvent.payloadAsJsonObject().getJsonObject("fileDetails").getString("fileName"))
                .add("mimeType", materialAddedEvent.payloadAsJsonObject().getJsonObject("fileDetails").getString("mimeType"))
                .add(DOCUMENT_TYPE_ID_FIELD, documentTypeId)
                .add(DOCUMENT_TYPE_DESCRIPTION_FIELD, documentTypeDescription)
                .add("materials", buildMaterials(materialId, receivedDateTime))
                .add("containsFinancialMeans", false);
        if (fileStoreId == null) {
            payloadBuilder.add(IS_UNBUNDLED_DOCUMENT, materialAddedEvent.payloadAsJsonObject().getBoolean(IS_UNBUNDLED_DOCUMENT, false));
            ofNullable(isCpsCase).ifPresent(aBoolean -> payloadBuilder.add(IS_CPS_CASE, aBoolean));
        }

        createDocumentCategoryField(ccMetadata, caseId, applicationId, documentCategory, payloadBuilder);

        final JsonObjectBuilder progressionPayloadBuilder = createObjectBuilder().
                add("courtDocument", payloadBuilder.build())
                .add(MATERIAL_ID_FIELD, materialId);

        callAddCourtDocument(materialAddedEvent, fileStoreId, progressionPayloadBuilder, applicationId);

        if (null != sectionCode && sectionCode.equalsIgnoreCase(IDPC_SECTION_CODE)) {
            final Metadata metadataForIdcpReceived = Envelope.metadataFrom(materialAddedEvent.metadata()).withName("public.prosecutioncasefile.defendant-idpc-added").build();

            final JsonObjectBuilder jsonObjectBuilderForIdcpReceived = createObjectBuilder()
                    .add(CASE_ID_FIELD, caseId)
                    .add(MATERIAL_ID_FIELD, materialId)
                    .add(DEFENDANT_ID_FIELD, ccMetadata.getString(DEFENDANT_ID_FIELD));

            if (receivedDateTime != null) {
                jsonObjectBuilderForIdcpReceived.add("publishedDate", receivedDateTime);
            }

            sender.send(envelopeFrom(metadataForIdcpReceived, jsonObjectBuilderForIdcpReceived.build()));
        }
    }

    private void createDocumentCategoryField(final JsonObject ccMetadata, final String caseId, final String applicationId, final String documentCategory, final JsonObjectBuilder payloadBuilder) {
        if (DocumentCategory.DEFENDANT_LEVEL.toString().equalsIgnoreCase(documentCategory)) {
            final String defendantId = ccMetadata.getString(DEFENDANT_ID_FIELD);
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("defendantDocument", createObjectBuilder()
                            .add("prosecutionCaseId", caseId)
                            .add("defendants", createArrayBuilder()
                                    .add(defendantId)
                                    .build())
                            .build())
                    .build());
        } else if (DocumentCategory.CASE_LEVEL.toString().equalsIgnoreCase(documentCategory)) {
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("caseDocument", createObjectBuilder()
                            .add("prosecutionCaseId", caseId)
                            .build())
                    .build());
        } else if (DocumentCategory.APPLICATIONS.toString().equalsIgnoreCase(documentCategory)) {
            payloadBuilder.add(DOCUMENT_CATEGORY_FIELD, createObjectBuilder()
                    .add("applicationDocument", createObjectBuilder()
                            .add(APPLICATION_ID_FIELD, applicationId)
                            .build())
                    .build());
        }
    }

    private JsonArray buildMaterials(final String materialId, final String receivedDateTime) {
        JsonObjectBuilder materialBuilder = createObjectBuilder().add("id", materialId);
        if (receivedDateTime != null) {
            materialBuilder = materialBuilder.add(RECEIVED_DATE_TIME, receivedDateTime);
        }
        return createArrayBuilder().add(materialBuilder.build())
                .build();
    }

    @Handles("prosecutioncasefile.events.defendant-idpc-already-exists")
    public void handleDefendantIdpcAlreadyExists(final JsonEnvelope envelope) throws FileServiceException {
        fileStorer.delete(UUID.fromString(envelope.payloadAsJsonObject().getString(FILE_SERVICE_ID_FIELD)));
    }

    private void callAddCourtDocument(final JsonEnvelope materialAddedEvent, final String fileStoreId, final JsonObjectBuilder progressionPayloadBuilder, final String applicationId) {
        final Metadata metadata;
        if (nonNull(fileStoreId)) {
            progressionPayloadBuilder.add("fileStoreId", fileStoreId);
            if (isNull(applicationId)) {
                metadata = Envelope.metadataFrom(materialAddedEvent.metadata()).withName("prosecutioncasefile.command.add-case-court-document").build();
            } else {
                metadata = Envelope.metadataFrom(materialAddedEvent.metadata()).withName("prosecutioncasefile.command.add-application-court-document").build();
                progressionPayloadBuilder.add(APPLICATION_ID_FIELD, applicationId);
            }
        } else {
            metadata = Envelope.metadataFrom(materialAddedEvent.metadata()).withName("progression.add-court-document").build();
        }
        final JsonObject progressionPayload = progressionPayloadBuilder.build();
        sender.sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(envelopeFrom(metadata, progressionPayload)));
    }

}
