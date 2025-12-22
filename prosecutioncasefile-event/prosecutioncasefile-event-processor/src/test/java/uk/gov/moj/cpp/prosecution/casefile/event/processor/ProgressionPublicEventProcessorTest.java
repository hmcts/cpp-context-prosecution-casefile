package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplicationCreated.courtApplicationCreated;
import static uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsApproved.publicProgressionCourtApplicationSummonsApproved;
import static uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected.publicProgressionCourtApplicationSummonsRejected;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.MetadataHelper.metadataWithIdpcProcessId;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptGroupCases;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CaseDefendantChangedCommand;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectGroupCases;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class ProgressionPublicEventProcessorTest {

    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final String PROSECUTOR_COST = "£300";
    private static final Boolean PERSONAL_SERVICE_FLAG = BOOLEAN.next();
    private static final Boolean SUMMONS_SUPPRESSED_FLAG = BOOLEAN.next();
    private static final String REJECTION_REASON_1 = "Rejected for reason 1";
    private static final String REJECTION_REASON_2 = "Rejected for reason 2";
    private static final String PROSECUTOR_EMAIL_ADDRESS = RandomGenerator.EMAIL_ADDRESS.next();

    @Mock
    private Sender sender;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @InjectMocks
    private ProgressionPublicEventProcessor progressionPublicEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<AcceptCase>> acceptCaseEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope> caseDefendantChangedCommandCaptor;

    @Captor
    private ArgumentCaptor<Envelope<AcceptGroupCases>> acceptGroupCasesEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<RejectGroupCases>> rejectGroupCasesEnvelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> jsonObjectEnvelopeCaptor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Test
    void shouldHandleCaseCreated() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.prosecution-case-created").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("prosecutionCase", createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("originatingOrganisation", "G01FT01AB")
                                .add("initiationCode", "J")
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", defendantId.toString())))
                                .build())
                        .build());

        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        verify(sender).send(acceptCaseEnvelopeCaptor.capture());

        final Envelope<AcceptCase> envelopeToSender = acceptCaseEnvelopeCaptor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.accept-case"));
        assertThat(metadata.clientCorrelationId(), is(envelope.metadata().clientCorrelationId()));

        assertThat(envelopeToSender.payload(), is(notNullValue()));
        final AcceptCase payload = envelopeToSender.payload();

        assertThat(payload.getCaseId(), is(caseId));
        assertThat(payload.getDefendantIds(), hasItem(defendantId));
    }

    @Test
    void shouldHandleCaseCreatedPublicEventIfPayloadHasMigrationSystem() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.prosecution-case-created").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("prosecutionCase", createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("originatingOrganisation", "G01FT01AB")
                                .add("initiationCode", "J")
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("id", defendantId.toString())))
                                .add("migrationSourceSystem", createObjectBuilder()
                                        .add("migrationSourceSystemName", "LIBRA")
                                        .add("migrationSourceSystemCaseIdentifier", "LIB-100002"))
                                .build())
                        .build());

        progressionPublicEventProcessor.handleProsecutionCaseCreated(envelope);

        verify(sender, times(0)).send(acceptCaseEnvelopeCaptor.capture());
    }

    @Test
    void shouldHandleProgressionPublicEvent() {
        assertThat(ProgressionPublicEventProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleProsecutionCaseCreated").thatHandles("public.progression.prosecution-case-created"))
        );
    }

    @Test
    void shouldHandleReferProsecutionCaseToCourtAccepted() {
        final UUID caseId = randomUUID();
        final UUID referralReasonId = randomUUID();

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.refer-prosecution-cases-to-court-accepted").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .add("referralReasonId", referralReasonId.toString())
                        .build());

        progressionPublicEventProcessor.handleReferProsecutionCaseToCourtAccepted(envelope);

        verify(sender).send(acceptCaseEnvelopeCaptor.capture());

        final Envelope envelopeToSender = acceptCaseEnvelopeCaptor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.record-refer-prosecution-cases-to-court-accepted"));
        assertThat(metadata.clientCorrelationId(), is(envelope.metadata().clientCorrelationId()));

        assertThat(envelopeToSender.payload(), is(notNullValue()));
        final String payload = envelopeToSender.payload().toString();
        assertThat(payload.contains(caseId.toString()), equalTo(true));
        assertThat(payload.contains(referralReasonId.toString()), equalTo(true));
    }

    @Test
    void shouldHandleCourtApplicationSummonsApproved() {
        final Envelope<PublicProgressionCourtApplicationSummonsApproved> envelope = envelopeFrom(
                metadataBuilder().withName("public.progression.court-application-summons-approved").withId(randomUUID()),
                publicProgressionCourtApplicationSummonsApproved()
                        .withId(APPLICATION_ID)
                        .withProsecutionCaseId(CASE_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorCost(PROSECUTOR_COST)
                                .withPersonalService(PERSONAL_SERVICE_FLAG)
                                .withSummonsSuppressed(SUMMONS_SUPPRESSED_FLAG)
                                .withProsecutorEmailAddress(PROSECUTOR_EMAIL_ADDRESS)
                                .build())
                        .build());

        progressionPublicEventProcessor.handleCourtApplicationSummonsApproved(envelope);

        verify(sender).send(jsonObjectEnvelopeCaptor.capture());
        final Envelope<JsonObject> resultEnvelope = jsonObjectEnvelopeCaptor.getValue();
        assertThat(resultEnvelope.metadata().name(), is("prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved"));
        assertThat(resultEnvelope.payload().toString(), isJson(allOf(
                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                withJsonPath("$.applicationId", equalTo(APPLICATION_ID.toString())),
                withJsonPath("$.summonsApprovedOutcome.prosecutorCost", equalTo(PROSECUTOR_COST)),
                withJsonPath("$.summonsApprovedOutcome.personalService", equalTo(PERSONAL_SERVICE_FLAG)),
                withJsonPath("$.summonsApprovedOutcome.prosecutorEmailAddress", equalTo(PROSECUTOR_EMAIL_ADDRESS))
        )));
    }

    @Test
    void shouldHandleCourtApplicationSummonsRejected() {
        final Metadata metadata = metadataBuilder()
                .withName("public.progression.court-application-summons-rejected")
                .withId(randomUUID())
                .build();
        final Envelope<PublicProgressionCourtApplicationSummonsRejected> summonsApplicationRejectedEnvelope = getSummonsApplicationRejectedEnvelope(metadata);

        progressionPublicEventProcessor.handleCourtApplicationSummonsRejected(summonsApplicationRejectedEnvelope);
        verify(sender).send(jsonObjectEnvelopeCaptor.capture());
        final Envelope<JsonObject> resultEnvelope = jsonObjectEnvelopeCaptor.getValue();
        assertThat(resultEnvelope.metadata().name(), is("prosecutioncasefile.command.reject-case-defendants-as-summons-application-rejected"));
        assertThat(resultEnvelope.payload().toString(), isJson(allOf(
                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                withJsonPath("$.applicationId", equalTo(APPLICATION_ID.toString())),
                withJsonPath("$.summonsRejectedOutcome.reasons", hasSize(2)),
                withJsonPath("$.summonsRejectedOutcome.reasons", contains(REJECTION_REASON_1, REJECTION_REASON_2)),
                withJsonPath("$.summonsRejectedOutcome.prosecutorEmailAddress", is(PROSECUTOR_EMAIL_ADDRESS))
        )));
    }

    @Test
    void shouldHandleCaseDefendantChanged() {
        final UUID defendantId = randomUUID();

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.case-defendant-changed").build(), randomUUID().toString()),
                createDefendantChangedPayload(defendantId));

        when(jsonObjectToObjectConverter.convert(createJsonAddress(), Address.class)).thenReturn(createAddress());
        when(jsonObjectToObjectConverter.convert(createJsonContactDetails(), ContactDetails.class)).thenReturn(createContactDetails());

        progressionPublicEventProcessor.handleCaseDefendantChanged(envelope);

        verify(sender, times(2)).send(caseDefendantChangedCommandCaptor.capture());
        final Envelope<CaseDefendantChangedCommand> envelopeToSender = caseDefendantChangedCommandCaptor.getAllValues().get(0);

        Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.case-defendant-changed"));
        assertThat(metadata.clientCorrelationId(), is(envelope.metadata().clientCorrelationId()));

        assertThat(envelopeToSender.payload(), is(notNullValue()));
        CaseDefendantChangedCommand payload = envelopeToSender.payload();

        assertThat(payload.getDefendantId(), is(defendantId));
        assertThat(payload.getDateOfBirth(), is(parse("1998-10-28")));
        PersonalInformation personalInformation = payload.getPersonDetails();
        assertThat(personalInformation.getFirstName(), is("Mark"));
        assertThat(personalInformation.getAddress().getAddress2(), is("Eddington"));
        assertThat(personalInformation.getContactDetails().getPrimaryEmail(), is("Mark@yahoo.com"));

        final Envelope<JsonObject> updateEvent = caseDefendantChangedCommandCaptor.getAllValues().get(1);
        metadata = updateEvent.metadata();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.update-case-with-defendant"));
        assertThat(metadata.clientCorrelationId(), is(envelope.metadata().clientCorrelationId()));

        assertThat(updateEvent.payload(), is(notNullValue()));

        assertThat(updateEvent.payload().getString("defendantId"), is(defendantId.toString()));
        assertThat(payload.getDateOfBirth(), is(parse("1998-10-28")));
        personalInformation = payload.getPersonDetails();
        assertThat(personalInformation.getFirstName(), is("Mark"));
        assertThat(personalInformation.getAddress().getAddress2(), is("Eddington"));
        assertThat(personalInformation.getContactDetails().getPrimaryEmail(), is("Mark@yahoo.com"));
    }


    @Test
    void shouldHandleCourtApplicationCreated() {

        final UUID applicationId = randomUUID();

        final Envelope<CourtApplicationCreated> envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.court-application-created").build(), randomUUID().toString()),
                courtApplicationCreated()
                        .withCourtApplication(CourtApplication.courtApplication().withId(applicationId).build())
                        .build());

        progressionPublicEventProcessor.handleCourtApplicationCreated(envelope);

        verify(sender).send(jsonObjectEnvelopeCaptor.capture());
        final Envelope<JsonObject> resultEnvelope = jsonObjectEnvelopeCaptor.getValue();

        Metadata metadata = resultEnvelope.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.update-application-status"));
        assertThat(resultEnvelope.payload().toString(), isJson(allOf(
                withJsonPath("$.courtApplication.id", equalTo(applicationId.toString()))
        )));
    }

    private Envelope<PublicProgressionCourtApplicationSummonsRejected> getSummonsApplicationRejectedEnvelope(final Metadata metadata) {
        final PublicProgressionCourtApplicationSummonsRejected summonsRejected = publicProgressionCourtApplicationSummonsRejected()
                .withId(APPLICATION_ID)
                .withProsecutionCaseId(CASE_ID)
                .withSummonsRejectedOutcome(SummonsRejectedOutcome.summonsRejectedOutcome()
                        .withReasons(ImmutableList.of(REJECTION_REASON_1, REJECTION_REASON_2))
                        .withProsecutorEmailAddress(PROSECUTOR_EMAIL_ADDRESS)
                        .build())
                .build();
        return Envelope.envelopeFrom(metadata, summonsRejected);
    }

    private JsonObject createDefendantChangedPayload(final UUID defendantId) {
        return createObjectBuilder()
                .add("defendant", createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("prosecutionCaseId", randomUUID().toString())
                        .add("personDefendant", createObjectBuilder()
                                .add("personDetails", createObjectBuilder()
                                        .add("title", "Mr")
                                        .add("firstName", "Mark")
                                        .add("lastName", "Taylor")
                                        .add("address", createJsonAddress())
                                        .add("contact", createJsonContactDetails())
                                        .add("dateOfBirth", "1998-10-28")
                                        .build())
                                .build())

                        .build())
                .build();
    }

    private ContactDetails createContactDetails() {
        return new ContactDetails.Builder()
                .withWork("775533")
                .withHome("7823546")
                .withMobile("767676")
                .withPrimaryEmail("Mark@yahoo.com")
                .withSecondaryEmail("Mark1@gmail.com")
                .build();
    }

    private Address createAddress() {
        return new Address.Builder()
                .withAddress1("152")
                .withAddress2("Eddington")
                .withAddress3("Peterborough")
                .withAddress4("Cambridgeshire")
                .withAddress5("UK")
                .withPostcode("PC52MN")
                .build();
    }

    private JsonObject createJsonContactDetails() {
        return createObjectBuilder()
                .add("home", "7823546")
                .add("work", "775533")
                .add("mobile", "767676")
                .add("primaryEmail", "Mark@yahoo.com")
                .add("secondaryEmail", "Mark1@gmail.com")
                .build();
    }

    private JsonObject createJsonAddress() {
        return createObjectBuilder()
                .add("address1", "152")
                .add("address2", "Eddington")
                .add("address3", "Peterborough")
                .add("address4", "Cambridgeshire")
                .add("address5", "UK")
                .add("postcode", "PC52MN")
                .build();
    }

    @Test
    void shouldHandleGroupCaseCreated() {
        final UUID groupId = randomUUID();
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.group-prosecution-cases-created").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("groupId", groupId.toString())
                        .build());

        progressionPublicEventProcessor.handleGroupProsecutionCasesCreated(envelope);

        verify(sender).send(acceptGroupCasesEnvelopeCaptor.capture());

        final Envelope<AcceptGroupCases> envelopeToSender = acceptGroupCasesEnvelopeCaptor.getValue();
        final Metadata metadata = envelopeToSender.metadata();

        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.name(), is("prosecutioncasefile.command.handler.accept-group-cases"));

        assertThat(envelopeToSender.payload(), is(notNullValue()));
        final AcceptGroupCases acceptGroupCases = envelopeToSender.payload();

        assertThat(acceptGroupCases.getGroupId(), is(groupId));
    }

    @Test
    void shouldHandleCivilCaseExists() {
        final UUID groupId = randomUUID();
        final UUID caseId = randomUUID();
        final String caseUrn = "TFL123456";

        final JsonEnvelope groupEnvelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.events.civil-case-exists").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("groupId", groupId.toString())
                        .add("caseUrn", caseUrn)
                        .build());

        progressionPublicEventProcessor.handleCivilCaseExists(groupEnvelope);

        final JsonEnvelope caseEnvelope = envelopeFrom(
                metadataWithIdpcProcessId(metadataWithRandomUUID("public.progression.events.civil-case-exists").build(), randomUUID().toString()),
                createObjectBuilder()
                        .add("prosecutionCaseId", caseId.toString())
                        .add("caseUrn", caseUrn)
                        .build());

        progressionPublicEventProcessor.handleCivilCaseExists(caseEnvelope);

        verify(sender, times(2)).send(rejectGroupCasesEnvelopeCaptor.capture());

        final Envelope<RejectGroupCases> rejectGroupCasesEnvelope1 = rejectGroupCasesEnvelopeCaptor.getAllValues().get(0);
        assertThat(rejectGroupCasesEnvelope1.metadata(), is(notNullValue()));
        assertThat(rejectGroupCasesEnvelope1.metadata().name(), is("prosecutioncasefile.command.handler.reject-group-cases"));
        assertThat(rejectGroupCasesEnvelope1.payload(), is(notNullValue()));
        assertThat(rejectGroupCasesEnvelope1.payload().getGroupId(), is(groupId));
        assertThat(rejectGroupCasesEnvelope1.payload().getCaseId(), is(nullValue()));
        assertThat(rejectGroupCasesEnvelope1.payload().getCaseUrn(), is(caseUrn));

        final Envelope<RejectGroupCases> rejectGroupCasesEnvelope2 = rejectGroupCasesEnvelopeCaptor.getAllValues().get(1);
        assertThat(rejectGroupCasesEnvelope2.metadata(), is(notNullValue()));
        assertThat(rejectGroupCasesEnvelope2.metadata().name(), is("prosecutioncasefile.command.handler.reject-group-cases"));
        assertThat(rejectGroupCasesEnvelope2.payload(), is(notNullValue()));
        assertThat(rejectGroupCasesEnvelope2.payload().getGroupId(), is(nullValue()));
        assertThat(rejectGroupCasesEnvelope2.payload().getCaseId(), is(caseId));
        assertThat(rejectGroupCasesEnvelope2.payload().getCaseUrn(), is(caseUrn));
    }
}


