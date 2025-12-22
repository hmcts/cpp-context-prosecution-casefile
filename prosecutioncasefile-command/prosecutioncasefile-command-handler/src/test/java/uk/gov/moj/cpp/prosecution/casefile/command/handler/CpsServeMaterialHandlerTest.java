package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult.formValidationResult;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServePtph;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.CpsServeMaterialAggregate;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecution.casefile.service.DefenceService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CpsFormValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CpsRejectBcmForTimerExpire;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CpsRejectPetForTimerExpire;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeBcm;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServeCotr;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsServePet;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.staging.ProcessReceivedCpsUpdateCotr;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class CpsServeMaterialHandlerTest {

    private static final String CPS_SERVE_PET_COMMAND = "prosecutioncasefile.command.process-received-cps-serve-pet";
    private static final String CPS_SERVE_BCM_COMMAND = "prosecutioncasefile.command.process-received-cps-serve-bcm";
    private static final String CPS_SERVE_COTR_COMMAND = "prosecutioncasefile.command.process-received-cps-serve-cotr";
    private static final String CPS_UPDATE_COTR_COMMAND = "prosecutioncasefile.command.process-received-cps-update-cotr";
    private static final String CPS_SERVE_PTPH_COMMAND = "prosecutioncasefile.command.process-received-cps-serve-ptph";
    private static final String CPS_SERVE_PTPH_EVENT = "prosecutioncasefile.events.received-cps-serve-ptph-processed";

    private static final String CPS_SERVE_PET_EVENT = "prosecutioncasefile.events.received-cps-serve-pet-processed";
    private static final String CPS_SERVE_BCM_EVENT = "prosecutioncasefile.events.received-cps-serve-bcm-processed";
    private static final String CPS_SERVE_COTR_EVENT = "prosecutioncasefile.events.received-cps-serve-cotr-processed";
    private static final String CPS_UPDATE_COTR_EVENT = "prosecutioncasefile.events.received-cps-update-cotr-processed";


    @Spy
    private final Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed.class,
                    uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed.class,
                    uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeCotrProcessed.class,
                    uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsUpdateCotrProcessed.class);
    @InjectMocks
    private CpsServeMaterialHandler cpsServeMaterialHandler;
    @Mock
    private EventStream eventStream;
    @Mock
    private EventSource eventSource;
    @Mock
    private AggregateService aggregateService;
    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;
    @Mock
    private CpsServeMaterialAggregate cpsServeMaterialAggregate;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private CpsFormValidator cpsFormValidator;

    @Mock
    private DefenceService defenceService;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        cpsServeMaterialAggregate = new CpsServeMaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CpsServeMaterialAggregate.class)).thenReturn(cpsServeMaterialAggregate);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(new ProsecutionCaseFile());
    }

    @Test
    public void shouldHandleCpsServePetReceived() {
        assertThat(cpsServeMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("cpsServePetReceived")
                        .thatHandles(CPS_SERVE_PET_COMMAND)
                ));
    }

    @Test
    public void shouldHandleBcmServeReceived() {
        assertThat(cpsServeMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("cpsServeBcmReceived")
                        .thatHandles(CPS_SERVE_BCM_COMMAND)
                ));
    }

    @Test
    public void shouldHandleCpsUpdateCotrReceived() {
        assertThat(cpsServeMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("cpsUpdateCotrReceived")
                        .thatHandles(CPS_UPDATE_COTR_COMMAND)
                ));
    }

    @Test
    public void shouldHandleCpsServePetReceivedWhenCaseNotCreated() throws Exception {

        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileJson);

        final ProcessReceivedCpsServePet cpsServePetReceived = readJson("json/process-received-cps-serve-pet.json", ProcessReceivedCpsServePet.class);

        final Envelope<ProcessReceivedCpsServePet> envelope =
                envelopeFrom(metadataFor(CPS_SERVE_PET_COMMAND), cpsServePetReceived);

        cpsServeMaterialHandler.cpsServePetReceived(envelope);

    }

    @Test
    public void shouldHandleCpsServeBcmReceivedWhenCaseIsPresent() throws Exception {

        String defendantId = randomUUID().toString();
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject formDefendants = readJson("json/bcmFormDefendants.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(formDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileJson);

        final ProcessReceivedCpsServeBcm processReceivedCpsServeBcm = readJson("json/process-received-cps-serve-bcm.json", ProcessReceivedCpsServeBcm.class);

        final Envelope<ProcessReceivedCpsServeBcm> envelope =
                envelopeFrom(metadataFor(CPS_SERVE_PET_COMMAND), processReceivedCpsServeBcm);

        cpsServeMaterialHandler.cpsServeBcmReceived(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                CPS_SERVE_BCM_EVENT,
                () -> readJson("json/receivedCpsServeBcmProcessed.json", JsonValue.class));
    }

    @Test
    public void shouldHandleCpsServePtphReceivedWhenCaseIsPresent() throws Exception {

        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject formDefendants = readJson("json/bcmFormDefendants.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(formDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileJson);

        final ProcessReceivedCpsServePtph processReceivedCpsServePtph = readJson("json/process-received-cps-serve-ptph.json", ProcessReceivedCpsServePtph.class);

        final Envelope<ProcessReceivedCpsServePtph> envelope =
                envelopeFrom(metadataFor(CPS_SERVE_PTPH_COMMAND), processReceivedCpsServePtph);

        cpsServeMaterialHandler.cpsServePtphReceived(envelope);
        //write assert
    }

    @Test
    public void shouldHandleCpsServeCotrReceivedWhenCaseIsPresent() throws Exception {

        final JsonObject formDefendants = readJson("json/bcmFormDefendants.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateCotr(any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(formDefendants)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileJson);

        final ProcessReceivedCpsServeCotr processReceivedCpsServeCotr = readJson("json/process-received-cps-serve-cotr.json", ProcessReceivedCpsServeCotr.class);

        final Envelope<ProcessReceivedCpsServeCotr> envelope =
                envelopeFrom(metadataFor(CPS_SERVE_COTR_COMMAND), processReceivedCpsServeCotr);

        cpsServeMaterialHandler.cpsServeCotrReceived(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                CPS_SERVE_COTR_EVENT,
                () -> readJson("json/receivedCpsServeCotrProcessed.json", JsonValue.class));
    }

    @Test
    public void shouldHandleCpsUpdateCotrReceivedWhenCaseIsPresent() throws Exception {

        final JsonObject formDefendants = readJson("json/bcmFormDefendants.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateCotr(any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(formDefendants)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        when(prosecutionCaseQueryService.getProsecutionCaseByCaseUrn(any(), any())).thenReturn(prosecutionCaseFileJson);

        final ProcessReceivedCpsUpdateCotr processReceivedCpsUpdateCotr = readJson("json/process-received-cps-update-cotr.json", ProcessReceivedCpsUpdateCotr.class);

        final Envelope<ProcessReceivedCpsUpdateCotr> envelope =
                envelopeFrom(metadataFor(CPS_UPDATE_COTR_COMMAND), processReceivedCpsUpdateCotr);

        cpsServeMaterialHandler.cpsUpdateCotrReceived(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                CPS_UPDATE_COTR_EVENT,
                () -> readJson("json/receivedCpsUpdateCotrProcessed.json", JsonValue.class));
    }

    @Test
    public void shouldCpsServePetTimerExpired() throws EventStreamException {
        final CpsRejectPetForTimerExpire cpsRejectPetForTimerExpire = readJson("json/cps-reject-pet-for-timer-expire.json", CpsRejectPetForTimerExpire.class);

        final Envelope<CpsRejectPetForTimerExpire> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.cps-reject-pet-for-timer-expire"), cpsRejectPetForTimerExpire);
        cpsServeMaterialHandler.cpsServePetTimerExpired(envelope);
    }

    @Test
    public void shouldCpsServeBcmTimerExpired() throws EventStreamException {
        final CpsRejectBcmForTimerExpire cpsRejectBcmForTimerExpire = readJson("json/cps-reject-bcm-for-timer-expire.json", CpsRejectBcmForTimerExpire.class);

        final Envelope<CpsRejectBcmForTimerExpire> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.cps-reject-bcm-for-timer-expire"), cpsRejectBcmForTimerExpire);
        cpsServeMaterialHandler.cpsServeBcmTimerExpired(envelope);
        //write assert
    }
}
