package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Benefits;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Employer;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.FinancialMeans;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Frequency;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Income;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PersonalDetails;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaPcqVisitedSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaSubmitted;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineHandlerTest {

    protected static final UUID CASE_ID = randomUUID();

    @InjectMocks
    private PleadOnlineHandler pleadOnlineHandler;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseFile prosecutionCaseFile;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(OnlinePleaSubmitted.class);

    @Spy
    private Enveloper enveloper1 = createEnveloperWithEvents(OnlinePleaPcqVisitedSubmitted.class);

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(prosecutionCaseFile);
    }

    @Test
    public void shouldPleadOnline() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();

        final PleadOnline pleadOnline = buildPleadOnline(defendantId, buildAddress(), false);


        final Envelope<PleadOnline> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.plead-online"), pleadOnline);

        final OnlinePleaSubmitted onlinePleaSubmitted = OnlinePleaSubmitted.onlinePleaSubmitted()
                .withReceivedDateTime(clock.now())
                .withPleadOnline(pleadOnline)
                .withCreatedBy(fromString(envelope.metadata().userId().get()))
                .withCaseId(CASE_ID)
                .build();

        when(prosecutionCaseFile.pleadOnline(any(), any(), any(), any())).thenReturn(Stream.of(onlinePleaSubmitted));

        pleadOnlineHandler.pleadOnline(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName("prosecutioncasefile.events.online-plea-submitted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(onlinePleaSubmitted.getCaseId().toString()))
                                ))))));
    }

    @Test
    public void shouldPleadOnlinePcqVisited() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();

        final PleadOnlinePcqVisited pleadOnlinePcqVisited = buildPleadOnlinePcqVisited(defendantId);

        final Envelope<PleadOnlinePcqVisited> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.plead-online-pcq-visited"), pleadOnlinePcqVisited);

        final OnlinePleaPcqVisitedSubmitted onlinePleaPcqVisitedSubmitted = OnlinePleaPcqVisitedSubmitted.onlinePleaPcqVisitedSubmitted()
                .withReceivedDateTime(clock.now())
                .withPleadOnlineVisited(pleadOnlinePcqVisited)
                .withCreatedBy(fromString(envelope.metadata().userId().get()))
                .withCaseId(CASE_ID)
                .build();

        when(prosecutionCaseFile.pleadOnlinePcqVisited(any(), any(), any(), any())).thenReturn(Stream.of(onlinePleaPcqVisitedSubmitted));

        pleadOnlineHandler.pleadOnlinePcqVisited(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName("prosecutioncasefile.events.online-plea-pcq-visited-submitted"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(onlinePleaPcqVisitedSubmitted.getCaseId().toString()))
                                ))))));
    }

    private Address buildAddress() {
        final Address address = Address.address()
                .withAddress1("l1")
                .withAddress2("l2")
                .withAddress3("l3")
                .withAddress4("l4")
                .withAddress5("l5")
                .withPostcode("postcode")
                .build();
        return address;
    }

    private PleadOnline buildPleadOnline(final UUID defendantId, final Address address, final Boolean outstandingFines) {
        final PleadOnline pleadOnline = PleadOnline.pleadOnline()
                .withCaseId(CASE_ID)
                .withDefendantId(defendantId)
                .withOffences(emptyList())
                .withUnavailability("unavailability")
                .withInterpreterLanguage("French")
                .withSpeakWelsh(true)
                .withWitnessDetails("witnessDetails")
                .withWitnessDispute("witnessDispute")
                .withOutstandingFines(outstandingFines)
                .withPersonalDetails(PersonalDetails.personalDetails()
                        .withFirstName("firstName")
                        .withLastName("lastName")
                        .withAddress(address)
                        .withContactDetails(ContactDetails.contactDetails()
                                .withBusiness("business")
                                .withHome("homeTelephone")
                                .withEmail("email1@aaa.bbb")
                                .withEmail2("email2@aaa.bbb")
                                .withMobile("mobile")
                                .build())
                        .withDateOfBirth(null)
                        .withNationalInsuranceNumber("nationalInsuranceNumber")
                        .withDriverNumber("TESTY708166G99KZ")
                        .withDriverLicenceDetails(null)
                        .build())
                .withFinancialMeans(FinancialMeans.financialMeans()
                        .withDefendantId(defendantId)
                        .withIncome(Income.income()
                                .withAmount(BigDecimal.valueOf(2000.22))
                                .withFrequency(Frequency.WEEKLY)
                                .build())
                        .withBenefits(Benefits.benefits().build())
                        .withEmploymentStatus("employmentStatus")
                        .build())
                .withEmployer(Employer.employer()
                        .withName("employer")
                        .withEmployeeReference("employeeReference")
                        .withPhone("phone")
                        .withAddress(address)
                        .build())
                .withOutgoings(emptyList())
                .withComeToCourt(false)
                .build();
        return pleadOnline;
    }


    private PleadOnlinePcqVisited buildPleadOnlinePcqVisited(final UUID defendantId) {
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = PleadOnlinePcqVisited.pleadOnlinePcqVisited()
                .withCaseId(CASE_ID)
                .withDefendantId(defendantId)
                .withPcqId(randomUUID())
                .withType("SJP")
                .withUrn("TFL123456")
                .build();
        return pleadOnlinePcqVisited;
    }
}
