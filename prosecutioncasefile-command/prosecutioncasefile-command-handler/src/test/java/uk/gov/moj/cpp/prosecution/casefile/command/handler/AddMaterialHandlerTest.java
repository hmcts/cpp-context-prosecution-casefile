package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData.documentTypeAccessReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import uk.gov.justice.core.courts.AddApplicationCourtDocument;
import uk.gov.justice.core.courts.AddCaseCourtDocument;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentBundleSectionReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.test.utils.FileResourceObjectMapper;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddApplicationMaterialV2;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddCpsMaterial;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterial;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterialV2;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AddMaterials;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddMaterialHandlerTest {

    private static final UUID DOCUMENT_TYPE_ID = fromString("51cac7fb-387c-4d19-9c80-8963fa8cf223");
    private static final String DOCUMENT_CATEGORY = "Defendant level";
    private static final String DOCUMENT_CATEGORY_CASE_LEVEL = "Case level";

    private static final UUID OFFENCE_ID = fromString("5f66994c-c8f2-458d-9828-d2923308a0ad");
    private static final String OFFENCE_CODE = "FOO";
    private static final LocalDate OFFENCE_COMMITTED_DATE = now();
    private static final LocalDate ARREST_DATE = now().minusMonths(3);
    private static final LocalDate OFFENCE_CHARGE_DATE = now().minusMonths(4);
    private static final String COURT_HEARING_LOCATION = "B01677";
    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String CUSTODY_STATUS = "U";

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    MaterialAdded.class,
                    MaterialPending.class,
                    MaterialRejected.class,
                    MaterialAddedV2.class,
                    MaterialPendingV2.class,
                    MaterialRejectedV2.class,
                    CaseDocumentReviewRequired.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private AddMaterialHandler addMaterialHandler;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    private ApplicationFile applicationFile = new ApplicationFile();

    private final FileResourceObjectMapper testHelper = new FileResourceObjectMapper();

    public void mockRefDataForDocCategory(final String docDategory){
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(docDategory)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(docDategory)
                .withSectionCode("IDPC")
                .build());

        when(referenceDataQueryService.retrieveOrganisationUnits("00TFL1A")).thenReturn(singletonList(OrganisationUnitReferenceData.organisationUnitReferenceData().build()));
    }

    @Test
    public void shouldHandleAddMaterialCommand() {
        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addMaterial")
                        .thatHandles("prosecutioncasefile.command.add-material")
                ));
    }

    @Test
    public void shouldHandleAddMaterialV2Command() {
        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addMaterialV2")
                        .thatHandles("prosecutioncasefile.command.add-material-v2")
                ));
    }

    @Test
    public void shouldHandleAddCpsMaterialCommand() {
        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addCpsMaterial")
                        .thatHandles("prosecutioncasefile.command.handler.add-cps-material")
                ));
    }

    @Test
    public void shouldHandleAddMaterialWhenCaseCreated() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        final AddMaterial addMaterial = readJson("json/addMaterial.json", AddMaterial.class);

        final Envelope<AddMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material"), addMaterial);

        addMaterialHandler.addMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added",
                () -> readJson("json/materialAdded.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddMaterialsWhenCaseCreated() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1))) {
                return aggregate;
            }
            else {
                return applicationFile;
            }
        });

        final AddMaterials addMaterials = readJson("json/addMaterials.json", AddMaterials.class);

        final Envelope<AddMaterials> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-materials"), addMaterials);

        addMaterialHandler.addMaterials(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added",
                asList(testHelper.convertFromFile("json/materialAdded_1.json", JsonValue.class),
                        testHelper.convertFromFile("json/materialAdded_2.json", JsonValue.class)
                ));
    }

    @Test
    public void shouldHandleAddMaterialsAsPendingWhenCaseNotCreated() throws Exception {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        final AddMaterials addMaterial = readJson("json/addMaterials.json", AddMaterials.class);

        final Envelope<AddMaterials> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-materials"), addMaterial);

        addMaterialHandler.addMaterials(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-pending",
                asList(testHelper.convertFromFile("json/materialPending_1.json", JsonValue.class),
                        testHelper.convertFromFile("json/materialPending_2.json", JsonValue.class)
                ));
    }

    @Test
    public void shouldHandleAddMaterialV2WhenCaseCreated() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        ReflectionUtil.setField(aggregate, "defendants", singletonList(buildDefendant()));
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.retrieveProsecutors(any())).thenReturn(ProsecutorsReferenceData.prosecutorsReferenceData().build());
        final AddMaterialV2 addMaterialV2 = readJson("json/addMaterialV2.json", AddMaterialV2.class);

        final Envelope<AddMaterialV2> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material-v2"), addMaterialV2);

        when(referenceDataQueryService.getProsecutorsByOuCode(any())).thenReturn(ProsecutorsReferenceData.prosecutorsReferenceData().withCpsFlag(true).build());

        addMaterialHandler.addMaterialV2(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added-v2",
                () -> readJson("json/materialAddedV2.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddMaterialV2ForApplication() throws Exception {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory("Applications")
                .withSectionCode("IDPC")
                .build()));

        final AddApplicationMaterialV2 addApplicationMaterialV2 = readJson("json/addMaterialV2WithApplication.json", AddApplicationMaterialV2.class);

        final Envelope<AddApplicationMaterialV2> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material-v2"), addApplicationMaterialV2);

        when(progressionService.getApplicationOnly(any())).thenReturn(CourtApplication.courtApplication().withType(CourtApplicationType.courtApplicationType().build()).build());

        addMaterialHandler.addApplicationMaterialV2(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added-v2",
                () -> readJson("json/materialAddedV2ForApplication.json", JsonValue.class));
    }

    @Test
    public void shouldHandleRejectedMaterialV2WhenApplicationDoesNotExist() throws Exception {

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory("Applications")
                .withSectionCode("IDPC")
                .build()));
        final AddApplicationMaterialV2 addApplicationMaterialV2 = readJson("json/addMaterialV2WithApplication.json", AddApplicationMaterialV2.class);

        final Envelope<AddApplicationMaterialV2> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-application-material-v2"), addApplicationMaterialV2);

        when(progressionService.getApplicationOnly(any())).thenReturn(null);

        addMaterialHandler.addApplicationMaterialV2(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-rejected-v2",
                () -> readJson("json/materialRejectedV2ForApplication.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddCpsMaterialWhenCaseCreated() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        final AddCpsMaterial addCpsMaterial = readJson("json/addCpsMaterial.json", AddCpsMaterial.class);

        final Envelope<AddCpsMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.handler.add-cps-material"), addCpsMaterial);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withCpsBundleCode("1")
                .withTargetSectionCode("IDPC")
                .build());
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build());

        addMaterialHandler.addCpsMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added",
                () -> readJson("json/materialAddedForCps.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddCpsMaterialWhenCaseCreatedWithRuleExecutions() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        ReflectionUtil.setField(aggregate, "caseType", CaseType.CC);
        final AddCpsMaterial addCpsMaterial = readJson("json/addCpsMaterial.json", AddCpsMaterial.class);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withCpsBundleCode("1")
                .withTargetSectionCode("IDPC")
                .build());
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build());

        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY_CASE_LEVEL)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY_CASE_LEVEL)
                .withSectionCode("IDPC")
                .build());

        final Envelope<AddCpsMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.handler.add-cps-material"), addCpsMaterial);

        addMaterialHandler.addCpsMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-added",
                () -> readJson("json/materialAddedForCps-cc-case.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddCpsMaterialWhenNotProsecutionAccepted() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", FALSE);
        final AddCpsMaterial addCpsMaterial = readJson("json/addCpsMaterial.json", AddCpsMaterial.class);

        final Envelope<AddCpsMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.handler.add-cps-material"), addCpsMaterial);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withCpsBundleCode("1")
                .withTargetSectionCode("IDPC")
                .build());
        when(referenceDataQueryService.getDocumentTypeAccessBySectionCode(any(), any())).thenReturn(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build());

        addMaterialHandler.addCpsMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.case-document-review-required",
                () -> readJson("json/caseDocumentReviewRequired.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddCpsMaterialAndRaiseRejectedEventWithNullDocumentType() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        ReflectionUtil.setField(aggregate, "caseType", CaseType.CC);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withCpsBundleCode("1")
                .withTargetSectionCode("IDPC")
                .build());
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData().build());

        final AddCpsMaterial addCpsMaterial = readJson("json/addCpsMaterial-null-document-type.json", AddCpsMaterial.class);

        final Envelope<AddCpsMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.handler.add-cps-material"), addCpsMaterial);

        addMaterialHandler.addCpsMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-rejected",
                () -> readJson("json/caseDocument-rejected.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddCpsMaterialAndRaiseReviewEventWithNullDocumentType() throws Exception {
        ReflectionUtil.setField(aggregate, "prosecutionAccepted", TRUE);
        ReflectionUtil.setField(aggregate, "caseType", CaseType.CC);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withCpsBundleCode("1")
                .withTargetSectionCode("IDPC")
                .build());
        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(documentTypeAccessReferenceData()
                .withId(DOCUMENT_TYPE_ID)
                .withSection("IDPC bundle")
                .withDocumentCategory(DOCUMENT_CATEGORY)
                .withSectionCode("IDPC")
                .build()));
        when(referenceDataQueryService.getParentBundleSectionByCpsBundleCode(any(), any())).thenReturn(ParentBundleSectionReferenceData.parentBundleSectionReferenceData().build());

        final AddCpsMaterial addCpsMaterial = readJson("json/addCpsMaterial-null-document-type.json", AddCpsMaterial.class);

        final Envelope<AddCpsMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.handler.add-cps-material"), addCpsMaterial);

        addMaterialHandler.addCpsMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.case-document-review-required",
                () -> readJson("json/caseDocumentReviewRequired-for-reject-case.json", JsonValue.class));
    }


    @Test
    public void shouldHandleAddMaterialAsPendingWhenCaseNotCreated() throws Exception {
        final AddMaterial addMaterial = readJson("json/addMaterial.json", AddMaterial.class);

        final Envelope<AddMaterial> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material"), addMaterial);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        addMaterialHandler.addMaterial(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-pending",
                () -> readJson("json/materialPending.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddMaterialV2AsPendingWhenCaseNotCreated() throws Exception {
        final AddMaterialV2 addMaterialV2 = readJson("json/addMaterialV2.json", AddMaterialV2.class);

        final Envelope<AddMaterialV2> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.add-material-v2"), addMaterialV2);
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        addMaterialHandler.addMaterialV2(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.events.material-pending-v2",
                () -> readJson("json/materialPendingV2.json", JsonValue.class));
    }

    @Test
    public void shouldHandleAddIdpcMaterialCommand() {
        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addIdpcMaterial")
                        .thatHandles("prosecutioncasefile.handler.add-idpc-material")
                ));
    }

    @Test
    public void shouldHandleSpiCcCaseUpdated() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.handler.case-updated-initiate-idpc-match"),
                readJson("json/acceptSjpCase.json", JsonValue.class));

        addMaterialHandler.spiCcCaseUpdated(envelope);

        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("spiCcCaseUpdated")
                        .thatHandles("prosecutioncasefile.handler.case-updated-initiate-idpc-match")
                ));
    }

    @Test
    public void shouldHandleAddApplicationCourtDocument() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        final AddApplicationCourtDocument addApplicationCourtDocument = AddApplicationCourtDocument.addApplicationCourtDocument().withApplicationId(randomUUID()).build();
        final Envelope<AddApplicationCourtDocument> envelope = envelopeFrom(metadataWithRandomUUID(
                "prosecutioncasefile.command.add-application-court-document"),
                addApplicationCourtDocument);

        addMaterialHandler.addApplicationCourtDocument(envelope);

        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addApplicationCourtDocument")
                        .thatHandles("prosecutioncasefile.command.add-application-court-document")
                ));
    }

    @Test
    public void shouldHandleAddCaseCourtDocument() throws EventStreamException {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eq(eventStream), any())).thenAnswer(invocationOnMock -> {
            if (ProsecutionCaseFile.class.isAssignableFrom(invocationOnMock.getArgument(1)))
                return aggregate;
            else
                return applicationFile;
        });

        final AddCaseCourtDocument addCaseCourtDocument = AddCaseCourtDocument.addCaseCourtDocument().build();
        final Envelope<AddCaseCourtDocument> envelope = envelopeFrom(metadataWithRandomUUID(
                        "prosecutioncasefile.command.add-case-court-document"),
                readJson("json/prosecutioncasefile.command.add-case-court-document.json",AddCaseCourtDocument.class));

        addMaterialHandler.addCaseCourtDocument(envelope);

        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addCaseCourtDocument")
                        .thatHandles("prosecutioncasefile.command.add-case-court-document")
                ));
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendant() {
        return defendant()
                .withId("2b9c0dac-deeb-4634-ba69-be5115bdc585")
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName("David")
                                .withLastName("Miller")
                                .withTitle("Mr").build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .build())
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withOffences(singletonList(offence()
                        .withArrestDate(ARREST_DATE)
                        .withOffenceId(randomUUID())
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                        .withChargeDate(OFFENCE_CHARGE_DATE)
                        .withOffenceId(OFFENCE_ID)
                        .build()))
                .build();
    }

}