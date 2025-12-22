package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.AssociateEnterpriseId.associateEnterpriseId;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived.sjpProsecutionReceived;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.enterpriseid.mapper.EnterpriseIdService;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileOffenceToSjpOffenceConverter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AssociateEnterpriseId;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionReceivedProcessorTest {

    private static final String PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED = "prosecutioncasefile.events.sjp-prosecution-received";
    private static final String PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS = "prosecutioncasefile.events.sjp-prosecution-received-with-warnings";
    private static final String PROSECUTIONCASEFILE_COMMANDS_ASSOCIATE_ENTERPRISE_ID = "prosecutioncasefile.command.associate-enterprise-id";

    private static final String MOCKED_ENTERPRISE_ID = randomAlphanumeric(10);
    private static final String MOCKED_OFFENCE_ID = UUID.randomUUID().toString();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<AssociateEnterpriseId>> argumentCaptor;

    @InjectMocks
    private SjpProsecutionProcessor sjpProsecutionProcessor;

    @Mock
    private ProsecutionCaseFileOffenceToSjpOffenceConverter.OffenceIdGenerator offenceIdGenerator;

    @Mock
    private EnterpriseIdService enterpriseIdService;

    @Spy
    @SuppressWarnings("unused")
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    public void shouldHandleSjpProsecutionReceivedEvent() {
        // GIVEN
        final Envelope<SjpProsecutionReceived> envelope = buildSjpProsecutionReceivedEvent();
        final UUID caseId = envelope.payload().getProsecution().getCaseDetails().getCaseId();

        when(enterpriseIdService.enterpriseIdForCase(caseId)).thenReturn(MOCKED_ENTERPRISE_ID);

        // WHEN
        sjpProsecutionProcessor.handleSjpProsecutionReceived(envelope);

        // THEN
        verifyEnterpriseIdAssociated(caseId, envelope.metadata());
    }

    @Test
    public void shouldHandleSjpProsecutionReceivedEventForMCC() {

        final Envelope<SjpProsecutionReceived> envelope = buildSjpProsecutionReceivedEventWithChannel(Channel.MCC);
        final UUID caseId = envelope.payload().getProsecution().getCaseDetails().getCaseId();

        when(enterpriseIdService.enterpriseIdForCase(caseId)).thenReturn(MOCKED_ENTERPRISE_ID);

        sjpProsecutionProcessor.handleSjpProsecutionReceived(envelope);
    }


    @Test
    public void shouldHandleSjpProsecutionReceivedWithWarningsEvent() {
        // GIVEN
        final Envelope<SjpProsecutionReceivedWithWarnings> envelope = buildSjpProsecutionReceivedWithWarningsEvent();
        final UUID caseId = envelope.payload().getProsecution().getCaseDetails().getCaseId();

        when(enterpriseIdService.enterpriseIdForCase(caseId)).thenReturn(MOCKED_ENTERPRISE_ID);

        // WHEN
        sjpProsecutionProcessor.handleSjpProsecutionReceivedWithWarnings(envelope);

        // THEN
        verifyEnterpriseIdAssociated(caseId, envelope.metadata());
    }

    @Test
    public void shouldHandleSjpProsecutionReceivedWithWarningsEventForMCC() {
        final Envelope<SjpProsecutionReceivedWithWarnings> envelope = buildSjpProsecutionReceivedWithWarningsEventWithChannel(Channel.MCC);
        final UUID caseId = envelope.payload().getProsecution().getCaseDetails().getCaseId();

        when(enterpriseIdService.enterpriseIdForCase(caseId)).thenReturn(MOCKED_ENTERPRISE_ID);

        sjpProsecutionProcessor.handleSjpProsecutionReceivedWithWarnings(envelope);
    }



    private void verifyEnterpriseIdAssociated(final UUID caseId, final Metadata metadata) {
        verify(sender).send(argumentCaptor.capture());
        verify(enterpriseIdService).enterpriseIdForCase(caseId);

        final Envelope<AssociateEnterpriseId> associateEnterpriseIdEnvelope = argumentCaptor.getValue();
        assertThat(associateEnterpriseIdEnvelope.metadata().name(), is(PROSECUTIONCASEFILE_COMMANDS_ASSOCIATE_ENTERPRISE_ID));
        assertThat(associateEnterpriseIdEnvelope.metadata().clientCorrelationId(),
                allOf(
                        not(equalTo(Optional.<String>empty())),
                        equalTo(metadata.clientCorrelationId())));

        assertThat(associateEnterpriseIdEnvelope.payload(), equalTo(buildAssociateEnterpriseId(caseId)));
    }

    private static Envelope<SjpProsecutionReceived> buildSjpProsecutionReceivedEvent() {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID()).build();

        final SjpProsecutionReceived sjpProsecutionReceived = sjpProsecutionReceived()
                .withProsecution(buildProsecution())
                .build();

        return envelopeFrom(metadata, sjpProsecutionReceived);
    }

    private static Envelope<SjpProsecutionReceived> buildSjpProsecutionReceivedEventWithChannel(Channel channel) {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID()).build();

        final SjpProsecutionReceived sjpProsecutionReceived = sjpProsecutionReceived()
                .withProsecution(buildProsecutionWithChannel(channel))
                .build();

        return envelopeFrom(metadata, sjpProsecutionReceived);
    }

    private static Envelope<SjpProsecutionReceivedWithWarnings> buildSjpProsecutionReceivedWithWarningsEvent(final Problem... problem) {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID()).build();

        final SjpProsecutionReceivedWithWarnings sjpProsecutionReceived = SjpProsecutionReceivedWithWarnings.sjpProsecutionReceivedWithWarnings()
                .withProsecution(buildProsecution())
                .withWarnings(Arrays.asList(problem))
                .build();

        return envelopeFrom(metadata, sjpProsecutionReceived);
    }

    private static Envelope<SjpProsecutionReceivedWithWarnings> buildSjpProsecutionReceivedWithWarningsEventWithChannel( final Channel channel,final Problem... problem) {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID()).build();

        final SjpProsecutionReceivedWithWarnings sjpProsecutionReceived = SjpProsecutionReceivedWithWarnings.sjpProsecutionReceivedWithWarnings()
                .withProsecution(buildProsecutionWithChannel(channel))
                .withWarnings(Arrays.asList(problem))
                .build();

        return envelopeFrom(metadata, sjpProsecutionReceived);
    }


    private static Prosecution buildProsecution() {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutor(buildProsecutionSubmissionDetails())
                        .build())
                .withDefendants(singletonList(buildDefendant()))
                .build();
    }

    public static Prosecution buildProsecutionWithChannel(Channel channel) {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("A16Xt4kCBJ")
                        .withProsecutor(buildProsecutionSubmissionDetails())
                        .build())
                .withChannel(channel)
                .withDefendants(singletonList(buildDefendant()))
                .build();
    }


    private static Prosecutor buildProsecutionSubmissionDetails() {
        return prosecutor()
                .withProsecutingAuthority("TVL")
                .build();
    }

    private static Defendant buildDefendant() {
        return Defendant.defendant()
                .withDocumentationLanguage(Language.E)
                .withHearingLanguage(Language.E)
                .withIndividual(Individual.individual()
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress4("address line 5")
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle("Mr")
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(Gender.MALE)
                                .build())
                        .withNationalInsuranceNumber("1922492")
                        .withDriverNumber("2362435")
                        .build())
                .withOffences(singletonList(Offence.offence()
                        .withBackDuty(BigDecimal.ONE)
                        .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                        .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                        .withChargeDate(LocalDate.of(2017, 8, 15))
                        .withAppliedCompensation(BigDecimal.TEN)
                        .withOffenceCode("TVL-ABC")
                        .withOffenceCommittedDate(LocalDate.of(2018, 2, 20))
                        .withOffenceCommittedEndDate(LocalDate.of(2018, 3, 15))
                        .withOffenceDateCode(4)
                        .withOffenceLocation("London")
                        .withOffenceSequenceNumber(6)
                        .withOffenceWording("TV LICENSE NOT PAID")
                        .withOffenceWordingWelsh("TV LICENSE NOT PAID IN WELSH")
                        .withStatementOfFacts("facts")
                        .withStatementOfFactsWelsh("welsh-facts")
                        .build()))
                .withPostingDate(LocalDate.of(2017, 10, 20))
                .withLanguageRequirement("No")
                .withNumPreviousConvictions(99)
                .build();
    }

    private static AssociateEnterpriseId buildAssociateEnterpriseId(final UUID caseId) {
        return associateEnterpriseId()
                .withCaseId(caseId)
                .withEnterpriseId(MOCKED_ENTERPRISE_ID)
                .build();
    }

}