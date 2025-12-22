package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.ImmutableList.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData.custodyStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.InitiationTypesRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialReceived;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class AddIdpcMaterialHandlerTest {

    private static final UUID CASE_ID_VALUE = fromString("8c74f505-d062-49bd-b2be-1f64e8da3233");
    private static final UUID OFFENCE_ID = fromString("5f66994c-c8f2-458d-9828-d2923308a0ad");
    private static final String OFFENCE_CODE = "FOO";
    private static final LocalDate OFFENCE_COMMITTED_DATE = now();
    private static final LocalDate ARREST_DATE = now().minusMonths(3);
    private static final LocalDate OFFENCE_CHARGE_DATE = now().minusMonths(4);
    private static final String COURT_HEARING_LOCATION = "B01677";
    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String CUSTODY_STATUS = "U";
    public static final String ORIGINATING_ORGANISATION = "GAEAA01";
    public static final String INITIATION_CODE = "C";


    @Spy
    private final Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    IdpcMaterialReceived.class,
                    IdpcDefendantMatched.class,
                    IdpcDefendantMatchPending.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock
    Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @InjectMocks
    private AddMaterialHandler addMaterialHandler;

    @InjectMocks
    private InitiationTypesRefDataEnricher initiationTypesRefDataEnricher;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> streamArgumentCaptor;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    @Test
    public void shouldHandleAddIdpcMaterialCommand() {
        assertThat(addMaterialHandler, isHandler(COMMAND_HANDLER)
                .with(method("addIdpcMaterial")
                        .thatHandles("prosecutioncasefile.handler.add-idpc-material")
                ));
    }

    @Test
    public void shouldHandleAddIdpcMaterialWhenCaseExists() throws Exception {
        final JsonObject jsonObjectPayload = readJson("json/addIdpcMaterial.json", JsonObject.class);
        final Metadata metadata = DefaultJsonMetadata.metadataBuilder().withId(CASE_ID_VALUE).withName("name").build();
        final JsonEnvelope envelope = envelopeFrom(metadata, jsonObjectPayload);

        when(eventSource.getStreamById(CASE_ID_VALUE)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        when(referenceDataQueryService.getInitiationCodes()).thenReturn(singletonList(INITIATION_CODE));
        when(referenceDataQueryService.retrieveOffenceData(any(), eq(INITIATION_CODE))).thenReturn(singletonList(OffenceReferenceData.offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).build()));


        aggregate.receiveCCCase(
                getProsecutionWithReferenceData(of(buildDefendant(null, "smith", null))),
                singletonList(initiationTypesRefDataEnricher),
                new ArrayList<DefendantRefDataEnricher>(),
                referenceDataQueryService);

        addMaterialHandler.addIdpcMaterial(envelope);
        verify();
        assertThat(streamArgumentCaptor.getAllValues(), hasSize(2));
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());

        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("prosecutioncasefile.events.idpc-material-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID_VALUE.toString()))))
                ),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("prosecutioncasefile.events.idpc-defendant-matched"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID_VALUE.toString()))
                        ))
                )
        ));
    }

    @Test
    public void shouldHandleAddIdpcMaterialWhenCaseAlreadyExist() throws Exception {
        final JsonObject jsonObjectPayload = readJson("json/addIdpcMaterial.json", JsonObject.class);
        final Metadata metadata = DefaultJsonMetadata.metadataBuilder().withId(CASE_ID_VALUE).withName("name").build();
        final JsonEnvelope envelope = envelopeFrom(metadata, jsonObjectPayload);
        when(eventSource.getStreamById(CASE_ID_VALUE)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        addMaterialHandler.addIdpcMaterial(envelope);
        verify();
        assertThat(streamArgumentCaptor.getAllValues(), hasSize(2));
        final List<JsonEnvelope> allValues = convertStreamToEventList(streamArgumentCaptor.getAllValues());


        assertThat(allValues, containsInAnyOrder(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("prosecutioncasefile.events.idpc-material-received"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID_VALUE.toString()))))
                ),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("prosecutioncasefile.events.idpc-defendant-match-pending"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID_VALUE.toString()))
                        ))
                )
        ));
    }
    private void verify() throws EventStreamException {
        Mockito.verify(aggregateService, times(2)).get(eventStream, ProsecutionCaseFile.class);
        Mockito.verify(eventStream, times(2)).append(streamArgumentCaptor.capture());
    }

    private List<JsonEnvelope> convertStreamToEventList(final List<Stream<JsonEnvelope>> listOfStreams) {
        return listOfStreams.stream()
                .flatMap(jsonEnvelopeStream -> jsonEnvelopeStream).collect(toList());
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        return new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withInitiationCode(INITIATION_CODE)
                        .withCaseId(CASE_ID_VALUE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .build())
                .withDefendants(defendantList)
                .withChannel(Channel.SPI)
                .build());
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendant(final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return defendant()
                .withId(randomUUID().toString())
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
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
                .withInitiationCode("C")
                .build();
    }

    private List<OrganisationUnitReferenceData> buildOrganisationUnits() {
        return singletonList(organisationUnitReferenceData()
                .withOucode(COURT_HEARING_LOCATION)
                .build());
    }

    private List<CustodyStatusReferenceData> buildCustodyStatuses() {
        return singletonList(custodyStatusReferenceData()
                .withStatusCode(CUSTODY_STATUS)
                .build());
    }
}
