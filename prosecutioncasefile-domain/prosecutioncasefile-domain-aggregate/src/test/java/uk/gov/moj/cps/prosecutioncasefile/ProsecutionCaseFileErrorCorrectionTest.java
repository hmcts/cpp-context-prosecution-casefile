package uk.gov.moj.cps.prosecutioncasefile;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData.bailStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData.custodyStatusReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData.organisationUnitReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ARREST_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.BIRTH_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CASE_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.COURT_HEARING_LOCATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CPS_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.CUSTODY_STATUS;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DATE_OF_HEARING;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.DEFENDANT_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_CHARGE_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_CODE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_COMMITTED_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.OFFENCE_START_DATE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.ORIGINATING_ORGANISATION;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.POLICE_SYSTEM_ID;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.PROSECUTOR_CASE_REFERENCE;
import static uk.gov.moj.cps.prosecutioncasefile.TestConstants.SURNAME;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.DefendantValidationPassed;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.BailStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CustodyStatusReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.OrganisationUnitWithCourtroomRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.InitiationTypesRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ResolvedCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ValidationCompleted;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileErrorCorrectionTest {

    private static final String DEFENDANT_WITH_ERROR_ID = randomUUID().toString();
    private static final String DEFENDANT_WITH_ERROR_ID2 = randomUUID().toString();
    private static final String VALID_CASE_MARKER_CODE = "AB";
    private static final String VALID_CPS_ORGANISATION = "A30AB00";
    private static final String INVALID_CASE_MARKER_CODE = "BC";
    private static final String DATE_OF_HEARING_IN_PAST = "1950-10-03";
    private static final String VALID_INITIATION_CODE = "C";
    private static final String INVALID_INITIATION_CODE = "D";
    private static final String BAIL_CONDITIONS = "Cannot leave the country";
    private static final LocalDate OFFENCE_CHARGE_DATE_IN_FUTURE = LocalDate.now().plusMonths(4);
    private static final LocalDate OFFENCE_ARREST_DATE_IN_FUTURE = LocalDate.now().plusMonths(4);
    private static final UUID OFFENCE_ID = randomUUID();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    private List<CaseRefDataEnricher> caseRefDataEnrichers;
    @Mock
    private InitiationTypesRefDataEnricher initiationTypesRefDataEnricher;

    @Mock
    private OrganisationUnitWithCourtroomRefDataEnricher organisationUnitWithCourtroomRefDataEnricher;

    private List<DefendantRefDataEnricher> defendantRefDataEnrichers;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;
    private JsonObject correctedFields;
    private JsonObject partiallyCorrectedFields;

    private ProsecutionCaseFile prosecutionCaseFile;

    @BeforeEach
    public void setup() {
        when(referenceDataQueryService.retrieveBailStatuses()).thenReturn(buildBailStatuses());
        when(referenceDataQueryService.getCaseMarkerDetails()).thenReturn(buildCaseMarkers());
        when(referenceDataQueryService.retrieveOffenceData(any(Offence.class), any(String.class))).thenReturn(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).build()));
        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ProsecutionWithReferenceData prosecutionWithReferenceData = (ProsecutionWithReferenceData) args[0];
            prosecutionWithReferenceData.setReferenceDataVO(getReferenceDataVO());
            return null;
        }).when(initiationTypesRefDataEnricher).enrich(any(ProsecutionWithReferenceData.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            DefendantsWithReferenceData defendantsWithReferenceData = (DefendantsWithReferenceData) args[0];
            final ReferenceDataVO referenceDataVO = defendantsWithReferenceData.getReferenceDataVO();
            final CaseDetails.Builder caseDetailsBuilder = new CaseDetails.Builder();
            defendantsWithReferenceData.setCaseDetails(caseDetailsBuilder.withInitiationCode(VALID_INITIATION_CODE).build());
            referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(ofNullable(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));
            return null;
        }).when(organisationUnitWithCourtroomRefDataEnricher).enrich(any(DefendantsWithReferenceData.class));

        caseRefDataEnrichers = singletonList(initiationTypesRefDataEnricher);
        defendantRefDataEnrichers = singletonList(organisationUnitWithCourtroomRefDataEnricher);

        correctedFields = getCorrectedFields();

        final JsonObject caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject = getCaseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject();
        final JsonObject defendantWithInValidDateOfHearingAndChargeDateJsonObject = getDefendantWithInValidDateOfHearingAndChargeDateJsonObject();

        final ProsecutionWithReferenceData prosecutionWithReferenceDataErrors = getProsecutionWithReferenceDataErrors(DEFENDANT_WITH_ERROR_ID);

        when(objectToJsonObjectConverter.convert(any(CaseDetails.class))).thenReturn(caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject);

        when(objectToJsonObjectConverter.convert(prosecutionWithReferenceDataErrors.getProsecution()
                .getDefendants().get(0))).thenReturn(defendantWithInValidDateOfHearingAndChargeDateJsonObject);

        final ProsecutionWithReferenceData prosecutionWithReferenceDataNoErrors = getProsecutionWithReferenceDataNoErrors(DEFENDANT_WITH_ERROR_ID);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CaseDetails.class)))
                .thenReturn(prosecutionWithReferenceDataNoErrors.getProsecution().getCaseDetails());
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.class)))
                .thenReturn(prosecutionWithReferenceDataNoErrors.getProsecution().getDefendants().get(0));

        prosecutionCaseFile = new ProsecutionCaseFile();

    }

    @Test
    public void shouldRaiseValidationCompletedEventWhenProsecutionFoundWithMultipleCaseAndDefendantErrors() {
        correctedFields = getCorrectedFieldsForMultipleDefendants();

        final JsonObject caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject = getCaseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject();
        final JsonObject defendantWithInValidDateOfHearingAndChargeDateJsonObject = getDefendantWithInValidDateOfHearingAndChargeDateJsonObject();
        final JsonObject defendantWithInValidDateOfHearingAndArrestDateJsonObject = getDefendantWithInValidDateOfHearingAndArrestDateJsonObject();

        final ProsecutionWithReferenceData prosecutionWithReferenceDataErrors = getProsecutionWithReferenceDataMultipleCaseMultipleDefendantErrors(DEFENDANT_WITH_ERROR_ID, DEFENDANT_WITH_ERROR_ID2);

        when(objectToJsonObjectConverter.convert(any(CaseDetails.class))).thenReturn(caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject);

        when(objectToJsonObjectConverter.convert(prosecutionWithReferenceDataErrors.getProsecution()
                .getDefendants().get(0))).thenReturn(defendantWithInValidDateOfHearingAndChargeDateJsonObject);
        when(objectToJsonObjectConverter.convert(prosecutionWithReferenceDataErrors.getProsecution()
                .getDefendants().get(1))).thenReturn(defendantWithInValidDateOfHearingAndArrestDateJsonObject);

        final ProsecutionWithReferenceData prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors = getProsecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors(DEFENDANT_WITH_ERROR_ID, DEFENDANT_WITH_ERROR_ID2);

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CaseDetails.class)))
                .thenReturn(prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors.getProsecution().getCaseDetails());
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.class)))
                .thenReturn(prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors.getProsecution().getDefendants().get(0), prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors.getProsecution().getDefendants().get(1));

        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        List<Object> eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(3));

        final DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventList.get(0);
        assertThat(defendantValidationFailed.getProblems().size(), is(3));
        assertThat(defendantValidationFailed.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed.getProblems().get(1).getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));
        assertThat(defendantValidationFailed.getProblems().get(2).getCode(), is("CHARGE_DATE_IN_FUTURE"));
        assertThat(defendantValidationFailed.getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(defendantValidationFailed.getUrn(), is(PROSECUTOR_CASE_REFERENCE));

        final DefendantValidationFailed defendantValidationFailed2 = (DefendantValidationFailed) eventList.get(1);
        assertThat(defendantValidationFailed2.getProblems().size(), is(2));
        assertThat(defendantValidationFailed2.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed2.getProblems().get(1).getCode(), is("ARREST_DATE_IN_FUTURE"));

        assertThat(defendantValidationFailed2.getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(defendantValidationFailed2.getUrn(), is(PROSECUTOR_CASE_REFERENCE));

        final CaseValidationFailed caseValidationFailed = (CaseValidationFailed) eventList.get(2);
        assertThat(caseValidationFailed.getProblems().size(), is(2));
        assertThat(caseValidationFailed.getProblems().get(0).getCode(), is("CASE_INITIATION_CODE_INVALID"));
        assertThat(caseValidationFailed.getProblems().get(1).getCode(), is("CASE_MARKER_IS_INVALID"));
        assertThat(caseValidationFailed.getInitialHearing().getDateOfHearing(), is(DATE_OF_HEARING_IN_PAST));

        objectStream = prosecutionCaseFile.receiveErrorCorrections(correctedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(3)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getCaseDetailsWithValidInitiationCodeAndCaseMarkerJsonObject()));
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(1), is(getDefendantWithValidDateOfHearingAndChargeDateJsonObject()));
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(2), is(getDefendantWithValidDateOfHearingAndArrestDateJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(5));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(2), is(instanceOf(CcCaseReceived.class)));
        assertThat(eventList.get(3), is(instanceOf(ValidationCompleted.class)));
        assertThat(eventList.get(4), is(instanceOf(ResolvedCase.class)));
    }

    @Test
    public void shouldRaiseValidationCompletedEventWhenProsecutionFoundWithMultipleCaseAndDefendantErrorsPartiallyFixed() {

        final JsonObject caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject = getCaseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject();
        final JsonObject defendantWithInValidDateOfHearingAndChargeDateJsonObject = getDefendantWithInValidDateOfHearingAndChargeDateJsonObject();
        final JsonObject defendantWithInValidDateOfHearingAndArrestDateJsonObject = getDefendantWithInValidDateOfHearingAndArrestDateJsonObject();

        final ProsecutionWithReferenceData prosecutionWithReferenceDataErrors = getProsecutionWithReferenceDataMultipleCaseMultipleDefendantErrors(DEFENDANT_WITH_ERROR_ID, DEFENDANT_WITH_ERROR_ID2);

        when(objectToJsonObjectConverter.convert(any(CaseDetails.class))).thenReturn(caseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject);

        when(objectToJsonObjectConverter.convert(prosecutionWithReferenceDataErrors.getProsecution()
                .getDefendants().get(0))).thenReturn(defendantWithInValidDateOfHearingAndChargeDateJsonObject);
        when(objectToJsonObjectConverter.convert(prosecutionWithReferenceDataErrors.getProsecution()
                .getDefendants().get(1))).thenReturn(defendantWithInValidDateOfHearingAndArrestDateJsonObject);

        final ProsecutionWithReferenceData prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors = getProsecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors(DEFENDANT_WITH_ERROR_ID, DEFENDANT_WITH_ERROR_ID2);

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.class)))
                .thenReturn(prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors.getProsecution().getDefendants().get(0), prosecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors.getProsecution().getDefendants().get(1));

        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        List<Object> eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(3));


        final DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventList.get(0);
        assertThat(defendantValidationFailed.getProblems().size(), is(3));
        assertThat(defendantValidationFailed.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed.getProblems().get(1).getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));
        assertThat(defendantValidationFailed.getProblems().get(2).getCode(), is("CHARGE_DATE_IN_FUTURE"));
        assertThat(defendantValidationFailed.getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(defendantValidationFailed.getUrn(), is(PROSECUTOR_CASE_REFERENCE));

        final DefendantValidationFailed defendantValidationFailed2 = (DefendantValidationFailed) eventList.get(1);
        assertThat(defendantValidationFailed2.getProblems().size(), is(2));
        assertThat(defendantValidationFailed2.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed2.getProblems().get(1).getCode(), is("ARREST_DATE_IN_FUTURE"));
        assertThat(defendantValidationFailed2.getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(defendantValidationFailed2.getUrn(), is(PROSECUTOR_CASE_REFERENCE));

        final CaseValidationFailed caseValidationFailed = (CaseValidationFailed) eventList.get(2);
        assertThat(caseValidationFailed.getProblems().size(), is(2));
        assertThat(caseValidationFailed.getProblems().get(0).getCode(), is("CASE_INITIATION_CODE_INVALID"));
        assertThat(caseValidationFailed.getProblems().get(1).getCode(), is("CASE_MARKER_IS_INVALID"));
        assertThat(caseValidationFailed.getInitialHearing().getDateOfHearing(), is(DATE_OF_HEARING_IN_PAST));

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(CaseDetails.class)))
                .thenReturn(getProsecutionWithReferenceDataMultipleCaseMultipleDefendantWithCaseErrors(DEFENDANT_WITH_ERROR_ID, DEFENDANT_WITH_ERROR_ID2).getProsecution().getCaseDetails());

        partiallyCorrectedFields = getPartiallyCorrectedFieldsForMultipleDefendants();
        objectStream = prosecutionCaseFile.receiveErrorCorrections(partiallyCorrectedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(3)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getCaseDetailsWithValidInitiationCodeAndOneInvalidCaseMarkerJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(2), is(instanceOf(CaseValidationFailed.class)));
        assertThat(eventList.get(3), is(instanceOf(ValidationCompleted.class)));
    }


    @Test
    public void shouldRaiseValidationCompletedEventWhenProsecutionFoundWithErrors() {
        final ProsecutionWithReferenceData prosecutionWithReferenceDataErrors = getProsecutionWithReferenceDataErrors(DEFENDANT_WITH_ERROR_ID);
        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        List<Object> eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(2));

        final DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventList.get(0);
        assertThat(defendantValidationFailed.getProblems().size(), is(3));
        assertThat(defendantValidationFailed.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed.getProblems().get(1).getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));

        final CaseValidationFailed caseValidationFailed = (CaseValidationFailed) eventList.get(1);
        assertThat(caseValidationFailed.getProblems().size(), is(2));
        assertThat(caseValidationFailed.getProblems().get(0).getCode(), is("CASE_INITIATION_CODE_INVALID"));
        assertThat(caseValidationFailed.getProblems().get(1).getCode(), is("CASE_MARKER_IS_INVALID"));
        assertThat(caseValidationFailed.getInitialHearing().getDateOfHearing(), is(DATE_OF_HEARING_IN_PAST));


        objectStream = prosecutionCaseFile.receiveErrorCorrections(correctedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(2)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getCaseDetailsWithValidInitiationCodeAndCaseMarkerJsonObject()));
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(1), is(getDefendantWithValidDateOfHearingAndChargeDateJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(CcCaseReceived.class)));
        assertThat(eventList.get(2), is(instanceOf(ValidationCompleted.class)));
        assertThat(eventList.get(3), is(instanceOf(ResolvedCase.class)));
    }


    @Test
    public void shouldRaiseValidationCompletedEventWhenCpsOrganisationFoundWithErrors() {
        final ProsecutionWithReferenceData prosecutionWithReferenceDataErrors = getProsecutionWithCpsOrganisationReferenceDataErrors(DEFENDANT_WITH_ERROR_ID);
        Stream<Object> objectStream = prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        List<Object> eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(2));

        final CaseValidationFailed caseValidationFailed = (CaseValidationFailed) eventList.get(1);
        assertThat(caseValidationFailed.getProblems().size(), is(0));
        assertThat(caseValidationFailed.getInitialHearing().getDateOfHearing(), is(DATE_OF_HEARING_IN_PAST));

        objectStream = prosecutionCaseFile.receiveErrorCorrections(correctedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(2)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getCaseDetailsWithValidInitiationCodeAndCaseMarkerJsonObject()));
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(1), is(getDefendantWithValidDateOfHearingAndChargeDateJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(CcCaseReceived.class)));
        assertThat(eventList.get(2), is(instanceOf(ValidationCompleted.class)));
        assertThat(eventList.get(3), is(instanceOf(ResolvedCase.class)));
    }

    @Test
    public void shouldRaiseValidationCompletedEventWhenReceivedDefendantFoundWithErrors() {
        final ProsecutionWithReferenceData prosecutionWithReferenceDataNoErrors = getProsecutionWithReferenceDataNoErrors(DEFENDANT_ID);
        prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataNoErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        Stream<Object> objectStream = prosecutionCaseFile.addErrorCorrectedDefendantsForSPI(CASE_ID, randomUUID(), getDefendantsWithInvalidDateOfHearingAndChargeDateRefData(), referenceDataQueryService,false);
        List<Object> eventList = objectStream.collect(Collectors.toList());
        final DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventList.get(0);
        assertThat(defendantValidationFailed.getProblems().size(), is(3));
        assertThat(defendantValidationFailed.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed.getProblems().get(1).getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));

        objectStream = prosecutionCaseFile.receiveErrorCorrections(correctedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getDefendantWithValidDateOfHearingAndChargeDateJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(ProsecutionDefendantsAdded.class)));
        assertThat(eventList.get(2), is(instanceOf(ValidationCompleted.class)));
        assertThat(eventList.get(3), is(instanceOf(ResolvedCase.class)));

        ProsecutionDefendantsAdded prosecutionDefendantsAdded = (ProsecutionDefendantsAdded) eventList.get(1);
        assertThat(prosecutionDefendantsAdded.getChannel(), is(SPI));
    }

    @Test
    public void shouldRaiseValidationCompletedEventWhenReceivedDefendantFoundWithErrorsPartiallyFixed() {
        final ProsecutionWithReferenceData prosecutionWithReferenceDataNoErrors = getProsecutionWithReferenceDataNoErrors(DEFENDANT_ID);
        prosecutionCaseFile.receiveCCCase(prosecutionWithReferenceDataNoErrors,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        Stream<Object> objectStream = prosecutionCaseFile.addErrorCorrectedDefendantsForSPI(CASE_ID, randomUUID(), getDefendantsWithInvalidDateOfHearingAndChargeDateRefData(), referenceDataQueryService,false);
        List<Object> eventList = objectStream.collect(Collectors.toList());
        final DefendantValidationFailed defendantValidationFailed = (DefendantValidationFailed) eventList.get(0);
        assertThat(defendantValidationFailed.getProblems().size(), is(3));
        assertThat(defendantValidationFailed.getProblems().get(0).getCode(), is("DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE"));
        assertThat(defendantValidationFailed.getProblems().get(1).getCode(), is("DATE_OF_HEARING_IN_THE_PAST"));

        objectStream = prosecutionCaseFile.receiveErrorCorrections(correctedFields,
                objectToJsonObjectConverter,
                jsonObjectToObjectConverter,
                caseRefDataEnrichers,
                defendantRefDataEnrichers,
                referenceDataQueryService);

        verify(jsonObjectToObjectConverter, times(1)).convert(jsonObjectArgumentCaptor.capture(), any());
        assertThat(jsonObjectArgumentCaptor.getAllValues().get(0), is(getDefendantWithValidDateOfHearingAndChargeDateJsonObject()));

        eventList = objectStream.collect(Collectors.toList());
        assertThat(eventList.size(), is(4));
        assertThat(eventList.get(0), is(instanceOf(DefendantValidationPassed.class)));
        assertThat(eventList.get(1), is(instanceOf(ProsecutionDefendantsAdded.class)));
        assertThat(eventList.get(2), is(instanceOf(ValidationCompleted.class)));
        assertThat(eventList.get(3), is(instanceOf(ResolvedCase.class)));
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataMultipleCaseMultipleDefendantErrors(final String defendantId1, final String defendantId2) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithInValidDateOfHearingAndChargeDate(defendantId1, "          ", SURNAME, BIRTH_DATE));
        defendants.add(buildDefendantWithInValidDateOfHearingAndArrestDate(defendantId2, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithInvalidInitiationCodeAndCaseMarkerRefData(defendants);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataMultipleCaseMultipleDefendantNoErrors(final String defendantId1, final String defendantId2) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithValidDateOfHearingAndChargeDate(defendantId1, "          ", SURNAME, BIRTH_DATE));
        defendants.add(buildDefendantWithValidDateOfHearingAndArrestDate(defendantId2, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithValidInitiationCodeAndCaseMarkerRefData(defendants);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataMultipleCaseMultipleDefendantWithCaseErrors(final String defendantId1, final String defendantId2) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithValidDateOfHearingAndChargeDate(defendantId1, "          ", SURNAME, BIRTH_DATE));
        defendants.add(buildDefendantWithValidDateOfHearingAndArrestDate(defendantId2, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithValidInitiationCodeAndWithMixedValidAndInvalidCaseMarkerRefData(defendants);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataErrors(final String defendantId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithInValidDateOfHearingAndChargeDate(defendantId, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithInvalidInitiationCodeAndCaseMarkerRefData(defendants);
    }

    private ProsecutionWithReferenceData getProsecutionWithCpsOrganisationReferenceDataErrors(final String defendantId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithInValidDateOfHearingAndChargeDate(defendantId, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithInvalidCpsOrganisationRefData(defendants);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceDataNoErrors(final String defendantId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(buildDefendantWithValidDateOfHearingAndChargeDate(defendantId, "          ", SURNAME, BIRTH_DATE));

        return getProsecutionWithValidInitiationCodeAndCaseMarkerRefData(defendants);
    }

    private DefendantsWithReferenceData getDefendantsWithInvalidDateOfHearingAndChargeDateRefData() {

        final ReferenceDataVO referenceDataVO = getReferenceDataVO();
        referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(ofNullable(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));

        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList = new ArrayList<>();
        defendantList.add(buildDefendantWithInValidDateOfHearingAndChargeDate(DEFENDANT_WITH_ERROR_ID, "          ", SURNAME, BIRTH_DATE));

        final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendantList);
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantsWithReferenceData.setCaseDetails(caseDetails()
                .withCaseId(CASE_ID)
                .withInitiationCode(VALID_INITIATION_CODE)
                .build());
        return defendantsWithReferenceData;
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithInValidDateOfHearingAndArrestDate(final String defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return populateDefendantBuilder(firstName, lastName, dateOfBirth)
                .withId(defendantId)
                .withInitialHearing(InitialHearing.initialHearing().withCourtHearingLocation(COURT_HEARING_LOCATION).withDateOfHearing(DATE_OF_HEARING_IN_PAST).build())
                .withOffences(getOffencesWithArrestDate(OFFENCE_ARREST_DATE_IN_FUTURE))
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithValidDateOfHearingAndArrestDate(final String defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return populateDefendantBuilderWithArrestDate(firstName, lastName, dateOfBirth)
                .withId(defendantId)
                .withInitiationCode(VALID_INITIATION_CODE)
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithInValidDateOfHearingAndChargeDate(final String defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return populateDefendantBuilder(firstName, lastName, dateOfBirth)
                .withId(defendantId)
                .withProsecutorDefendantReference(defendantId)
                .withInitialHearing(InitialHearing.initialHearing().withCourtHearingLocation(COURT_HEARING_LOCATION).withDateOfHearing(DATE_OF_HEARING_IN_PAST).build())
                .withOffences(getOffencesWithChargeDate(OFFENCE_CHARGE_DATE_IN_FUTURE))
                .withInitiationCode(VALID_INITIATION_CODE)
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant buildDefendantWithValidDateOfHearingAndChargeDate(final String defendantId, final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return populateDefendantBuilder(firstName, lastName, dateOfBirth)
                .withId(defendantId)
                .withProsecutorDefendantReference(defendantId)
                .withInitiationCode(VALID_INITIATION_CODE)
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.Builder populateDefendantBuilderWithArrestDate(final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return defendant()
                .withId(DEFENDANT_ID)
                .withProsecutorDefendantReference(DEFENDANT_ID)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .withBailConditions(BAIL_CONDITIONS)
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withOffences(getOffencesWithChargeDate(ARREST_DATE))
                .withInitiationCode(VALID_INITIATION_CODE);
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.Builder populateDefendantBuilder(final String firstName, final String lastName, final LocalDate dateOfBirth) {
        return defendant()
                .withId(DEFENDANT_ID)
                .withId(DEFENDANT_ID)
                .withIndividual(individual()
                        .withPersonalInformation(personalInformation()
                                .withFirstName(firstName)
                                .withLastName(lastName).build())
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .withBailConditions(BAIL_CONDITIONS)
                        .build())
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(DATE_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .withCustodyStatus(CUSTODY_STATUS)
                .withOffences(getOffencesWithChargeDate(OFFENCE_CHARGE_DATE));
    }

    private List<Offence> getOffencesWithArrestDate(final LocalDate offenceArrestDate) {
        return singletonList(offence()
                .withOffenceId(OFFENCE_ID)
                .withArrestDate(offenceArrestDate)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withChargeDate(OFFENCE_CHARGE_DATE)
                .withOffenceDateCode(2)
                .build());
    }

    private List<Offence> getOffencesWithChargeDate(final LocalDate offenceChargeDate) {
        return singletonList(offence()
                .withOffenceId(OFFENCE_ID)
                .withArrestDate(ARREST_DATE)
                .withOffenceCode(OFFENCE_CODE)
                .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                .withChargeDate(offenceChargeDate)
                .withOffenceDateCode(2)
                .build());
    }

    private ProsecutionWithReferenceData getProsecutionWithInvalidInitiationCodeAndCaseMarkerRefData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        return getProsecutionWithReferenceData(caseDetails()
                .withInitiationCode(INVALID_INITIATION_CODE)
                .withCaseMarkers(Arrays.asList(
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build(),
                        caseMarker()
                                .withMarkerTypeCode(INVALID_CASE_MARKER_CODE)
                                .build()))
                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                .withPoliceSystemId(POLICE_SYSTEM_ID)
                .withCpsOrganisation(CPS_ORGANISATION)
                .build(), defendantList);
    }


    private ProsecutionWithReferenceData getProsecutionWithInvalidCpsOrganisationRefData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        return getProsecutionWithReferenceData(caseDetails()
                .withInitiationCode(VALID_INITIATION_CODE)
                .withCaseMarkers(Arrays.asList(
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build(),
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build()))
                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                .withPoliceSystemId(POLICE_SYSTEM_ID)
                .build(), defendantList);
    }

    private ProsecutionWithReferenceData getProsecutionWithValidInitiationCodeAndCaseMarkerRefData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        return getProsecutionWithReferenceData(caseDetails()
                .withInitiationCode(VALID_INITIATION_CODE)
                .withCaseMarkers(Arrays.asList(
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build(),
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build()))
                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                .withCpsOrganisation(CPS_ORGANISATION)
                .build(), defendantList);
    }

    private ProsecutionWithReferenceData getProsecutionWithValidInitiationCodeAndWithMixedValidAndInvalidCaseMarkerRefData(final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        return getProsecutionWithReferenceData(caseDetails()
                .withInitiationCode(VALID_INITIATION_CODE)
                .withCaseMarkers(Arrays.asList(
                        caseMarker()
                                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                                .build(),
                        caseMarker()
                                .withMarkerTypeCode(INVALID_CASE_MARKER_CODE)
                                .build()))
                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                .build(), defendantList);
    }

    private ProsecutionWithReferenceData getProsecutionWithReferenceData(final CaseDetails caseDetails, final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantList) {
        final ReferenceDataVO referenceDataVO = getReferenceDataVO();
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(caseDetails)
                .withDefendants(defendantList)
                .withChannel(SPI)
                .build());
        prosecutionWithReferenceData.setReferenceDataVO(referenceDataVO);
        return prosecutionWithReferenceData;
    }

    private ReferenceDataVO getReferenceDataVO() {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setOffenceReferenceData(singletonList(offenceReferenceData().withCjsOffenceCode(OFFENCE_CODE).withOffenceStartDate(OFFENCE_START_DATE).build()));
        referenceDataVO.addCountryNationalityReferenceData(referenceDataCountryNationality().build());
        referenceDataVO.setInitiationTypes(Arrays.asList("J", "C"));
        referenceDataVO.setProsecutorsReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(UUID.randomUUID())
                .build());
        return referenceDataVO;
    }

    private JsonObject getDefendantWithValidDateOfHearingAndArrestDateJsonObject() {
        return createObjectBuilder()
                .add("id", DEFENDANT_WITH_ERROR_ID2)
                .add("initialHearing", createObjectBuilder()
                        .add("dateOfHearing", DATE_OF_HEARING)
                        .build())
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", OFFENCE_ID.toString())
                                .add("arrestDate", ARREST_DATE.toString())
                                .build())
                        .build())
                .add("initiationCode", VALID_INITIATION_CODE)
                .build();
    }


    private JsonObject getDefendantWithInValidDateOfHearingAndArrestDateJsonObject() {
        return createObjectBuilder()
                .add("id", DEFENDANT_WITH_ERROR_ID2)
                .add("initialHearing", createObjectBuilder()
                        .add("dateOfHearing", DATE_OF_HEARING_IN_PAST)
                        .build())
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", OFFENCE_ID.toString())
                                .add("arrestDate", OFFENCE_ARREST_DATE_IN_FUTURE.toString())
                                .build())
                        .build())
                .add("initiationCode", VALID_INITIATION_CODE)
                .build();
    }


    private JsonObject getDefendantWithValidDateOfHearingAndChargeDateJsonObject() {
        return createObjectBuilder()
                .add("id", DEFENDANT_WITH_ERROR_ID)
                .add("initialHearing", createObjectBuilder()
                        .add("dateOfHearing", DATE_OF_HEARING)
                        .build())
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", OFFENCE_ID.toString())
                                .add("chargeDate", OFFENCE_CHARGE_DATE.toString())
                                .build())
                        .build())
                .add("cpsOrganisation", CPS_ORGANISATION)
                .add("postingDate", OFFENCE_CHARGE_DATE.toString())
                .build();
    }

    private JsonObject getValidCpsOrganisationJsonObject() {
        return createObjectBuilder()
                .add("cpsOrganisation", VALID_CPS_ORGANISATION)
                .build();
    }


    private JsonObject getDefendantWithInValidDateOfHearingAndChargeDateJsonObject() {
        return createObjectBuilder()
                .add("id", DEFENDANT_WITH_ERROR_ID)
                .add("initialHearing", createObjectBuilder()
                        .add("dateOfHearing", DATE_OF_HEARING_IN_PAST)
                        .build())
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", OFFENCE_ID.toString())
                                .add("chargeDate", OFFENCE_CHARGE_DATE_IN_FUTURE.toString())
                                .build())
                        .build())
                .add("cpsOrganisation", CPS_ORGANISATION)
                .build();
    }


    private JsonObject getCaseDetailsWithValidInitiationCodeAndOneInvalidCaseMarkerJsonObject() {
        return createObjectBuilder()
                .add("initiationCode", VALID_INITIATION_CODE)
                .add("caseMarkers",
                        createArrayBuilder()
                                .add(createObjectBuilder().add("markerTypeCode", VALID_CASE_MARKER_CODE).build())
                                .add(createObjectBuilder().add("markerTypeCode", INVALID_CASE_MARKER_CODE).build())
                                .build())
                .add("prosecutorCaseReference", PROSECUTOR_CASE_REFERENCE)
                .add("policeSystemId", POLICE_SYSTEM_ID)
                .add("cpsOrganisation", CPS_ORGANISATION)
                .build();
    }

    private JsonObject getCaseDetailsWithInvalidInitiationCodeAndCaseMarkerJsonObject() {
        return createObjectBuilder()
                .add("initiationCode", INVALID_INITIATION_CODE)
                .add("caseMarkers",
                        createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder().add("markerTypeCode", VALID_CASE_MARKER_CODE).build())
                                .add(JsonObjects.createObjectBuilder().add("markerTypeCode", INVALID_CASE_MARKER_CODE).build())
                                .build())
                .add("prosecutorCaseReference", PROSECUTOR_CASE_REFERENCE)
                .add("policeSystemId", POLICE_SYSTEM_ID)
                .add("cpsOrganisation", CPS_ORGANISATION)
                .build();
    }

    private JsonObject getCaseDetailsWithValidInitiationCodeAndCaseMarkerJsonObject() {
        return createObjectBuilder()
                .add("initiationCode", VALID_INITIATION_CODE)
                .add("caseMarkers",
                        createArrayBuilder()
                                .add(createObjectBuilder().add("markerTypeCode", VALID_CASE_MARKER_CODE).build())
                                .add(createObjectBuilder().add("markerTypeCode", VALID_CASE_MARKER_CODE).build())
                                .build())
                .add("prosecutorCaseReference", PROSECUTOR_CASE_REFERENCE)
                .add("policeSystemId", POLICE_SYSTEM_ID)
                .add("cpsOrganisation", CPS_ORGANISATION)
                .build();
    }

    private JsonObject getPartiallyCorrectedFieldsForMultipleDefendants() {
        return createObjectBuilder()
                .add("errors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("fieldName", "initiationCode")
                                .add("value", "C")
                                .build())
                        .build())
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_WITH_ERROR_ID)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "initialHearing_dateOfHearing")
                                                .add("value", DATE_OF_HEARING)
                                                .build())
                                        .add(createObjectBuilder()
                                                .add("id", OFFENCE_ID.toString())
                                                .add("fieldName", "offence_chargeDate")
                                                .add("value", OFFENCE_CHARGE_DATE.toString())
                                                .build())
                                        .build())
                                .build())
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_WITH_ERROR_ID2)
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

    private JsonObject getCorrectedFieldsForMultipleDefendants() {
        return createObjectBuilder()
                .add("errors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("fieldName", "initiationCode")
                                .add("value", "C")
                                .build())
                        .add(createObjectBuilder()
                                .add("id", "1")
                                .add("fieldName", "caseMarkers")
                                .add("value", VALID_CASE_MARKER_CODE)
                                .build())

                        .build())
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_WITH_ERROR_ID)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "initialHearing_dateOfHearing")
                                                .add("value", DATE_OF_HEARING)
                                                .build())
                                        .add(createObjectBuilder()
                                                .add("id", OFFENCE_ID.toString())
                                                .add("fieldName", "offence_chargeDate")
                                                .add("value", OFFENCE_CHARGE_DATE.toString())
                                                .build())
                                        .build())
                                .build())
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_WITH_ERROR_ID2)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "initialHearing_dateOfHearing")
                                                .add("value", DATE_OF_HEARING)
                                                .build())
                                        .add(createObjectBuilder()
                                                .add("id", OFFENCE_ID.toString())
                                                .add("fieldName", "offence_arrestDate")
                                                .add("value", ARREST_DATE.toString())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private JsonObject getCorrectedFields() {
        return createObjectBuilder()
                .add("errors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("fieldName", "initiationCode")
                                .add("value", "C")
                                .build())
                        .add(createObjectBuilder()
                                .add("id", "1")
                                .add("fieldName", "caseMarkers")
                                .add("value", VALID_CASE_MARKER_CODE)
                                .build())

                        .build())
                .add("defendants", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DEFENDANT_WITH_ERROR_ID)
                                .add("errors", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("fieldName", "initialHearing_dateOfHearing")
                                                .add("value", DATE_OF_HEARING)
                                                .build())
                                        .add(createObjectBuilder()
                                                .add("id", OFFENCE_ID.toString())
                                                .add("fieldName", "offence_chargeDate")
                                                .add("value", OFFENCE_CHARGE_DATE.toString())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private List<CaseMarker> buildCaseMarkers() {
        return singletonList(caseMarker()
                .withMarkerTypeCode(VALID_CASE_MARKER_CODE)
                .build());
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

    private ProsecutorsReferenceData getProsecutionReferenceData() {
        return ProsecutorsReferenceData.prosecutorsReferenceData()
                .withId(UUID.randomUUID())
                .build();
    }

}
