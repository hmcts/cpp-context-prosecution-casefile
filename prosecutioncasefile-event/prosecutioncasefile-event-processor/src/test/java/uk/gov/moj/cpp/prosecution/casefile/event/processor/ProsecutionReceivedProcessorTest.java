package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings.ccCaseReceivedWithWarnings;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.buildProsecutionWithReferenceDataWithChannel;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.readResourcesFile;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.CCCaseToProsecutionCaseConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileDefendantToDefenceDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ValidationCompleted;

import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProsecutionReceivedProcessorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionReceivedProcessorTest.class);
    private static final String PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";
    private static final String PUBLIC_PROSECUTIONCASEFILE_EVENTS_VALIDATION_COMPLETE = "public.prosecutioncasefile.events.validation-completed";
    private static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED = "public.prosecutioncasefile.cc-case-received";

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @Captor
    private ArgumentCaptor<Envelope> captor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> captorAdmin;

    @Mock
    private CCCaseToProsecutionCaseConverter ccCaseToProsecutionCaseConverter;

    @Mock
    private ProsecutionCaseFileDefendantToDefenceDefendantConverter prosecutionCaseFileDefendantToDefenceDefendantConverter;

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionReceivedProcessor prosecutionReceivedProcessor;

    private final static String PROBLEM_CODE = "code";
    private final static String PROBLEM_VALUE_KEY_1 = "value_key_1";
    private final static String PROBLEM_VALUE_KEY_2 = "value_key_2";
    private final static String PROBLEM_VALUE_1 = "value_1";
    private final static String PROBLEM_VALUE_2 = "value_2";
    private final static String PROSECUTION_CASE_REFERENCE = "TFLDEF001";

    @Test
    public void shouldHandleValidationCompletedEvent() {
        final ValidationCompleted validationCompleted = ValidationCompleted.validationCompleted()
                .withCaseId(randomUUID())
                .build();
        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.events.validation-completed")
                .withId(randomUUID())
                .build();

        final Envelope<ValidationCompleted> envelope = envelopeFrom(metadata, validationCompleted);

        prosecutionReceivedProcessor.handleValidationCompleted(envelope);

        verify(sender).send(captor.capture());

        final Envelope outputEnvelope = captor.getValue();

        assertThat(outputEnvelope.metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_EVENTS_VALIDATION_COMPLETE));
    }

    @Test
    public void shouldHandleCCCaseReceived() {

        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(buildProsecutionWithReferenceData("Either Way")).build();

        final List<Envelope> envelopeList = setUpTestEnvelopeListForCCCase(ccCaseReceived);
        assertThat(envelopeList.get(0).metadata().name(), is(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH));
        assertThat(envelopeList.get(1).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED));
    }

    @Test
    public void shouldHandleCCCaseReceivedForMCC() {

        final CcCaseReceived ccCaseReceived = ccCaseReceived().withProsecutionWithReferenceData(buildProsecutionWithReferenceDataWithChannel(MCC)).build();

        final List<Envelope> envelopeList = setUpTestEnvelopeListForCCCase(ccCaseReceived);

        assertThat(envelopeList.get(0).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED));
        assertThat(envelopeList.get(1).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED));
    }

    private List<Envelope> setUpTestEnvelopeListForCCCase(final CcCaseReceived ccCaseReceived) {
        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.events.cc-case-received")
                .withId(randomUUID())
                .build();

        final String initiateCourtProceedingsJson = readResourcesFile("initiate-court-proceedings.json");

        final JsonReader reader = Json.createReader(new StringReader(initiateCourtProceedingsJson));
        final JsonObject jsonObject = reader.readObject();
        reader.close();
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);

        final JsonEnvelope envelope1 = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(envelope1);

        when(ccCaseToProsecutionCaseConverter.convert(any())).thenReturn(buildInitiateCourtProceedings());
        when(prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(any()))
                .thenReturn(asList(DefenceDefendant.defenceDefendant().build()));

        prosecutionReceivedProcessor.handleCcCaseReceived(envelopeFrom(metadata, ccCaseReceived));

        verify(sender).sendAsAdmin(captorAdmin.capture());
        verify(sender, times(2)).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captorAdmin.getValue();
        assertThat(jsonEnvelope, is(envelope1));

        return captor.getAllValues();
    }

    private List<Problem> getProblems() {
        return singletonList(Problem.problem()
                .withCode(PROBLEM_CODE)
                .withValues(getProblemValues())
                .build());
    }

    private List<ProblemValue> getProblemValues() {
        return asList(
                problemValue().withKey(PROBLEM_VALUE_KEY_1).withValue(PROBLEM_VALUE_1).build(),
                problemValue().withKey(PROBLEM_VALUE_KEY_2).withValue(PROBLEM_VALUE_2).build());
    }

    @Test
    public void shouldHandleCCCaseReceivedWithWarnings() {

        final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = ccCaseReceivedWithWarnings().withProsecutionWithReferenceData(buildProsecutionWithReferenceData("Either Way")).build();
        final JsonEnvelope envelope1 = getJsonEnvelope(PROSECUTION_CASE_REFERENCE, ccCaseReceivedWithWarnings);

        verify(sender).sendAsAdmin(captorAdmin.capture());
        verify(sender, times(2)).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captorAdmin.getValue();
        assertThat(jsonEnvelope, is(envelope1));

        final List<Envelope> envelopeList = captor.getAllValues();
        assertThat(envelopeList.get(0).metadata().name(), is(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH));
        assertThat(envelopeList.get(1).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED));
    }

    @Test
    public void shouldHandleCCCaseReceivedWithWarningsForMCC() {

        final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = ccCaseReceivedWithWarnings().withProsecutionWithReferenceData(buildProsecutionWithReferenceDataWithChannel(MCC)).build();
        final JsonEnvelope envelope1 = getJsonEnvelope(PROSECUTION_CASE_REFERENCE, ccCaseReceivedWithWarnings);

        verify(sender).sendAsAdmin(captorAdmin.capture());
        verify(sender, times(2)).send(captor.capture());

        final JsonEnvelope jsonEnvelope = captorAdmin.getValue();
        assertThat(jsonEnvelope, is(envelope1));

        final List<Envelope> envelopeList = captor.getAllValues();

        assertThat(envelopeList.get(0).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED));
        assertThat(envelopeList.get(1).metadata().name(), is(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED));
    }

    private JsonEnvelope getJsonEnvelope(final String prosecutorDefendantReference, final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings) {
        final CcCaseReceivedWithWarnings newCcCaseReceivedWithWarnings = ccCaseReceivedWithWarnings()
                .withProsecutionWithReferenceData(ccCaseReceivedWithWarnings.getProsecutionWithReferenceData())
                .withDefendantWarnings(of(new DefendantProblem(getProblems(), prosecutorDefendantReference)))
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("prosecutioncasefile.events.cc-case-received-with-warnings")
                .withId(randomUUID())
                .build();

        final String initiateCourtProceedingsJson = readResourcesFile("initiate-court-proceedings.json");

        final JsonReader reader = Json.createReader(new StringReader(initiateCourtProceedingsJson));
        final JsonObject jsonObject = reader.readObject();
        reader.close();
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);

        final JsonEnvelope envelope1 = JsonEnvelope.envelopeFrom(metadataWithRandomUUIDAndName(), NULL);
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(envelope1);

        when(ccCaseToProsecutionCaseConverter.convert(any())).thenReturn(buildInitiateCourtProceedings());
        when(prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(any()))
                .thenReturn(asList(DefenceDefendant.defenceDefendant().build()));

        prosecutionReceivedProcessor.handleCcCaseReceivedWithWarnings(envelopeFrom(metadata, newCcCaseReceivedWithWarnings));
        return envelope1;
    }

    private InitiateCourtProceedings buildInitiateCourtProceedings() {
        final CourtReferral courtReferral = CourtReferral.courtReferral()
                .withListHearingRequests(asList(ListHearingRequest.listHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre()
                                .withId(randomUUID())
                                .withName("Port Talbot")
                                .withWelshName("Welsh Name")
                                .build())
                        .withEstimateMinutes(20)
                        .withHearingType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("Plea & Trial Preparation")
                                .build())
                        .withJurisdictionType(JurisdictionType.MAGISTRATES)
                        .withListedStartDateTime(ZonedDateTime.now())
                        .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                                .withDefendantId(randomUUID())
                                .withProsecutionCaseId(randomUUID())
                                .withHearingLanguageNeeds(HearingLanguage.ENGLISH)
                                .withDefendantOffences(asList(randomUUID()))
                                .build()))
                        .withListedStartDateTime(ZonedDateTime.now().minusDays(1))
                        .withListedEndDateTime(ZonedDateTime.now().plusMinutes(20))
                        .withCourtScheduleId("test-courtscheduleId")
                        .build()))
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withInitiationCode(InitiationCode.C)
                        .withOriginatingOrganisation("Bo1NM00")
                        .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                                .withId(randomUUID())
                                .withMasterDefendantId(randomUUID())
                                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                                .withProsecutionCaseId(randomUUID())
                                .build()))
                        .build()))
                .build();

        final InitiateCourtProceedings initiateCourtProceedings = InitiateCourtProceedings.initiateCourtProceedings()
                .withInitiateCourtProceedings(courtReferral)
                .build();

        return initiateCourtProceedings;
    }

}
