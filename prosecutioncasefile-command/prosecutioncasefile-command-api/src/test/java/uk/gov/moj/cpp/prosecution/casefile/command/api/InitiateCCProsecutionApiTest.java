package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.IndicatedPleaValue.INDICATED_GUILTY;
import static uk.gov.justice.core.courts.MigrationSourceSystem.migrationSourceSystem;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.CONVICTING_COURT_CODE_IS_MANDATORY;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.EARLIEST_START_DATE_MUST_BE_FUTURE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.EARLIEST_START_DATE_MUST_BE_PROVIDED;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.LIST_NEW_HEARING_AND_INITIAL_HEARING_ARE_MUTUALLY_EXCLUSIVE;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.PLEA_DATE_MUST_BE_TODAY_OR_IN_THE_PAST;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.VERDICT_DATE_MUST_BE_TODAY_OR_IN_THE_PAST;
import static uk.gov.moj.cpp.prosecution.casefile.command.api.InitiateCCProsecutionApi.WEEK_COMMENCING_MUST_BE_PROVIDED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.WEEK_COMMENCING;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea.plea;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Verdict.verdict;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.VerdictType.verdictType;


import uk.gov.justice.core.courts.CivilOffence;
import uk.gov.justice.core.courts.DefendantFineAccountNumber;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.MigrationSourceSystem;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;

import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Plea;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VehicleRelatedOffence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Verdict;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.VerdictType;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InitiateCCProsecutionApiTest {

    private static final String INITIATE_CC_PROSECUTION_WITH_REFERENCE_DATA_COMMAND = "prosecutioncasefile.command.initiate-cc-prosecution-with-reference-data";
    private static final String POLICE_SYSTEM_ID = "00301PoliceCaseSystem";
    public static final BigDecimal APPLIED_PROSECUTOR_COSTS = new BigDecimal(85);
    public static final UUID OFFENCE_ID = randomUUID();
    public static final BigDecimal APPLIED_COMPENSATION = new BigDecimal("30.00");
    public static final BigDecimal BACK_DUTY = new BigDecimal("150.10");
    public static final LocalDate BACK_DUTY_DATE_FROM = LocalDate.of(2011, 1, 1);
    public static final LocalDate BACK_DUTY_DATE_TO = LocalDate.of(2015, 1, 1);
    public static final LocalDate CHARGE_DATE = LocalDate.of(2017, 11, 8);
    private static final UUID MOT_REASON_ID = randomUUID();
    public static final String OFFENCE_CODE = "OFCODE12";
    public static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.of(2017, 6, 1);
    public static final LocalDate OFFENCE_COMMITTED_END_DATE = LocalDate.of(2017, 6, 20);
    public static final Integer OFFENCE_DATE_CODE = 15;
    public static final Integer OFFENCE_SEQUENCE_NUMBER = 3;
    public static final String OFFENCE_TITLE = "Offence Title";
    public static final String OFFENCE_TITLE_WELSH = "Offence Title (Welsh)";
    public static final String OFFENCE_WORDING = "TV Licence not paid";
    public static final String OFFENCE_WORDING_WELSH = "TV Licence not paid (Welsh)";
    public static final String STATEMENT_OF_FACTS = "Prosecution charge wording";
    public static final String STATEMENT_OF_FACTS_WELSH = "Prosecution charge wording (Welsh)";
    public static final String DEFENDANT_ID = "64fad682-f4d3-4566-868d-7621fd20ae2c";
    public static final String ASN = "arrest/summons";
    public static final String LANGUAGE_REQUIREMENTS = "No Language Requirements";
    public static final Integer NUM_OF_PREVIOUS_CONVICTIONS = 3;
    private static final String VEHICLE_MAKE = "Ford";
    private static final String DVLA_OUCODE = "DVLA_OUCODE";
    private static final Prosecutor DVLA_PROSECUTOR = new Prosecutor.Builder().withProsecutingAuthority(DVLA_OUCODE).build();
    private static final ProsecutorsReferenceData DVLA_PROSECUTORS_REFERENCE_DATA = new ProsecutorsReferenceData.Builder().withShortName("DVLA").build();
    private static final String NON_DVLA_OUCODE = "NON_DVLA_OUCODE";
    private static final Prosecutor NON_DVLA_PROSECUTOR = new Prosecutor.Builder().withProsecutingAuthority(NON_DVLA_OUCODE).build();
    private static final ProsecutorsReferenceData NON_DVLA_PROSECUTORS_REFERENCE_DATA = new ProsecutorsReferenceData.Builder().withShortName("NON_DVLA").build();
    private static final String XHIBIT = "XHIBIT";
    private static final String XHIBIT_IDENTIFIER = "XHIBIT_IDENTIFIER";
    private static final String CONVICTING_COURT_CODE = "C50EX03";


    @Captor
    private ArgumentCaptor<Envelope<ProsecutionWithReferenceData>> envelopeArgumentCaptor;

    @InjectMocks
    private InitiateCCProsecutionApi initiateCCProsecutionApi;
    @Mock
    private Sender sender;
    @Mock
    private InitiateProsecution initiateProsecution;
    @Mock
    private CaseDetails caseDetails;
    @Mock
    private Prosecutor prosecutor;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;
    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;
    @Mock
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    private List<Defendant> defendantList = new ArrayList<>();

    @Test
    void shouldSendReceiveCCProsecutionWithReferenceDataCommandWithCorrectPayload() {
        createValidPayloadToAssert(Channel.SPI, false);
    }

    private void createValidPayloadToAssert(final Channel channel, final boolean isMigration) {
        final UUID externalId = randomUUID();
        when(initiateProsecution.getCaseDetails()).thenReturn(caseDetails);
        when(initiateProsecution.getDefendants()).thenReturn(defendantList);
        when(initiateProsecution.getChannel()).thenReturn(channel);
        when(caseDetails.getProsecutor()).thenReturn(prosecutor);
        when(caseDetails.getPoliceSystemId()).thenReturn(POLICE_SYSTEM_ID);
        when(prosecutor.getProsecutingAuthority()).thenReturn("OWTW");
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(initiateProsecution.getExternalId()).thenReturn(externalId);
        if (isMigration && channel == MCC) {
            when(initiateProsecution.getMigrationSourceSystem()).thenReturn(
                    migrationSourceSystem()
                            .withMigrationSourceSystemName(XHIBIT)
                            .withMigrationSourceSystemCaseIdentifier(XHIBIT_IDENTIFIER)
                            .withDefendantFineAccountNumbers(singletonList(DefendantFineAccountNumber
                                    .defendantFineAccountNumber()
                                    .withDefendantId(randomUUID())
                                    .withFineAccountNumber("FINE98756")
                                    .build()))
                            .build());
        }
        final Envelope<InitiateProsecution> envelope = envelope(initiateProsecution);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> prosecutionWithReferenceDataEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(prosecutionWithReferenceDataEnvelope.metadata().name(), is(INITIATE_CC_PROSECUTION_WITH_REFERENCE_DATA_COMMAND));
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getCaseDetails(), is(caseDetails));
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getDefendants(), is(defendantList));
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getExternalId(), is(externalId));
        if (isMigration && channel == MCC) {
            assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
            assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getMigrationSourceSystem().getDefendantFineAccountNumbers().get(0).getFineAccountNumber(), is("FINE98756"));
        }
    }

    @Test
    void shouldSendReceiveCCProsecutionWithReferenceDataCommandWithCorrectPayloadMCCMigration() {
        createValidPayloadToAssert(MCC, true);
    }

    @ParameterizedTest
    @MethodSource("parametersForCCCaseTest")
    void shouldSendReceiveCCProsecutionWithReferenceDataCommandWithLocationSetPayload(boolean isCivil, Channel channel, CivilOffence civilOffence, Boolean isExParte, LocalDate chargeDate) {
        final Offence offence = Offence.offence()
                .withAppliedCompensation(APPLIED_COMPENSATION)
                .withBackDuty(BACK_DUTY)
                .withBackDutyDateFrom(BACK_DUTY_DATE_FROM)
                .withBackDutyDateTo(BACK_DUTY_DATE_TO)
                .withChargeDate(chargeDate)
                .withCivilOffence(civilOffence)
                .withMotReasonId(MOT_REASON_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE)
                .withOffenceDateCode(OFFENCE_DATE_CODE)
                .withOffenceId(OFFENCE_ID)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                .withOffenceWording(OFFENCE_WORDING)
                .withOffenceWordingWelsh(OFFENCE_WORDING_WELSH)
                .withPlea(plea().build())
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .withVehicleMake(VEHICLE_MAKE)
                .withVehicleRelatedOffence(VehicleRelatedOffence.vehicleRelatedOffence().build())
                .withVerdict(verdict().build())
                .build();

        final Offence offenceWithLocation = Offence.offence()
                .withAppliedCompensation(APPLIED_COMPENSATION)
                .withBackDuty(BACK_DUTY)
                .withBackDutyDateFrom(BACK_DUTY_DATE_FROM)
                .withBackDutyDateTo(BACK_DUTY_DATE_TO)
                .withChargeDate(chargeDate)
                .withCivilOffence(civilOffence)
                .withMotReasonId(MOT_REASON_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withOffenceCommittedEndDate(OFFENCE_COMMITTED_END_DATE)
                .withOffenceDateCode(OFFENCE_DATE_CODE)
                .withOffenceId(OFFENCE_ID)
                .withOffenceSequenceNumber(OFFENCE_SEQUENCE_NUMBER)
                .withOffenceTitle(OFFENCE_TITLE)
                .withOffenceTitleWelsh(OFFENCE_TITLE_WELSH)
                .withOffenceWording(OFFENCE_WORDING)
                .withOffenceWordingWelsh(OFFENCE_WORDING_WELSH)
                .withPlea(plea().build())
                .withReferenceData(OffenceReferenceData.offenceReferenceData().withLocationRequired("N").withExParte(isExParte).build())
                .withStatementOfFacts(STATEMENT_OF_FACTS)
                .withStatementOfFactsWelsh(STATEMENT_OF_FACTS_WELSH)
                .withVehicleMake(VEHICLE_MAKE)
                .withVehicleRelatedOffence(VehicleRelatedOffence.vehicleRelatedOffence().build())
                .withVerdict(verdict().build())
                .build();

        final Defendant defendantNotEmpty = defendant().withAsn(ASN)
                .withAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS)
                .withDocumentationLanguage(null)
                .withHearingLanguage(null)
                .withId(DEFENDANT_ID)
                .withIndividual(Individual.individual().build())
                .withLanguageRequirement(LANGUAGE_REQUIREMENTS)
                .withNumPreviousConvictions(NUM_OF_PREVIOUS_CONVICTIONS)
                .withOffences(singletonList(offence))
                .withAddress(Address.address().build())
                .build();
        Defendant defendantWithLocation = defendant().withAsn(ASN)
                .withAppliedProsecutorCosts(APPLIED_PROSECUTOR_COSTS)
                .withDocumentationLanguage(null)
                .withHearingLanguage(null)
                .withId(DEFENDANT_ID)
                .withIndividual(Individual.individual().build())
                .withLanguageRequirement(LANGUAGE_REQUIREMENTS)
                .withNumPreviousConvictions(NUM_OF_PREVIOUS_CONVICTIONS)
                .withOffences(singletonList(offenceWithLocation))
                .withAddress(Address.address().build())
                .build();
        when(initiateProsecution.getCaseDetails()).thenReturn(caseDetails);
        when(initiateProsecution.getDefendants()).thenReturn(defendantList);
        when(initiateProsecution.getChannel()).thenReturn(channel);
        when(initiateProsecution.getIsCivil()).thenReturn(isCivil);
        when(caseDetails.getProsecutor()).thenReturn(prosecutor);
        when(prosecutor.getProsecutingAuthority()).thenReturn("OWTW");
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("N")
                .withExParte(isExParte)
                .build()));
        initiateProsecution.getDefendants().add(defendantNotEmpty);
        initiateCCProsecutionApi.initiateCCProsecution(envelope(initiateProsecution));
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> prosecutionWithReferenceDataEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getProsecution().getDefendants(), is(ImmutableList.of(defendantWithLocation)));
        assertThat(prosecutionWithReferenceDataEnvelope.payload().getExternalId(), nullValue());
    }

    @Test
    void shouldSetDefaultOffenceLocationWhenOffenceHasNullLocationAndProsecutingAuthorityIsDVLA() {
        final Offence offence = offence().withOffenceLocation(null).build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).withAddress(Address.address().build()).build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, DVLA_PROSECUTOR, null, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), DVLA_OUCODE)).thenReturn(DVLA_PROSECUTORS_REFERENCE_DATA);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        assertThat(actualOffence.getOffenceLocation(), is("No location provided"));
    }

    @Disabled("HERE!!!")
    @Test
    void shouldGetProsecutorByIdWhenOUCodeIsNull() {
        final Offence offence = offence().withOffenceLocation(null).build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).withAddress(Address.address().build()).build();
        final UUID prosecutorId = randomUUID();
        final Prosecutor prosecutor = new Prosecutor.Builder()
                .withProsecutionAuthorityId(prosecutorId)
                .withProsecutingAuthority("OWTW")
                .build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, prosecutor, null, null));
        when(initiateProsecution.getCaseDetails()).thenReturn(caseDetails);
        when(initiateProsecution.getDefendants()).thenReturn(defendantList);
        when(initiateProsecution.getChannel()).thenReturn(Channel.SPI);
        when(caseDetails.getProsecutor()).thenReturn(prosecutor);
        when(caseDetails.getPoliceSystemId()).thenReturn(POLICE_SYSTEM_ID);
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(Collections.emptyList());

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorById(prosecutorId)).thenReturn(DVLA_PROSECUTORS_REFERENCE_DATA);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        assertThat(actualOffence.getOffenceLocation(), is("No location provided"));
    }

    @Test
    void shouldSetOffenceLocationProvidedWhenOffenceHasLocationAndProsecutingAuthorityIsDVLA() {
        final Offence offence = offence().withOffenceLocation("My Location").build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).withAddress(Address.address().build()).build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, DVLA_PROSECUTOR, null, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        assertThat(actualOffence.getOffenceLocation(), is("My Location"));
    }

    @Test
    void shouldLeaveOffenceLocationAsIsWhenProsecutingAuthorityIsNonDVLA() {
        final Offence offence = offence().withOffenceLocation("Canada").build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).withAddress(Address.address().build()).build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, NON_DVLA_PROSECUTOR, null, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), NON_DVLA_OUCODE)).thenReturn(NON_DVLA_PROSECUTORS_REFERENCE_DATA);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        assertThat(actualOffence.getOffenceLocation(), is("Canada"));
    }

    @Test
    void shouldLeaveOffenceLocationAsIsWhenProsecutingAuthorityIsUnknown() {
        final Offence offence = offence().withOffenceLocation("Canada").build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).withAddress(Address.address().build()).build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, NON_DVLA_PROSECUTOR, null, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), NON_DVLA_OUCODE)).thenReturn(null);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        assertThat(actualOffence.getOffenceLocation(), is("Canada"));
    }

    @Test
    void shouldPopulatePleaAndVerdict() {
        final String categoryType = "GUILTY";
        final String category = "Guilty";
        final UUID verdictId = randomUUID();
        final LocalDate verdictDate = now().minusDays(1);
        final String pleaValue = "GUILTY";
        final LocalDate pleaDate = now().minusDays(1);
        final String description = "Description";
        final String libraReferenceNumber = "1234567890";

        final Offence offence = offence()
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(pleaValue)
                        .build())
                .withVerdict(verdict()
                        .withVerdictDate(verdictDate)
                        .withVerdictType(verdictType()
                                .withId(verdictId)
                                .withCategoryType(categoryType)
                                .withCategory(category)
                                .withDescription(description)
                                .build())
                        .build())
                .withConvictingCourtCode("COURTCODE")
                .build();
        final Defendant defendant = defendant()
                .withOffences(ImmutableList.of(offence))
                .withAddress(Address.address().build())
                .withLibraReferenceNumber(libraReferenceNumber)
                .build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, NON_DVLA_PROSECUTOR, null, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);


        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), NON_DVLA_OUCODE)).thenReturn(null);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);
        final Defendant actualDefendant = actual.payload().getProsecution().getDefendants().get(0);

        final Plea plea = actualOffence.getPlea();
        assertThat(plea.getPleaDate(), is(pleaDate));
        assertThat(plea.getPleaValue(), is(pleaValue));

        final Verdict verdict = actualOffence.getVerdict();
        final VerdictType verdictType = verdict.getVerdictType();
        assertThat(verdict.getVerdictDate(), is(verdictDate));
        assertThat(verdictType.getCategory(), is(category));
        assertThat(verdictType.getCategoryType(), is(categoryType));
        assertThat(verdictType.getDescription(), is(description));

        assertThat(actualDefendant.getLibraReferenceNumber(), is(libraReferenceNumber));

    }

    @Test
    void shouldReturnErrorWhenDefendantDoesNotHaveAddress() {
        final Offence offence = offence().withOffenceLocation(null).build();
        final Defendant defendant = defendant().withOffences(List.of(offence))
                .withIndividual(Individual.individual().withPersonalInformation(PersonalInformation.personalInformation().build()).build()).build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, DVLA_PROSECUTOR, null, null));

        assertThrows(BadRequestException.class, () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));
    }

    @Test
    void shouldCheckConvictingCourtCodeConditionalMandatory() {
        final String pleaValue = "INDICATED_GUILTY";
        final LocalDate pleaDate = now().minusDays(1);

        final MigrationSourceSystem migrationSourceSystem = migrationSourceSystem()
                .withMigrationSourceSystemName(XHIBIT)
                .withMigrationSourceSystemCaseIdentifier(XHIBIT_IDENTIFIER)
                .build();

        final Offence offence = offence()
                .withConvictingCourtCode(CONVICTING_COURT_CODE)
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(pleaValue)
                        .build())
                .build();
        final Defendant defendant = defendant()
                .withOffences(singletonList(offence))
                .withAddress(Address.address()
                        .build())
                .build();
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(defendant, NON_DVLA_PROSECUTOR, migrationSourceSystem, null));
        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);
        when(referenceDataQueryService.getProsecutorsByOuCode(envelope.metadata(), NON_DVLA_OUCODE)).thenReturn(null);

        initiateCCProsecutionApi.initiateCCProsecution(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> actual = envelopeArgumentCaptor.getValue();
        final Offence actualOffence = actual.payload().getProsecution().getDefendants().get(0).getOffences().get(0);

        final Plea plea = actualOffence.getPlea();
        assertThat(plea.getPleaDate(), is(pleaDate));
        assertThat(plea.getPleaValue(), is(pleaValue));
        assertThat(actual.payload().getProsecution().getMigrationSourceSystem().getMigrationSourceSystemName(), is(XHIBIT));
        assertThat(actualOffence.getConvictingCourtCode(), is(CONVICTING_COURT_CODE));

    }

    @Test
    void shouldThrowBadRequestExceptionConvictingCourtCodeConditionalMandatoryIsNull() {
        final String pleaValue = "INDICATED_GUILTY";
        final LocalDate pleaDate = now().plusDays(1);

        final Offence offence = offence()
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(pleaValue)
                        .build())
                .build();

        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence), NON_DVLA_PROSECUTOR, buildMigrationSourceSystem(), null));

        assertThrows(BadRequestException.class, () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));
    }

    @Test
    void shouldThrowBadRequestExceptionWhenPleaDateIsInTheFuture() {
        final String pleaValue = INDICATED_GUILTY.name();
        final LocalDate pleaDate = now().plusDays(1);

        final Offence offence = offence()
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(pleaValue)
                        .build())
                .withConvictingCourtCode("ABCDEF")
                .build();

        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence), NON_DVLA_PROSECUTOR, buildMigrationSourceSystem(), null));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(PLEA_DATE_MUST_BE_TODAY_OR_IN_THE_PAST, exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INDICATED_GUILTY", "GUILTY"})
    void shouldThrowBadRequestExceptionWhenConvictingCourtIsNullWithPleaValue(String value) {
        final LocalDate pleaDate = now().plusDays(1);

        final Offence offence = offence()
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(value)
                        .build())
                .build();

        final Envelope<InitiateProsecution> envelope = envelope(
                caseProsecution(buildDefendant(offence), NON_DVLA_PROSECUTOR, buildMigrationSourceSystem(), null)
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(CONVICTING_COURT_CODE_IS_MANDATORY, exception.getMessage());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenVerdictIsNotNullAndVerdictDateIsNull() {
        final String pleaValue = INDICATED_GUILTY.name();
        final LocalDate pleaDate = now().minusDays(2);

        final Offence offence = offence()
                .withPlea(plea()
                        .withPleaDate(pleaDate)
                        .withPleaValue(pleaValue)
                        .build())
                .withConvictingCourtCode("CONVICTING_COURT_CODE")
                .withVerdict(verdict()
                        .withVerdictType(verdictType()
                                .withId(randomUUID())
                                .withCategory("VERDICT_CATEGORY")
                                .build())
                        .withVerdictDate(now().plusDays(1))
                        .build())
                .build();

        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence), NON_DVLA_PROSECUTOR, buildMigrationSourceSystem(), null));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(VERDICT_DATE_MUST_BE_TODAY_OR_IN_THE_PAST, exception.getMessage());
    }

    @Test()
    public void shouldReturnErrorWhenDefendantHaveChargeDate() {
        final Offence offence = offence().withChargeDate(LocalDate.now()).build();
        final Defendant defendant = defendant().withOffences(ImmutableList.of(offence)).build();

        final InitiateProsecution caseProsecution = new InitiateProsecution.Builder()
                .withCaseDetails(new CaseDetails.Builder().withProsecutor(DVLA_PROSECUTOR).build())
                .withDefendants(ImmutableList.of(defendant))
                .withIsCivil(true)
                .build();
        final Envelope<InitiateProsecution> envelope = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), caseProsecution);

        assertThrows(BadRequestException.class, () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));
    }


    private InitiateProsecution caseProsecution(final Defendant defendant, final Prosecutor prosecutor, final MigrationSourceSystem migrationSourceSystem,
                                                final HearingRequest listNewHearingRequest) {
        return new InitiateProsecution.Builder().withCaseDetails(new CaseDetails.Builder()
                        .withProsecutor(prosecutor).withInitiationCode("O").build())
                .withDefendants(ImmutableList.of(defendant))
                .withMigrationSourceSystem(migrationSourceSystem)
                .withChannel(MCC)
                .withListNewHearing(listNewHearingRequest)
                .build();
    }

    @Test
    void shouldThrowBadReuestWhenIsMCCWithNewListAndInitialHearing() {

        final Envelope<InitiateProsecution> envelope = envelope(caseProsecutionWithNewListHearingAndInitialhearing());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(LIST_NEW_HEARING_AND_INITIAL_HEARING_ARE_MUTUALLY_EXCLUSIVE, exception.getMessage());

    }

    @Test
    void shouldThrowBadRequestExceptionWhenFixedEarliestDateIsInThePast() {
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence().build()),
                NON_DVLA_PROSECUTOR,
                buildMigrationSourceSystem(),
                buildHearingRequestWithFixedDateHearing(FIXED, ZonedDateTime.now().minusDays(1))));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(EARLIEST_START_DATE_MUST_BE_FUTURE_DATE, exception.getMessage());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenWcdIsNotProvidedWhenDateTypeIsWeekCommencing() {
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence().build()),
                NON_DVLA_PROSECUTOR,
                buildMigrationSourceSystem(),
                buildHearingRequestWithFixedDateHearing(WEEK_COMMENCING, ZonedDateTime.now().plusDays(5))));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(WEEK_COMMENCING_MUST_BE_PROVIDED, exception.getMessage());
    }

    @Test
    void shouldThrowBadRequestExceptionWhenFixedDateIsNotProvidedWhenDateTypeIsFixed() {
        final Envelope<InitiateProsecution> envelope = envelope(caseProsecution(buildDefendant(offence().build()),
                NON_DVLA_PROSECUTOR,
                buildMigrationSourceSystem(),
                buildHearingRequestWithWeekCommencing(FIXED, LocalDate.now().plusDays(5))));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> initiateCCProsecutionApi.initiateCCProsecution(envelope));

        assertEquals(EARLIEST_START_DATE_MUST_BE_PROVIDED, exception.getMessage());
    }

    private InitiateProsecution caseProsecutionWithNewListHearingAndInitialhearing() {
        return new InitiateProsecution.Builder().withCaseDetails(new CaseDetails.Builder()
                        .withProsecutor(prosecutor).withInitiationCode("O").build())
                .withDefendants(singletonList(defendant().withInitialHearing(InitialHearing.initialHearing().build()).build()))
                .withListNewHearing(HearingRequest.hearingRequest().build())
                .withMigrationSourceSystem(null)
                .withChannel(MCC)
                .build();
    }

    private Envelope<InitiateProsecution> envelope(final InitiateProsecution caseProsecution) {
        return envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), caseProsecution);
    }

    private static Stream<Arguments> parametersForCCCaseTest() {
        //Stream of isCivil, civilOffence, isExparte, chargedate
        return Stream.of(
                Arguments.of(true, Channel.MCC, CivilOffence.civilOffence().withIsExParte(true).build(), true, null),
                Arguments.of(true, Channel.MCC, CivilOffence.civilOffence().withIsExParte(false).build(), false, null),
                Arguments.of(false, Channel.MCC, null, null, CHARGE_DATE),
                Arguments.of(false, Channel.SPI, null, null, CHARGE_DATE),
                Arguments.of(false, Channel.CPPI, null, null, CHARGE_DATE),
                Arguments.of(true, Channel.CIVIL, CivilOffence.civilOffence().withIsExParte(true).build(), true, null),
                Arguments.of(true, Channel.CIVIL, CivilOffence.civilOffence().withIsExParte(false).build(), false, null)
        );
    }

    private static MigrationSourceSystem buildMigrationSourceSystem() {
        return migrationSourceSystem()
                .withMigrationSourceSystemCaseIdentifier(XHIBIT_IDENTIFIER)
                .withMigrationSourceSystemName(XHIBIT)
                .build();
    }

    private static Defendant buildDefendant(final Offence offence) {
        return defendant()
                .withOffences(singletonList(offence))
                .withAddress(Address.address().build())
                .build();
    }

    private static HearingRequest buildHearingRequestWithWeekCommencing(final HearingDateTimeType hearingDateTimeType, final LocalDate startDate) {
        return HearingRequest.hearingRequest()
                .withHearingDateTimeType(hearingDateTimeType)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(startDate.toString())
                        .build())
                .build();

    }

    private static HearingRequest buildHearingRequestWithFixedDateHearing(final HearingDateTimeType hearingDateTimeType, final ZonedDateTime earliestDate) {
        return HearingRequest.hearingRequest()
                .withHearingDateTimeType(hearingDateTimeType)
                .withEarliestStartDateTime(earliestDate)
                .build();
    }

}
