package uk.gov.moj.cpp.prosecution.casefile.it;


import org.junit.AfterClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.assertErrorsExpected;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.getCustomComparator;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithNotFound;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitsReturnsEmptyList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubProsecutorsReturns404;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

public class ValidationErrorForNoProsecutorAndOrganisationUnitIT extends BaseIT {

    private static final String CASE_MARKER_CODE = "YO";
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private static final String OFFENCE_ID = "032575aa-85e7-11e9-bc42-526af7764f84";
    private static final List<String> ERROR_LIST = Arrays.asList("PROSECUTOR_OUCODE_NOT_RECOGNISED", "SUMMONS_CODE_INVALID", "DATE_OF_HEARING_IN_THE_PAST", "DEFENDANT_PARENT_GUARDIAN_DATE_OF_BIRTH_IN_FUTURE",
            "DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE", "DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID", "DEFENDANT_SELF_DEFINED_ETHNICITY_INVALID", "DEFENDANT_PARENT_GUARDIAN_PRIMARY_EMAIL_ADDRESS_INVALID",
            "DEFENDANT_NATIONALITY_INVALID", "OFFENDER_CODE_IS_INVALID", "DEFENDANT_PARENT_GUARDIAN_SECONDARY_EMAIL_ADDRESS_INVALID", "DEFENDANT_OBSERVED_ETHNICITY_INVALID", "DEFENDANT_PERCEIVED_BIRTH_YEAR_IN_FUTURE",
            "DEFENDANT_PARENT_GUARDIAN_SELF_DEFINED_ETHNICITY_INVALID", "DEFENDANT_PARENT_GUARDIAN_OBSERVED_ETHNICITY_INVALID", "COURT_HEARING_LOCATION_OUCODE_INVALID", "DEFENDANT_DOB_IN_FUTURE",
            "DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID");
    private String caseUrn;
    private String defendantId1;
    private String defendantId2;
    private String defendantId3;
    private String offenceId1;
    private String offenceId2;
    private UUID externalId;

    @BeforeAll
    public static void setupOnce() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubProsecutorsReturns404();
        stubGetOrganisationUnitsReturnsEmptyList();
        stubGetOrganisationUnitWithNotFound();
    }

    @AfterClass
    public static void resetProsecutorAndOrganisationUnit() {
        stubGetOrganisationUnits();
    }

    @BeforeEach
    public void setUp() {
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        externalId = randomUUID();
    }


    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasErrors() {

        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/defendant_validation_error_problems.json", privateEvent.get());

        final Optional<JsonEnvelope> privateEvent2 = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(privateEvent2.isPresent(), is(true));
        assertErrorsExpected("expected/case_validation_error_problems.json", privateEvent2.get());

        final String expectedErrorsPayload = readFile("expected/expected_case_errors.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "B", "2022-04-04", OFFENCE_ID,
                ERROR_LIST.toArray(new String[0])));


    }

    @Test
    public void shouldRaiseProsecutionRejectedEventWhenCPPIPayloadHasRejectionErrors() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.givenInitiateSummonsCaseIsRaisedWithErrorsByChannel("CPPI");
        helper.thenProsecutionShouldRejectTheCase("public.prosecutioncasefile.prosecution-rejected", "expected/expected_cppi_rejection_errors.json");
    }

    @Test
    public void shouldRaiseProsecutionRejectedEventWhenMCCPayloadHasRejectionErrors() {
        final InitiateCCProsecutionHelper helper = new InitiateCCProsecutionHelper();
        helper.givenInitiateSummonsCaseIsRaisedWithErrorsByChannel("MCC");
        helper.thenProsecutionShouldRejectTheCase("public.prosecutioncasefile.manual-case-received", "expected/expected_mcc_rejection_errors.json");
    }

    @Test
    public void shouldRaiseValidationErrorForCorporateDefendantWhenPayloadHasErrors() {
        stubGetOrganisationUnitWithOneCourtroom();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-corporate-cc-prosecution-with-error.json");
        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/organisation_defendant_validation_error_problems.json", privateEvent.get());

        final Optional<JsonEnvelope> privateEvent2 = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(privateEvent2.isPresent(), is(true));
        assertErrorsExpected("expected/case_validation_error_problems.json", privateEvent2.get());


        final String expectedErrorsPayload = readFile("expected/expected_corporate_case_errors.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "B", "05-04-2015", OFFENCE_ID,
                "PROSECUTOR_OUCODE_NOT_RECOGNISED", "SUMMONS_CODE_INVALID", "DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID", "DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID"));

    }

    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasErrorsWithInvalidCourtHearingLocation() {

        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-additional-court-hearing-invalid-location.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/defendant_validation_error_problems_also_with_invalid_court_hearing_location.json", privateEvent.get());

        final Optional<JsonEnvelope> privateEvent2 = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(privateEvent2.isPresent(), is(true));
        assertErrorsExpected("expected/case_validation_error_problems.json", privateEvent2.get());

        final String expectedErrorsPayload = readFile("expected/expected_case_errors_with_invalid_court_hearing_location.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "B", "2022-04-04", OFFENCE_ID,
                ERROR_LIST.toArray(new String[0])));

    }


    public String replaceValues(final String payload, final String caseId) {
        return payload
                .replace("CASE-ID", caseId)
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE3", this.defendantId3)
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("DATE_OF_HEARING", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replace("CHANNEL_TYPE", "SPI")
                .replace("SUMMONS_CODE", "M");
    }


}
