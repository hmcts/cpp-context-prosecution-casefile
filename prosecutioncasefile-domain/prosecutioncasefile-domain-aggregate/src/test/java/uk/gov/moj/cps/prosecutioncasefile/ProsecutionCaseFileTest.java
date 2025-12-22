package uk.gov.moj.cps.prosecutioncasefile;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.LocalDate.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;
import static uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails.summonsApplicationApprovedDetails;
import static uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationRejectedDetails.summonsApplicationRejectedDetails;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData.bailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData.custodyStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem.defendantProblem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData.prosecutorsReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData.summonsCodeReferenceData;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.APPLICATION_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.APPLICATION_ID_2;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ARREST_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ARREST_SUMMON_NUMBER;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.BIRTH_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CASE_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.COURT_HEARING_LOCATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CPS_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CUSTODY_STATUS;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DATE_OF_HEARING;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DATE_OF_HEARING_IN_PAST;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.EXTERNAL_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.EXTERNAL_ID_2;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.EXTERNAL_ID_3;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FIFTH_DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FITH_SURNAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FORENAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FOURTH_DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.FOURTH_SURNAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_CHARGE_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_CODE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_START_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_COST;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_ONE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_THREE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_TWO;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SECOND_BIRTH_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SECOND_DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SECOND_FORENAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SECOND_SURNAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SUMMONS_INITIATION_CODE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SURNAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.THIRD_BIRTH_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.THIRD_DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.THIRD_FORENAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.THIRD_SURNAME;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.URN;

import com.google.common.collect.Lists;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.CaseReceivedWithDuplicateDefendants;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.DefendantValidationPassed;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.*;
import uk.gov.moj.cpp.prosecution.casefile.event.*;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CivilFees;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsPersonDefendantDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentTypeAccessReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MojOffences;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutionCaseSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SummonsCodeReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OffenceDataRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.InitiationTypesRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Matchers;
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

// TODO: remove LENIENT strictness
@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileTest {

    private static final String VALID_CASE_MARKER_CODE = "AB";
    private static final String INVALID_CASE_MARKER_CODE = "BC";
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    @InjectMocks
    private final InitiationTypesRefDataEnricher initiationTypesRefDataEnricher = new InitiationTypesRefDataEnricher();

    @Spy
    @InjectMocks
    private final OffenceDataRefDataEnricher offenceDataRefDataEnricher = new OffenceDataRefDataEnricher();

    private ProsecutionCaseFile prosecutionCaseFile;

    private UUID offenceId;

    private final boolean isCivil = true;

    public static Stream<Arguments> validChannels() {
        return Stream.of(
                Arguments.of(CPPI),
                Arguments.of(MCC),
                Arguments.of(SPI)
        );
    }

    public static Stream<Arguments> nonSpiChannels() {
        return Stream.of(
                Arguments.of(CPPI),
                Arguments.of(MCC)
        );
    }

    public static Stream<Arguments> allowSummonsCodeWithSubsequentNonSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("S","S"),
                Arguments.of("S","Q"),
                Arguments.of("S","C"),
                Arguments.of("S","R"),
                Arguments.of("S","O"),
                Arguments.of("S","Z")
        );
    }

    public static Stream<Arguments> allowWithSubsequentNonSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("C","C"),
                Arguments.of("C","Q"),
                Arguments.of("C","R"),
                Arguments.of("C","O"),
                Arguments.of("Q","C"),
                Arguments.of("Q","Q"),
                Arguments.of("Q","R"),
                Arguments.of("Q","O"),
                Arguments.of("R","C"),
                Arguments.of("R","Q"),
                Arguments.of("R","R"),
                Arguments.of("R","O")
        );
    }
    public static Stream<Arguments> rejectSubsequentSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("C","J"),
                Arguments.of("S","J"),
                Arguments.of("Q","J"),
                Arguments.of("R","J"),
                Arguments.of("O","J"),
                Arguments.of("Z","J")
        );
    }

    public static Stream<Arguments> rejectSubsequentNonSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("J","C"),
                Arguments.of("J","Q"),
                Arguments.of("J","R"),
                Arguments.of("J","O"),
                Arguments.of("J","Z")
        );
    }

    public static Stream<Arguments> addDefendantAfterSummonsRejectedWithSubsequentNonSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("C","S","Q"),
                Arguments.of("Q","S","C")
        );
    }

    public static Stream<Arguments> defendantsParkedForSummonsApprovalFollowingNonSjpInitiationCodes() {
        return Stream.of(
                Arguments.of("C","S"),
                Arguments.of("Q","S"),
                Arguments.of("R","S")
        );
    }

    @BeforeEach
    public void setup() {
        when(referenceDataQueryService.isInitiationCodeValid(any())).thenReturn(true);
        when(referenceDataQueryService.retrieveOrganisationUnits(COURT_HEARING_LOCATION)).thenReturn(buildOrganisationUnits());
        when(referenceDataQueryService.retrieveCustodyStatuses()).thenReturn(buildCustodyStatuses());
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(buildBailStatuses());
        when(referenceDataQueryService.retrieveProsecutors(any(String.class))).thenReturn(getProsecutorsReferenceData());
        when(referenceDataQueryService.getProsecutorById(any(UUID.class))).thenReturn(getProsecutorsReferenceData());
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(anyString())).thenReturn(ofNullable(organisationUnitWithCourtroomReferenceData().build()));
        when(referenceDataQueryService.retrieveSummonsCodes()).thenReturn(buildSummonsCodeReferenceData());
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(buildCaseMarkers());
        when(referenceDataQueryService.getInitiationCodes()).thenReturn(of("J", "C", SUMMONS_INITIATION_CODE));
        when(referenceDataQueryService.retrieveOffenceData(any(Offence.class), any(String.class))).thenReturn(of(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).build()));
        when(referenceDataQueryService.retrieveOffencesByType("VP")).thenReturn(Collections.singletonList(MojOffences.mojOffences().withOffenceId(offenceId).withCjsOffenceCode(OFFENCE_CODE).build()));

        prosecutionCaseFile = new ProsecutionCaseFile();

        offenceId = randomUUID();
    }

    @Test
    public void shouldCreateCCCaseWithWarningsForCPPI() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate.minusMonths(4), PROSECUTOR_DEFENDANT_REFERENCE_TWO))), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));

        final Optional<CcCaseReceivedWithWarnings> firstMatchingEvent = getFirstMatching(eventList, CcCaseReceivedWithWarnings.class);

        assertThat(firstMatchingEvent.get().getDefendantWarnings().size(), is(1));
        assertThat(firstMatchingEvent.get().getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_ONE));
    }

    @Test
    public void shouldRaiseCaseValidationWarningsWithProsecutorDefendantReference() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendantWithAsnAndOffence(offenceCommittedDate, offenceChargeDate,
                        ARREST_SUMMON_NUMBER))),
                new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));

        final Optional<CcCaseReceivedWithWarnings> firstMatchingEvent = getFirstMatching(eventList, CcCaseReceivedWithWarnings.class);

        assertThat(firstMatchingEvent.get().getDefendantWarnings().size(), is(1));
        assertThat(firstMatchingEvent.get().getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(ARREST_SUMMON_NUMBER));
    }

    @Test
    public void shouldRaiseCaseRejectedWWithProsecutorDefendantReference() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), CPPI, "C");
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(referenceData).build());
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), CPPI, "C");

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());

        final Optional<CcProsecutionRejected> ccProsecutionRejected = getFirstMatching(eventList, CcProsecutionRejected.class);

        assertThat(ccProsecutionRejected.isPresent(), is(true));
        assertThat(ccProsecutionRejected.get().getCaseErrors(), hasSize(1));
        assertThat(ccProsecutionRejected.get().getDefendantErrors().size(), is(1));
        assertThat(ccProsecutionRejected.get().getDefendantErrors().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_TWO));
    }

    @Test
    public void shouldCreateCCCaseWithCivilFeesAndWarningsForCPPI() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceDataAndCivilFees(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate.minusMonths(4), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), CPPI, false, Collections.emptyList()), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));

        final Optional<CcCaseReceivedWithWarnings> firstMatchingEvent = getFirstMatching(eventList, CcCaseReceivedWithWarnings.class);

        assertThat(firstMatchingEvent.get().getDefendantWarnings().size(), is(1));
        assertThat(firstMatchingEvent.get().getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_ONE));
    }

    @ParameterizedTest
    @MethodSource("provideParametersForCCCaseTest")
    void shouldCreateCCCaseWithCivilFeesAndWarningsForCivil(String paymentRef, String actualFeeStatus, String expectedFeeStatus) {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        List<CivilFees> civilFees = new ArrayList<>();
        //if (!paymentRef.isEmpty()) {
            civilFees.add(CivilFees.civilFees().withPaymentReference(paymentRef).withFeeStatus(actualFeeStatus).build());
      //  }

        final ProsecutionWithReferenceData prosecutionWithReferenceDataAndCivilFees = getProsecutionWithReferenceDataAndCivilFees(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate.minusMonths(4), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), CIVIL, true, civilFees);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataAndCivilFees, new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));

        final Optional<CcCaseReceivedWithWarnings> firstMatchingEvent = getFirstMatching(eventList, CcCaseReceivedWithWarnings.class);

        assertThat(firstMatchingEvent.get().getDefendantWarnings().size(), is(1));
        assertThat(firstMatchingEvent.get().getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_ONE));

        assertCivilFees(firstMatchingEvent, expectedFeeStatus);
    }

    private void assertCivilFees(Optional<CcCaseReceivedWithWarnings> firstMatchingEvent, String expectedStatus) {
        String initialFeeStatus = firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getCaseDetails().getFeeStatus();
        assertThat(initialFeeStatus, is(expectedStatus));
    }

    private static Stream<Arguments> provideParametersForCCCaseTest() {
        return Stream.of(
                Arguments.of(null, null, FeeStatus.OUTSTANDING.name()),
                Arguments.of("paymentRef01", null, FeeStatus.SATISFIED.name()),
                Arguments.of("paymentRef02", FeeStatus.NOT_APPLICABLE.name(), FeeStatus.NOT_APPLICABLE.name())
        );
    }

    @Test
    public void shouldCreateCCCaseWithWarningsForCPPIWithMultipleDefendants() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate.plusMonths(8), PROSECUTOR_DEFENDANT_REFERENCE_TWO))), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(instanceOf(CcCaseReceivedWithWarnings.class)));

        final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = (CcCaseReceivedWithWarnings) eventList.get(0);
        assertThat(ccCaseReceivedWithWarnings.getDefendantWarnings().size(), is(2));
        assertThat(ccCaseReceivedWithWarnings.getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_ONE));
        assertThat(ccCaseReceivedWithWarnings.getDefendantWarnings().get(1).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_TWO));
    }

    @Test
    public void shouldCreateCCCaseWithOffenceNotInEffectWarningForCPPI() {

        final String offenceStartDateRefData = of(2018, 5, 2).toString();
        final String offenceEndDateRefData = of(2018, 12, 2).toString();

        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 6, 2);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                buildDefendantWithOffence(offenceCommittedDate.plusMonths(4), offenceChargeDate.plusMonths(2), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), offenceStartDateRefData, offenceEndDateRefData);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(instanceOf(CcCaseReceivedWithWarnings.class)));

        final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = (CcCaseReceivedWithWarnings) eventList.get(0);
        assertThat(ccCaseReceivedWithWarnings.getDefendantWarnings().size(), is(1));

        final DefendantProblem defendantProblem = ccCaseReceivedWithWarnings.getDefendantWarnings().get(0);
        assertThat(defendantProblem.getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_ONE));
        assertThat(defendantProblem.getProblems().get(0).getCode(), is(OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE));
    }

    @Test
    public void shouldNotCreateCCCaseWithWarningsForSPI() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(3));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(2), is(instanceOf(CcCaseReceived.class)));
    }

    @Test
    public void shouldRaiseDefendantsParkedForSummonsApplicationApprovalEventForSubsequentMessages() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, SUMMONS_INITIATION_CODE);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        final UUID externalIdForSecondMessage = randomUUID();
        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, randomAlphabetic(10))), SPI, SUMMONS_INITIATION_CODE, externalIdForSecondMessage);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        final Optional<DefendantsParkedForSummonsApplicationApproval> firstMatchingEvent = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(eventList.size(), is(2));
        assertThat(firstMatchingEvent.isPresent(), is(true));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getExternalId(), is(externalIdForSecondMessage));
    }

    @Test
    public void shouldUpdateCaseWhenNoAssociatedDefendant() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant("          ", SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames("          ").withOucode(ouCode).withSurname(SURNAME).build();

        prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);

        final Stream<Object> objectStream = prosecutionCaseFile.caseUpdated();
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldExpirePendingIDPCMaterial() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant("          ", SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames("          ").withOucode(ouCode).withSurname(SURNAME).build();

        prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        final Stream<Object> objectStream = prosecutionCaseFile.expirePendingIdpcMaterial(materialId, ZonedDateTime.now());
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(instanceOf(IdpcMaterialRejected.class)));

    }


    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventMultipleSurnameMatchAndEmptyFirstNameAndValidDOB() {

        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant("          ", SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames("          ").withOucode(ouCode).withSurname(SURNAME).build();

        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventSurnameEmptyAndValidFirstnameAndDOB() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, "       ", BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(null).withOucode(ouCode).withSurname("       ").build();

        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventMultipleSurnameMatchButMissingFirstNameAndDOB() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(null).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventMultipleSurnameMatchButMissingFirstName() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(null).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventWhenNoMatch() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(null).withForenames(FORENAME + 2).withOucode(ouCode).withSurname(SURNAME + 2).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventWhenMultipleSurnameFirstDOBNameMatch() {

        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);

        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();

        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventWhenMultipleSurnameFirstNameMatchButNoOtherMatch() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(null).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventWhenMultipleSurnameMatchButNoOtherMatch() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(SECOND_FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(null).withForenames(FORENAME + 1).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchPendingEventWhenOnlyDateOfBirthMatched() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(MCC, false);
        prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(null).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME + 1).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatchPending.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchedEventWhenSurnameMatched() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(MCC, false);
        prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId(), URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchedEventWhenSurnameMatchWithWhiteSpaceCharacterAtStartAndEnd() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(MCC, false);
        prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname("  " + SURNAME + "  ").build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(prosecutionWithReferenceData.getProsecution().getCaseDetails().getCaseId(), URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchedEventWhenFornamesMatch() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(SECOND_FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(SECOND_BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchedEventWhenFornamesMatchedWithWhiteSpaceCharacherAtStart() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(SECOND_FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(SECOND_BIRTH_DATE.toString()).withForenames("  " + FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
    }

    @Test
    public void shouldRaiseIdpcDefendantMatchEventWhenDOBMatched() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(FORENAME, SURNAME, SECOND_BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(SECOND_BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
    }

    @Test
    public void shouldRaiseIdpcMaterialReceivedEvent() {
        final UUID materialId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(SECOND_BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.populateIdpcMaterialReceived(CASE_ID, URN, materialId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcMaterialReceived.class);
    }

    @Test
    public void shouldRaiseDefendantIdpcAlreadyExitsEventWhenDefendantIdpcIsPresent() {
        prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, SECOND_BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "C"), new ArrayList<>(), new ArrayList<>(),
                referenceDataQueryService);
        final UUID fileServiceId = fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf");
        final String materialType = "IDPC";
        final String ouCode = "B01AF00";
        final Defendant defendant = new Defendant.Builder().withDefendantId(DEFENDANT_ID).withDob(BIRTH_DATE.toString()).withForenames(FORENAME).withOucode(ouCode).withSurname(SURNAME).build();
        final Stream<Object> objectStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, fileServiceId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(objectStream, IdpcDefendantMatched.class);
        final Stream<Object> defendantIdpcAlreadyExistsStream = prosecutionCaseFile.addIdpcCaseMaterial(CASE_ID, URN, fileServiceId, materialType, defendant);
        assertThatTheEventReturnedIsOfType(defendantIdpcAlreadyExistsStream, DefendantIdpcAlreadyExists.class);
    }

    @Test
    public void shouldRaiseBulkScanMaterialRejectedEvent() {
        final ZonedDateTime now = ZonedDateTime.now();
        final UUID fileStoreId = randomUUID();
        prosecutionCaseFile.addMaterial(randomUUID(), "B01AF00", randomUUID().toString(),
                new Material("SJPN", fileStoreId, "pdf", false),
                referenceDataQueryService, false, null);
        final Stream<Object> objectStream = prosecutionCaseFile.expireBulkScanPendingMaterial(fileStoreId, now);
        assertThatTheEventReturnedIsOfType(objectStream, BulkscanMaterialRejected.class);
    }

    @Test
    public void shouldRaiseMaterialAddedEventsWhenCaseIsAccepted() {
        ReflectionUtil.setField(prosecutionCaseFile, "prosecutionAccepted", TRUE);
        final UUID fileStoreId = randomUUID();
        final Stream<Object> objectStream = prosecutionCaseFile.addMaterials(randomUUID(), "B01AF00", randomUUID().toString(),
                singletonList(new Material("SJPN", fileStoreId, "pdf", false)),
                referenceDataQueryService, false, null);
        assertThatTheEventReturnedIsOfType(objectStream, MaterialAdded.class);
    }

    @Test
    public void shouldRaiseMaterialPendingEventsWhenCaseIsNotAccepted() {
        ReflectionUtil.setField(prosecutionCaseFile, "prosecutionAccepted", FALSE);
        final UUID fileStoreId = randomUUID();
        final Stream<Object> objectStream = prosecutionCaseFile.addMaterials(randomUUID(), "B01AF00", randomUUID().toString(),
                asList(new Material("SJPN", fileStoreId, "pdf", false), new Material("PLEA", fileStoreId, "pdf", false)),
                referenceDataQueryService, false, null);
        assertThatTheEventReturnedIsOfType(objectStream, MaterialPending.class, 2);
    }

    @Test
    public void shouldRaiseBulkScanMaterialRejectedEventV2() {
        final ZonedDateTime now = ZonedDateTime.now();
        final UUID fileStoreId = randomUUID();
        prosecutionCaseFile.addMaterialV2(AddMaterialCommonV2.addMaterialCommonV2()
                        .withMaterial(fileStoreId)
                        .build(),
                referenceDataQueryService);
        final Stream<Object> objectStream = prosecutionCaseFile.expireBulkScanPendingMaterial(fileStoreId, now);
        assertThatTheEventReturnedIsOfType(objectStream, BulkscanMaterialRejected.class);
    }

    @ParameterizedTest
    @MethodSource("validChannels")
    public void shouldRaiseDefendantsParkedForSummonsApplicationApprovalEventWhenInitiationCodeIsS(final Channel validChannel) {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final LocalDate offenceChargeDate = of(2018, 11, 2);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, offenceChargeDate, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), validChannel, SUMMONS_INITIATION_CODE), newArrayList(), newArrayList(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(SPI == validChannel ? 3 : 1));
        final Optional<DefendantsParkedForSummonsApplicationApproval> firstMatching = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatching.get(), notNullValue());
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getExternalId(), is(EXTERNAL_ID));
    }

    @Test
    public void shouldRaiseProsecutionCaseUnSupportedEventWhenMultipleDefendantsProsecutionReceivedForSjpCaseViaSpiChannel() {
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(SPI, true);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), is(instanceOf(ProsecutionCaseUnsupported.class)));
        final ProsecutionCaseUnsupported eventObject = (ProsecutionCaseUnsupported) eventList.get(0);
        assertThat(eventObject.getErrorMessage(), containsString("Multiple Defendants Found"));
    }

    @ParameterizedTest
    @MethodSource("rejectSubsequentNonSjpInitiationCodes")
    public void shouldRaiseProsecutionCaseUnsupportedEventWhenMultipleMessagesReceivedForSjpCaseViaSpiChannel(final String initialInitiationCode, final String newInitiationCode) {

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getSjpProsecutionWithReferenceData(initialInitiationCode);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), newInitiationCode);
        final Stream<Object> newObjectStream = prosecutionCaseFile.receiveCCCase(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = newObjectStream.collect(toList());
        final ProsecutionCaseUnsupported eventObject = (ProsecutionCaseUnsupported) eventList.get(0);

        assertThat(eventList.size(), is(1));
        assertThat(eventObject.getErrorMessage(), containsString(format("Original Case with initiation code %s has received a case update with different case initiation code %s", initialInitiationCode, newInitiationCode)));
        assertThat(eventObject.getUrn(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference()));
        assertThat(eventObject.getPoliceSystemId(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId()));
        assertThat(eventObject.getChannel(), is(SPI));
        assertThat(eventObject.getExternalId(), is(subsequentProsecutionWithReferenceData.getExternalId()));

        assertThat(eventList.size(), is(1));

    }
    @Test
    public void shouldRaiseProsecutionCaseUnsupportedEventWhenDuplicateMessageReceivedForSjpWithNewDefendant() {

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getSjpProsecutionWithReferenceData("J");

        final Stream<Object> objectStream = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), "J");
        final Stream<Object> newObjectStream = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = newObjectStream.collect(toList());
        final ProsecutionCaseUnsupported eventObject = (ProsecutionCaseUnsupported) eventList.get(0);

        assertThat(eventList.size(), is(1));
        assertThat(eventObject.getErrorMessage(), containsString("Multiple Defendants Found"));
        assertThat(eventObject.getPoliceSystemId(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId()));
        assertThat(eventObject.getChannel(), is(SPI));
        assertThat(eventObject.getExternalId(), is(subsequentProsecutionWithReferenceData.getExternalId()));
        assertThat(eventList.size(), is(1));
    }

    public void shouldNotRaiseProsecutionCaseUnsupportedEventWhenMultipleMessagesReceivedForSjpCaseViaSpiChannel(final String initialInitiationCode, final String newInitiationCode) {

        final ProsecutionWithReferenceData prosecutionWithReferenceData = getSjpProsecutionWithReferenceData(initialInitiationCode);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> events = objectStream.collect(toList());
        assertThat(events.size(), is(1));

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), newInitiationCode);
        final Stream<Object> newObjectStream = prosecutionCaseFile.receiveCCCase(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = newObjectStream.collect(toList());
        final ProsecutionCaseUnsupported eventObject = (ProsecutionCaseUnsupported) eventList.get(0);

        assertThat(eventList.size(), is(1));
        assertThat(eventObject.getErrorMessage(), containsString(format("Original Case with initiation code %s has received a case update with different case initiation code %s", initialInitiationCode, newInitiationCode)));
        assertThat(eventObject.getUrn(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference()));
        assertThat(eventObject.getPoliceSystemId(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId()));
        assertThat(eventObject.getChannel(), is(SPI));
        assertThat(eventObject.getExternalId(), is(subsequentProsecutionWithReferenceData.getExternalId()));

        assertThat(eventList.size(), is(1));

    }

    @ParameterizedTest
    @MethodSource("rejectSubsequentSjpInitiationCodes")
    public void shouldRaiseProsecutionCaseUnsupportedWhenSubsequentSJPMessageWithDifferentInitiationCodeReceived(final String initialInitiationCode, final String newInitiationCode) {
        final ProsecutionWithReferenceData initialProsecutionReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, initialInitiationCode);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialProsecutionReferenceData).build());

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), newInitiationCode);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveSjpProsecution(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        final ProsecutionCaseUnsupported eventObject = (ProsecutionCaseUnsupported) eventList.get(0);

        assertThat(eventList.size(), is(1));
        assertThat(eventObject.getErrorMessage(), containsString("Multiple Defendants Found"));
        assertThat(eventObject.getUrn(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference()));
        assertThat(eventObject.getPoliceSystemId(), is(subsequentProsecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId()));
        assertThat(eventObject.getChannel(), is(SPI));
        assertThat(eventObject.getExternalId(), is(subsequentProsecutionWithReferenceData.getExternalId()));

        assertThat(eventList.size(), is(1));
    }

    @ParameterizedTest
    @MethodSource("allowSummonsCodeWithSubsequentNonSjpInitiationCodes")
    public void shouldNotRaiseProsecutionCaseUnsupportedWhenSubsequentSPiMessageISDifferentToSummonsInitiationCode(final String initialInitiationCode
            , String newInitiationCode) {
        final ProsecutionWithReferenceData initialProsecutionReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, initialInitiationCode);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialProsecutionReferenceData).build());

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), newInitiationCode);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(2));
    }

    @ParameterizedTest
    @MethodSource("allowWithSubsequentNonSjpInitiationCodes")
    public void shouldAddDefendantWhenSubsequentSPiMessageIsDifferentToInitialInitiationCode(final String initialInitiationCode
            , String newInitiationCode) {

        final ProsecutionWithReferenceData initialProsecutionReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, initialInitiationCode)), SPI, initialInitiationCode);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialProsecutionReferenceData).build());

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, newInitiationCode)), newInitiationCode);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(2));
        ProsecutionDefendantsAdded prosecutionDefendantsAdded = (ProsecutionDefendantsAdded) eventList.get(1);
        assertThat(prosecutionDefendantsAdded.getDefendants().size(), is(1));
        assertThat(prosecutionDefendantsAdded.getDefendants().get(0).getInitiationCode(), is(newInitiationCode));
    }
    @ParameterizedTest
    @MethodSource("defendantsParkedForSummonsApprovalFollowingNonSjpInitiationCodes")
    public void shouldParkDefendantsForSummonsApprovalWhenSubsequentSPiMessageIsSummonsInitiationCode(final String initialInitiationCode
            , String newInitiationCode) {
        // given
        final ProsecutionWithReferenceData initialProsecutionReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, initialInitiationCode)), SPI, initialInitiationCode);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialProsecutionReferenceData).build());

        final ProsecutionWithReferenceData subsequentProsecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, newInitiationCode)), newInitiationCode);

        //when
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentProsecutionWithReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());

        //then
        assertThat(eventList.size(), is(2));
        DefendantsParkedForSummonsApplicationApproval defendantsParkedForSummonsApplicationApproval = (DefendantsParkedForSummonsApplicationApproval) eventList.get(1);
        assertThat(defendantsParkedForSummonsApplicationApproval.getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getInitiationCode(), is("S"));
    }

    @ParameterizedTest
    @MethodSource("addDefendantAfterSummonsRejectedWithSubsequentNonSjpInitiationCodes")
    public void shouldAddDefendantWhenSummonsApplicationWithEarlierApplicationWasRejectedForSameCaseReceivedWithDifferentInitiationCodeViaSpiChannel(
            final String initialInitiationCode, final String summonsInitiationCode, final String newInitiationCode) {

        // c -s- sr- q  // q - s sr - c
        final ProsecutionWithReferenceData initialProsecutionReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, initialInitiationCode)), SPI, initialInitiationCode);
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialProsecutionReferenceData).build());

        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, summonsInitiationCode)), SPI, summonsInitiationCode);

        final Stream<Object> objectStreamNew = prosecutionCaseFile.receiveCCCase(referenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList1 = objectStreamNew.collect(toList());
        assertThat(eventList1.size(), is(2));
        DefendantsParkedForSummonsApplicationApproval defendantsParkedForSummonsApplicationApproval = (DefendantsParkedForSummonsApplicationApproval) eventList1.get(1);
        assertThat(defendantsParkedForSummonsApplicationApproval.getProsecutionWithReferenceData().getProsecution().getDefendants().size(), is(1));

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, referenceData, emptyList()));
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID, newArrayList(fromString(SECOND_DEFENDANT_ID)), summonsRejectedOutcome().withReasons(ImmutableList.of("Rejection Reason")).build()));

        final String updatedSurname = STRING.next();
        final ProsecutionWithReferenceData subsequentReferenceDataForSameDefendant = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, updatedSurname, BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, newInitiationCode)), SPI, newInitiationCode, EXTERNAL_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceDataForSameDefendant, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(2));
        ProsecutionDefendantsAdded prosecutionDefendantsAdded = (ProsecutionDefendantsAdded) eventList.get(1);
        assertThat(prosecutionDefendantsAdded.getDefendants().size(), is(1));
        assertThat(prosecutionDefendantsAdded.getDefendants().get(0).getInitiationCode(), is(newInitiationCode));
    }
    @Test
    public void shouldAddNewDefendantWhenSummonsApplicationIsApprovedToExistingCaseWithDifferentInitiationCodeViaSpiChannel() {
        // Given first SummonsMessage with 2 defendants
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE);

        final Stream<Object> objectStream1 = prosecutionCaseFile.receiveCCCase(firstMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList1 = objectStream1.collect(toList());
        assertThat(eventList1, hasSize(3));
        DefendantsParkedForSummonsApplicationApproval defendantsParkedForSummonsApplicationApproval = (DefendantsParkedForSummonsApplicationApproval) eventList1.get(2);
        assertThat(defendantsParkedForSummonsApplicationApproval.getProsecutionWithReferenceData().getProsecution().getDefendants().size(), is(2));
        // approve the application
        final SummonsApplicationApprovedDetails secondSummonsApplicationApprovedDetails = getSummonsApplicationApprovedDetails(defendantsParkedForSummonsApplicationApproval.getApplicationId());
        final Stream<Object> objectStream = prosecutionCaseFile.approveCaseDefendants(secondSummonsApplicationApprovedDetails, of(), of(), isCivil);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));


        // add a defendant
        final ProsecutionWithReferenceData sameMessageWithDifferentInitiationCode = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(THIRD_FORENAME, THIRD_SURNAME, THIRD_BIRTH_DATE, THIRD_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_THREE, "C")), SPI, "C");

        final Stream<Object> objectStream2 = prosecutionCaseFile.receiveCCCase(sameMessageWithDifferentInitiationCode, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList2 = objectStream2.collect(toList());
        assertThat(eventList2, hasSize(2));
        assertThat(eventList2.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList2.get(1), is(instanceOf(ProsecutionDefendantsAdded.class)));
    }

    @Test
    public void shouldRaiseNewCCCaseForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaSpiChannel() {
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "S")), SPI, "S");
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, referenceData, emptyList()));
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID, newArrayList(fromString(DEFENDANT_ID)), summonsRejectedOutcome().withReasons(ImmutableList.of("Rejection Reason")).build()));

        final String updatedSurname = STRING.next();
        final ProsecutionWithReferenceData subsequentReferenceDataForSameDefendant = getProsecutionWithReferenceData(
                of(buildDefendantWithInitiationCode(FORENAME, updatedSurname, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "Q")), SPI, "Q", EXTERNAL_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceDataForSameDefendant, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(2));
        final Optional<CcCaseReceived> firstMatching = getFirstMatching(eventList, CcCaseReceived.class);
        assertThat(firstMatching.isPresent(), is(true));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getInitiationCode(), is("Q"));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(DEFENDANT_ID));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getIndividual().getPersonalInformation().getFirstName(), is(FORENAME));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getIndividual().getPersonalInformation().getLastName(), is(updatedSurname));
    }


    @Test
    public void shouldAddSpiChannelDefendantsWhenSubsequentMessagesReceivedWithKnownAndUniqueDifferentDefendants() {
        final ProsecutionWithReferenceData initialCaseMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialCaseMessage).build());
        final String thirdDefendantId = randomUUID().toString();
        final ProsecutionWithReferenceData subsequentCaseMessage = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO),
                        buildDefendant(FORENAME, SURNAME, BIRTH_DATE, thirdDefendantId, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentCaseMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());

        final Optional<ProsecutionDefendantsAdded> newDefendantMatchingEvent = getFirstMatching(eventList, ProsecutionDefendantsAdded.class);
        assertThat(newDefendantMatchingEvent.isPresent(), is(true));
        assertThat(newDefendantMatchingEvent.get(), notNullValue());
        assertThat(newDefendantMatchingEvent.get().getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));

        final Optional<CaseReceivedWithDuplicateDefendants> duplicateDefendantMatchingEvent = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        assertThat(duplicateDefendantMatchingEvent.isPresent(), is(true));
        assertThat(duplicateDefendantMatchingEvent.get(), notNullValue());
        assertThat(duplicateDefendantMatchingEvent.get().getDefendants().get(0).getId(), is(thirdDefendantId));
    }

    @Test
    public void shouldRaiseCaseReceivedWithDuplicateDefendantsWhenSubsequentSPIMessagesReceivedWithKnownDefendants() {
        final ProsecutionWithReferenceData initialReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(initialReferenceData).build());
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.get(0), is(instanceOf(CaseReceivedWithDuplicateDefendants.class)));
    }

    @Test
    public void shouldNotDeduplicateDefendantsWhenCPPIMessageReceivedWithDuplicateDefendants() {
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)
                ), CPPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(referenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.get(0), is(instanceOf(CcCaseReceived.class)));

        final CcCaseReceived ccCaseReceived = (CcCaseReceived) eventList.get(0);
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = ccCaseReceived.getProsecutionWithReferenceData().getProsecution().getDefendants();
        assertThat(defendants.size(), is(2));
        assertThat(defendants.get(0).getId(), is(DEFENDANT_ID));
        assertThat(defendants.get(1).getId(), is(SECOND_DEFENDANT_ID));
    }

    @Test
    public void shouldRaiseDefendantValidationFailedWhenSubsequentSPIMessagesReceivedWithInvalidDefendants() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(referenceData).build());
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationFailed.class)));
    }

    @MethodSource("nonSpiChannels")
    @ParameterizedTest
    public void shouldRaiseCaseRejectedWhenFirstCPPIMessagesReceivedWithInvalidDefendants(final Channel channel) {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), channel, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(firstMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        final Optional<CcProsecutionRejected> eventObject = getFirstMatching(eventList, CcProsecutionRejected.class);

        assertThat(eventObject.isPresent(), is(true));
        assertThat(eventObject.get().getCaseErrors(), hasSize(0));
        assertThat(eventObject.get().getDefendantErrors(), hasSize(greaterThan(0)));
    }

    @Test
    public void shouldRaiseCaseRejectedWhenSubsequentCPPIMessagesReceived() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), CPPI, "C");
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(referenceData).build());
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), CPPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        final List<Object> eventList = objectStream.collect(toList());

        final Optional<CcProsecutionRejected> event = getFirstMatching(eventList, CcProsecutionRejected.class);

        assertThat(event.isPresent(), is(true));
        assertThat(event.get().getCaseErrors(), hasSize(1));
        assertThat(event.get().getDefendantErrors(), hasSize(greaterThan(0)));
    }

    @Test
    public void shouldRaiseCaseValidationFailedForFirstSPIMessageFoundWithErrors() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "C");
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(firstMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        final Optional<CaseValidationFailed> caseValidationFailedEvent = getFirstMatching(eventList, CaseValidationFailed.class);
        final Optional<DefendantValidationFailed> defendantValidationFailedEvent = getFirstMatching(eventList, DefendantValidationFailed.class);
        assertThat(caseValidationFailedEvent.isPresent(), is(true));
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));
    }

    @Test
    public void shouldRaiseDefendantsReceivedNotAddedForSubsequentMessageWhenInitialSPIMessageFoundWithErrors() {
        final LocalDate offenceCommittedDate = of(2018, 3, 2);
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendantWithOffence(offenceCommittedDate, now().plusYears(1), PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "C");
        prosecutionCaseFile.apply(new CaseValidationFailed(referenceData.getProsecution(), ImmutableList.of(new Problem("CODE", new ArrayList<>())), randomUUID(), getInitialHearing()));
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "C");

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(DefendantsReceivedNotAdded.class)));
    }

    @Test
    public void shouldCreateSingleSummonsApplicationWithAllParkedDefendantsOnceInitialCaseErrorsAreClearedForSPIChannel() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithMixedValidAndInvalidCaseMarkerRefData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)));

        prosecutionCaseFile.apply(new CaseValidationFailed(firstMessage.getProsecution(), of(new Problem("CASE_MARKER_IS_INVALID", of(
                new ProblemValue("0", "caseMarkers", VALID_CASE_MARKER_CODE), new ProblemValue("1", "caseMarkers", INVALID_CASE_MARKER_CODE)))), EXTERNAL_ID, getInitialHearing()));

        final ProsecutionWithReferenceData secondMessage = getProsecutionWithValidCaseMarker(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)));

        final Stream<Object> receiveCCCaseObjectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = receiveCCCaseObjectStream.collect(toList());
        assertThat(eventList, hasSize(2));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(DefendantsReceivedNotAdded.class)));

        // correct error for first message
        final Stream<Object> receiveErrorCorrectionsObjectStream = prosecutionCaseFile.receiveErrorCorrections(getCaseMarkerCorrectedField(), objectToJsonObjectConverter,
                jsonObjectToObjectConverter, of(initiationTypesRefDataEnricher), new ArrayList<>(), referenceDataQueryService);

        eventList = receiveErrorCorrectionsObjectStream.collect(toList());
        assertThat(eventList, hasSize(5));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(((DefendantValidationPassed) eventList.get(0)).getDefendantId().toString(), is(DEFENDANT_ID));
        assertThat(eventList.get(1), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(((DefendantValidationPassed) eventList.get(1)).getDefendantId().toString(), is(SECOND_DEFENDANT_ID));
        assertThat(eventList.get(2), is(instanceOf(DefendantsParkedForSummonsApplicationApproval.class)));
        assertThat(((DefendantsParkedForSummonsApplicationApproval) eventList.get(2)).getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(2));
        assertThat(((DefendantsParkedForSummonsApplicationApproval) eventList.get(2)).getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(DEFENDANT_ID));
        assertThat(((DefendantsParkedForSummonsApplicationApproval) eventList.get(2)).getProsecutionWithReferenceData().getProsecution().getDefendants().get(1).getId(), is(SECOND_DEFENDANT_ID));
    }

    @Test
    public void shouldCreateSummonsApplicationForValidDefendantsOnlyWhenCaseHasAlreadyBeenCreated() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));
        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SECOND_SURNAME, BIRTH_DATE, SECOND_DEFENDANT_ID, randomAlphabetic(10)),
                        buildDefendant(FORENAME, THIRD_SURNAME, BIRTH_DATE, THIRD_DEFENDANT_ID, randomAlphabetic(10)),
                        buildDefendant(FORENAME, FOURTH_SURNAME, BIRTH_DATE, FOURTH_DEFENDANT_ID, randomAlphabetic(10), DATE_OF_HEARING_IN_PAST)
                ), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(4));
        Optional<DefendantsParkedForSummonsApplicationApproval> firstMatchingEvent = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatchingEvent.isPresent(), is(true));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(2));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(1).getId(), is(THIRD_DEFENDANT_ID));

        final Stream<Object> receiveErrorCorrectionsObjectStream = prosecutionCaseFile.receiveErrorCorrections(getDefendantDateOfHearingCorrectedField(), objectToJsonObjectConverter,
                jsonObjectToObjectConverter, of(initiationTypesRefDataEnricher), of(offenceDataRefDataEnricher), referenceDataQueryService);

        eventList = receiveErrorCorrectionsObjectStream.collect(toList());
        assertThat(eventList.size(), is(4));
        firstMatchingEvent = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatchingEvent.isPresent(), is(true));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatchingEvent.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(FOURTH_DEFENDANT_ID));
    }

    @Test
    public void shouldCreateSjpCaseOnceChargeDateValidationErrorIsResolved(){
        uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant defendant =
                buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE);
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithValidCaseMarker(Lists.newArrayList(defendant));
        final Problem problem = new Problem(ProblemCode.CHARGE_DATE_IN_FUTURE.name(),
                of(new ProblemValue("0", "offence_chargeDate", "Charge date not provided")));
        final DefendantProblemsVO defendantProblemsVO = new DefendantProblemsVO(defendant,asList(problem));
        prosecutionCaseFile.apply(new SjpProsecutionRejected(asList(problem), firstMessage.getExternalId(), firstMessage.getProsecution()));
        prosecutionCaseFile.apply(new SjpValidationFailed(firstMessage.getProsecution(), of(problem), asList(defendantProblemsVO),
                firstMessage.getReferenceDataVO(),
                getInitialHearing()));

        final Stream<Object> receiveErrorCorrectionsObjectStream = prosecutionCaseFile.receiveErrorCorrections(offenceChargeDateCorrectlySet(), objectToJsonObjectConverter,
                jsonObjectToObjectConverter, of(initiationTypesRefDataEnricher), of(offenceDataRefDataEnricher), referenceDataQueryService);
        List<Object> eventList = receiveErrorCorrectionsObjectStream.collect(toList());
        assertThat(eventList.size(), is(3));
        Optional<SjpProsecutionReceived> sjpProsecutionReceived = getFirstMatching(eventList, SjpProsecutionReceived.class);
        assertThat(sjpProsecutionReceived.isPresent(), is(true));
        assertThat(sjpProsecutionReceived.get().getProsecution().getDefendants().get(0).getPostingDate().toString(), is("2100-07-02"));
    }

    @Test
    public void shouldCreateSummonsApplicationForSubsequentMessagesWithValidDefendantsWhenEarlierDefendantsHaveBeenParked() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID,
                                PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE);

        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));

        final ProsecutionWithReferenceData secondMessageWithValidAndInvalidDefendants = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, THIRD_SURNAME, BIRTH_DATE, THIRD_DEFENDANT_ID, randomAlphabetic(10)),
                        buildDefendant(FORENAME, FOURTH_SURNAME, BIRTH_DATE, FOURTH_DEFENDANT_ID, randomAlphabetic(10), DATE_OF_HEARING_IN_PAST)
                ), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessageWithValidAndInvalidDefendants, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(3));
        Optional<DefendantsParkedForSummonsApplicationApproval> firstMatchingForParkedDefendant = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatchingForParkedDefendant.isPresent(), is(true));
        assertThat(firstMatchingForParkedDefendant.get().getApplicationId(), is(not(APPLICATION_ID)));
        assertThat(firstMatchingForParkedDefendant.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatchingForParkedDefendant.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(THIRD_DEFENDANT_ID));

        final Optional<DefendantValidationFailed> firstMatchingForValidationFailed = getFirstMatching(eventList, DefendantValidationFailed.class);
        assertThat(firstMatchingForValidationFailed.isPresent(), is(true));
        assertThat(firstMatchingForValidationFailed.get().getDefendant().getId(), is(FOURTH_DEFENDANT_ID));

        final ProsecutionWithReferenceData thirdMessageWithValidDefendant = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, FITH_SURNAME, BIRTH_DATE, FIFTH_DEFENDANT_ID, randomAlphabetic(10))), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_3);
        objectStream = prosecutionCaseFile.receiveCCCase(thirdMessageWithValidDefendant, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(2));
        firstMatchingForParkedDefendant = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatchingForParkedDefendant.isPresent(), is(true));
        assertThat(firstMatchingForParkedDefendant.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatchingForParkedDefendant.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(FIFTH_DEFENDANT_ID));
    }

    @Test
    public void shouldRaiseSummonsApplicationRejectedEventAndRemoveOnlyRejectedDefendants() {
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "S");
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, referenceData, emptyList()));
        final ProsecutionWithReferenceData subsequentReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "S");

        prosecutionCaseFile.receiveCCCase(subsequentReferenceData, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final SummonsApplicationRejectedDetails summonsApplicationRejectedDetails = summonsApplicationRejectedDetails()
                .withApplicationId(APPLICATION_ID)
                .withCaseId(CASE_ID)
                .withSummonsRejectedOutcome(summonsRejectedOutcome()
                        .withReasons(ImmutableList.of("Rejection Reason"))
                        .build())
                .build();
        final Stream<Object> objectStream = prosecutionCaseFile.rejectCaseDefendants(summonsApplicationRejectedDetails);

        final List<Object> eventList = objectStream.collect(toList());
        final uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected applicationRejected = (uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected) eventList.get(0);
        assertThat(applicationRejected, is(instanceOf(uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected.class)));
        assertThat(applicationRejected.getDefendantIds(), contains(fromString(DEFENDANT_ID)));
        assertThat(applicationRejected.getDefendantIds(), not(contains(fromString(SECOND_DEFENDANT_ID))));
        assertThat(applicationRejected.getSummonsRejectedOutcome().getReasons(), contains("Rejection Reason"));
        assertThat(prosecutionCaseFile.isProsecutionReceived(), is(false));
        assertThat(prosecutionCaseFile.getDefendants(), hasSize(1));
        assertThat(prosecutionCaseFile.getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));
    }

    @Test
    public void shouldRaiseNewSummonsApplicationForSameDefendantWhenEarlierApplicationWasRejectedForSameCaseReceivedViaSpiChannel() {
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "S");
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, referenceData, emptyList()));
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID, newArrayList(fromString(DEFENDANT_ID), fromString(SECOND_DEFENDANT_ID)), summonsRejectedOutcome().withReasons(ImmutableList.of("Rejection Reason")).build()));

        final String updatedSurname = STRING.next();
        final ProsecutionWithReferenceData subsequentReferenceDataForSameDefendant = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, updatedSurname, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, "S", EXTERNAL_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceDataForSameDefendant, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(2));
        final Optional<DefendantsParkedForSummonsApplicationApproval> firstMatching = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatching.isPresent(), is(true));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(firstMatching.get().getApplicationId(), is(not(APPLICATION_ID)));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(DEFENDANT_ID));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getIndividual().getPersonalInformation().getFirstName(), is(FORENAME));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getIndividual().getPersonalInformation().getLastName(), is(updatedSurname));
    }

    @Test
    public void shouldInitiateCaseCreationWhenSummonsApplicationIsApprovedAndAllEarlierApplicationsWereRejectedViaSpiChannel() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID, newArrayList(fromString(DEFENDANT_ID), fromString(SECOND_DEFENDANT_ID)), summonsRejectedOutcome().withReasons(newArrayList("Rejection Reason")).build()));

        final String updatedSurname = STRING.next();
        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, updatedSurname, BIRTH_DATE, THIRD_DEFENDANT_ID, randomAlphabetic(10))), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(2));
        final Optional<DefendantsParkedForSummonsApplicationApproval> firstMatching = getFirstMatching(eventList, DefendantsParkedForSummonsApplicationApproval.class);
        assertThat(firstMatching.isPresent(), is(true));
        final UUID applicationId2 = firstMatching.get().getApplicationId();
        assertThat(applicationId2, is(not(APPLICATION_ID)));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(THIRD_DEFENDANT_ID));

        objectStream = prosecutionCaseFile.approveCaseDefendants(getSummonsApplicationApprovedDetails(applicationId2), of(), of(), isCivil);

        eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final Optional<CcCaseReceived> firstMatchingCaseReceived = getFirstMatching(eventList, CcCaseReceived.class);
        assertThat(firstMatchingCaseReceived.isPresent(), is(true));
        assertThat(firstMatchingCaseReceived.get().getProsecutionWithReferenceData().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(1));
        assertThat(firstMatching.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(THIRD_DEFENDANT_ID));
    }

    @Test
    public void shouldAddDefendantWhenSummonsApplicationIsApprovedForNewlyAddedDefendantToExistingCaseViaSpiChannel() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE);
        final String updatedSurname = STRING.next();
        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, updatedSurname, BIRTH_DATE, THIRD_DEFENDANT_ID, randomAlphabetic(10))), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));
        prosecutionCaseFile.apply(new CcCaseReceived(firstMessage, summonsApprovedOutcome().withPersonalService(true).withProsecutorCost("£300.00").withSummonsSuppressed(false).build(), randomUUID()));
        prosecutionCaseFile.apply(new CaseCreatedSuccessfully(CASE_ID, SPI, EXTERNAL_ID));
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID_2, secondMessage, emptyList()));

        final SummonsApplicationApprovedDetails secondSummonsApplicationApprovedDetails = getSummonsApplicationApprovedDetails(APPLICATION_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.approveCaseDefendants(secondSummonsApplicationApprovedDetails, of(), of(), isCivil);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final Optional<ProsecutionDefendantsAdded> defendantAdded = getFirstMatching(eventList, ProsecutionDefendantsAdded.class);
        assertThat(defendantAdded.isPresent(), is(true));
        assertThat(defendantAdded.get().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(defendantAdded.get().getDefendants(), hasSize(1));
        assertThat(defendantAdded.get().getDefendants().get(0).getId(), is(THIRD_DEFENDANT_ID));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome(), notNullValue());
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getSummonsSuppressed(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getSummonsSuppressed()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorCost(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorCost()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getPersonalService(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getPersonalService()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorEmailAddress(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorEmailAddress()));
    }

    @Test
    public void shouldAddSubsequentDefendantsToTheCaseWhenCaseHasBeenCreatedAndSummonsIsApprovedForDefendantsThatWereNotPartOfTheCaseCreationViaSpiChannel() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);
        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID_2, secondMessage, emptyList()));
        prosecutionCaseFile.apply(new CcCaseReceived(firstMessage, summonsApprovedOutcome().withPersonalService(true).withProsecutorCost("£300.00").withSummonsSuppressed(false).build(), randomUUID()));
        prosecutionCaseFile.apply(new CaseCreatedSuccessfully(CASE_ID, SPI, EXTERNAL_ID));

        final SummonsApplicationApprovedDetails secondSummonsApplicationApprovedDetails = getSummonsApplicationApprovedDetails(APPLICATION_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.approveCaseDefendants(secondSummonsApplicationApprovedDetails, of(), of(), isCivil);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final Optional<ProsecutionDefendantsAdded> defendantAdded = getFirstMatching(eventList, ProsecutionDefendantsAdded.class);
        assertThat(defendantAdded.isPresent(), is(true));
        assertThat(defendantAdded.get().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(defendantAdded.get().getDefendants(), hasSize(1));
        assertThat(defendantAdded.get().getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome(), notNullValue());
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getSummonsSuppressed(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getSummonsSuppressed()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorCost(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorCost()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getPersonalService(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getPersonalService()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorEmailAddress(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorEmailAddress()));
    }

    @Test
    public void shouldAddSubsequentDefendantsToTheCaseWhenCaseHasBeenCreatedWithWarningsAndSummonsIsApprovedForDefendantsThatWereNotPartOfTheCaseCreationViaSpiChannel() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);
        final DefendantProblem firstDefendantWarning = defendantProblem().withProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE_ONE).withProblems(of(problem().withCode(OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE).build())).build();
        final ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID_2);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, of(firstDefendantWarning)));
        final DefendantProblem secondDefendantWarning = defendantProblem().withProsecutorDefendantReference(PROSECUTOR_DEFENDANT_REFERENCE_TWO).withProblems(of(problem().withCode(OFFENCE_NOT_IN_EFFECT_ON_OFFENCE_COMMITTED_DATE).build())).build();
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID_2, secondMessage, of(secondDefendantWarning)));
        prosecutionCaseFile.apply(new CcCaseReceivedWithWarnings(firstMessage, emptyList(), of(firstDefendantWarning), summonsApprovedOutcome().withPersonalService(true).withProsecutorCost("£300.00").withSummonsSuppressed(false).build(), randomUUID()));
        prosecutionCaseFile.apply(new CaseCreatedSuccessfullyWithWarnings(CASE_ID, emptyList(), SPI, of(firstDefendantWarning), EXTERNAL_ID, firstDefendantWarning.getProblems()));

        final SummonsApplicationApprovedDetails secondSummonsApplicationApprovedDetails = getSummonsApplicationApprovedDetails(APPLICATION_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.approveCaseDefendants(secondSummonsApplicationApprovedDetails, of(), of(), isCivil);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final Optional<ProsecutionDefendantsAdded> defendantAdded = getFirstMatching(eventList, ProsecutionDefendantsAdded.class);
        assertThat(defendantAdded.isPresent(), is(true));
        assertThat(defendantAdded.get().getExternalId(), is(EXTERNAL_ID_2));
        assertThat(defendantAdded.get().getDefendants(), hasSize(1));
        assertThat(defendantAdded.get().getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));
        assertThat(defendantAdded.get().getDefendantWarnings(), hasSize(1));
        assertThat(defendantAdded.get().getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(PROSECUTOR_DEFENDANT_REFERENCE_TWO));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome(), notNullValue());
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getSummonsSuppressed(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getSummonsSuppressed()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorCost(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorCost()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getPersonalService(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getPersonalService()));
        assertThat(defendantAdded.get().getSummonsApprovedOutcome().getProsecutorEmailAddress(), is(secondSummonsApplicationApprovedDetails.getSummonsApprovedOutcome().getProsecutorEmailAddress()));
    }

    @Test
    public void shouldIndicateAsDuplicateDefendantWhenSameDefendantIsReceivedAndSummonsApplicationForTheDefendantHasAlreadyBeenApprovedViaSpiChannel() {
        final ProsecutionWithReferenceData referenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "S");
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, referenceData, emptyList()));
        prosecutionCaseFile.apply(new CcCaseReceived(referenceData, summonsApprovedOutcome().withPersonalService(true).withProsecutorCost("£300.00").withSummonsSuppressed(false).build(), randomUUID()));

        final ProsecutionWithReferenceData subsequentReferenceDataForSameDefendant = getProsecutionWithReferenceData(
                of(buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, "S", EXTERNAL_ID_2);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(subsequentReferenceDataForSameDefendant, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        final List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final CaseReceivedWithDuplicateDefendants caseReceivedWithDuplicateDefendants = (CaseReceivedWithDuplicateDefendants) eventList.get(0);
        assertThat(caseReceivedWithDuplicateDefendants.getCaseId(), is(CASE_ID));
        assertThat(caseReceivedWithDuplicateDefendants.getDefendants(), hasSize(1));
        assertThat(caseReceivedWithDuplicateDefendants.getDefendants().get(0).getId(), is(SECOND_DEFENDANT_ID));
    }

    @MethodSource("nonSpiChannels")
    @ParameterizedTest
    public void shouldRaiseProsecutionCaseRejectedForMCCAndCPPIChannels(final Channel nonSpiChannel) {
        final SummonsApplicationRejectedDetails applicationRejectedCommandPayload = summonsApplicationRejectedDetails()
                .withApplicationId(APPLICATION_ID)
                .withCaseId(CASE_ID)
                .withSummonsRejectedOutcome(summonsRejectedOutcome()
                        .withReasons(ImmutableList.of("First Reason"))
                        .build())
                .build();
        final ProsecutionWithReferenceData prosecutionWithReferenceData = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE)), nonSpiChannel, "S");
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, prosecutionWithReferenceData, emptyList()));

        // application rejected and all defendants cleaned
        final Stream<Object> objectStreamPostApplicationRejection = prosecutionCaseFile.rejectCaseDefendants(applicationRejectedCommandPayload);

        final List<Object> rejectionEventList = objectStreamPostApplicationRejection.collect(toList());
        assertThat(rejectionEventList.get(0), is(instanceOf(uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected.class)));
        assertThat(rejectionEventList.get(1), is(instanceOf(CcProsecutionRejected.class)));
        final uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected applicationRejected = (uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected) rejectionEventList.get(0);
        assertThat(applicationRejected.getDefendantIds(), contains(fromString(DEFENDANT_ID)));
        assertThat(applicationRejected.getSummonsRejectedOutcome().getReasons(), contains("First Reason"));
        assertThat(prosecutionCaseFile.isProsecutionReceived(), is(false));
        assertThat(prosecutionCaseFile.getDefendants(), empty());

        // subsequent message rec'd after application rejection
        final Stream<Object> objectStreamPostSubsequentMessageReceived = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceData, emptyList(), emptyList(), referenceDataQueryService);
        final List<Object> subsequentCaseMessageEventList = objectStreamPostSubsequentMessageReceived.collect(toList());
        assertThat(subsequentCaseMessageEventList.get(0), is(instanceOf(DefendantsParkedForSummonsApplicationApproval.class)));
        assertThat(prosecutionCaseFile.getDefendants(), hasSize(1));
    }

    @Test
    public void shouldNotRaiseAnyEventWhenAcceptCaseIsCalledAndCaseNotReceivedForSJP() {
        final Stream<Object> resultObjectStream = prosecutionCaseFile.acceptCase(CASE_ID, null, referenceDataQueryService);

        final List<Object> resultEventList = resultObjectStream.collect(toList());
        assertThat(resultEventList, hasSize(0));
    }


    @Test
    public void shouldRaiseMaterialAddedEventsWhenNextDefendantValidated() {
        final UUID caseId = randomUUID();
        final UUID cpsDefendantId = randomUUID();

        when(referenceDataQueryService.retrieveDocumentsTypeAccess()).thenReturn(singletonList(DocumentTypeAccessReferenceData.documentTypeAccessReferenceData().withSection("SECTION").withId(randomUUID()).withDocumentCategory("Defendant level").build()));
        when(referenceDataQueryService.getProsecutorsByOuCode(any())).thenReturn(ProsecutorsReferenceData.prosecutorsReferenceData().withCpsFlag(true).build());
        prosecutionCaseFile.apply(ccCaseReceived()
                .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(prosecution()
                        .withCaseDetails(caseDetails().withCaseId(caseId).build())
                        .withDefendants(singletonList(defendant()
                                .withId(randomUUID().toString())
                                .withIndividual(individual()
                                        .withPersonalInformation(personalInformation()
                                                .withFirstName("John")
                                                .withLastName("Doe")
                                                .build())
                                        .build())
                                .build()))
                        .build()))
                .build());

        prosecutionCaseFile.apply(CaseCreatedSuccessfully.caseCreatedSuccessfully()
                .withCaseId(caseId)
                .build());

        List<Object> events = prosecutionCaseFile.addMaterialV2(AddMaterialCommonV2.addMaterialCommonV2()
                .withMaterial(randomUUID())
                .withCaseId(caseId)
                .withMaterialType("SECTION")
                .withMaterialContentType("image/jpeg")
                .withProsecutionCaseSubject(ProsecutionCaseSubject.prosecutionCaseSubject()
                        .withDefendantSubject(DefendantSubject.defendantSubject()
                                .withCpsDefendantId(cpsDefendantId.toString())
                                .build())
                        .withProsecutingAuthority("PA")
                        .build())
                .build(), referenceDataQueryService).collect(Collectors.toList());

        assertThat(events.size(), Matchers.is(1));
        MaterialPendingV2 pendingEvent = (MaterialPendingV2) events.get(0);
        assertThat(pendingEvent.getWarnings().size(), is(1));

        events = prosecutionCaseFile.addMaterialV2(AddMaterialCommonV2.addMaterialCommonV2()
                .withMaterial(randomUUID())
                .withCaseId(caseId)
                .withMaterialType("SECTION")
                .withMaterialContentType("image/jpeg")
                .withProsecutionCaseSubject(ProsecutionCaseSubject.prosecutionCaseSubject()
                        .withDefendantSubject(DefendantSubject.defendantSubject()
                                .withCpsDefendantId(cpsDefendantId.toString())
                                .withCpsPersonDefendantDetails(CpsPersonDefendantDetails.cpsPersonDefendantDetails()
                                        .withForename("John")
                                        .withSurname("Doe")
                                        .build())
                                .build())
                        .withProsecutingAuthority("PA")
                        .build())
                .build(), referenceDataQueryService).collect(Collectors.toList());

        assertThat(events.size(), Matchers.is(2));

    }

    @Test
    public void shouldRaiseNewCCCaseForSameDefendantWhenEarlierApplicationWasRejected() {
        final ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE),
                        buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)), SPI, SUMMONS_INITIATION_CODE);
        prosecutionCaseFile.apply(new DefendantsParkedForSummonsApplicationApproval(APPLICATION_ID, firstMessage, emptyList()));
        prosecutionCaseFile.apply(new SummonsApplicationRejected(APPLICATION_ID, CASE_ID, newArrayList(fromString(DEFENDANT_ID), fromString(SECOND_DEFENDANT_ID)), summonsRejectedOutcome().withReasons(newArrayList("Rejection Reason")).build()));

        Stream<Object> objectStream = prosecutionCaseFile.approveCaseDefendants(getSummonsApplicationApprovedDetails(APPLICATION_ID), of(), of(), isCivil);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList, hasSize(1));
        final Optional<CcCaseReceived> firstMatchingCaseReceived = getFirstMatching(eventList, CcCaseReceived.class);
        assertThat(firstMatchingCaseReceived.isPresent(), is(true));
        assertThat(firstMatchingCaseReceived.get().getProsecutionWithReferenceData().getProsecution().getDefendants(), hasSize(2));
        assertThat(firstMatchingCaseReceived.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(0).getId(), is(DEFENDANT_ID));
        assertThat(firstMatchingCaseReceived.get().getProsecutionWithReferenceData().getProsecution().getDefendants().get(1).getId(), is(SECOND_DEFENDANT_ID));

    }


    @Test
    public void shouldRejectDuplicateDefendantWithSameAsnForSpiIn() {


        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Jack", "Smith", LocalDate.of(1991, 01, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn123"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(buildDefendantWithAsn("David", "James", LocalDate.of(1991, 02, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn123"));

        ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
        assertThat(defendants.get(0).getAsn(), is("asn123"));


    }

    @Test
    public void shouldRejectDuplicateDefendantWithSameDefendantDetailsForSpiIn() {


        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Mary", "Cal", LocalDate.of(1991, 01, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn100"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(buildDefendantWithAsn("Mary", "Cal", LocalDate.of(1991, 01, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn101"));

        ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getFirstName(), is("Mary"));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getLastName(), is("Cal"));
        assertThat(defendants.get(0).getIndividual().getSelfDefinedInformation().getDateOfBirth().toString(), is("1991-01-10"));


    }

    @Test
    public void shouldRejectDuplicateDefendantWithDobNotPresentForSpiIn() {


        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Mary", "Cal", null,
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn100"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, MCC, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(buildDefendantWithAsn("Mary", "Cal", null,
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn101"));

        ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);


        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getFirstName(), is("Mary"));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getLastName(), is("Cal"));
    }

    @Test
    public void shouldRejectDuplicateDefendantWithFirstNamePresentAndSameDobForSpiIn() {
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Mary", "Cal", LocalDate.of(1991, 01, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn100"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(buildDefendantWithAsn(null, "Cal", LocalDate.of(1991, 01, 10),
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn101"));

        ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);


        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getLastName(), is("Cal"));
    }

    @Test
    public void shouldRejectDuplicateDefendantWithDobNotPresentForMCCAndThenSpi() {
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Mary", "Cal", null,
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn100"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, MCC, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());

        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(buildDefendantWithAsn("Mary", "Cal", null,
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn101"));

        ProsecutionWithReferenceData secondMessage = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);


        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(secondMessage, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);

        List<Object> eventList = objectStream.collect(toList());
        assertThat(eventList.size(), is(1));
        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getFirstName(), is("Mary"));
        assertThat(defendants.get(0).getIndividual().getPersonalInformation().getLastName(), is("Cal"));
    }

    @Test
    public void shouldProcessUniqueDefendantsAndIgnoreDuplicatesWithinSameRequest() {
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList =
                asList(buildDefendantWithAsn("Mary", "Cal", null,
                        DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE, "asn101"));
        ProsecutionWithReferenceData firstMessage = getProsecutionWithReferenceData(defendantList, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);

        prosecutionCaseFile.apply(ccCaseReceived().withProsecutionWithReferenceData(firstMessage).build());
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList2 =
                asList(
                        buildDefendantWithAsn("John", "Doe", LocalDate.of(1990, 05, 15),
                                DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, "asn100"),
                        buildDefendantWithAsn("John", "Doe", LocalDate.of(1990, 05, 15),
                                DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO, "asn100")
                );
        ProsecutionWithReferenceData messageWithDuplicateDefendants = getProsecutionWithReferenceData(defendantList2, SPI, SUMMONS_INITIATION_CODE, EXTERNAL_ID);
        final Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(messageWithDuplicateDefendants, new ArrayList<>(), new ArrayList<>(), referenceDataQueryService);
        List<Object> eventList = objectStream.collect(toList());

        Optional<CaseReceivedWithDuplicateDefendants> caseReceivedWithDuplicateDefendants = getFirstMatching(eventList, CaseReceivedWithDuplicateDefendants.class);
        List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = caseReceivedWithDuplicateDefendants.get().getDefendants();
        assertThat(caseReceivedWithDuplicateDefendants.isPresent(), is(true));
    }




    private void assertThatTheEventReturnedIsOfType(final Stream<Object> objectStream, final Class<?> eventClass) {
        final Optional<Object> event = objectStream.findFirst();
        assertThat(event.isPresent(), is(true));
        assertThat(event.get(), is(instanceOf(eventClass)));
    }

    private void assertThatTheEventReturnedIsOfType(final Stream<Object> objectStream, final Class<?> eventClass, final long eventCount) {
        assertThat(objectStream.filter(eventClass::isInstance).count(), is(eventCount));
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList, final String initiationCode) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6 ").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C", "S"));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(initiationCode)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .build())
                .withDefendants(defendantList)
                .withChannel(SPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C"));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .build())
                .withDefendants(defendantList)
                .withChannel(CPPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataAndCivilFees(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList, final Channel channel, boolean isCivil, List<CivilFees> civilFees) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C"));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withFeeStatus(CollectionUtils.isNotEmpty(civilFees) ? civilFees.get(0).getFeeStatus(): null)
                        .withPaymentReference(CollectionUtils.isNotEmpty(civilFees) ? civilFees.get(0).getPaymentReference(): null)
                        .build())
                .withDefendants(defendantList)
                .withChannel(channel)
                .withIsCivil(isCivil)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithMixedValidAndInvalidCaseMarkerRefData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C", TestConstants.SUMMONS_INITIATION_CODE));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());

        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(SUMMONS_INITIATION_CODE)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withCaseMarkers(asList(
                                caseMarker().withMarkerTypeCode(VALID_CASE_MARKER_CODE).build(),
                                caseMarker().withMarkerTypeCode(INVALID_CASE_MARKER_CODE).build()))
                        .build())
                .withDefendants(defendantList)
                .withChannel(SPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList,
                                                                         final String offenceStartDate, final String offenceEndDate) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6").withOffenceStartDate(offenceStartDate).withOffenceEndDate(offenceEndDate).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C"));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode("C")
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .build())
                .withDefendants(defendantList)
                .withChannel(CPPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList, final Channel channel, final String initiationCode) {
        return getProsecutionWithReferenceData(defendantList, channel, initiationCode, EXTERNAL_ID);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList, final Channel channel, final String initiationCode, final UUID externalId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6 ").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C", initiationCode));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(initiationCode)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .build())
                .withDefendants(defendantList)
                .withChannel(channel)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(externalId);
        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getProsecutionWithValidCaseMarker(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withProsecutionTimeLimit("6 ").withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C", TestConstants.SUMMONS_INITIATION_CODE));
        referenceDataVO.setProsecutorsReferenceData(prosecutorsReferenceData()
                .withId(randomUUID())
                .build());
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(CASE_ID)
                        .withInitiationCode(SUMMONS_INITIATION_CODE)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withCaseMarkers(of(caseMarker().withMarkerTypeCode(VALID_CASE_MARKER_CODE).build()))
                        .build())
                .withDefendants(defendantList)
                .withChannel(SPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        prosecutionWithReferenceData.setExternalId(TestConstants.EXTERNAL_ID_2);
        return prosecutionWithReferenceData;
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendant(final String firstName, final String lastName,
                                                                                      final LocalDate dateOfBirth,
                                                                                      final String defendantId,
                                                                                      final String prosecutorDefendantReference) {
        return buildDefendant(firstName, lastName, dateOfBirth, defendantId, prosecutorDefendantReference, DATE_OF_HEARING);
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithAsn(final String firstName, final String lastName,
                                                                                             final LocalDate dateOfBirth,
                                                                                             final String defendantId,
                                                                                             final String prosecutorDefendantReference,
                                                                                             final String asn

    ) {
        return buildDefendantWithAsn(firstName, lastName, dateOfBirth, defendantId, prosecutorDefendantReference, DATE_OF_HEARING, asn);
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendant(final String firstName,
                                                                                      final String lastName,
                                                                                      final LocalDate dateOfBirth,
                                                                                      final String defendantId,
                                                                                      final String prosecutorDefendantReference,
                                                                                      final String dateOfHearing) {
        return defendant()
                .withId(defendantId)
                .withInitiationCode("C")
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(dateOfHearing)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withOffences(singletonList(offence()
                        .withOffenceId(TestConstants.OFFENCE_ID)
                        .withOffenceSequenceNumber(1)
                        .withArrestDate(ARREST_DATE)
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                        .withChargeDate(OFFENCE_CHARGE_DATE)
                        .withOffenceDateCode(2)
                        .build()))
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithAsn(final String firstName,
                                                                                             final String lastName,
                                                                                             final LocalDate dateOfBirth,
                                                                                             final String defendantId,
                                                                                             final String prosecutorDefendantReference,
                                                                                             final String dateOfHearing,
                                                                                             final String asn
    ) {
        return defendant()
                .withAsn(asn)
                .withId(defendantId)
                .withInitiationCode("C")
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(dateOfHearing)
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
                .build();
    }
    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithInitiationCode(final String initiationCode) {
        return defendant()
                .withId(TestConstants.DEFENDANT_ID)
                .withInitiationCode(initiationCode)
                .withProsecutorDefendantReference(TestConstants.PROSECUTOR_DEFENDANT_REFERENCE_ONE)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(TestConstants.FORENAME)
                                .withLastName(TestConstants.SURNAME).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(TestConstants.BIRTH_DATE)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(TestConstants.DATE_OF_HEARING)
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
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithInitiationCode(final String firstName, final String lastName, final LocalDate dateOfBirth, final String defendantId, final String prosecutorDefendantReference, final String initiationCode) {
        return defendant()
                .withId(defendantId)
                .withInitiationCode(initiationCode)
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(TestConstants.DATE_OF_HEARING)
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
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithOffence(final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final String prosecutorDefendantReference) {
        return defendant()
                .withId(DEFENDANT_ID)
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName("Ben")
                                .withLastName("Westwood").build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(SECOND_BIRTH_DATE)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withOffences(singletonList(offence()
                        .withArrestDate(ARREST_DATE)
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceSequenceNumber(1)
                        .withOffenceId(offenceId)
                        .withOffenceCommittedDate(offenceCommittedDate)
                        .withChargeDate(offenceChargeDate)
                        .withOffenceDateCode(2)
                        .build()))
                .withInitiationCode("C")
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithAsnAndOffence(final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final String asn) {
        return defendant()
                .withId(DEFENDANT_ID)
                .withAsn(asn)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName("Ben")
                                .withLastName("Westwood").build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(SECOND_BIRTH_DATE)
                                .build())
                        .withBailConditions("bailConditions")
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withOffences(singletonList(offence()
                        .withArrestDate(ARREST_DATE)
                        .withOffenceCode(OFFENCE_CODE)
                        .withOffenceSequenceNumber(1)
                        .withOffenceId(offenceId)
                        .withOffenceCommittedDate(offenceCommittedDate)
                        .withChargeDate(offenceChargeDate)
                        .withOffenceDateCode(2)
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

    private List<BailStatusReferenceData> buildBailStatuses() {
        return singletonList(bailStatusReferenceData()
                .withStatusCode(CUSTODY_STATUS)
                .build());
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final Channel channel, final Boolean hasMultipleDefendants) {

        ProsecutorsReferenceData prd = new ProsecutorsReferenceData.Builder()
                .withId(UUID.fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf"))
                .withShortName("ShortName").withSjpFlag(true).withAocpApproved(true).build();

        Prosecutor prosecutor = new Prosecutor.Builder().withReferenceData(prd).withProsecutingAuthority("TFL").build();
        CaseDetails caseDetails = new CaseDetails.Builder()
                .withProsecutor(prosecutor)
                .withCaseId(randomUUID())
                .withProsecutorCaseReference("1234243")
                .withInitiationTypeFound(true)
                .withOriginatingOrganisation("TFL")
                .withCpsOrganisation("CPS")
                .withInitiationCode("J")
                .build();

        final ImmutableList<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants = hasMultipleDefendants ?
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE), buildDefendant(SECOND_FORENAME, SECOND_SURNAME, SECOND_BIRTH_DATE, SECOND_DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_TWO)) :
                of(buildDefendant(FORENAME, SURNAME, BIRTH_DATE, DEFENDANT_ID, PROSECUTOR_DEFENDANT_REFERENCE_ONE));
        Prosecution prosecution = new Prosecution.Builder()
                .withCaseDetails(caseDetails)
                .withChannel(channel)
                .withDefendants(defendants)
                .build();

        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(
                singletonList(offenceReferenceData()
                        .withCjsOffenceCode(OFFENCE_CODE)
                        .withOffenceStartDate(OFFENCE_START_DATE)
                        .withModeOfTrial("SNONIMP")
                        .build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C"));
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        referenceDataVO.setProsecutorsReferenceData(getProsecutorsReferenceData());

        return prosecutionWithReferenceData;
    }

    private ProsecutionWithReferenceData getSjpProsecutionWithReferenceData(final String initiationCode) {

        ProsecutorsReferenceData prd = new ProsecutorsReferenceData.Builder()
                .withId(UUID.fromString("c9ad5fef-7d97-4df5-b796-0eed3eed1acf"))
                .withShortName("ShortName").withSjpFlag(true).withAocpApproved(true).build();

        Prosecutor prosecutor = new Prosecutor.Builder().withReferenceData(prd).withProsecutingAuthority("TFL").build();
        CaseDetails caseDetails = new CaseDetails.Builder()
                .withProsecutor(prosecutor)
                .withCaseId(randomUUID())
                .withProsecutorCaseReference("1234243")
                .withInitiationTypeFound(true)
                .withOriginatingOrganisation("TFL")
                .withCpsOrganisation("CPS")
                .withInitiationCode(initiationCode)
                .build();

        final ImmutableList<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendants =
                of(buildDefendantWithInitiationCode(initiationCode));
        Prosecution prosecution = new Prosecution.Builder()
                .withCaseDetails(caseDetails)
                .withChannel(Channel.SPI)
                .withDefendants(defendants)
                .build();

        final ProsecutionWithReferenceData prosecutionWithReferenceData =
                new ProsecutionWithReferenceData(prosecution);
        prosecutionWithReferenceData.setExternalId(EXTERNAL_ID);

        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(
                singletonList(offenceReferenceData()
                        .withCjsOffenceCode(OFFENCE_CODE)
                        .withOffenceStartDate(OFFENCE_START_DATE)
                        .withModeOfTrial("SNONIMP")
                        .build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(asList("J", "C"));
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        referenceDataVO.setProsecutorsReferenceData(getProsecutorsReferenceData());

        return prosecutionWithReferenceData;
    }

    private ProsecutorsReferenceData getProsecutorsReferenceData() {
        return prosecutorsReferenceData()
                .withId(randomUUID())
                .build();
    }

    private List<SummonsCodeReferenceData> buildSummonsCodeReferenceData() {
        return asList(
                summonsCodeReferenceData()
                        .withId(randomUUID())
                        .withSeqNo(INTEGER.next())
                        .withSummonsCode("A")
                        .withSummonsCodeDescription("Application / Complaint")
                        .build(),
                summonsCodeReferenceData()
                        .withId(randomUUID())
                        .withSeqNo(INTEGER.next())
                        .withSummonsCode("W")
                        .withSummonsCodeDescription("Witness Summons")
                        .build(),
                summonsCodeReferenceData()
                        .withId(randomUUID())
                        .withSeqNo(INTEGER.next())
                        .withSummonsCode("B")
                        .withSummonsCodeDescription("Breach offences")
                        .build(),
                summonsCodeReferenceData()
                        .withId(randomUUID())
                        .withSeqNo(INTEGER.next())
                        .withSummonsCode("M")
                        .withSummonsCodeDescription("MCA Case")
                        .build(),
                summonsCodeReferenceData()
                        .withId(randomUUID())
                        .withSeqNo(INTEGER.next())
                        .withSummonsCode("E")
                        .withSummonsCodeDescription("Either Way or serious offences")
                        .build()
        );
    }

    private List<CaseMarker> buildCaseMarkers() {
        return of(caseMarker()
                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                .build());
    }

    private <T> Optional<T> getFirstMatching(final List<Object> eventList, Class<T> clazz) {
        return eventList.stream().filter(clazz::isInstance).map(clazz::cast).findFirst();
    }

    private JsonObject getCaseMarkerCorrectedField() {
        return createObjectBuilder()
                .add("errors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", "1")
                                .add("fieldName", "caseMarkers")
                                .add("value", VALID_CASE_MARKER_CODE))
                )
                .build();
    }

    private JsonObject getDefendantDateOfHearingCorrectedField() {
        return createObjectBuilder()
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", TestConstants.FOURTH_DEFENDANT_ID)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "initialHearing_dateOfHearing")
                                                .add("value", DATE_OF_HEARING)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private JsonObject offenceChargeDateCorrectlySet() {
        return createObjectBuilder()
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_ID)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "offence_chargeDate")
                                                .add("id", TestConstants.OFFENCE_ID.toString())
                                                .add("value", "2100-07-02")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private SummonsApplicationApprovedDetails getSummonsApplicationApprovedDetails(final UUID applicationId) {
        return summonsApplicationApprovedDetails()
                .withApplicationId(applicationId)
                .withCaseId(TestConstants.CASE_ID)
                .withSummonsApprovedOutcome(summonsApprovedOutcome().withSummonsSuppressed(false).withPersonalService(true).withProsecutorCost(PROSECUTOR_COST).build())
                .build();
    }

    private InitialHearing getInitialHearing() {
        InitialHearing initialHearing = InitialHearing.initialHearing()
                .withDateOfHearing("2050-02-04")
                .build();
        return initialHearing;
    }
}

