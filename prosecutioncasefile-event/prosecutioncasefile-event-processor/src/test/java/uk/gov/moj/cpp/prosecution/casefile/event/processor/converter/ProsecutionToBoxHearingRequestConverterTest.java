package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.integer;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.exception.ConverterException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
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