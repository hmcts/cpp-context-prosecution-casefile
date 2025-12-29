package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.*;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material.material;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending.materialPending;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2.materialPendingV2;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived.sjpProsecutionReceived;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.messaging.Metadata;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Exhibit;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CheckPendingEventsForNewDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcceptCaseHandlerTest {

    private static final UUID CASE_ID = fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222");
    private static final UUID DEFENDANT_ID = fromString("2e9b2e31-a41f-4f8e-8e34-507237e4f4cf");
    private static final UUID EXTERNAL_ID = fromString("f511084a-b7dd-495b-8e97-a06daceea5c0");
    private static final UUID CASE_SUBMISSION_ID = randomUUID();
    private static final UUID PDF_SUBMISSION_ID = randomUUID();
    private static final UUID CSV_SUBMISSION_ID = randomUUID();
    private static final UUID PDF_FILE_STORE_ID = randomUUID();
    private static final UUID CSV_FILE_STORE_ID = randomUUID();
    private static final String PROSECUTING_AUTHORITY = "TVLOUCODE";
    private static final String PROSECUTOR_DEFENDANT_ID = "TVL_DEF001";
    private static final String MATERIAL_TYPE = "SJPN";
    private static final String PDF_FILE_TYPE = "application/pdf";
    private static final String CSV_FILE_TYPE = "application/csv";

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Spy
    @SuppressWarnings("unused")
    private final Enveloper enveloper = createEnveloperWithEvents(CaseCreatedSuccessfully.class,
            SjpCaseCreatedSuccessfully.class,
            MaterialAdded.class,
            MaterialRejected.class,
            MaterialRejectedV2.class,
            MaterialAddedV2.class);

    @InjectMocks
    private AcceptCaseHandler acceptCaseHandler;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> captor;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    private Defendant defendant;

    @Mock
    private Runnable onCloseMock;

    @BeforeEach
    public void setup() {
        defendant = defendant().withId(DEFENDANT_ID.toString())
                .withIndividual(Individual.individual()
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withFirstName("John")
                                .withLastName("Smith")
                                .withTitle("Baron")
                                .build())
                        .build())
                .withInitiationCode("C")
                .build();
    }

    @Test
    public void shouldHandleAcceptCaseCommand() {
        assertThat(acceptCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAcceptCase")
                        .thatHandles("prosecutioncasefile.command.accept-case")
                ));
    }

    @Test
    public void shouldRaiseCaseCreatedEventWhenCaseGetsAcceptedForSJP() throws Exception {
        aggregate.apply(buildSjpCaseReceivedEvent());

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(eventStream.read()).thenReturn(empty());

        final AcceptCase acceptCase = readJson("json/acceptSjpCase.json", AcceptCase.class);

        final Envelope<AcceptCase> envelope = envelopeFrom(metadataFor("prosecutioncasefile.command.accept-case"), acceptCase);

        acceptCaseHandler.handleAcceptCase(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.sjp-case-created-successfully",
                () -> readJson("json/sjpCaseCreatedSuccessfully.json", JsonValue.class));
    }

    @Test
    public void shouldRaiseCaseCreatedEventWhenCaseGetsAcceptedForCC() throws Exception {
        aggregate.apply(buildCcCaseReceivedEvent());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(eventStream.read()).thenReturn(empty());

        final AcceptCase acceptCase = readJson("json/acceptCcCase.json", AcceptCase.class);

        final Envelope<AcceptCase> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.accept-case"), acceptCase);

        acceptCaseHandler.handleAcceptCase(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.case-created-successfully",
                () -> readJson("json/caseCreatedSuccessfully.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAcceptSjpCaseWithPendingMaterials() throws Exception {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        aggregate.apply(buildSjpCaseReceivedEvent());
        final MaterialPending pdfMaterialPending = buildMaterialPendingEvent(PDF_FILE_STORE_ID, PDF_FILE_TYPE);
        final MaterialPending csvMaterialPending = buildMaterialPendingEvent(CSV_FILE_STORE_ID, CSV_FILE_TYPE);
        final List<MaterialPending> pendingMaterialList = newArrayList(pdfMaterialPending, csvMaterialPending);

        mockPreviousMaterialEvents(materialPendingEventEnvelope(PDF_SUBMISSION_ID, pdfMaterialPending), materialPendingEventEnvelope(CSV_SUBMISSION_ID, csvMaterialPending));

        ReflectionUtil.setField(aggregate, "pendingMaterials", pendingMaterialList);

        final AcceptCase acceptCase = readJson("json/acceptSjpCase.json", AcceptCase.class);

        final Envelope<AcceptCase> envelope =
                envelopeFrom(withSubmissionId(metadataFor("prosecutioncasefile.command.accept-case"), CASE_SUBMISSION_ID.toString()), acceptCase);

        acceptCaseHandler.handleAcceptCase(envelope);

        verify(eventStream).append(captor.capture());

        final Stream<JsonEnvelope> appendedEvents = captor.getAllValues().get(0);

        final Map<String, JsonEnvelope> events = appendedEvents.collect(toMap(e -> e.metadata().name(), identity()));

        assertThat(events.size(), is(3));
        final JsonEnvelope materialAddedEvent = events.get("prosecutioncasefile.events.material-added");
        final JsonEnvelope materialRejectedEvent = events.get("prosecutioncasefile.events.material-rejected");
        assertThat(materialAddedEvent.metadata().asJsonObject().getString("submissionId"), is(PDF_SUBMISSION_ID.toString()));
        assertThat(materialRejectedEvent.metadata().asJsonObject().getString("submissionId"), is(CSV_SUBMISSION_ID.toString()));

        final JsonObject materialAddedPayload = materialAddedEvent.payloadAsJsonObject();
        final JsonObject materialRejectedPayload = materialRejectedEvent.payloadAsJsonObject();

        assertThat(materialAddedPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(CASE_ID.toString())),
                        withJsonPath("$.prosecutingAuthority", is(PROSECUTING_AUTHORITY)),
                        withJsonPath("$.prosecutorDefendantId", is(PROSECUTOR_DEFENDANT_ID)),
                        withJsonPath("$.material.documentType", is(MATERIAL_TYPE)),
                        withJsonPath("$.material.fileStoreId", is(PDF_FILE_STORE_ID.toString())),
                        withJsonPath("$.material.fileType", is(PDF_FILE_TYPE))
                ))
        );

        assertThat(materialRejectedPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(CASE_ID.toString())),
                        withJsonPath("$.prosecutingAuthority", is(PROSECUTING_AUTHORITY)),
                        withJsonPath("$.prosecutorDefendantId", is(PROSECUTOR_DEFENDANT_ID)),
                        withJsonPath("$.material.documentType", is(MATERIAL_TYPE)),
                        withJsonPath("$.material.fileStoreId", is(CSV_FILE_STORE_ID.toString())),
                        withJsonPath("$.material.fileType", is(CSV_FILE_TYPE)),
                        withJsonPath("$.errors", hasSize(1)),
                        withJsonPath("$.errors[0].code", is("INVALID_FILE_TYPE")),
                        withJsonPath("$.isCpsCase", is(FALSE))
                ))
        );

        final JsonEnvelope sjpCaseCreatedEvent = events.get("prosecutioncasefile.events.sjp-case-created-successfully");
        assertThat(sjpCaseCreatedEvent.metadata().asJsonObject().getString("submissionId"), is(CASE_SUBMISSION_ID.toString()));
        assertThat(sjpCaseCreatedEvent.payloadAsJsonObject(), is(notNullValue()));

        verify(onCloseMock).run(); //Underlying Stream uses DB connection, need to verify if close was properly called for previous events
    }

    @Test
    public void shouldHandleAcceptCaseWithPendingMaterialsV2() throws Exception {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(randomUUID())
                .withSection(MATERIAL_TYPE).build()));
        when(referenceDataQueryService.retrieveProsecutors(any(String.class))).thenReturn(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        aggregate.apply(buildCcCaseReceivedEvent());
        final MaterialPendingV2 pdfMaterialPending = buildMaterialPendingV2Event(PDF_FILE_STORE_ID, PDF_FILE_TYPE);
        final MaterialPendingV2 csvMaterialPending = buildMaterialPendingV2Event(CSV_FILE_STORE_ID, CSV_FILE_TYPE);
        final List<MaterialPendingV2> pendingMaterialList = newArrayList(pdfMaterialPending, csvMaterialPending);

        mockPreviousMaterialEvents(materialPendingEventEnvelopeV2(PDF_SUBMISSION_ID, pdfMaterialPending), materialPendingEventEnvelopeV2(CSV_SUBMISSION_ID, csvMaterialPending));

        ReflectionUtil.setField(aggregate, "pendingMaterialsV2", pendingMaterialList);

        final AcceptCase acceptCase = readJson("json/acceptCcCase.json", AcceptCase.class);

        final Envelope<AcceptCase> envelope =
                envelopeFrom(withSubmissionId(metadataFor("prosecutioncasefile.command.accept-case"), CASE_SUBMISSION_ID.toString()), acceptCase);

        when(referenceDataQueryService.getProsecutorsByOuCode(any())).thenReturn(ProsecutorsReferenceData.prosecutorsReferenceData().withCpsFlag(true).build());
        acceptCaseHandler.handleAcceptCase(envelope);

        verify(eventStream).append(captor.capture());

        final Stream<JsonEnvelope> appendedEvents = captor.getAllValues().get(0);

        final Map<String, JsonEnvelope> events = appendedEvents.collect(toMap(e -> e.metadata().name(), identity()));

        assertThat(events.size(), is(3));
        final JsonEnvelope materialAddedEvent = events.get("prosecutioncasefile.events.material-added-v2");
        final JsonEnvelope materialRejectedEvent = events.get("prosecutioncasefile.events.material-rejected-v2");
        assertThat(materialAddedEvent.metadata().asJsonObject().getString("submissionId"), is(PDF_SUBMISSION_ID.toString()));
        assertThat(materialRejectedEvent.metadata().asJsonObject().getString("submissionId"), is(CSV_SUBMISSION_ID.toString()));

        final JsonObject materialAddedPayload = materialAddedEvent.payloadAsJsonObject();
        final JsonObject materialRejectedPayload = materialRejectedEvent.payloadAsJsonObject();

        assertThat(materialAddedPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(CASE_ID.toString())),
                        withJsonPath("$.caseSubFolderName", is("caseSubFolderName")),
                        withJsonPath("$.exhibit.reference", is("test-reference")),
                        withJsonPath("$.fileName",is("Material-File")),
                        withJsonPath("$.isCpsCase" , is (false)),
                        withJsonPath("$.material" , is (PDF_FILE_STORE_ID.toString())),
                        withJsonPath("$.materialContentType", is(PDF_FILE_TYPE)),
                        withJsonPath("$.materialName", is("defendant document")),
                        withJsonPath("$.materialType" , is("SJPN")),
                        withJsonPath("$.prosecutionCaseSubject.caseUrn", is("CASETFL001")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.dateOfBirth", is ("1990-10-23")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.forename", is("John")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(DEFENDANT_ID.toString())),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.surname", is("Smith")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.title", is("Baron")),
                        withJsonPath("$.prosecutionCaseSubject.prosecutingAuthority", is("00TFL1A")),
                        withJsonPath("$.sectionOrderSequence", is(1))
                ))
        );

        assertThat(materialRejectedPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(CASE_ID.toString())),
                        withJsonPath("$.caseSubFolderName", is("caseSubFolderName")),
                        withJsonPath("$.exhibit.reference", is("test-reference")),
                        withJsonPath("$.fileName",is("Material-File")),
                        withJsonPath("$.isCpsCase" , is (FALSE)),
                        withJsonPath("$.material" , is (CSV_FILE_STORE_ID.toString())),
                        withJsonPath("$.materialContentType", is(CSV_FILE_TYPE)),
                        withJsonPath("$.materialName", is("defendant document")),
                        withJsonPath("$.materialType" , is("SJPN")),
                        withJsonPath("$.prosecutionCaseSubject.caseUrn", is("CASETFL001")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.dateOfBirth", is ("1990-10-23")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.forename", is("John")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.prosecutorDefendantId", is(DEFENDANT_ID.toString())),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.surname", is("Smith")),
                        withJsonPath("$.prosecutionCaseSubject.defendantSubject.prosecutorPersonDefendantDetails.title", is("Baron")),
                        withJsonPath("$.prosecutionCaseSubject.prosecutingAuthority", is("00TFL1A")),
                        withJsonPath("$.sectionOrderSequence", is(1)),
                        withJsonPath("$.errors", hasSize(1)),
                        withJsonPath("$.errors[0].code", is("INVALID_FILE_TYPE"))
                ))
        );

        final JsonEnvelope sjpCaseCreatedEvent = events.get("prosecutioncasefile.events.case-created-successfully");
        assertThat(sjpCaseCreatedEvent.metadata().asJsonObject().getString("submissionId"), is(CASE_SUBMISSION_ID.toString()));
        assertThat(sjpCaseCreatedEvent.payloadAsJsonObject(), is(notNullValue()));

        verify(onCloseMock).run(); //Underlying Stream uses DB connection, need to verify if close was properly called for previous events
    }

    @Test
    public void shouldHandleCheckPendingEvents() throws EventStreamException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        aggregate.apply(buildSjpCaseReceivedEvent());

        when(eventStream.read()).thenReturn(empty());

        final Envelope<CheckPendingEventsForNewDefendants> envelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.command.check-pending-events-for-new-defendants"),
                readJson("json/acceptSjpCase.json", CheckPendingEventsForNewDefendants.class));

        acceptCaseHandler.handleCheckPendingEvents(envelope);

        assertThat(acceptCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleCheckPendingEvents")
                        .thatHandles("prosecutioncasefile.command.check-pending-events-for-new-defendants")
                ));
    }

    private MaterialPending buildMaterialPendingEvent(final UUID fileStoreId, final String fileType) {
        return materialPending()
                .withCaseId(CASE_ID)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .withProsecutorDefendantId(PROSECUTOR_DEFENDANT_ID)
                .withMaterial(material()
                        .withDocumentType(MATERIAL_TYPE)
                        .withFileStoreId(fileStoreId)
                        .withFileType(fileType)
                        .build())
                .withIsCpsCase(FALSE)
                .build();
    }

    private MaterialPendingV2 buildMaterialPendingV2Event(final UUID fileStoreId, final String fileType) {
        return materialPendingV2()
                .withCaseId(CASE_ID)
                .withCaseSubFolderName("caseSubFolderName")
                .withExhibit(Exhibit.exhibit().withReference("test-reference").build())
                .withFileName("Material-File")
                .withIsCpsCase(FALSE)
                .withMaterial(fileStoreId)
                .withMaterialContentType(fileType)
                .withMaterialName("defendant document")
                .withMaterialType("sjpn")
                .withProsecutionCaseSubject(ProsecutionCaseSubject.prosecutionCaseSubject()
                        .withCaseUrn("CASETFL001")
                        .withDefendantSubject(DefendantSubject.defendantSubject()
                                .withProsecutorPersonDefendantDetails(ProsecutorPersonDefendantDetails.prosecutorPersonDefendantDetails()
                                        .withDateOfBirth("1990-10-23")
                                        .withForename("John")
                                        .withProsecutorDefendantId(DEFENDANT_ID.toString())
                                        .withSurname("Smith")
                                        .withTitle("Baron")
                                        .build())
                                .build())
                        .withProsecutingAuthority("00TFL1A")
                        .build())
                .withSectionOrderSequence(1)
                .withSubmissionId(CASE_SUBMISSION_ID)
                .build();
    }

    private SjpProsecutionReceived buildSjpCaseReceivedEvent() {
        return sjpProsecutionReceived()
                .withProsecution(prosecution()
                        .withCaseDetails(caseDetails().withInitiationCode("J").build())
                        .withDefendants(singletonList(defendant)).build())
                .withExternalId(EXTERNAL_ID)
                .build();
    }

    private CcCaseReceived buildCcCaseReceivedEvent() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode("C")
                        .build())
                .withDefendants(of(defendant))
                .build());
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return ccCaseReceived().withProsecutionWithReferenceData(prosecutionWithReferenceData).build();
    }

    private void mockPreviousMaterialEvents(final JsonEnvelope... pendingMaterials) {
        final Stream<JsonEnvelope> previousEventsStream = Stream.of(pendingMaterials).onClose(onCloseMock);
        when(eventStream.read()).thenReturn(previousEventsStream);
    }

    private JsonEnvelope materialPendingEventEnvelope(final UUID submissionId, final MaterialPending materialPending) {
        final Metadata metadata = metadataFrom(createObjectBuilder(metadataWithRandomUUID("prosecutioncasefile.events.material-pending").build().asJsonObject())
                .add("submissionId", submissionId.toString()).build())
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("caseId", materialPending.getCaseId().toString())
                .add("prosecutingAuthority", materialPending.getProsecutingAuthority())
                .add("prosecutorDefendantId", materialPending.getProsecutorDefendantId())
                .add("material", JsonObjects.createObjectBuilder()
                        .add("fileStoreId", materialPending.getMaterial().getFileStoreId().toString())
                        .add("documentType", materialPending.getMaterial().getDocumentType())
                        .add("fileType", materialPending.getMaterial().getFileType())
                        .build())
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private JsonEnvelope materialPendingEventEnvelopeV2(final UUID submissionId, final MaterialPendingV2 materialPending) {
        final Metadata metadata = metadataFrom(JsonObjects.createObjectBuilder(metadataWithRandomUUID("prosecutioncasefile.events.material-pending-v2").build().asJsonObject())
                .add("submissionId", submissionId.toString()).build())
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("caseId", materialPending.getCaseId().toString())
                .add("caseSubFolderName", materialPending.getCaseSubFolderName())
                .add("exhibit", createObjectBuilder()
                        .add("reference", materialPending.getExhibit().getReference())
                        .build())
                .add("fileName", materialPending.getFileName())
                .add("isCpsCase", materialPending.getIsCpsCase())
                .add("material", materialPending.getMaterial().toString())
                .add("materialContentType", materialPending.getMaterialContentType())
                .add("materialName", materialPending.getMaterialName())
                .build();

        return JsonEnvelope.envelopeFrom(metadata, payload);
    }

    private Metadata withSubmissionId(final Metadata metadata, final String submissionId) {
        return metadataFrom(JsonObjects.createObjectBuilder(metadata.asJsonObject()).add("submissionId", submissionId).build()).build();
    }
}
