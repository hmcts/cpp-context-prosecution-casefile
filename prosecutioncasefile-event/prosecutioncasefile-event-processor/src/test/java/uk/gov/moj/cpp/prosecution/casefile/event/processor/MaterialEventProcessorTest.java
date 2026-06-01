package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.PROBLEM_CODE_DOCUMENT_NOT_MATCHED;
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.SOURCE_CPS_FOR_PUBLIC_EVENTS;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.MaterialEventProcessor.IS_CPS_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.MaterialEventProcessor.PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataWithIdpcProcessId;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CmsDocumentIdentifier.cmsDocumentIdentifier;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject.prosecutionCaseSubject;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant.defendant;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending.idpcDefendantMatchPending;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched.idpcDefendantMatched;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected.idpcMaterialRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded.materialAdded;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2.materialAddedV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedWithWarnings.materialAddedWithWarnings;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending.materialPending;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2.materialPendingV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected.materialRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2.materialRejectedV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedWithWarnings.materialRejectedWithWarnings;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.BulkScanMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.IdpcUploadMaterialProcessService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingIdpcMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingMaterialExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentType;
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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MaterialEventProcessorTest {

    private static final String UPLOAD_FILE_TO_MATERIAL_CONTEXT = "material.command.upload-file";
    private static final String MATERIAL_ADDED_IN_MATERIAL_CONTEXT = "material.material-added";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String FILE_NAME = "File name";
    private static final String FILE_CLOUD_LOCATION = "fileCloudLocation";

    @Mock
    private Sender sender;
    @Mock
    private EnvelopeHelper envelopeHelper;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private MetadataHelper metadataHelper;
    @Mock
    private PendingMaterialExpiration pendingMaterialExpiration;
    @Mock
    private PendingIdpcMaterialExpiration pendingIdpcMaterialExpiration;
    @Mock
    private IdpcUploadMaterialProcessService idpcUploadMaterialProcessService;
    @Mock
    private BulkScanMaterialExpiration bulkScanMaterialExpiration;
    @InjectMocks
    private MaterialEventProcessor materialEventProcessor;
    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Captor
    private ArgumentCaptor<Metadata> metadataArgumentCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Mock
    private ReferenceDataService referencedataService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());


    private static Envelope<MaterialAdded> createMaterialAddedEvent() {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.material-added"),
                materialAdded()
                        .withCaseId(randomUUID())
                        .withMaterial(randomMaterial(null, false))
                        .withCaseType("SJP")
                        .build());
    }

    private static JsonEnvelope createDocumentAddedPayloadForNonSJP(Boolean isCpsCase) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectCaseLevel(isCpsCase, false))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", randomUUID().toString())
                .add("fileDetails", createObjectBuilder().add("fileName", FILE_NAME).add("mimeType", "application/pdf"))
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonEnvelope createDocumentAddedV2PayloadForNonSJP(Boolean isCpsCase, String fileStoreId) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectCaseLevel(isCpsCase, fileStoreId))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", randomUUID().toString())
                .add("fileDetails", createObjectBuilder().add("fileName", FILE_NAME).add("mimeType", "application/pdf"))
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonEnvelope createDocumentAddedPrivateV2PayloadForNonSJP(Boolean isCpsCase, String fileStoreId) {
        final Metadata metadata =
                JsonEnvelope.metadataFrom(
                                getCCMetadataJsonObjectCaseLevel(isCpsCase, fileStoreId))
                        .build();

        final JsonObject payload = createObjectBuilder()
                .add("materialId", randomUUID().toString())
                .add("courtDocument", createObjectBuilder().add("courtDocumentId", "courtDocumentId")
                        .add("name", FILE_NAME).build()).build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private static JsonObject getCCMetadataJsonObjectCaseLevel(Boolean isCpsCase, boolean isMigratedCase) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", ZonedDateTime.now().toString())
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        if (isMigratedCase) {
            builder.add(FILE_CLOUD_LOCATION, "azure.net");
        }

        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder)
                .build();
    }

    private static JsonObject getCCMetadataJsonObjectCaseLevel(Boolean isCpsCase, String fileStoreId) {
        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("documentTypeDescription", "SJPN")
                .add("defendantId", randomUUID().toString())
                .add("documentCategory", "Case level")
                .add("documentTypeId", randomUUID().toString())
                .add("receivedDateTime", ZonedDateTime.now().toString())
                .add("sectionCode", "IDPC");
        if (isCpsCase != null) {
            builder.add(IS_CPS_CASE, isCpsCase);
        }

        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", builder)
                .add("fileStoreId", fileStoreId)
                .build();
    }

    private static JsonObject getCCMetadataJsonObjectDefendantLevel() {
        return JsonObjects.createObjectBuilder(metadataBuilder()
                        .withId(randomUUID())
                        .withName(MATERIAL_ADDED_IN_MATERIAL_CONTEXT)
                        .build().asJsonObject())
                .add("ccMetadata", createObjectBuilder()
                        .add("caseId", randomUUID().toString())
                        .add("documentTypeDescription", "SJPN")
                        .add("defendantId", randomUUID().toString())
                        .add("documentCategory", "Defendant level")
                        .add("documentTypeId", randomUUID().toString())
                        .add("receivedDateTime", ZonedDateTime.now().toString())
                        .add("sectionCode", "IDPC"))


                .build();
    }

    private static Envelope<MaterialAdded> createMaterialAddedEventForNonSJP(final Boolean isCpsCase, final boolean isUnbundledDocument) {
        MaterialAdded.Builder builder = materialAdded()
                .withCaseId(randomUUID())
                .withMaterial(randomMaterial(null, isUnbundledDocument))
                .withCaseType("CC")
                .withDocumentCategory("Defendant level")
                .withDocumentTypeId(randomUUID().toString())
                .withDocumentType("DocumentType")
                .withProsecutorDefendantId(randomUUID().toString())
                .withDefendantId(randomUUID())
                .withReceivedDateTime(ZonedDateTime.now())
                .withSectionCode("IPDC");

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.material-added"),
                builder.build());
    }

    private static Envelope<MaterialAddedV2> createMaterialAddedV2EventForNonSJP(final Boolean isCpsCase, final boolean isUnbundledDocument) {
        MaterialAddedV2.Builder builder = materialAddedV2()
                .withCaseId(randomUUID())
                .withCaseType("CC")
                .withMaterial(randomUUID())
                .withMaterialType("IDPC bundle")
                .withMaterialContentType("application/pdf")
                .withMaterialName("Statement")
                .withFileName(FILE_NAME)
                .withDocumentTypeId("51cac7fb-387c-4d19-9c80-8963fa8cf223")
                .withDocumentCategory("Case level")
                .withDefendantId(randomUUID())
                .withDefendantName("John Wick")
                .withProsecutionCaseSubject(prosecutionCaseSubject()
                        .withProsecutingAuthority(randomAlphanumeric(5))
                        .withDefendantSubject(DefendantSubject.defendantSubject()
                                .withProsecutorDefendantId(randomAlphanumeric(5))
                                .build())
                        .build())
                .withReceivedDateTime(ZonedDateTime.now());

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.material-added-v2"),
                builder.build());
    }

    private static Envelope<MaterialAddedWithWarnings> createMaterialAddedWithWarningsEventForNonSJP(final Boolean isCpsCase, final boolean isUnbundledDocument) {
        final Problem warning = Problem.problem()
                .withCode("problemCode")
                .build();
        MaterialAddedWithWarnings.Builder builder = materialAddedWithWarnings()
                .withCaseId(randomUUID())
                .withCaseType("CC")
                .withMaterial(randomUUID())
                .withMaterialType("IDPC bundle")
                .withMaterialContentType("application/pdf")
                .withMaterialName("Statement")
                .withFileName(FILE_NAME)
                .withDocumentTypeId("51cac7fb-387c-4d19-9c80-8963fa8cf223")
                .withDocumentCategory("Case level")
                .withDefendantId(randomUUID())
                .withDefendantName("John Wick")
                .withProsecutionCaseSubject(prosecutionCaseSubject()
                        .withProsecutingAuthority(randomAlphanumeric(5))
                        .withDefendantSubject(DefendantSubject.defendantSubject()
                                .withProsecutorDefendantId(randomAlphanumeric(5))
                                .build())
                        .build())
                .withReceivedDateTime(ZonedDateTime.now())
                .withWarnings(ImmutableList.of(warning));

        if (isCpsCase != null) {
            builder.withIsCpsCase(isCpsCase);
        }

        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.material-added-with-warnings"),
                builder.build());
    }

    private static Envelope<MaterialPending> createMaterialPendingEvent() {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-pending"),
                materialPending()
                        .withCaseId(randomUUID())
                        .withMaterial(randomMaterial(null, false))
                        .build());
    }

    private static Envelope<MaterialPendingV2> createMaterialPendingV2Event() {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-pending-v2"),
                materialPendingV2()
                        .withCaseId(randomUUID())
                        .withMaterial(randomUUID())
                        .build());
    }

    private static Envelope<MaterialPendingV2> createMaterialPendingWithWarningsEvent() {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-pending-v2"),
                materialPendingV2()
                        .withCaseId(randomUUID())
                        .withMaterial(randomUUID())
                        .withWarnings(Collections.singletonList(Problem.problem()
                                .withCode("code")
                                .build()))
                        .build());
    }

    private static Material randomMaterial(final String documentType, final boolean isUnbundledDocument) {
        return material()
                .withFileStoreId(randomUUID())
                .withDocumentType(nonNull(documentType) ? documentType : randomAlphanumeric(5))
                .withFileType(randomAlphanumeric(5))
                .withIsUnbundledDocument(isUnbundledDocument)
                .build();
    }

    @Test
    void shouldHandleMaterialRejected() {
        assertThat(new MaterialEventProcessor(), isHandler(EVENT_PROCESSOR)
                .with(method("handleMaterialAdded").thatHandles("prosecutioncasefile.events.material-added"))
                .with(method("handleMaterialRejected").thatHandles("prosecutioncasefile.events.material-rejected"))
                .with(method("handleMaterialPending").thatHandles("prosecutioncasefile.events.material-pending"))
                .with(method("handleDocumentReviewRequired").thatHandles("prosecutioncasefile.events.case-document-review-required"))
        );
    }

    @Test
    void shouldHandleMaterialRejectedV2() {
        assertThat(new MaterialEventProcessor(), isHandler(EVENT_PROCESSOR)
                .with(method("handleMaterialAddedV2").thatHandles("prosecutioncasefile.events.material-added-v2"))
                .with(method("handleMaterialAddedWithWarnings").thatHandles("prosecutioncasefile.events.material-added-with-warnings"))
                .with(method("handleMaterialRejectedV2").thatHandles("prosecutioncasefile.events.material-rejected-v2"))
                .with(method("handleMaterialRejectedWithWarnings").thatHandles("prosecutioncasefile.events.material-rejected-with-warnings"))
                .with(method("handleMaterialPendingV2").thatHandles("prosecutioncasefile.events.material-pending-v2"))
                .with(method("handleMaterialPendingWithWarnings").thatHandles("prosecutioncasefile.events.material-pending-with-warnings"))
                .with(method("handleDocumentReviewRequiredV2").thatHandles("prosecutioncasefile.events.case-document-review-required-v2"))
        );
    }

    @Test
    void shouldHandleMaterialAddedEventForSJP() {
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEvent();
        final MaterialAdded materialAdded = materialAddedEvent.payload();

        final Envelope envelope = Envelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);

        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        assertThat(jsonEnvelopeCaptor.getValue(), is(jsonEnvelope(
                metadata().envelopedWith(materialAddedEvent.metadata())
                        .withName("sjp.upload-case-document")
                        .withCausationIds(),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(materialAdded.getCaseId().toString())),
                        withJsonPath("$.caseDocument", equalTo(materialAdded.getMaterial().getFileStoreId().toString())),
                        withJsonPath("$.caseDocumentType", equalTo(materialAdded.getMaterial().getDocumentType()))
                ))).thatMatchesSchema()
        ));

        verify(pendingMaterialExpiration).cancelMaterialTimer(materialAdded.getMaterial().getFileStoreId());
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventForNonSJP() {
        final boolean isUnbundledDocument = true;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventForNonSJP(null, isUnbundledDocument);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, false, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedV2EventForNonSJP() {
        final boolean isUnbundledDocument = true;
        final Envelope<MaterialAddedV2> materialAddedEvent = createMaterialAddedV2EventForNonSJP(null, isUnbundledDocument);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAddedV2(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedV2EventForNonSJP(materialAddedEvent, jsonEnvelope, false, materialAddedEvent.payload().getMaterial().toString());
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedWithWarningsEventForNonSJP() {
        final boolean isUnbundledDocument = true;
        final Envelope<MaterialAddedWithWarnings> materialAddedEvent = createMaterialAddedWithWarningsEventForNonSJP(null, isUnbundledDocument);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAddedWithWarnings(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedWithWarningsEventForNonSJP(materialAddedEvent, jsonEnvelope, false, materialAddedEvent.payload().getMaterial().toString());
    }

    @Test
    void shouldNotInitiateUnbundlingWhenMaterialHasNoCmsIdendifier() {
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventForNonSJP(null, false);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyNoInteractions(referencedataService);
    }

    @Test
    void shouldInitiateUnbundlingWhenMaterialHasCmsIdendifierAndDocumentNeedUnbundling() {
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventWithCmsIdentifier();

        final var jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);
        when(referencedataService.isDocumentNeedsUnBundling(anyInt())).thenReturn(true);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verify(referencedataService).isDocumentNeedsUnBundling(anyInt());
        verify(sender, times(2)).send(jsonEnvelopeCaptor.capture());

        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).metadata().name(), is(PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE));
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getString("caseId"), notNullValue());
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getString("prosecutorDefendantId"), notNullValue());
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getString("prosecutingAuthority"), notNullValue());
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getJsonObject("material"), notNullValue());
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getJsonObject("cmsDocumentIdentifier").getInt("materialType"), is(1));
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getString("receivedDateTime"), notNullValue());
        assertThat(jsonEnvelopeCaptor.getAllValues().get(1).payloadAsJsonObject().getString("defendantName"), is("John Wick"));
    }

    @Test
    void shouldNotEmitDocumentBundleArrivedForUnbundlingEventWhenDocumentNotMarkedForUnbundling() {
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventWithCmsIdentifier();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);
        when(referencedataService.isDocumentNeedsUnBundling(anyInt())).thenReturn(false);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verify(referencedataService).isDocumentNeedsUnBundling(anyInt());
        verify(sender, times(2)).send(any());
    }

    @Test
    void shouldHandleDocumentAddedEventForNonSJPCaseLevel() throws Exception {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPayloadForNonSJP(null);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(null, false).getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleDocumentAddedEvent(envelope, ccMetadataJsonObject, null);
    }

    @Test
    void shouldHandleMaterialAddedPublicEventWhenFileCloudLocationIsNotNull() throws Exception {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPayloadForNonSJP(null);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(null, true).getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verify(idpcUploadMaterialProcessService,times(0)).signalUploadFileProcess(any(),any(),any());
        verify(sender,times(0)).sendAsAdmin(envelopeHelper.withMetadataInPayloadForEnvelope(any()));
    }


    @Test
    void shouldCallCommandDocumentAddedWhenEventIsV2() throws Exception {
        final String material = UUID.randomUUID().toString();
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedV2PayloadForNonSJP(null, material);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(null, false).getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));
        when(metadataHelper.getFileStoreId(materialAddedEventFromMaterialContext)).thenReturn(material);

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleAddDocumentCommand(envelope, ccMetadataJsonObject, null, material);
    }

    @Test
    void shouldHandleDocumentAddedEventForNonSJPCaseLevelForV2() {
        final String material = UUID.randomUUID().toString();
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPrivateV2PayloadForNonSJP(null, material);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(null, false).getJsonObject("ccMetadata");
        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleCourtDocumentAdded(materialAddedEventFromMaterialContext);

        verifyHandleDocumentAddedV2Event(envelope, ccMetadataJsonObject, null);
    }

    @Test
    void shouldHandleDocumentAddedEventForIsCPSCaseFlagIsTrue() throws Exception {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPayloadForNonSJP(true);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(true, false).getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleDocumentAddedEvent(envelope, ccMetadataJsonObject, true);
    }

    @Test
    void shouldHandleDocumentAddedEventForIsCPSCaseFlagIsFalse() throws Exception {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPayloadForNonSJP(false);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectCaseLevel(false, false).getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));

        final Envelope envelope = Envelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verifyHandleDocumentAddedEvent(envelope, ccMetadataJsonObject, false);
    }

    @Test
    void shouldHandleDocumentAddedEventForNonSJPDefendantLevel() throws Exception {
        final JsonEnvelope materialAddedEventFromMaterialContext = createDocumentAddedPayloadForNonSJP(null);
        final JsonObject ccMetadataJsonObject = getCCMetadataJsonObjectDefendantLevel().getJsonObject("ccMetadata");

        when(metadataHelper.getCCMetadata(materialAddedEventFromMaterialContext)).thenReturn(Optional.of(ccMetadataJsonObject));

        final Envelope envelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        materialEventProcessor.handleMaterialAddedFromMaterialContext(materialAddedEventFromMaterialContext);

        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName(PROGRESSION_ADD_COURT_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.courtDocument.documentCategory.defendantDocument.defendants[0]", equalTo(ccMetadataJsonObject.getString("defendantId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId")))
                )))));
    }

    @Test
    void shouldHandleMaterialRejectedEvent() throws Exception {
        final Envelope<MaterialRejected> materialRejectedEnvelope = createMaterialRejectedEvent(null, null, null, null);
        final MaterialRejected materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejected> publicMaterialRejectedEvent = envelopeArgumentCaptor.getValue();

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected"));

        final MaterialRejected publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial().getFileStoreId());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial().getFileStoreId());
    }

    @Test
    void shouldHandleMaterialRejectedV2Event() throws Exception {
        final Envelope<MaterialRejectedV2> materialRejectedEnvelope = createMaterialRejectedV2Event(null, null, null, null);
        final MaterialRejectedV2 materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejectedV2(materialRejectedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejectedV2> publicMaterialRejectedEvent = envelopeArgumentCaptor.getValue();

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected-v2"));

        final MaterialRejectedV2 publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial());
    }

    @Test
    void shouldHandleMaterialRejectedWithWarningsEvent() throws Exception {
        final Envelope<MaterialRejectedWithWarnings> materialRejectedEnvelope = createMaterialRejectedWithWarningsEvent(null, null, null, null);
        final MaterialRejectedWithWarnings materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejectedWithWarnings(materialRejectedEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejectedWithWarnings> publicMaterialRejectedEvent = envelopeArgumentCaptor.getValue();

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected-with-warnings"));

        final MaterialRejectedWithWarnings publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial());
    }

    @Test
    void shouldHandleMaterialRejectedEventRaisedDocumentRequiredReview() throws Exception {
        UUID documentId = randomUUID();
        final Envelope<MaterialRejected> materialRejectedEnvelope = createMaterialRejectedEvent(true, documentId.toString(), ZonedDateTime.now(), "APPLICATION");
        final MaterialRejected materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejected> publicMaterialRejectedEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected"));

        final MaterialRejected publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial().getFileStoreId());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial().getFileStoreId());
    }

    @Test
    void shouldHandleMaterialRejectedV2EventRaisedDocumentRequiredReview() throws Exception {
        UUID material = randomUUID();
        final Envelope<MaterialRejectedV2> materialRejectedEnvelope = createMaterialRejectedV2Event(true, material, ZonedDateTime.now(), "APPLICATION");
        final MaterialRejectedV2 materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejectedV2(materialRejectedEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejectedV2> publicMaterialRejectedEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected-v2"));

        final MaterialRejectedV2 publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial().getFileStoreId());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial());
    }

    @Test
    void shouldHandleMaterialRejectedEventRaisedDocumentRequiredReviewForOther() throws Exception {
        UUID documentId = randomUUID();
        final Envelope<MaterialRejected> materialRejectedEnvelope = createMaterialRejectedEvent(true, documentId.toString(), ZonedDateTime.now(), null);
        final MaterialRejected materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejected(materialRejectedEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejected> publicMaterialRejectedEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected"));

        final MaterialRejected publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial().getFileStoreId());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial().getFileStoreId());
    }

    @Test
    void shouldHandleMaterialRejectedV2EventRaisedDocumentRequiredReviewForOther() throws Exception {
        UUID material = randomUUID();
        final Envelope<MaterialRejectedV2> materialRejectedEnvelope = createMaterialRejectedV2Event(true, material, ZonedDateTime.now(), null);
        final MaterialRejectedV2 materialRejectedEvent = materialRejectedEnvelope.payload();

        materialEventProcessor.handleMaterialRejectedV2(materialRejectedEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialRejectedV2> publicMaterialRejectedEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicMaterialRejectedEvent.metadata(), metadata().envelopedWith(materialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.material-rejected-v2"));

        final MaterialRejectedV2 publicEventPayload = publicMaterialRejectedEvent.payload();
        assertThat(publicEventPayload, is(materialRejectedEvent));

        //verify(fileStorer).delete(materialRejectedEvent.getMaterial().getFileStoreId());
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialRejectedEvent.getMaterial());
    }

    @Test
    void shouldhandleDocumentReviewRequiredEventAndRaiseDocumentRequiredReview() throws Exception {
        final UUID documentId = randomUUID();
        final UUID fileStoreId = randomUUID();
        final Envelope<CaseDocumentReviewRequired> caseDocumentReviewRequiredEnvelope = createMaterialRejectedEvent(documentId.toString(), ZonedDateTime.now(), fileStoreId);

        materialEventProcessor.handleDocumentReviewRequired(caseDocumentReviewRequiredEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<DocumentReviewRequired> publicDocumentReviewEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicDocumentReviewEvent.metadata(), metadata().envelopedWith(caseDocumentReviewRequiredEnvelope.metadata()).withName("public.prosecutioncasefile.document-review-required"));


        final DocumentReviewRequired payload = publicDocumentReviewEvent.payload();
        assertThat(payload.getCmsDocumentId(), is(documentId.toString()));
        assertThat(payload.getFileStoreId(), is(fileStoreId));
        assertThat(payload.getErrorCodes().get(0), is(PROBLEM_CODE_DOCUMENT_NOT_MATCHED));

    }

    @Test
    void shouldHandleMaterialPendingEvent() {
        final Envelope<MaterialPending> materialPendingEvent = createMaterialPendingEvent();
        final MaterialPending materialPending = materialPendingEvent.payload();

        materialEventProcessor.handleMaterialPending(materialPendingEvent);

        verify(pendingMaterialExpiration).startMaterialTimer(materialPending.getMaterial().getFileStoreId(), materialPending.getCaseId(), materialPendingEvent.metadata());
    }

    @Test
    void shouldHandleMaterialPendingV2Event() {
        final Envelope<MaterialPendingV2> materialPendingEvent = createMaterialPendingV2Event();
        final MaterialPendingV2 materialPending = materialPendingEvent.payload();

        materialEventProcessor.handleMaterialPendingV2(materialPendingEvent);

        verify(pendingMaterialExpiration).startMaterialTimer(materialPending.getMaterial(), materialPending.getCaseId(), materialPendingEvent.metadata());
    }

    @Test
    void shouldHandleMaterialPendingWithWarningsEvent() {
        final Envelope<MaterialPendingV2> materialPendingEvent = createMaterialPendingWithWarningsEvent();
        final MaterialPendingV2 materialPending = materialPendingEvent.payload();

        materialEventProcessor.handleMaterialPendingV2(materialPendingEvent);

        verify(pendingMaterialExpiration).startMaterialTimer(materialPending.getMaterial(), materialPending.getCaseId(), materialPendingEvent.metadata());

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope<MaterialPendingV2> publicMaterialPendingEvent = envelopeArgumentCaptor.getValue();

        assertThat(publicMaterialPendingEvent.metadata().asJsonObject().getString("name"), is("public.prosecutioncasefile.material-pending-with-warnings"));

        final MaterialPendingV2 publicEventPayload = publicMaterialPendingEvent.payload();
        assertThat(publicEventPayload, is(materialPending));

    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventWihIsCspsCaseIsTRUE() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventForNonSJP(true, isUnbundledDocument);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, true, isUnbundledDocument);
    }

    @Test
    void shouldUploadMaterialWhenMaterialAddedEventWihIsCspsCaseIsFALSE() {
        final boolean isUnbundledDocument = false;
        final Envelope<MaterialAdded> materialAddedEvent = createMaterialAddedEventForNonSJP(false, isUnbundledDocument);

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(materialAddedEvent.metadata(), NULL);
        when(metadataHelper.envelopeWithCustomMetadata(any(), any(), any(), any())).thenReturn(jsonEnvelope);

        materialEventProcessor.handleMaterialAdded(materialAddedEvent);

        verifyUploadMaterialWhenMaterialAddedEventForNonSJP(materialAddedEvent, jsonEnvelope, true, isUnbundledDocument);
    }

    private void verifyHandleDocumentAddedEvent(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        org.hamcrest.Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }


        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName(PROGRESSION_ADD_COURT_DOCUMENT),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadataJsonObject.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        isCpsCaseMatcher
                )))));
    }

    private void verifyHandleDocumentAddedV2Event(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        org.hamcrest.Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }


        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName("progression.add-court-document-v2"),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        isCpsCaseMatcher
                )))));
    }


    private void verifyHandleAddDocumentCommand(Envelope envelope, JsonObject ccMetadataJsonObject, Boolean isCpsCase, String fileStoreId) {
        verify(envelopeHelper).withMetadataInPayloadForEnvelope(jsonEnvelopeCaptor.capture());
        verify(sender).sendAsAdmin(envelope);

        final JsonEnvelope addCourtDocument = jsonEnvelopeCaptor.getValue();

        org.hamcrest.Matcher isCpsCaseMatcher;
        if (isCpsCase == null) {
            isCpsCaseMatcher = hasNoJsonPath("$.courtDocument.isCpsCase");
        } else {
            isCpsCaseMatcher = withJsonPath("$.courtDocument.isCpsCase", equalTo(isCpsCase));
        }


        assertThat(addCourtDocument, is(jsonEnvelope(
                metadata()
                        .withName("prosecutioncasefile.command.add-case-court-document"),
                payloadIsJson(allOf(
                        withJsonPath("$.courtDocument"),
                        withJsonPath("$.courtDocument.courtDocumentId"),
                        withJsonPath("$.courtDocument.name", equalTo(FILE_NAME)),
                        withJsonPath("$.courtDocument.documentTypeId", equalTo(ccMetadataJsonObject.getString("documentTypeId"))),
                        withJsonPath("$.courtDocument.documentTypeDescription", equalTo(ccMetadataJsonObject.getString("documentTypeDescription"))),
                        withJsonPath("$.courtDocument.materials[0].id", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.courtDocument.documentCategory.caseDocument.prosecutionCaseId", equalTo(ccMetadataJsonObject.getString("caseId"))),
                        withJsonPath("$.materialId", equalTo(addCourtDocument.payloadAsJsonObject().getString("materialId"))),
                        withJsonPath("$.fileStoreId", equalTo(fileStoreId)),
                        isCpsCaseMatcher
                )))));
    }

    private void verifyUploadMaterialWhenMaterialAddedEventForNonSJP(Envelope<MaterialAdded> materialAddedEvent, JsonEnvelope jsonEnvelope, boolean withIsCpsCase, final boolean isUnbundledDocument) {
        final MaterialAdded materialAdded = materialAddedEvent.payload();
        verify(metadataHelper).envelopeWithCustomMetadata(
                metadataArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                eq(null));

        final String commandName = metadataArgumentCaptor.getValue().name();
        final JsonObject ccMetadata = jsonObjectArgumentCaptor.getAllValues().get(0);
        final JsonObject materialPayload = jsonObjectArgumentCaptor.getAllValues().get(1);

        assertEquals(UPLOAD_FILE_TO_MATERIAL_CONTEXT, commandName);

        assertTrue(materialPayload.containsKey("materialId"));
        assertEquals(materialPayload.getString("fileServiceId"), materialAddedEvent.payload().getMaterial().getFileStoreId().toString());

        if (isUnbundledDocument) {
            assertTrue(materialPayload.getBoolean("isUnbundledDocument"));
        } else {
            assertNull(materialPayload.get("isUnbundledDocument"));
        }

        assertEquals(ccMetadata.getString("caseId"), materialAddedEvent.payload().getCaseId().toString());
        assertEquals(ccMetadata.getString("documentTypeId"), materialAddedEvent.payload().getDocumentTypeId());
        assertEquals(ccMetadata.getString("documentTypeDescription"), materialAddedEvent.payload().getDocumentType());
        assertEquals(ccMetadata.getString("documentCategory"), materialAddedEvent.payload().getDocumentCategory());
        assertEquals(ccMetadata.getString("defendantId"), materialAddedEvent.payload().getDefendantId().toString());
        if (withIsCpsCase) {
            assertEquals(ccMetadata.getBoolean("isCpsCase"), materialAddedEvent.payload().getIsCpsCase());
        }
        verify(sender).send(jsonEnvelope);
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialAdded.getMaterial().getFileStoreId());
    }

    private void verifyUploadMaterialWhenMaterialAddedV2EventForNonSJP(Envelope<MaterialAddedV2> materialAddedEvent, JsonEnvelope jsonEnvelope, boolean withIsCpsCase, final String fileServiceId) {
        final MaterialAddedV2 materialAdded = materialAddedEvent.payload();
        verify(metadataHelper).envelopeWithCustomMetadata(
                metadataArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                eq(fileServiceId));

        final String commandName = metadataArgumentCaptor.getValue().name();
        final JsonObject ccMetadata = jsonObjectArgumentCaptor.getAllValues().get(0);
        final JsonObject materialPayload = jsonObjectArgumentCaptor.getAllValues().get(1);

        assertEquals(UPLOAD_FILE_TO_MATERIAL_CONTEXT, commandName);

        assertTrue(materialPayload.containsKey("materialId"));
        assertEquals(materialPayload.getString("fileServiceId"), materialAddedEvent.payload().getMaterial().toString());
        assertEquals(ccMetadata.getString("caseId"), materialAddedEvent.payload().getCaseId().toString());
        assertEquals(ccMetadata.getString("documentTypeId"), materialAddedEvent.payload().getDocumentTypeId());
        assertEquals(ccMetadata.getString("documentTypeDescription"), materialAddedEvent.payload().getMaterialType());
        assertEquals(ccMetadata.getString("documentCategory"), materialAddedEvent.payload().getDocumentCategory());

        if (withIsCpsCase) {
            assertEquals(ccMetadata.getBoolean("isCpsCase"), materialAddedEvent.payload().getIsCpsCase());
        }
        verify(sender).send(jsonEnvelope);
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialAdded.getMaterial());
    }

    private void verifyUploadMaterialWhenMaterialAddedWithWarningsEventForNonSJP(Envelope<MaterialAddedWithWarnings> materialAddedEvent, JsonEnvelope jsonEnvelope, boolean withIsCpsCase, final String fileServiceId) {
        final MaterialAddedWithWarnings materialAdded = materialAddedEvent.payload();
        verify(metadataHelper).envelopeWithCustomMetadata(
                metadataArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                jsonObjectArgumentCaptor.capture(),
                eq(fileServiceId));

        final String commandName = metadataArgumentCaptor.getValue().name();
        final JsonObject ccMetadata = jsonObjectArgumentCaptor.getAllValues().get(0);
        final JsonObject materialPayload = jsonObjectArgumentCaptor.getAllValues().get(1);

        assertEquals(UPLOAD_FILE_TO_MATERIAL_CONTEXT, commandName);

        assertTrue(materialPayload.containsKey("materialId"));
        assertEquals(materialPayload.getString("fileServiceId"), materialAddedEvent.payload().getMaterial().toString());
        assertEquals(ccMetadata.getString("caseId"), materialAddedEvent.payload().getCaseId().toString());
        assertEquals(ccMetadata.getString("documentTypeId"), materialAddedEvent.payload().getDocumentTypeId());
        assertEquals(ccMetadata.getString("documentTypeDescription"), materialAddedEvent.payload().getMaterialType());
        assertEquals(ccMetadata.getString("documentCategory"), materialAddedEvent.payload().getDocumentCategory());

        if (withIsCpsCase) {
            assertEquals(ccMetadata.getBoolean("isCpsCase"), materialAddedEvent.payload().getIsCpsCase());
        }
        verify(sender).send(jsonEnvelope);
        verify(pendingMaterialExpiration).cancelMaterialTimer(materialAdded.getMaterial());
    }

    private Envelope<MaterialRejected> createMaterialRejectedEvent(Boolean isCpsCase, String documentId, ZonedDateTime receivedDateTime, String documentType) {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-rejected").build()
                , materialRejected()
                        .withCaseId(randomUUID())
                        .withProsecutorDefendantId(randomUUID().toString())
                        .withProsecutingAuthority("GAEAA01")
                        .withErrors(asList(problem().withCode("INVALID_FILE_TYPE").withValues(
                                asList(problemValue().withKey("fileType").withValue("csv").build())).build()))
                        .withMaterial(randomMaterial(documentType, false))
                        .withIsCpsCase(isCpsCase)
                        .withReceivedDateTime(receivedDateTime)
                        .withCmsDocumentId(documentId)
                        .build());
    }

    private Envelope<MaterialRejectedV2> createMaterialRejectedV2Event(Boolean isCpsCase, UUID material, ZonedDateTime receivedDateTime, String materialType) {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-rejected-v2").build()
                , materialRejectedV2()
                        .withCaseId(randomUUID())
                        .withErrors(asList(problem().withCode("INVALID_FILE_TYPE").withValues(
                                asList(problemValue().withKey("materialContentType").withValue("csv").build())).build()))
                        .withMaterial(material)
                        .withMaterialType(materialType)
                        .withMaterialName("Statement")
                        .withSectionOrderSequence(000001)
                        .withIsCpsCase(isCpsCase)
                        .withReceivedDateTime(receivedDateTime)
                        .build());
    }

    private Envelope<MaterialRejectedWithWarnings> createMaterialRejectedWithWarningsEvent(Boolean isCpsCase, UUID material, ZonedDateTime receivedDateTime, String materialType) {
        final Problem warning = Problem.problem()
                .withCode("problemCode")
                .build();
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.material-rejected-with-warnings").build()
                , materialRejectedWithWarnings()
                        .withCaseId(randomUUID())
                        .withErrors(asList(problem().withCode("INVALID_FILE_TYPE").withValues(
                                asList(problemValue().withKey("materialContentType").withValue("csv").build())).build()))
                        .withMaterial(material)
                        .withMaterialType(materialType)
                        .withMaterialName("Statement")
                        .withSectionOrderSequence(000001)
                        .withIsCpsCase(isCpsCase)
                        .withReceivedDateTime(receivedDateTime)
                        .withWarnings(ImmutableList.of(warning))
                        .build());
    }

    private Envelope<CaseDocumentReviewRequired> createMaterialRejectedEvent(final String documentId, final ZonedDateTime receivedDateTime, final UUID fileStoreId) {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.case-document-review-required").build()
                , CaseDocumentReviewRequired.caseDocumentReviewRequired()
                        .withCaseId(randomUUID())
                        .withCmsDocumentId(documentId)
                        .withDocumentType(DocumentType.OTHER)
                        .withErrorCodes(Collections.singletonList(PROBLEM_CODE_DOCUMENT_NOT_MATCHED))
                        .withFileStoreId(fileStoreId)
                        .withReceivedDateTime(receivedDateTime)
                        .withSource(SOURCE_CPS_FOR_PUBLIC_EVENTS)
                        .build()
        );
    }


    @Test
    void shouldHandleIdpcMaterialRejectedEvent() throws Exception {
        final Envelope<IdpcMaterialRejected> idpcMaterialRejectedEnvelope = createIdpcMaterialRejectedEvent();
        final IdpcMaterialRejected idpcMaterialRejectedEvent = idpcMaterialRejectedEnvelope.payload();

        materialEventProcessor.handleIdpcMaterialRejected(idpcMaterialRejectedEnvelope);

        verify(fileStorer).delete(idpcMaterialRejectedEvent.getFileServiceId());
        verify(pendingIdpcMaterialExpiration).cancelIdpcMaterialTimer(idpcMaterialRejectedEvent.getFileServiceId());
    }

    private Envelope<IdpcMaterialRejected> createIdpcMaterialRejectedEvent() {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.idpc-material-rejected").build()
                , idpcMaterialRejected()
                        .withCaseId(randomUUID())
                        .withErrors(asList(problem().withCode("INVALID_FILE_TYPE").withValues(
                                asList(problemValue().withKey("fileType").withValue("csv").build())).build()))
                        .withFileServiceId(randomUUID())
                        .build());
    }

    @Test
    void shouldHandleIdpcDefendantMatch() throws Exception {
        final Envelope<IdpcDefendantMatched> idpcDefendantMatch = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.idpc-defendant-matched"),
                idpcDefendantMatched().withCaseId(randomUUID())
                        .withFileServiceId(randomUUID())
                        .withCaseUrn("12345")
                        .withMaterialType("idpc")
                        .withDefendantId(randomUUID().toString())
                        .build());

        materialEventProcessor.idpcDefendantMatch(idpcDefendantMatch);
        verify(idpcUploadMaterialProcessService).startUploadFileProcess(idpcDefendantMatch);
    }

    @Test
    void shouldHandleIdpcDefendantMatchPending() {
        final Envelope<IdpcDefendantMatchPending> idpcDefendantMatch = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.idpc-defendant-match-pending"),
                idpcDefendantMatchPending().withCaseId(randomUUID())
                        .withFileServiceId(randomUUID())
                        .withCaseUrn("12345")
                        .withMaterialType("idpc")
                        .withDefendant(defendant().build())
                        .build());
        final IdpcDefendantMatchPending payload = idpcDefendantMatch.payload();

        materialEventProcessor.handleIdpcDefendantMatchPending(idpcDefendantMatch);
        verify(pendingIdpcMaterialExpiration).startMaterialTimer(payload.getFileServiceId(), payload.getCaseId(), idpcDefendantMatch.metadata());
    }

    @Test
    void shouldHandleIdpcMaterialAdded() throws Exception {
        final JsonEnvelope addIdpcMaterial = JsonEnvelope.envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("material.material-added").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("materialId", randomUUID().toString())
                        .add("fileDetails", createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=").add("mimeType", "text/plain").add("fileName", "abc.txt").build())
                        .add("materialAddedDate", "2016-04-26T13:01:787.345")
                        .add("isUnbundledDocument", true)
                        .build());

        when(metadataHelper.getCCMetadata(any())).thenReturn(Optional.empty());
        when(idpcUploadMaterialProcessService.signalUploadFileProcess(any(), any(), any())).thenReturn(UUID.randomUUID().toString());
        materialEventProcessor.handleMaterialAddedFromMaterialContext(addIdpcMaterial);
        verify(idpcUploadMaterialProcessService).signalUploadFileProcess(any(), any(), any());
    }

    @Test
    void shouldHandleDefendantIdpcAlreadyExists() throws Exception {
        final JsonEnvelope idpcDefendantAlreadyExists = JsonEnvelope.envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("prosecutioncasefile.events.defendant-idpc-already-exists").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("defendantId", "34123423")
                        .add("fileServiceId", randomUUID().toString())
                        .add("caseId", "2016-04-26T13:01:787.345")
                        .build());

        materialEventProcessor.handleDefendantIdpcAlreadyExists(idpcDefendantAlreadyExists);
        verify(fileStorer, times(1)).delete(any(UUID.class));

    }

    private Envelope<MaterialAdded> createMaterialAddedEventWithCmsIdentifier() {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.material-added"),
                materialAdded()
                        .withCaseId(randomUUID())
                        .withMaterial(randomMaterial())
                        .withCaseType("CC")
                        .withDocumentCategory("Defendant level")
                        .withDocumentTypeId(randomUUID().toString())
                        .withDocumentType("DocumentType")
                        .withProsecutorDefendantId(randomUUID().toString())
                        .withProsecutingAuthority(randomUUID().toString())
                        .withDefendantId(randomUUID())
                        .withIsCpsCase(false)
                        .withCmsDocumentIdentifier(cmsDocumentIdentifier()
                                .withMaterialType(1)
                                .withDocumentId("documentId").build())
                        .withReceivedDateTime(ZonedDateTime.now())
                        .withDefendantName("John Wick")
                        .build());
    }

    private Material randomMaterial() {
        return material()
                .withFileStoreId(randomUUID())
                .withDocumentType(randomAlphanumeric(5))
                .withFileType(randomAlphanumeric(5))
                .build();
    }

    @Test
    void shouldhandleDocumentReviewRequiredEventV2AndRaiseDocumentRequiredReview() throws Exception {
        final UUID documentId = randomUUID();
        final UUID fileStoreId = randomUUID();
        final Envelope<CaseDocumentReviewRequired> caseDocumentReviewRequiredEnvelope = createMaterialRejectedEvent(documentId.toString(), ZonedDateTime.now(), fileStoreId);

        materialEventProcessor.handleDocumentReviewRequiredV2(caseDocumentReviewRequiredEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<DocumentReviewRequired> publicDocumentReviewEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicDocumentReviewEvent.metadata(), metadata().envelopedWith(caseDocumentReviewRequiredEnvelope.metadata()).withName("public.prosecutioncasefile.document-review-required-v2"));


        final DocumentReviewRequired payload = publicDocumentReviewEvent.payload();
        assertThat(payload.getCmsDocumentId(), is(documentId.toString()));
        assertThat(payload.getFileStoreId(), is(fileStoreId));
        assertThat(payload.getErrorCodes().get(0), is(PROBLEM_CODE_DOCUMENT_NOT_MATCHED));
    }

    @Test
    void shouldHandleBulkScanMaterialRejected() throws Exception {
        final UUID documentId = randomUUID();
        final UUID fileStoreId = randomUUID();
        final Envelope<BulkscanMaterialRejected> bulkscanMaterialRejectedEnvelope = createBulkscanMaterialRejected(documentId.toString(), ZonedDateTime.now(), fileStoreId);

        materialEventProcessor.handleBulkScanMaterialRejected(bulkscanMaterialRejectedEnvelope);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final Envelope<BulkscanMaterialRejected> publicEvent = envelopeArgumentCaptor.getAllValues().get(0);

        assertThat(publicEvent.metadata(), metadata().envelopedWith(bulkscanMaterialRejectedEnvelope.metadata()).withName("public.prosecutioncasefile.bulkscan-material-followup"));
    }

    private Envelope<BulkscanMaterialRejected> createBulkscanMaterialRejected(final String documentId, final ZonedDateTime receivedDateTime, final UUID fileStoreId) {
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.events.bulkscan-material-rejected").build()
                , BulkscanMaterialRejected.bulkscanMaterialRejected()
                        .withCaseId(randomUUID())
                        .withMaterial(material()
                                .withDocumentType("documentType")
                                .withFileStoreId(randomUUID())
                                .withFileType("fileType")
                                .withIsUnbundledDocument(false)
                                .build())
                        .build()
        );
    }
}