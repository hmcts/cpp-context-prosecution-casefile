package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.OffenceActiveOrder.COURT_ORDER;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.CASE_NOT_FOUND;

import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.AssociatedPerson;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;
import uk.gov.moj.cpp.prosecution.casefile.validation.SubmitApplicationValidator;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.SubmitApplication;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmitApplicationHandlerTest {

    public static final String PROSECUTIONCASEFILE_COMMAND_SUBMIT_APPLICATION = "prosecutioncasefile.command.submit-application";
    public static final String PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_ACCEPTED = "prosecutioncasefile.events.submit-application-accepted";

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            SubmitApplicationAccepted.class,
            SubmitApplicationValidationFailed.class
    );

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private SubmitApplicationValidator submitApplicationValidator;

    @InjectMocks
    private SubmitApplicationHandler submitApplicationHandler;

    private ApplicationAggregate applicationAggregate;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private SubmitApplication submitApplication;

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommand() throws Exception {
        //given
        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final Metadata metadata = Envelope.metadataBuilder().withName(PROSECUTIONCASEFILE_COMMAND_SUBMIT_APPLICATION)
                .withId(randomUUID())
                .build();
        final Envelope<SubmitApplication> envelope = envelopeFrom(metadata, submitApplication);

        when(submitApplicationValidator.validate(any(), any())).thenReturn(emptyList());
        //when
        submitApplicationHandler.submitCCApplication(envelope);

        //then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_ACCEPTED),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommandWithEmptyRespondents() throws Exception {
        //given
        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());


        final Metadata metadata = Envelope.metadataBuilder().withName(PROSECUTIONCASEFILE_COMMAND_SUBMIT_APPLICATION)
                .withId(randomUUID())
                .build();
        submitApplication.getCourtApplication().getRespondents().clear();
        final Envelope<SubmitApplication> envelope = envelopeFrom(metadata, submitApplication);

        when(submitApplicationValidator.validate(any(), any())).thenReturn(emptyList());
        //when
        submitApplicationHandler.submitCCApplication(envelope);

        //then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_ACCEPTED),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommandWithNullRespondentsAsn() throws Exception {
        //given
        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final Metadata metadata = Envelope.metadataBuilder().withName(PROSECUTIONCASEFILE_COMMAND_SUBMIT_APPLICATION)
                .withId(randomUUID())
                .build();

        final Envelope<SubmitApplication> envelope = envelopeFrom(metadata,
                new JsonObjectToObjectConverter(objectMapper).convert(generateSubmitApplicationPayloadV2(), SubmitApplication.class));

        when(submitApplicationValidator.validate(any(), any())).thenReturn(emptyList());
        //when
        submitApplicationHandler.submitCCApplication(envelope);

        //then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(PROSECUTIONCASEFILE_EVENTS_SUBMIT_APPLICATION_ACCEPTED),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldRaiseFailureEventForApplicationCommand() throws Exception {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final Metadata metadata = Envelope.metadataBuilder().withName(PROSECUTIONCASEFILE_COMMAND_SUBMIT_APPLICATION)
                .withId(randomUUID())
                .build();
        final Envelope<SubmitApplication> envelope = envelopeFrom(metadata, submitApplication);

        when(submitApplicationValidator.validate(any(), any())).thenReturn(singletonList(of(CASE_NOT_FOUND)));
        //when
        submitApplicationHandler.submitCCApplication(envelope);

        //then
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("prosecutioncasefile.events.submit-application-validation-failed"),
                        payload().isJson(allOf(
                                withJsonPath("$.errorDetails.errorDetails[0].errorCode", is(CASE_NOT_FOUND.getCode())),
                                withJsonPath("$.errorDetails.errorDetails[0].errorDescription", is(CASE_NOT_FOUND.getText())))
                        )
                )));
    }

    @Test
    public void shouldReturnNullOffenceListWhenCourtApplicationTypeIsNotOffence() {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final UUID matchedDefendantId = randomUUID();
        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType()
                .withOffenceActiveOrder(COURT_ORDER)
                .build();
        final List<Respondent> enrichedRespondentList = asList(getRespondant(matchedDefendantId, true), getRespondant(randomUUID(), false));
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("INACTIVE")
                .withDefendants(asList(Defendant.defendant()
                        .withId(matchedDefendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(randomUUID())
                                .build()))

                        .build()))
                .build();
        //when
        final List<Offence> offences = submitApplicationHandler.getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase);

        //then
        assertThat(offences, nullValue());
    }

    @Test
    public void shouldReturnNullOffenceListWhenCaseStatusIsNotInactive() {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final UUID matchedDefendantId = randomUUID();
        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType()
                .withOffenceActiveOrder(OFFENCE)
                .build();
        final List<Respondent> enrichedRespondentList = asList(getRespondant(matchedDefendantId, true), getRespondant(randomUUID(), false));
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("ACTIVE")
                .withDefendants(asList(Defendant.defendant()
                        .withId(matchedDefendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();
        //when
        final List<Offence> offences = submitApplicationHandler.getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase);

        //then
        assertThat(offences, nullValue());
    }

    @Test
    public void shouldReturnNullOffenceListWhenRespondentIsNotSubject() {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final UUID matchedDefendantId = randomUUID();
        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType()
                .withOffenceActiveOrder(OFFENCE)
                .build();
        final List<Respondent> enrichedRespondentList = asList(getRespondant(matchedDefendantId, false), getRespondant(randomUUID(), false));
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("INACTIVE")
                .withDefendants(asList(Defendant.defendant()
                        .withId(matchedDefendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();
        //when
        final List<Offence> offences = submitApplicationHandler.getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase);

        //then
        assertThat(offences, nullValue());
    }

    @Test
    public void shouldReturnNullOffenceListWhenRespondentIsNotDefendant() {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final UUID matchedDefendantId = randomUUID();
        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType()
                .withOffenceActiveOrder(OFFENCE)
                .build();
        final List<Respondent> enrichedRespondentList = asList(getRespondant(randomUUID(), true), getRespondant(randomUUID(), false));
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("INACTIVE")
                .withDefendants(asList(Defendant.defendant()
                        .withId(matchedDefendantId)
                        .withOffences(asList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build()))
                .build();
        //when
        final List<Offence> offences = submitApplicationHandler.getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase);

        //then
        assertThat(offences, nullValue());
    }

    @Test
    public void shouldReturnOffenceListWhenAllConditionsSatisfied() {

        JsonObject jsonObject = generateSubmitApplicationPayload();
        submitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplication.class);
        applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new SubmitApplicationAccepted.Builder()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build());

        final UUID matchedDefendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final uk.gov.justice.core.courts.CourtApplicationType courtApplicationType = CourtApplicationType.courtApplicationType()
                .withOffenceActiveOrder(OFFENCE)
                .build();
        final List<Respondent> enrichedRespondentList = asList(getRespondant(matchedDefendantId, true), getRespondant(randomUUID(), false));
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withCaseStatus("INACTIVE")
                .withDefendants(asList(Defendant.defendant()
                                .withId(matchedDefendantId)
                                .withOffences(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .build()))
                                .build(),
                        Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(asList(Offence.offence()
                                        .withId(randomUUID())
                                        .build()))
                                .build()))
                .build();
        //when
        final List<Offence> offences = submitApplicationHandler.getOffences(courtApplicationType, enrichedRespondentList, prosecutionCase);

        //then
        assertThat(offences.size(), is(2));
        assertThat(offences.get(0).getId(), is(offenceId1));
        assertThat(offences.get(1).getId(), is(offenceId2));
    }

    private Respondent getRespondant(final UUID defendantId, final boolean isSubject) {
        return Respondent.respondent()
                .withDefendantId(defendantId)
                .withIsSubject(isSubject)
                .build();
    }


    private JsonObject generateSubmitApplicationPayload() {
        String payloadStr = resourceToString("json/submitApplication.json")
                .replace("%APPLICATION_ID%", randomUUID().toString())
                .replace("%BOX_HEARING_ID%", randomUUID().toString());
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private JsonObject generateSubmitApplicationPayloadV2() {
        String payloadStr = resourceToString("json/submitApplicationV2.json")
                .replace("%APPLICATION_ID%", randomUUID().toString())
                .replace("%BOX_HEARING_ID%", randomUUID().toString());
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    public static String resourceToString(final String path) {
        try (final InputStream systemResourceAsStream = getSystemResourceAsStream(path)) {
            assert systemResourceAsStream != null;
            return IOUtils.toString(systemResourceAsStream);
        } catch (final IOException e) {
            fail("Error consuming file from location " + path);
            throw new UncheckedIOException(e);
        }
    }
}