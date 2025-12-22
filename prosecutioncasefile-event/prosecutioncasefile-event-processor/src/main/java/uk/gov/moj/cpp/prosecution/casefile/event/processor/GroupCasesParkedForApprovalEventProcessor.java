package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.GroupCasesSummonsTemplateHelper.createTemplatePayload;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.DocumentGeneratorService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupCasesParkedForApprovalEventProcessor {
    private static final Logger LOGGER = getLogger(GroupCasesParkedForApprovalEventProcessor.class);

    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION = "progression.initiate-court-proceedings-for-application";
    public static final String PROSECUTIONCASEFILE_COMMAND_RECORD_GROUP_ID_FOR_SUMMONS_APPLICATION = "prosecutioncasefile.command.record-group-id-for-summons-application";
    public static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.add-court-document";

    public static final String MATERIAL_ID = "materialId";
    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String DOCUMENT_TYPE_DESCRIPTION = "Applications";
    public static final UUID DOCUMENT_TYPE_ID = fromString("460fa7ce-c002-11e8-a355-529269fb1459");
    public static final String APPLICATION_PDF = "application/pdf";

    @Inject
    private Sender sender;

    @Inject
    private GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter converter;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("prosecutioncasefile.events.group-cases-parked-for-approval")
    public void handleGroupCasesParkedForApproval(final Envelope<GroupCasesParkedForApproval> envelope) {
        final GroupCasesParkedForApproval payload = envelope.payload();
        final Optional<GroupProsecutionWithReferenceData> masterCase = payload.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                .filter(g -> g.getGroupProsecution().getIsGroupMaster())
                .findFirst();

        if (!masterCase.isPresent()) {
            return;
        }

        sendCommandProgressionInitiateCourtProceedingsForApplication(envelope, payload, masterCase.get());

        final UUID masterCaseId = masterCase.get().getGroupProsecution().getCaseDetails().getCaseId();
        sendPrivateCommandRecordGroupId(envelope, masterCase.get(), masterCaseId);

        final JsonObject documentPayload = createTemplatePayload(payload.getGroupProsecutionList(), masterCase.get());
        final UUID materialId = randomUUID();
        LOGGER.info("GroupCasesSummonsDocument generated from MasterCaseId: {} with MaterialId: {} ", masterCaseId, materialId);

        final String filename = this.documentGeneratorService.generateGroupCasesSummonsDocument(envelope, documentPayload, materialId);
        sendCommandProgressionAddDocument(envelope, payload, masterCaseId, materialId, filename);
    }

    private void sendCommandProgressionAddDocument(final Envelope<GroupCasesParkedForApproval> envelope, final GroupCasesParkedForApproval payload, final UUID masterCaseId, final UUID materialId, final String filename) {
        final JsonObject jsonObject = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, this.objectToJsonObjectConverter
                        .convert(buildCourtDocument(payload.getApplicationId(), masterCaseId, materialId, filename))).build();

        LOGGER.info("court document is being created '{}' ", jsonObject.getJsonString(MATERIAL_ID));
        this.sender.send(envelopeFrom(
                metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT),
                jsonObject
        ));
    }

    private void sendPrivateCommandRecordGroupId(final Envelope<GroupCasesParkedForApproval> envelope, final GroupProsecutionWithReferenceData masterCase, final UUID masterCaseId) {
        final Metadata metadataForRecordGroupId = metadataFrom(envelope.metadata())
                .withName(PROSECUTIONCASEFILE_COMMAND_RECORD_GROUP_ID_FOR_SUMMONS_APPLICATION)
                .build();

        final Envelope<JsonObject> commandEnvelopeForRecordGroupId = envelopeFrom(metadataForRecordGroupId, createObjectBuilder().add("caseId", masterCaseId.toString()).add("groupId", masterCase.getGroupProsecution().getGroupId().toString()).build());
        this.sender.send(commandEnvelopeForRecordGroupId);
    }

    private void sendCommandProgressionInitiateCourtProceedingsForApplication(Envelope<GroupCasesParkedForApproval> envelope, GroupCasesParkedForApproval payload, GroupProsecutionWithReferenceData masterCase) {
        LOGGER.info("calling {}  for submission id {} and application id {} and group id {}", PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION, envelope.metadata().streamId(), payload.getApplicationId(), masterCase.getGroupProsecution().getGroupId());

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION)
                .build();

        final Envelope<InitiateCourtApplicationProceedings> commandEnvelope = envelopeFrom(metadata, this.converter.convert(payload, envelope.metadata()));
        this.sender.send(commandEnvelope);
    }

    private CourtDocument buildCourtDocument(final UUID applicationId, final UUID caseId, final UUID materialId, final String filename) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withApplicationDocument(ApplicationDocument.applicationDocument()
                        .withApplicationId(applicationId)
                        .withProsecutionCaseId(caseId)
                        .build())
                .build();

        final Material material = Material.material()
                .withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now(ZoneId.of("UTC")))
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(filename)
                .withMaterials(Collections.singletonList(material))
                .withContainsFinancialMeans(false)
                .build();
    }

}
