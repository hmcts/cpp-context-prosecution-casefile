package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.FileUtil.jsonFromString;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.CASE_URN_NOT_FOUND;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence.defence;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.FormData.formData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.FormDefendants.formDefendants;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed.receivedCpsServePetProcessed;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingCpsServeBcmExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.activiti.PendingCpsServePetExpiration;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsCaseContact;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BcmDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeBcmSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeCotrSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServeMaterialStatusUpdated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsServePtphSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsUpdateCotrSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormdefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePtphProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsUpdateCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.Lists;
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
public class CpsServeMaterialEventProcessorTest {

    private static final String PET_EVENT = "prosecutioncasefile.events.received-cps-serve-pet-processed";
    private static final String BCM_EVENT = "prosecutioncasefile.events.received-cps-serve-bcm-processed";
    private static final String PTPH_EVENT = "prosecutioncasefile.events.received-cps-serve-ptph-processed";
    private static final String COTR_EVENT = "prosecutioncasefile.events.received-cps-serve-cotr-processed";
    private static final String UPDATE_COTR_EVENT = "prosecutioncasefile.events.received-cps-update-cotr-processed";
    private static final String MATERIAL_STATUS_EVENT = "public.prosecutioncasefile.cps-serve-material-status-updated";
    private static final String STAGING_COTR_PUBLIC_EVENT = "public.stagingprosecutors.cps-serve-cotr-received";
    private static final String STAGING_COTR_UPDATE_PUBLIC_EVENT = "public.stagingprosecutors.cps-update-cotr-received";
    private static final String STATUS_UPDATED_PUBLIC_EVENT = "public.prosecutioncasefile.cps-update-cotr-submitted";


    private static final String N = "N";
    private static final String Y = "Y";
    private static final String CPS_USER_NAME = "cps user name";
    private static final String CPS_USER_EMAIL = "cpsuser@email.com";
    private static final String PHONE = "5723423456";
    private static final UUID CASE_ID_VALUE = randomUUID();
    private static final UUID submissionId = randomUUID();
    private static final String NO = "N";
    public static final String VALIDATION_DATA = "validationData";
    public static final String VALID_OFFENCES = "validOffences";
    public static final String DEFENDANT_IDS = "defendantIds";
    public static final String PROSECUTION_CASE_SUBJECT = "prosecutionCaseSubject";
    public static final String SUBMISSION_ID = "submissionId";
    public static final String EVIDENCE_PRE_PTPH = "evidencePrePTPH";
    public static final String CPS_DEFENDANT_OFFENCES = "cpsDefendantOffences";
    public static final String CPS_OFFENCE_DETAILS = "cpsOffenceDetails";
    public static final String CJS_OFFENCE_CODE = "cjsOffenceCode";
    public static final String OFFENCE_WORDING = "offenceWording";
    public static final String OFFENCE_DATE = "offenceDate";
    public static final String CASE_URN = "urn";

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private CpsServeMaterialEventProcessor cpsServeMaterialEventProcessor;
    @Mock
    private Sender sender;
    @Mock
    private PendingCpsServePetExpiration pendingCpsServePetExpiration;
    @Mock
    private PendingCpsServeBcmExpiration pendingCpsServeBcmExpiration;
    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsServeMaterialStatusUpdated>> cpsServeMaterialStatusUpdatedArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope<CpsServePtphSubmitted>> cpsServePtphSubmittedArgumentCaptor;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;
    @Mock
    private ProgressionService progressionService;

    private static ReceivedCpsServePetProcessed createCpsServePetReceived(final SubmissionStatus submissionStatus, List<Problem> errors) {

        final List<Witnesses> witnesses = new ArrayList<>();

        witnesses.add(Witnesses.witnesses()
                .withInterpreterRequired("No")
                .build());

        return receivedCpsServePetProcessed().withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withCpsDefendantOffences(createCpsDefendantOffences())
                .withPetFormData(createPetFormData())
                .withPetDefendants(singletonList(PetDefendants.petDefendants().withDefendantId(randomUUID()).build()))
                .withReviewingLawyer(CpsCaseContact.cpsCaseContact()
                        .withName(CPS_USER_NAME)
                        .withEmail(CPS_USER_EMAIL)
                        .withPhone(PHONE)
                        .build())
                .withErrors(errors)
                .withIsYouth(false)
                .build();
    }


    private static List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences> createDefendantOffences() {
        List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences> cpsDefendantOffences = new ArrayList<>();

        List<CpsOffenceDetails> cpsOffenceDetails = new ArrayList<>();

        cpsOffenceDetails.add(CpsOffenceDetails.cpsOffenceDetails()
                .withOffenceWording(OFFENCE_WORDING)
                .withCjsOffenceCode("CA03013")
                .withOffenceDate(convertToLocalDate("2021-12-10"))
                .build());

        cpsDefendantOffences.add(CpsDefendantOffences.cpsDefendantOffences()
                .withAsn("1000NP0004444000203B")
                .withCpsDefendantId("d123ed32-ce29-11ec-9d64-0242ac120002")
                .withCpsOffenceDetails(cpsOffenceDetails)
                .withDateOfBirth(convertToLocalDate("1990-10-10"))
                .withForename("John")
                .withForename2("forename2")
                .withForename3("forename3")
                .withMatchingId(randomUUID())
                .withOrganisationName("organisation name")
                .withProsecutorDefendantId("prosecutor defandant ID")
                .withSurname("Smith")
                .withTitle("Mr")
                .build());

        return cpsDefendantOffences;
    }

    private static ProsecutionCaseSubject createCpsProsecutionCaseSubject() {
        return ProsecutionCaseSubject.prosecutionCaseSubject()
                .withUrn("TVL1234")
                .withProsecutingAuthority("OUCODE")
                .build();
    }

    private static List<CpsDefendantOffences> createCpsDefendantOffences() {

        final List<CpsDefendantOffences> cpsDefendantOffences = new ArrayList<>();
        final List<uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails> cpsOffenceDetails = new ArrayList<>();

        cpsOffenceDetails.add(CpsOffenceDetails.cpsOffenceDetails()
                .withCjsOffenceCode("CJSCODE001")
                .withOffenceDate(convertToLocalDate("2020-12-10"))
                .withOffenceWording("Test Offence Wording")
                .build());

        cpsDefendantOffences.add(CpsDefendantOffences.cpsDefendantOffences()
                .withAsn("ASN")
                .withForename("Forename")
                .withSurname("Surname")
                .withDateOfBirth(convertToLocalDate("2000-12-10"))
                .withForename2("Forname 2")
                .withForename3("Forename 3")
                .withTitle("Mr")
                .withCpsDefendantId("")
                .withOrganisationName("Organization Name")
                .withProsecutorDefendantId("PDEFID001")
                .withSurname("Surname")
                .withCpsOffenceDetails(cpsOffenceDetails).build());


        return cpsDefendantOffences;
    }


    private static ReceivedCpsServeBcmProcessed createCpsServeBcmReceived(final SubmissionStatus submissionStatus, final UUID caseId, final UUID defendantId) {
        return ReceivedCpsServeBcmProcessed.receivedCpsServeBcmProcessed().withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .withCaseId(caseId)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withCpsDefendantOffences(createDefendantOffences())
                .withFormDefendants(asList(formDefendants().withDefendantId(defendantId).build()))
                .withFormData(formData()
                        .withBcmDefendants(asList(BcmDefendants.bcmDefendants()
                                .withProsecutorOffences(asList(ProsecutorOffences.prosecutorOffences()
                                        .withDate(LocalDate.now())
                                        .withWording("wording")
                                        .withOffenceCode("offenceCode")
                                        .build()))
                                .withOtherAreasBeforePtph("otherAreasBeforePtph")
                                .withAnyOther("anyOther")
                                .withOtherAreasAfterPtph("otherAreasAfterPtph")
                                .withId(defendantId).build()))
                        .build()).build();

    }

    private static PetFormData createPetFormData() {

        final List<Defendants> defendants = new ArrayList<>();
        final List<CpsOffences> cpsOffences = new ArrayList<>();

        final List<Witnesses> witnesses = new ArrayList<>();

        witnesses.add(Witnesses.witnesses()
                .withInterpreterRequired("No")
                .build());

        defendants.add(Defendants.defendants()
                .withCpsDefendantId("CPSDEF001")
                .withId(randomUUID())
                .withProsecutorDefendantId("PDEF999")
                .build());


        return PetFormData.petFormData()
                .withDefence(defence().withDefendants(defendants).build())
                .withProsecution(createProsecution()).build();

    }

    private static PtphFormData createPtphFormData() {

        final List<Defendants> defendants = new ArrayList<>();

        final List<Witnesses> witnesses = new ArrayList<>();

        witnesses.add(Witnesses.witnesses()
                .withInterpreterRequired("No")
                .build());

        defendants.add(Defendants.defendants()
                .withCpsDefendantId("CPSDEF001")
                .withId(randomUUID())
                .withProsecutorDefendantId("PDEF999")
                .build());


        return PtphFormData.ptphFormData()
                .withPtphFormdefendants(Lists.newArrayList(PtphFormdefendants.ptphFormdefendants()
                        .withId(randomUUID().toString()).build()))
                .build();

    }


    private static uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution createProsecution() {

        final ApplicationsForDirectionsGroup applicationsForDirectionsGroup = ApplicationsForDirectionsGroup.applicationsForDirectionsGroup()
                .withVariationStandardDirectionsProsecutor(Y)
                .withGroundRulesQuestioning(Y).build();

        final ProsecutorGroup prosecutorGroup = ProsecutorGroup.prosecutorGroup()
                .withPointOfLaw(N)
                .withPointOfLawYesGroup(PointOfLawYesGroup.pointOfLawYesGroup().withPointOfLawDetails(N).build())
                .withSlaveryOrExploitationYesGroup(SlaveryOrExploitationYesGroup.slaveryOrExploitationYesGroup()
                        .withSlaveryOrExploitationDetails(N).build())
                .withPendingLinesOfEnquiry(Y)
                .withDisplayEquipment(Y)
                .withSlaveryOrExploitation(Y).build();

        final DynamicFormAnswers dynamicFormAnswers = DynamicFormAnswers.dynamicFormAnswers()
                .withProsecutorGroup(prosecutorGroup)
                .withApplicationsForDirectionsGroup(applicationsForDirectionsGroup).build();

        return Prosecution.prosecution().withDynamicFormAnswers(dynamicFormAnswers).build();

    }

    private static uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup createApplicationForDirection() {
        return ApplicationsForDirectionsGroup.applicationsForDirectionsGroup()
                .withVariationStandardDirectionsProsecutor(Y)
                .withGroundRulesQuestioning(Y).build();
    }

    private static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup createProsecutorGroup() {
        return ProsecutorGroup.prosecutorGroup()
                .withPointOfLaw(N)
                .withPointOfLawYesGroup(PointOfLawYesGroup.pointOfLawYesGroup().withPointOfLawDetails(N).build())
                .withSlaveryOrExploitationYesGroup(SlaveryOrExploitationYesGroup.slaveryOrExploitationYesGroup()
                        .withSlaveryOrExploitationDetails(N).build())
                .withPendingLinesOfEnquiry(N)
                .withDisplayEquipment(Y)
                .withSlaveryOrExploitation(Y).build();
    }

    private static LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCpsServePetReceivedPublicEvent() {
        final JsonObject jsonPayload = jsonFromString(getPayload("process-pending-cps-serve-pet.json"));

        final JsonObject offencePayload = jsonFromString(getPayload("referenceOffenceData.json"));
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(asList(jsonObjectToObjectConverter.convert(offencePayload, OffenceReferenceData.class)));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder().add("caseId", CASE_ID_VALUE.toString()).build();
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseQueryResponse);

        final JsonObject prosecutionCaseFromProgressionResponse = jsonFromString(getPayload("prosecutionCaseFromProgression.json"));
        when(progressionService.getProsecutionCase(CASE_ID_VALUE)).thenReturn(prosecutionCaseFromProgressionResponse);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("prosecutioncasefile.command.process-received-cps-serve-pet"),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServePetReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldRaisePublicEventOnPetSubmissionSuccess() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PET_EVENT)
                .withId(randomUUID())
                .build();

        final ReceivedCpsServePetProcessed cpsServePetReceived = createCpsServePetReceived(SubmissionStatus.SUCCESS, null);

        final Envelope<ReceivedCpsServePetProcessed> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePetReceived);

        assertThat(cpsServePetReceivedEnvelope.payload().getSubmissionId(), is(submissionId));
        assertThat(cpsServePetReceivedEnvelope.payload().getPetFormData(), notNullValue());

        cpsServeMaterialEventProcessor.handleCpsServePetReceivedPrivateEvent(cpsServePetReceivedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldHandleCpsServeBcmReceivedPublicEvent() {

        final JsonObject jsonPayload = jsonFromString(getPayload("process-pending-cps-serve-bcm.json"));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder()
                .add("caseId", CASE_ID_VALUE.toString()).add("prosecutionCase", createObjectBuilder().add("defendants", createArrayBuilder())).build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("prosecutioncasefile.command.process-received-cps-serve-bcm"),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServeBcmReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldRaisePublicEventOnPetSubmissionExpired() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PET_EVENT)
                .withId(randomUUID())
                .build();
        List<Problem> errors = asList(problem()
                .withCode("CASE_NOT_FOUND")
                .withValues(asList(problemValue()
                        .withKey("urn")
                        .withValue("ABC0342D")
                        .build()))
                .build());
        final ReceivedCpsServePetProcessed cpsServePetReceived = createCpsServePetReceived(SubmissionStatus.EXPIRED, errors);

        final Envelope<ReceivedCpsServePetProcessed> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePetReceived);
        cpsServeMaterialEventProcessor.handleCpsServePetReceivedPrivateEvent(cpsServePetReceivedEnvelope);

        verify(sender).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.EXPIRED));
        assertThat(payload.getErrors(), is(errors));
    }

    @Test
    public void shouldRaisePublicEventOnPetSubmission_WhenSuccessWithWarnings() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PET_EVENT)
                .withId(randomUUID())
                .build();
        List<Problem> errors = asList(problem()
                .withCode("CASE_NOT_FOUND")
                .withValues(asList(problemValue()
                        .withKey("urn")
                        .withValue("ABC0342D")
                        .build()))
                .build());
        final ReceivedCpsServePetProcessed cpsServePetReceived = createCpsServePetReceived(SubmissionStatus.SUCCESS_WITH_WARNINGS, errors);

        final Envelope<ReceivedCpsServePetProcessed> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePetReceived);
        cpsServeMaterialEventProcessor.handleCpsServePetReceivedPrivateEvent(cpsServePetReceivedEnvelope);

        verify(sender, times(2)).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.SUCCESS_WITH_WARNINGS));
        assertThat(payload.getWarnings(), is(errors));
    }

    @Test
    public void shouldRaisePublicEventOnBcmSubmissionSuccess() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(BCM_EVENT)
                .withId(randomUUID())
                .build();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();


        ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = createCpsServeBcmReceived(SubmissionStatus.SUCCESS, caseId, defendantId);

        final Envelope<ReceivedCpsServeBcmProcessed> receivedCpsServeBcmProcessedEnvelope =
                envelopeFrom(metadata, receivedCpsServeBcmProcessed);
        cpsServeMaterialEventProcessor.handleCpsServeBcmReceivedPrivateEvent(receivedCpsServeBcmProcessedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues(), hasSize(2));
        Envelope<CpsServeBcmSubmitted> cpsServeBcmSubmittedEnvelope = (Envelope<CpsServeBcmSubmitted>) envelopeCaptor.getAllValues().get(0);
        assertThat(cpsServeBcmSubmittedEnvelope.metadata().name(), is("public.prosecutioncasefile.cps-serve-bcm-submitted"));
        assertThat(cpsServeBcmSubmittedEnvelope.payload().getCaseId(), is(caseId));
        assertThat(cpsServeBcmSubmittedEnvelope.payload().getFormDefendants().get(0).getDefendantId(), is(defendantId));
        assertThat(cpsServeBcmSubmittedEnvelope.payload().getSubmissionId(), is(submissionId));
        assertThat(cpsServeBcmSubmittedEnvelope.payload().getFormData(), notNullValue());
        final String formDataString = cpsServeBcmSubmittedEnvelope.payload().getFormData();
        final JsonObject formDataJsonObject = stringToJsonObjectConverter.convert(formDataString);
        assertThat(formDataJsonObject.getJsonArray("defendants"), notNullValue());
        final List<JsonObject> defendantList = formDataJsonObject.getJsonArray("defendants").getValuesAs(JsonObject.class);
        assertThat(defendantList, hasSize(1));
        assertThat(defendantList.get(0).getString("id"), is(defendantId.toString()));
        assertThat(defendantList.get(0).getString("otherAreasBeforePtph"), is("otherAreasBeforePtph"));
        assertThat(defendantList.get(0).getString("otherAreasAfterPtph"), is("otherAreasAfterPtph"));
        assertThat(defendantList.get(0).getString("anyOther"), is("anyOther"));

        assertThat(defendantList.get(0).getJsonArray("prosecutorOffences"), notNullValue());
        final List<JsonObject> prosecutorOffencesList = defendantList.get(0).getJsonArray("prosecutorOffences").getValuesAs(JsonObject.class);
        assertThat(prosecutorOffencesList, hasSize(1));
        assertThat(prosecutorOffencesList.get(0).getString("offenceCode"), is("offenceCode"));
        assertThat(prosecutorOffencesList.get(0).getString("wording"), is("wording"));
        assertThat(prosecutorOffencesList.get(0).getString("date"), notNullValue());
    }

    @Test
    public void shouldRaisePublicEventOnBcmSubmissionExpired() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(BCM_EVENT)
                .withId(randomUUID())
                .build();
        final UUID defedantId = randomUUID();
        final UUID caseId = randomUUID();

        ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = createCpsServeBcmReceived(SubmissionStatus.EXPIRED, caseId, defedantId);

        final Envelope<ReceivedCpsServeBcmProcessed> receivedCpsServeBcmProcessedEnvelope =
                envelopeFrom(metadata, receivedCpsServeBcmProcessed);
        cpsServeMaterialEventProcessor.handleCpsServeBcmReceivedPrivateEvent(receivedCpsServeBcmProcessedEnvelope);

        verify(sender).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.EXPIRED));
    }

    @Test
    public void shouldHandleCpsServeBcmReceivedPublicEvent_WithValidData() {
        String submissionId = randomUUID().toString();
        final JsonObject jsonPayload = jsonFromString(getPayload("process-pending-cps-serve-bcm.json").replaceAll("%SUBMISSION_ID%", submissionId));

        final JsonObject offencePayload = jsonFromString(getPayload("referenceOffenceData.json"));
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(asList(jsonObjectToObjectConverter.convert(offencePayload, OffenceReferenceData.class)));

        final JsonObject prosecutionCaseFileResponseByUrn = jsonFromString(getPayload("prosecutionCaseFileQueryByCaseUrn.json").replaceAll("%CASE_ID%", CASE_ID_VALUE.toString()));
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileResponseByUrn);

        final JsonObject prosecutionCaseFromProgressionResponse = jsonFromString(getPayload("prosecutionCaseFromProgression.json"));
        when(progressionService.getProsecutionCase(CASE_ID_VALUE)).thenReturn(prosecutionCaseFromProgressionResponse);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.stagingprosecutors.cps-serve-bcm-received"),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServeBcmReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final Envelope commandEventEnvelope = envelopeCaptor.getValue();
        assertThat(commandEventEnvelope, notNullValue());
        assertThat(commandEventEnvelope.metadata().name(), is("prosecutioncasefile.command.process-received-cps-serve-bcm"));
        final JsonObject commandPayload = (JsonObject) commandEventEnvelope.payload();

        assertThat(commandPayload, notNullValue());

        assertThat(commandPayload.getJsonObject(VALIDATION_DATA), notNullValue());
        final JsonObject validationData = commandPayload.getJsonObject(VALIDATION_DATA);
        assertThat(validationData.getJsonArray(VALID_OFFENCES), notNullValue());
        final List<JsonObject> validaOffences = validationData.getJsonArray(VALID_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(validaOffences, hasSize(1));
        assertThat(validationData.getJsonArray(DEFENDANT_IDS), notNullValue());
        assertThat(validationData.getJsonArray(DEFENDANT_IDS), hasSize(1));

        assertThat(commandPayload.getJsonObject(PROSECUTION_CASE_SUBJECT), notNullValue());
        assertThat(commandPayload.getJsonObject(PROSECUTION_CASE_SUBJECT).getString("urn"), is("caseURN"));

        assertThat(commandPayload.getString(SUBMISSION_ID), is(submissionId));
        assertThat(commandPayload.getString(EVIDENCE_PRE_PTPH), is("evidencePrePTPH text"));

        assertThat(commandPayload.getJsonArray(CPS_DEFENDANT_OFFENCES), hasSize(1));
        final List<JsonObject> cpsDefendantOffences = commandPayload.getJsonArray(CPS_DEFENDANT_OFFENCES).getValuesAs(JsonObject.class);
        assertThat(cpsDefendantOffences.get(0).getJsonArray(CPS_OFFENCE_DETAILS), hasSize(1));
        final List<JsonObject> cpsOffenceDetails = cpsDefendantOffences.get(0).getJsonArray(CPS_OFFENCE_DETAILS).getValuesAs(JsonObject.class);
        assertThat(cpsOffenceDetails.get(0).getString(CJS_OFFENCE_CODE), is("TH68023"));
        assertThat(cpsOffenceDetails.get(0).getString(OFFENCE_WORDING), is(OFFENCE_WORDING));
        assertThat(cpsOffenceDetails.get(0).getString(OFFENCE_DATE), is("2022-05-11"));
    }

    @Test
    public void shouldHandleCpsServePtphReceivedPublicEvent() {
        final JsonObject jsonPayload = jsonFromString(getPayload("process-pending-cps-serve-ptph.json"));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder()
                .add("caseId", CASE_ID_VALUE.toString()).add("prosecutionCase", createObjectBuilder().add("defendants", createArrayBuilder())).build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("prosecutioncasefile.command.process-received-cps-serve-ptph"),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServePtphReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldRaisePublicEventOnPtphSubmissionSuccess() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PTPH_EVENT)
                .withId(randomUUID())
                .build();

        final ReceivedCpsServePtphProcessed cpsServePtphReceived = jsonObjectToObjectConverter.convert(jsonFromString(getPayload("process-pending-cps-processed-ptph.json")), ReceivedCpsServePtphProcessed.class);

        final Envelope<ReceivedCpsServePtphProcessed> cpsServePtphReceivedEnvelope = envelopeFrom(metadata, cpsServePtphReceived);

        assertThat(cpsServePtphReceivedEnvelope.payload().getSubmissionId(), notNullValue());
        assertThat(cpsServePtphReceivedEnvelope.payload().getPtphFormData(), notNullValue());

        cpsServeMaterialEventProcessor.handleCpsServePtphReceivedPrivateEvent(cpsServePtphReceivedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldRaisePublicEventOnPtphSubmissionSuccessWithWitnessDetails() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PTPH_EVENT)
                .withId(randomUUID())
                .build();

        final ReceivedCpsServePtphProcessed cpsServePtphReceived = jsonObjectToObjectConverter.convert(jsonFromString(getPayload("process-pending-cps-processed-ptph.json")), ReceivedCpsServePtphProcessed.class);

        final Envelope<ReceivedCpsServePtphProcessed> cpsServePtphReceivedEnvelope = envelopeFrom(metadata, cpsServePtphReceived);

        assertThat(cpsServePtphReceivedEnvelope.payload().getSubmissionId(), notNullValue());
        assertThat(cpsServePtphReceivedEnvelope.payload().getPtphFormData(), notNullValue());

        cpsServeMaterialEventProcessor.handleCpsServePtphReceivedPrivateEvent(cpsServePtphReceivedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());


        verify(sender, times(2)).send(cpsServePtphSubmittedArgumentCaptor.capture());
        final Envelope<CpsServePtphSubmitted> receivedCpsServePtphProcessedEnvelope = cpsServePtphSubmittedArgumentCaptor.getAllValues().get(0);
        final CpsServePtphSubmitted payload = receivedCpsServePtphProcessedEnvelope.payload();
        assertThat(receivedCpsServePtphProcessedEnvelope.metadata().name(), is("public.prosecutioncasefile.cps-serve-ptph-submitted"));
        assertThat(payload.getFormData().contains("OIC"), is(true));
    }

    @Test
    public void shouldRaisePublicEventOnPtphSubmission_WhenSuccessWithWarnings() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PTPH_EVENT)
                .withId(randomUUID())
                .build();
        List<Problem> errors = asList(problem()
                .withCode("CASE_NOT_FOUND")
                .withValues(asList(problemValue()
                        .withKey("urn")
                        .withValue("ABC0342D")
                        .build()))
                .build());
        final ReceivedCpsServePtphProcessed cpsServePtphReceived = jsonObjectToObjectConverter.convert(jsonFromString(getPayload("process-pending-cps-processed-ptph-withWarning.json")), ReceivedCpsServePtphProcessed.class);

        final Envelope<ReceivedCpsServePtphProcessed> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePtphReceived);
        cpsServeMaterialEventProcessor.handleCpsServePtphReceivedPrivateEvent(cpsServePetReceivedEnvelope);

        verify(sender, times(2)).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.SUCCESS_WITH_WARNINGS));
        assertThat(payload.getWarnings(), is(errors));
    }

    @Test
    public void shouldRaisePublicEventWithOICDetails() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(PTPH_EVENT)
                .withId(randomUUID())
                .build();
        final ReceivedCpsServePtphProcessed cpsServePtphReceived = jsonObjectToObjectConverter.convert(jsonFromString(getPayload("process-pending-cps-processed-ptph-withOIC.json")), ReceivedCpsServePtphProcessed.class);

        final Envelope<ReceivedCpsServePtphProcessed> cpsServePetReceivedEnvelope =
                envelopeFrom(metadata, cpsServePtphReceived);
        cpsServeMaterialEventProcessor.handleCpsServePtphReceivedPrivateEvent(cpsServePetReceivedEnvelope);

        verify(sender, times(2)).send(cpsServePtphSubmittedArgumentCaptor.capture());
        final Envelope<CpsServePtphSubmitted> cpsServePtphSubmittedEnvelope = cpsServePtphSubmittedArgumentCaptor.getAllValues().get(0);
        final CpsServePtphSubmitted payload = cpsServePtphSubmittedEnvelope.payload();
        assertThat(cpsServePtphSubmittedEnvelope.metadata().name(), is("public.prosecutioncasefile.cps-serve-ptph-submitted"));
        assertThat(payload.getFormData(), is(containsString("OIC")));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedPublicEvent_WhenCaseUrnNotFound() {
        String submissionId = randomUUID().toString();
        final JsonObject jsonPayload = jsonFromString(getPayload("public.stagingprosecutors.cps-serve-cotr-received.json").replaceAll("%SUBMISSION_ID%", submissionId));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(STAGING_COTR_PUBLIC_EVENT),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServeCotrReceivedPublicEvent(envelope);

        final List<Problem> errors = new ArrayList<>();
        final Problem problem = Problem.problem()
                .withCode(CASE_URN_NOT_FOUND.getCode())
                .withValues(asList(ProblemValue.problemValue()
                        .withKey(CASE_URN)
                        .withValue(jsonPayload.getJsonObject(PROSECUTION_CASE_SUBJECT).getString("urn"))
                        .build()))
                .build();
        errors.add(problem);

        verify(sender).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(payload.getErrors(), is(errors));
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedPublicEvent() {
        String submissionId = randomUUID().toString();
        final JsonObject jsonPayload = jsonFromString(getPayload("public.stagingprosecutors.cps-serve-cotr-received.json").replaceAll("%SUBMISSION_ID%", submissionId));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder().add("caseId", CASE_ID_VALUE.toString()).build();
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseQueryResponse);

        final JsonObject prosecutionCaseFromProgressionResponse = jsonFromString(getPayload("prosecutionCaseFromProgression.json"));
        when(progressionService.getProsecutionCase(CASE_ID_VALUE)).thenReturn(prosecutionCaseFromProgressionResponse);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(STAGING_COTR_PUBLIC_EVENT),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServeCotrReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final Envelope commandEventEnvelope = envelopeCaptor.getValue();
        assertThat(commandEventEnvelope, notNullValue());
        assertThat(commandEventEnvelope.metadata().name(), is("prosecutioncasefile.command.process-received-cps-serve-cotr"));

        final JsonObject commandPayload = (JsonObject) commandEventEnvelope.payload();
        assertThat(commandPayload, notNullValue());
        assertThat(commandPayload.getJsonObject(VALIDATION_DATA), notNullValue());
    }

    @Test
    public void shouldRaisePublicEventOnCotrSubmissionSuccess() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(COTR_EVENT)
                .withId(randomUUID())
                .build();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed = createCpsServeCotrReceived(SubmissionStatus.SUCCESS, caseId, defendantId);

        final Envelope<ReceivedCpsServeCotrProcessed> receivedCpsServeCotrProcessedEnvelope =
                envelopeFrom(metadata, receivedCpsServeCotrProcessed);
        cpsServeMaterialEventProcessor.handleCpsServeCotrReceivedEvent(receivedCpsServeCotrProcessedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues(), hasSize(2));
        Envelope<CpsServeCotrSubmitted> cpsServeCotrSubmittedEnvelope = (Envelope<CpsServeCotrSubmitted>) envelopeCaptor.getAllValues().get(0);
        assertThat(cpsServeCotrSubmittedEnvelope.metadata().name(), is("public.prosecutioncasefile.cps-serve-cotr-submitted"));
        assertThat(cpsServeCotrSubmittedEnvelope.payload().getCaseId(), is(caseId));
        assertThat(cpsServeCotrSubmittedEnvelope.payload().getFormDefendants().get(0).getDefendantId(), is(defendantId));
        assertThat(cpsServeCotrSubmittedEnvelope.payload().getSubmissionId(), is(submissionId));
    }

    private static ReceivedCpsServeCotrProcessed createCpsServeCotrReceived(final SubmissionStatus submissionStatus, final UUID caseId, final UUID defendantId) {
        return ReceivedCpsServeCotrProcessed.receivedCpsServeCotrProcessed().withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .withCaseId(caseId)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withFormDefendants(asList(formDefendants().withDefendantId(defendantId).build()))
                .build();

    }

    @Test
    public void shouldHandleCpsUpdateCotrReceivedPublicEvent() {
        String submissionId = randomUUID().toString();
        final JsonObject jsonPayload = jsonFromString(getPayload("public.stagingprosecutors.cps-update-cotr-received.json").replaceAll("%SUBMISSION_ID%", submissionId));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder().add("caseId", CASE_ID_VALUE.toString()).build();
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseQueryResponse);

        final JsonObject prosecutionCaseFromProgressionResponse = jsonFromString(getPayload("prosecutionCaseFromProgression.json"));
        when(progressionService.getProsecutionCase(CASE_ID_VALUE)).thenReturn(prosecutionCaseFromProgressionResponse);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(STAGING_COTR_UPDATE_PUBLIC_EVENT),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleUpdateCotrReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final Envelope commandEventEnvelope = envelopeCaptor.getValue();
        assertThat(commandEventEnvelope, notNullValue());
        assertThat(commandEventEnvelope.metadata().name(), is("prosecutioncasefile.command.process-received-cps-update-cotr"));

        final JsonObject commandPayload = (JsonObject) commandEventEnvelope.payload();
        assertThat(commandPayload, notNullValue());
        assertThat(commandPayload.getJsonObject(VALIDATION_DATA), notNullValue());
    }

    @Test
    public void shouldHandleCpsUpdateCotrReceivedPublicEvent_WhenCaseUrnNotFound() {
        String submissionId = randomUUID().toString();
        final JsonObject jsonPayload = jsonFromString(getPayload("public.stagingprosecutors.cps-update-cotr-received.json").replaceAll("%SUBMISSION_ID%", submissionId));

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(STAGING_COTR_UPDATE_PUBLIC_EVENT),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleUpdateCotrReceivedPublicEvent(envelope);

        final List<Problem> errors = new ArrayList<>();
        final Problem problem = Problem.problem()
                .withCode(CASE_URN_NOT_FOUND.getCode())
                .withValues(asList(ProblemValue.problemValue()
                        .withKey(CASE_URN)
                        .withValue(jsonPayload.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(CASE_URN))
                        .build()))
                .build();
        errors.add(problem);

        verify(sender).send(cpsServeMaterialStatusUpdatedArgumentCaptor.capture());
        final Envelope<CpsServeMaterialStatusUpdated> cpsServeMaterialStatusUpdatedEnvelope = cpsServeMaterialStatusUpdatedArgumentCaptor.getValue();
        final CpsServeMaterialStatusUpdated payload = cpsServeMaterialStatusUpdatedEnvelope.payload();
        assertThat(cpsServeMaterialStatusUpdatedEnvelope.metadata().name(), is(MATERIAL_STATUS_EVENT));
        assertThat(payload.getSubmissionStatus(), is(SubmissionStatus.REJECTED));
        assertThat(payload.getErrors(), is(errors));
    }

    @Test
    public void shouldRaisePublicEventOnCotrUpdateSubmissionSuccess() {
        final Metadata metadata = Envelope.metadataBuilder()
                .withName(UPDATE_COTR_EVENT)
                .withId(randomUUID())
                .build();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        ReceivedCpsUpdateCotrProcessed receivedCpsUpdateCotrProcessed = updateCpsServeCotrReceived(SubmissionStatus.SUCCESS, caseId, defendantId);

        final Envelope<ReceivedCpsUpdateCotrProcessed> receivedCpsUpdateCotrProcessedEnvelope =
                envelopeFrom(metadata, receivedCpsUpdateCotrProcessed);
        cpsServeMaterialEventProcessor.handleCpsUpdateCotrReceivedEvent(receivedCpsUpdateCotrProcessedEnvelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues(), hasSize(2));
        Envelope<CpsUpdateCotrSubmitted> cpsUpdateCotrSubmittedEnvelope = (Envelope<CpsUpdateCotrSubmitted>) envelopeCaptor.getAllValues().get(0);
        assertThat(cpsUpdateCotrSubmittedEnvelope.metadata().name(), is(STATUS_UPDATED_PUBLIC_EVENT));
        assertThat(cpsUpdateCotrSubmittedEnvelope.payload().getCaseId(), is(caseId));
        assertThat(cpsUpdateCotrSubmittedEnvelope.payload().getFormDefendants().get(0).getDefendantId(), is(defendantId));
        assertThat(cpsUpdateCotrSubmittedEnvelope.payload().getSubmissionId(), is(submissionId));
    }

    private static ReceivedCpsUpdateCotrProcessed updateCpsServeCotrReceived(final SubmissionStatus submissionStatus, final UUID caseId, final UUID defendantId) {
        return ReceivedCpsUpdateCotrProcessed.receivedCpsUpdateCotrProcessed().withSubmissionId(submissionId)
                .withSubmissionStatus(submissionStatus)
                .withCaseId(caseId)
                .withProsecutionCaseSubject(createCpsProsecutionCaseSubject())
                .withFormDefendants(asList(formDefendants().withDefendantId(defendantId).build()))
                .build();

    }
    @Test
    public void shouldHandleCpsServePetReceivedWithAdditionalParamsPublicEvent() {
        final JsonObject jsonPayload = jsonFromString(getPayload("process-pending-cps-serve-pet1.json"));

        final JsonObject offencePayload = jsonFromString(getPayload("referenceOffenceData.json"));
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(asList(jsonObjectToObjectConverter.convert(offencePayload, OffenceReferenceData.class)));

        final JsonObject prosecutionCaseQueryResponse = createObjectBuilder()
                .add("caseId", CASE_ID_VALUE.toString())
                .add("isCivil", true)

                .build();
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseQueryResponse);

        final JsonObject prosecutionCaseFromProgressionResponse = jsonFromString(getPayload("prosecutionCaseFromProgression.json"));
        when(progressionService.getProsecutionCase(CASE_ID_VALUE)).thenReturn(prosecutionCaseFromProgressionResponse);

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("prosecutioncasefile.command.process-received-cps-serve-pet"),
                jsonPayload);

        cpsServeMaterialEventProcessor.handleServePetReceivedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
    }
}
