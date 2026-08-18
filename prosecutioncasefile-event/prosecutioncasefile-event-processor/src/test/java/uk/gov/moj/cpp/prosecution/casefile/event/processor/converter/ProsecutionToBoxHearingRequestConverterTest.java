package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.DATE_TO_BE_FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.FIXED;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType.WEEK_COMMENCING;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest.hearingRequest;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class ProsecutionToBoxHearingRequestConverterTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String INITIATION_CODE = "S";
    private static final String PROSECUTOR_CASE_REFERENCE = "Prosecutor Case Reference";
    private static final String ORIGINATING_ORGANISATION = "Originating Organisation";
    private static final String CPS_ORGANISATION = "A30AB00";
    private static final String OFFENCE_CODE = "A00PCD7073";
    private static final LocalDate ARREST_DATE = now().minusMonths(4);
    private static final LocalDate OFFENCE_COMMITTED_DATE = now().minusMonths(5);
    private static final LocalDate OFFENCE_CHARGE_DATE = now().minusMonths(4);
    private static final String SURNAME = "Bloggs";
    private static final LocalDate BIRTH_DATE = PAST_LOCAL_DATE.next();
    private static final String COURT_HEARING_LOCATION = "B016771";
    private static final LocalDate DEFAULT_DATE_OF_HEARING = now();
    private static final String TIME_OF_HEARING = "09:05:01.001";
    private static final String TIME_OF_HEARING_WITHOUT_MILLIS = "09:05:01";
    private static final String CUSTODY_STATUS = "B";
    private static final String FORENAME = "Joe";
    private static final LocalDate CASE_RECEIVED_DATE = now();
    private static final String DEFAULT_OU_CODE_L1_CODE = "B";
    private static final String BOOKED_SLOT_OU_CODE = "B10LY00";

    public static Stream<Arguments> dateOfHearingToExpectedApplicationDueDate() {
        return Stream.of(
                Arguments.of(now().plusDays(integer(14).next()), now()),
                Arguments.of(now().plusMonths(1), now().plusMonths(1).minusDays(14))
        );
    }

    public static Stream<Arguments> ouCodeL1CodeToExpectedJurisdiction() {
        return Stream.of(
                Arguments.of("B", MAGISTRATES),
                Arguments.of("C", CROWN)
        );
    }

    @Spy
    private final Clock clock = new StoppedClock(ZonedDateTime.now());

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private OrganisationUnitToCourtCentreConverter organisationUnitToCourtCentreConverter;

    @Mock
    private List<OrganisationUnitReferenceData> organisationUnits;

    @Mock
    private OrganisationUnitReferenceData organisationUnit;

    @Mock
    private CourtCentre courtCentre;

    @InjectMocks
    private ProsecutionToBoxHearingRequestConverter target;

    @BeforeEach
    public void setup() {
        given(referenceDataQueryService.retrieveOrganisationUnits(COURT_HEARING_LOCATION)).willReturn(organisationUnits);
        given(referenceDataQueryService.retrieveOrganisationUnits(BOOKED_SLOT_OU_CODE)).willReturn(organisationUnits);
        given(organisationUnits.get(0)).willReturn(organisationUnit);
        given(organisationUnit.getOucodeL1Code()).willReturn(DEFAULT_OU_CODE_L1_CODE);
        given(organisationUnitToCourtCentreConverter.convert(organisationUnit)).willReturn(courtCentre);
    }

    @ParameterizedTest
    @MethodSource("dateOfHearingToExpectedApplicationDueDate")
    public void shouldCalculateApplicationDueDateAndConvertProsecutionToBoxHearingRequest(final LocalDate dateOfHearing, final LocalDate expectedApplicationDueDate) {
        final Prosecution source = buildProsecution(dateOfHearing);

        final BoxHearingRequest result = target.convert(source);

        assertThat(result.getApplicationDueDate(), is(expectedApplicationDueDate.toString()));
        assertThat(result.getCourtCentre(), is(courtCentre));
        assertThat(result.getJurisdictionType(), is(MAGISTRATES));
    }

    @ParameterizedTest
    @MethodSource("ouCodeL1CodeToExpectedJurisdiction")
    public void shouldCalculateJurisdictionAndConvertProsecutionToBoxHearingRequest(final String ouCodeL1Code, final JurisdictionType expectedJurisdiction) {
        given(organisationUnit.getOucodeL1Code()).willReturn(ouCodeL1Code);
        final Prosecution source = buildProsecution_WithTimeOfHearingPatternWithoutMillis(DEFAULT_DATE_OF_HEARING);

        final BoxHearingRequest result = target.convert(source);

        assertThat(result.getApplicationDueDate(), is(now().toString()));
        assertThat(result.getCourtCentre(), is(courtCentre));
        assertThat(result.getJurisdictionType(), is(expectedJurisdiction));
    }

    @Test
    public void shouldThrowExceptionWhenNoDefendantsFoundInProsecution() {
        final Prosecution source = buildProsecutionWithoutDefendants();

        final ConverterException expectedException = assertThrows(ConverterException.class, () -> target.convert(source));

        assertThat(expectedException.getMessage(), is(format("Error converting from DefendantsParkedForSummonsApplicationApproval to InitiateCourtApplicationProceedings for case %s: no defendants found for case", CASE_ID)));
    }

    @Test
    public void shouldThrowExceptionWhenOrganisationUnitNotFoundForTheOuCode() {
        given(referenceDataQueryService.retrieveOrganisationUnits(COURT_HEARING_LOCATION)).willReturn(emptyList());
        final Prosecution source = buildProsecution();

        final ConverterException expectedException = assertThrows(ConverterException.class, () -> target.convert(source));

        assertThat(expectedException.getMessage(), is(format("Error converting from DefendantsParkedForSummonsApplicationApproval to InitiateCourtApplicationProceedings for case %s: no organisation unit found in reference data for ouCode %s", CASE_ID, COURT_HEARING_LOCATION)));
    }

    /**
     * DD-43173 AC-002 / AC-006: the court centre is the reference-data-enriched unit for
     * listNewHearing.bookedSlots[0].oucode — never the thin court centre the UI supplies — and a
     * magistrates' unit (oucodeL1Code "B") yields MAGISTRATES.
     */
    @Test
    public void shouldBuildBoxHearingFromFoundHearingBookedSlotOuCodeWhenThereIsNoInitialHearing() {
        final Prosecution source = buildFindAHearingProsecution(fixedHearing(now().plusMonths(1).atStartOfDay(systemDefault())));

        final BoxHearingRequest result = target.convert(source);

        verify(referenceDataQueryService).retrieveOrganisationUnits(BOOKED_SLOT_OU_CODE);
        assertThat(result.getCourtCentre(), is(courtCentre));
        assertThat(result.getJurisdictionType(), is(MAGISTRATES));
    }

    /**
     * DD-43173 AC-003: FIXED with an earliest start more than 14 days out — due date is that date minus 14.
     */
    @Test
    public void shouldCalculateApplicationDueDateFromEarliestStartDateTimeWhenHearingIsMoreThanTwoWeeksAway() {
        final ZonedDateTime earliestStart = now().plusMonths(1).atStartOfDay(systemDefault());
        final Prosecution source = buildFindAHearingProsecution(fixedHearing(earliestStart));

        final BoxHearingRequest result = target.convert(source);

        assertThat(result.getApplicationDueDate(), is(earliestStart.minusDays(14).toLocalDate().toString()));
    }

    /**
     * DD-43173 AC-004: FIXED with an earliest start inside 14 days — due date is floored at today.
     */
    @Test
    public void shouldFloorApplicationDueDateAtTodayWhenFoundHearingIsLessThanTwoWeeksAway() {
        final Prosecution source = buildFindAHearingProsecution(fixedHearing(now().plusDays(3).atStartOfDay(systemDefault())));

        final BoxHearingRequest result = target.convert(source);

        assertThat(result.getApplicationDueDate(), is(now().toString()));
    }

    /**
     * DD-43173 AC-003 variant: FIXED with no earliest start falls back to the listed start.
     */
    @Test
    public void shouldFallBackToListedStartDateTimeWhenFixedHearingHasNoEarliestStartDateTime() {
        final ZonedDateTime listedStart = now().plusMonths(2).atStartOfDay(systemDefault());
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withListedStartDateTime(listedStart)
                .withBookedSlots(singletonList(bookedSlot(BOOKED_SLOT_OU_CODE)))
                .build();

        final BoxHearingRequest result = target.convert(buildFindAHearingProsecution(listNewHearing));

        assertThat(result.getApplicationDueDate(), is(listedStart.minusDays(14).toLocalDate().toString()));
    }

    /**
     * DD-43173 AC-005: WEEK_COMMENCING derives the due date from weekCommencingDate.startDate.
     */
    @Test
    public void shouldCalculateApplicationDueDateFromWeekCommencingStartDate() {
        final LocalDate weekCommencing = now().plusMonths(1);
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(WEEK_COMMENCING)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(weekCommencing.toString())
                        .withDuration(1)
                        .build())
                .withBookedSlots(singletonList(bookedSlot(BOOKED_SLOT_OU_CODE)))
                .build();

        final BoxHearingRequest result = target.convert(buildFindAHearingProsecution(listNewHearing));

        assertThat(result.getApplicationDueDate(), is(weekCommencing.minusDays(14).toString()));
    }

    /**
     * DD-43173 AC-005 variant: WEEK_COMMENCING inside 14 days is floored at today.
     */
    @Test
    public void shouldFloorApplicationDueDateAtTodayWhenWeekCommencingIsLessThanTwoWeeksAway() {
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(WEEK_COMMENCING)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(now().plusDays(2).toString())
                        .withDuration(1)
                        .build())
                .withBookedSlots(singletonList(bookedSlot(BOOKED_SLOT_OU_CODE)))
                .build();

        final BoxHearingRequest result = target.convert(buildFindAHearingProsecution(listNewHearing));

        assertThat(result.getApplicationDueDate(), is(now().toString()));
    }

    /**
     * No usable hearing date at all still yields a box hearing, due today.
     */
    @Test
    public void shouldDefaultApplicationDueDateToTodayWhenFoundHearingHasNoUsableDate() {
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(DATE_TO_BE_FIXED)
                .withBookedSlots(singletonList(bookedSlot(BOOKED_SLOT_OU_CODE)))
                .build();

        final BoxHearingRequest result = target.convert(buildFindAHearingProsecution(listNewHearing));

        assertThat(result.getApplicationDueDate(), is(now().toString()));
        assertThat(result.getCourtCentre(), is(courtCentre));
    }

    /**
     * DD-43173 AC-015: neither an initial hearing nor a booked-slot oucode fails legibly with a
     * ConverterException naming the case, not a NullPointerException.
     */
    @Test
    public void shouldThrowConverterExceptionWhenThereIsNeitherAnInitialHearingNorABookedSlotOuCode() {
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withEarliestStartDateTime(now().plusMonths(1).atStartOfDay(systemDefault()))
                .build();

        final ConverterException expectedException = assertThrows(ConverterException.class,
                () -> target.convert(buildFindAHearingProsecution(listNewHearing)));

        assertThat(expectedException.getMessage(), is(format(
                "Error converting from DefendantsParkedForSummonsApplicationApproval to "
                        + "InitiateCourtApplicationProceedings for case %s: no initial hearing and no "
                        + "booked-slot oucode on listNewHearing", CASE_ID)));
    }

    /**
     * DD-43173 AC-015: a booked slot without an oucode is the same legible failure.
     */
    @Test
    public void shouldThrowConverterExceptionWhenTheBookedSlotHasNoOuCode() {
        final HearingRequest listNewHearing = hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withEarliestStartDateTime(now().plusMonths(1).atStartOfDay(systemDefault()))
                .withBookedSlots(singletonList(bookedSlot(null)))
                .build();

        assertThrows(ConverterException.class, () -> target.convert(buildFindAHearingProsecution(listNewHearing)));
    }

    /**
     * DD-43173 AC-016 (NFR-1): branch on the presence of initialHearing, never on channel. A payload that
     * carries both must still be built from the initial hearing exactly as it is today —
     * InitiateCCProsecutionApi enforces the exclusivity for MCC only.
     */
    @Test
    public void shouldHonourInitialHearingWhenThePayloadCarriesBothHearingSources() {
        final Prosecution withInitialHearing = buildProsecution(now().plusMonths(1));
        final Prosecution withBoth = prosecution()
                .withValuesFrom(withInitialHearing)
                .withChannel(MCC)
                .withListNewHearing(fixedHearing(now().plusYears(1).atStartOfDay(systemDefault())))
                .build();

        final BoxHearingRequest result = target.convert(withBoth);

        verify(referenceDataQueryService).retrieveOrganisationUnits(COURT_HEARING_LOCATION);
        verify(referenceDataQueryService, never()).retrieveOrganisationUnits(BOOKED_SLOT_OU_CODE);
        assertThat(result.getApplicationDueDate(), is(now().plusMonths(1).minusDays(14).toString()));
    }

    private HearingRequest fixedHearing(final ZonedDateTime earliestStartDateTime) {
        return hearingRequest()
                .withHearingDateTimeType(FIXED)
                .withEarliestStartDateTime(earliestStartDateTime)
                .withBookedSlots(singletonList(bookedSlot(BOOKED_SLOT_OU_CODE)))
                .build();
    }

    private RotaSlot bookedSlot(final String ouCode) {
        return RotaSlot.rotaSlot()
                .withOucode(ouCode)
                .withStartTime(now().plusMonths(1).atStartOfDay(systemDefault()))
                .build();
    }

    private Prosecution buildFindAHearingProsecution(final HearingRequest listNewHearing) {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withDateReceived(CASE_RECEIVED_DATE)
                        .build())
                .withChannel(MCC)
                .withListNewHearing(listNewHearing)
                .withDefendants(ImmutableList.of(defendant()
                        .withId(DEFENDANT_ID)
                        .withIndividual(individual()
                                .withPersonalInformation(personalInformation()
                                        .withFirstName(FORENAME)
                                        .withLastName(SURNAME).build())
                                .withSelfDefinedInformation(selfDefinedInformation()
                                        .withDateOfBirth(BIRTH_DATE)
                                        .build())
                                .build())
                        .withCustodyStatus(CUSTODY_STATUS)
                        .withOffences(singletonList(offence()
                                .withOffenceId(randomUUID())
                                .withOffenceSequenceNumber(1)
                                .withOffenceCode(OFFENCE_CODE)
                                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                                .withOffenceDateCode(2)
                                .build()))
                        .withInitiationCode(INITIATION_CODE)
                        .build()))
                .build();
    }

    private Prosecution buildProsecution() {
        return buildProsecution(DEFAULT_DATE_OF_HEARING);
    }

    private Prosecution buildProsecution(final LocalDate dateOfHearing) {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withDateReceived(CASE_RECEIVED_DATE)
                        .build())
                .withChannel(CPPI)
                .withDefendants(ImmutableList.of(defendant()
                        .withId(DEFENDANT_ID)
                        .withIndividual(individual()
                                .withPersonalInformation(personalInformation()
                                        .withFirstName(FORENAME)
                                        .withLastName(SURNAME).build())
                                .withSelfDefinedInformation(selfDefinedInformation()
                                        .withDateOfBirth(BIRTH_DATE)
                                        .build())
                                .withBailConditions("bailConditions")
                                .build())
                        .withInitialHearing(initialHearing()
                                .withDateOfHearing(LocalDates.to(dateOfHearing))
                                .withTimeOfHearing(TIME_OF_HEARING)
                                .withCourtHearingLocation(COURT_HEARING_LOCATION)
                                .build())
                        .withCustodyStatus(CUSTODY_STATUS)
                        .withOffences(singletonList(offence()
                                .withOffenceId(randomUUID())
                                .withOffenceSequenceNumber(1)
                                .withArrestDate(ARREST_DATE)
                                .withOffenceCode(OFFENCE_CODE)
                                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                                .withChargeDate(OFFENCE_CHARGE_DATE)
                                .withOffenceDateCode(2)
                                .build()))
                        .withInitiationCode(INITIATION_CODE)
                        .build()))
                .build();
    }

    private Prosecution buildProsecution_WithTimeOfHearingPatternWithoutMillis(final LocalDate dateOfHearing) {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withDateReceived(CASE_RECEIVED_DATE)
                        .build())
                .withChannel(CPPI)
                .withDefendants(ImmutableList.of(defendant()
                        .withId(DEFENDANT_ID)
                        .withIndividual(individual()
                                .withPersonalInformation(personalInformation()
                                        .withFirstName(FORENAME)
                                        .withLastName(SURNAME).build())
                                .withSelfDefinedInformation(selfDefinedInformation()
                                        .withDateOfBirth(BIRTH_DATE)
                                        .build())
                                .withBailConditions("bailConditions")
                                .build())
                        .withInitialHearing(initialHearing()
                                .withDateOfHearing(LocalDates.to(dateOfHearing))
                                .withTimeOfHearing(TIME_OF_HEARING_WITHOUT_MILLIS)
                                .withCourtHearingLocation(COURT_HEARING_LOCATION)
                                .build())
                        .withCustodyStatus(CUSTODY_STATUS)
                        .withOffences(singletonList(offence()
                                .withOffenceId(randomUUID())
                                .withOffenceSequenceNumber(1)
                                .withArrestDate(ARREST_DATE)
                                .withOffenceCode(OFFENCE_CODE)
                                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                                .withChargeDate(OFFENCE_CHARGE_DATE)
                                .withOffenceDateCode(2)
                                .build()))
                        .withInitiationCode(INITIATION_CODE)
                        .build()))
                .build();
    }

    private Prosecution buildProsecutionWithoutDefendants() {
        return prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withDateReceived(CASE_RECEIVED_DATE)
                        .build())
                .withChannel(CPPI)
                .withDefendants(ImmutableList.of())
                .build();
    }
}