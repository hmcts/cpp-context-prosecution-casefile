package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.Address.address;
import static uk.gov.justice.json.schemas.domains.sjp.ContactDetails.contactDetails;
import static uk.gov.justice.json.schemas.domains.sjp.Gender.MALE;
import static uk.gov.justice.json.schemas.domains.sjp.Language.W;
import static uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase.createSjpCase;
import static uk.gov.justice.json.schemas.domains.sjp.commands.Defendant.defendant;
import static uk.gov.justice.json.schemas.domains.sjp.commands.Offence.offence;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData.prosecutorsReferenceData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported.prosecutionCaseUnsupported;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated.sjpProsecutionInitiated;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiatedWithWarnings.sjpProsecutionInitiatedWithWarnings;

import uk.gov.justice.json.schemas.domains.sjp.commands.CreateSjpCase;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.IntegerGenderToSjpGenderConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileAddressToSjpAddressConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileDefendantToSjpDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileLanguageToSjpLanguageConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileOffenceToSjpOffenceConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileOffenceToSjpOffenceConverter.OffenceIdGenerator;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.SjpProsecutionToSjpCaseConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiatedWithWarnings;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpProsecutionInitiatedProcessorTest {
    private static final String PROSECUTIONCASEFILE_EVENTS_PROSECUTION_CASE_UNSUPPORTED = "prosecutioncasefile.events.prosecution-case-unsupported";
    private static final String PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_INITIATED = "prosecutioncasefile.events.sjp-prosecution-initiated";
    private static final String PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_INITIATED_WITH_WARNINGS = "prosecutioncasefile.events.sjp-prosecution-initiated-with-warnings";
    private static final String SJP_COMMAND_CREATE_CASE = "sjp.create-sjp-case";
    private static final UUID MOCKED_OFFENCE_ID = randomUUID();
    private static final UUID EXTERNAL_ID = randomUUID();
    public static final String PROSECUTING_AUTHORITY_TVL = "TVL";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> argumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PublicProsecutionCaseUnsupported>> publicArgumentCaptor;

    @Mock
    private JsonEnvelope jsonEnvelopeWithCustomMetadata;

    @Mock
    private EnvelopeHelper envelopeHelper;

    private final String defendantId = randomUUID().toString();;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private SjpProsecutionToSjpCaseConverter sjpProsecutionToSjpCaseConverter = newSjpProsecutionToSjpCaseConverter();

    private SjpProsecutionToSjpCaseConverter newSjpProsecutionToSjpCaseConverter() {
        var prosecutionCaseFileOffenceToSjpOffenceConverter = new ProsecutionCaseFileOffenceToSjpOffenceConverter(new OffenceIdGenerator());

        final IntegerGenderToSjpGenderConverter integerGenderToSjpGenderConverter = new IntegerGenderToSjpGenderConverter();
        final ProsecutionCaseFileAddressToSjpAddressConverter prosecutionAddressToSjpAddressConverter = new ProsecutionCaseFileAddressToSjpAddressConverter();
        final ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter prosecutionCaseFileContactDetailsToSjpContactDetailsConverter = new ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter();
        final ProsecutionCaseFileLanguageToSjpLanguageConverter prosecutionCaseFileLanguageToSjpLanguageConverter = new ProsecutionCaseFileLanguageToSjpLanguageConverter();

        final ProsecutionCaseFileDefendantToSjpDefendantConverter prosecutionCaseFileDefendantToSjpDefendantConverter = new ProsecutionCaseFileDefendantToSjpDefendantConverter();

        setField(prosecutionCaseFileDefendantToSjpDefendantConverter, "integerGenderToSjpGenderConverter", integerGenderToSjpGenderConverter);
        setField(prosecutionCaseFileDefendantToSjpDefendantConverter, "prosecutionAddressToSjpAddressConverter", prosecutionAddressToSjpAddressConverter);
        setField(prosecutionCaseFileDefendantToSjpDefendantConverter, "prosecutionCaseFileOffenceToSjpOffenceConverter", prosecutionCaseFileOffenceToSjpOffenceConverter);
        setField(prosecutionCaseFileDefendantToSjpDefendantConverter, "prosecutionCaseFileContactDetailsToSjpContactDetailsConverter", prosecutionCaseFileContactDetailsToSjpContactDetailsConverter);
        setField(prosecutionCaseFileDefendantToSjpDefendantConverter, "prosecutionCaseFileLanguageToSjpLanguageConverter", prosecutionCaseFileLanguageToSjpLanguageConverter);


        return new SjpProsecutionToSjpCaseConverter(prosecutionCaseFileDefendantToSjpDefendantConverter);
    }

    @InjectMocks
    private SjpProsecutionProcessor sjpProsecutionProcessor;

    @Test
    public void shouldHandleSjpProsecutionInitiatedEvent() throws IOException {

        // TODO(ATCM-3453): Remove it, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelopeWithCustomMetadata);

        final Envelope<SjpProsecutionInitiated> sjpProsecutionInitiatedEvent = buildSjpProsecutionInitiatedEvent();
        final SjpProsecutionInitiated sjpProsecutionInitiated = sjpProsecutionInitiatedEvent.payload();

        sjpProsecutionProcessor.handleSjpProsecutionInitiated(sjpProsecutionInitiatedEvent);

        verifyCreateSjpCaseCommandSent(sjpProsecutionInitiated.getProsecution(), sjpProsecutionInitiated.getEnterpriseId());
    }

    @Test
    public void shouldHandleProsecutionCaseUnsupportedEvent() throws IOException {
        final Envelope<ProsecutionCaseUnsupported> envelope = buildProsecutionCaseUnsupported();
        final ProsecutionCaseUnsupported prosecutionCaseUnsupported = envelope.payload();

        sjpProsecutionProcessor.handleProsecutionCaseUnsupported(envelope);

        verify(sender).send(publicArgumentCaptor.capture());

        final Envelope<PublicProsecutionCaseUnsupported> sentEnvelope = publicArgumentCaptor.getValue();
        assertThat(sentEnvelope.metadata().name(), is("public.prosecutioncasefile.prosecution-case-unsupported"));
        final PublicProsecutionCaseUnsupported payload = sentEnvelope.payload();
        assertThat(prosecutionCaseUnsupported.getErrorMessage(), is(payload.getErrorMessage()));
        assertThat(prosecutionCaseUnsupported.getChannel(), is(payload.getChannel()));
        assertThat(prosecutionCaseUnsupported.getExternalId(), is(payload.getExternalId()));
        assertThat(prosecutionCaseUnsupported.getPoliceSystemId(), is(payload.getPoliceSystemId()));
        assertThat(prosecutionCaseUnsupported.getUrn(), is(payload.getUrn()));
    }

    @Test
    public void shouldHandleSjpProsecutionInitiatedWithWarningsEvent() throws IOException {
        // TODO(ATCM-3453): Remove it, once either the pojo generation generates the annotations or the sendAsAdmin accepts typed Envelope
        when(envelopeHelper.withMetadataInPayload(any())).thenReturn(jsonEnvelopeWithCustomMetadata);
        final Envelope<SjpProsecutionInitiatedWithWarnings> sjpProsecutionInitiatedWithWarningsEvent = buildSjpProsecutionInitiatedWithWarningsEvent();
        final SjpProsecutionInitiatedWithWarnings sjpProsecutionInitiatedWithWarnings = sjpProsecutionInitiatedWithWarningsEvent.payload();

        sjpProsecutionProcessor.handleSjpProsecutionInitiatedWithWarnings(sjpProsecutionInitiatedWithWarningsEvent);

        verifyCreateSjpCaseCommandSent(sjpProsecutionInitiatedWithWarnings.getProsecution(), sjpProsecutionInitiatedWithWarnings.getEnterpriseId());
    }

    private void verifyCreateSjpCaseCommandSent(final Prosecution prosecution, final String enterpriseId) throws IOException {
        verify(envelopeHelper).withMetadataInPayload(argumentCaptor.capture());
        verify(sender).sendAsAdmin(jsonEnvelopeWithCustomMetadata);

        final JsonEnvelope sentEnvelope = argumentCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is(SJP_COMMAND_CREATE_CASE));
        assertThat(sentEnvelope.metadata().clientCorrelationId(), is(sentEnvelope.metadata().clientCorrelationId()));

        final CreateSjpCase actualCreateSjpCase = objectMapper.readValue(sentEnvelope.payload().toString(), CreateSjpCase.class);
        final CreateSjpCase expectedCreateSjpCase = buildExpectedCreateSjpCase(prosecution, enterpriseId);
        assertThat(actualCreateSjpCase, equalTo(expectedCreateSjpCase));
    }

    private static Envelope<ProsecutionCaseUnsupported> buildProsecutionCaseUnsupported() {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_PROSECUTION_CASE_UNSUPPORTED)
                .withId(randomUUID())
                .build();
        final ProsecutionCaseUnsupported sjpProsecutionInitiated = prosecutionCaseUnsupported()
                .withChannel(SPI)
                .withErrorMessage("ErrorMessage")
                .withExternalId(EXTERNAL_ID)
                .withPoliceSystemId("PoliceSystemId")
                .withUrn("urn")
                .build();

        return envelopeFrom(metadata, sjpProsecutionInitiated);
    }

    private Envelope<SjpProsecutionInitiated> buildSjpProsecutionInitiatedEvent() {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_INITIATED)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final SjpProsecutionInitiated sjpProsecutionInitiated = sjpProsecutionInitiated()
                .withProsecution(buildProsecution())
                .build();

        return envelopeFrom(metadata, sjpProsecutionInitiated);
    }

    private Envelope<SjpProsecutionInitiatedWithWarnings> buildSjpProsecutionInitiatedWithWarningsEvent(final Problem... warnings) {
        final Metadata metadata = metadataBuilder()
                .withName(PROSECUTIONCASEFILE_EVENTS_SJP_PROSECUTION_INITIATED_WITH_WARNINGS)
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final SjpProsecutionInitiatedWithWarnings sjpProsecutionInitiatedWithWarnings = sjpProsecutionInitiatedWithWarnings()
                .withProsecution(buildProsecution())
                .withWarnings(Arrays.asList(warnings))
                .build();

        return envelopeFrom(metadata, sjpProsecutionInitiatedWithWarnings);
    }

    private Prosecution buildProsecution() {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(randomUUID())
                        .withProsecutorCaseReference("TVL12345")
                        .withProsecutor(prosecutor()
                                .withProsecutingAuthority(PROSECUTING_AUTHORITY_TVL)
                                .withReferenceData(prosecutorsReferenceData()
                                        .withOucode("GAEAA01")
                                        .withShortName(PROSECUTING_AUTHORITY_TVL)
                                        .withId(randomUUID())
                                        .build())
                                .build())
                        .build())
                .withDefendants(singletonList(buildDefendant()))
                .build();
    }

    private Defendant buildDefendant() {
        return Defendant.defendant()
                .withId(defendantId)
                .withDocumentationLanguage(Language.E)
                .withHearingLanguage(Language.W)
                .withAppliedProsecutorCosts(BigDecimal.TEN)
                .withAsn("asn")
                .withPncIdentifier("pncId")
                .withOrganisationName("organisationName")
                .withIndividual(Individual.individual()
                        .withPersonalInformation(PersonalInformation.personalInformation()
                                .withAddress(Address.address()
                                        .withAddress1("66 Exeter Street")
                                        .withAddress2("address line 2")
                                        .withAddress3("address line 3")
                                        .withAddress4("address line 4")
                                        .withAddress5(null)
                                        .withPostcode("M60 1NW")
                                        .build())
                                .withFirstName("Eugene")
                                .withLastName("Tooms")
                                .withTitle("Mr")
                                .withContactDetails(ContactDetails.contactDetails()
                                        .withHome("0207 000 0000")
                                        .withMobile("11111 111 111")
                                        .withPrimaryEmail("primary@email.com")
                                        .withSecondaryEmail("secondary@email.com")
                                        .withWork("0207 111 1111")
                                        .build())
                                .build())
                        .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                .withEthnicity("Unknown")
                                .withDateOfBirth(LocalDate.of(1989, 4, 18))
                                .withGender(Gender.MALE)
                                .build())
                        .withNationalInsuranceNumber("AB123456C")
                        .withDriverNumber("2362435")
                        .build())
                .withOffences(singletonList(Offence.offence()
                        .withOffenceId(MOCKED_OFFENCE_ID)
                        .withBackDuty(BigDecimal.valueOf(2))
                        .withBackDutyDateFrom(LocalDate.of(2012, 1, 1))
                        .withBackDutyDateTo(LocalDate.of(2016, 1, 1))
                        .withChargeDate(LocalDate.of(2017, 8, 15))
                        .withAppliedCompensation(BigDecimal.valueOf(3))
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
                        .withReferenceData(CaseReceivedHelper.buildNonPressRestrictableReferenceData())
                        .build()))
                .withPostingDate(LocalDate.of(2017, 10, 20))
                .withLanguageRequirement("No")
                .withNumPreviousConvictions(99)
                .build();
    }

    private CreateSjpCase buildExpectedCreateSjpCase(final Prosecution prosecution, final String enterpriseId) {
        final Defendant defendant = prosecution.getDefendants().get(0);
        final Individual individual = prosecution.getDefendants().get(0).getIndividual();
        final PersonalInformation personalInformation = individual.getPersonalInformation();
        final Address address = personalInformation.getAddress();
        final SelfDefinedInformation selfDefinedInformation = individual.getSelfDefinedInformation();
        final Offence offence = prosecution.getDefendants().get(0).getOffences().get(0);
        final ContactDetails contactDetails = individual.getPersonalInformation().getContactDetails();

        return createSjpCase()
                .withId(prosecution.getCaseDetails().getCaseId())
                .withCosts(defendant.getAppliedProsecutorCosts())
                .withEnterpriseId(enterpriseId)
                .withPostingDate(defendant.getPostingDate())
                .withProsecutingAuthority(PROSECUTING_AUTHORITY_TVL)
                .withUrn(prosecution.getCaseDetails().getProsecutorCaseReference())
                .withDefendant(defendant()
                        .withId(UUID.fromString(defendantId))
                        .withTitle(personalInformation.getTitle())
                        .withNumPreviousConvictions(defendant.getNumPreviousConvictions())
                        .withLastName(personalInformation.getLastName())
                        .withFirstName(personalInformation.getFirstName())
                        .withLegalEntityName(defendant.getOrganisationName())
                        .withGender(MALE)
                        .withHearingLanguage(W)
                        .withAsn(defendant.getAsn())
                        .withPncIdentifier(defendant.getPncIdentifier())
                        .withDateOfBirth(selfDefinedInformation.getDateOfBirth().toString())
                        .withAddress(address()
                                .withAddress1(address.getAddress1())
                                .withAddress2(address.getAddress2())
                                .withAddress3(address.getAddress3())
                                .withAddress4(address.getAddress4())
                                .withAddress5(address.getAddress5())
                                .withPostcode(address.getPostcode())
                                .build())
                        .withOffences(singletonList(offence()
                                .withId(MOCKED_OFFENCE_ID.toString())
                                .withProsecutionFacts(offence.getStatementOfFacts())
                                .withOffenceWording(offence.getOffenceWording())
                                .withOffenceWordingWelsh(offence.getOffenceWordingWelsh())
                                .withOffenceSequenceNo(offence.getOffenceSequenceNumber())
                                .withOffenceCommittedDate(offence.getOffenceCommittedDate().toString())
                                .withLibraOffenceCode(offence.getOffenceCode())
                                .withLibraOffenceDateCode(offence.getOffenceDateCode())
                                .withCompensation(offence.getAppliedCompensation())
                                .withChargeDate(offence.getChargeDate())
                                .withBackDuty(offence.getBackDuty())
                                .withBackDutyDateFrom(offence.getBackDutyDateFrom())
                                .withBackDutyDateTo(offence.getBackDutyDateTo())
                                .withVehicleMake(offence.getVehicleMake())
                                .withVehicleRegistrationMark(offence.getVehicleRegistrationMark())
                                .withPressRestrictable(false)
                                .build()))
                        .withContactDetails(contactDetails()
                                .withHome(contactDetails.getHome())
                                .withMobile(contactDetails.getMobile())
                                .withEmail(contactDetails.getPrimaryEmail())
                                .withEmail2(contactDetails.getSecondaryEmail())
                                .withBusiness(contactDetails.getWork())
                                .build())
                        .withNationalInsuranceNumber(individual.getNationalInsuranceNumber())
                        .withDriverNumber("2362435")
                        .build())
                .build();
    }

}
