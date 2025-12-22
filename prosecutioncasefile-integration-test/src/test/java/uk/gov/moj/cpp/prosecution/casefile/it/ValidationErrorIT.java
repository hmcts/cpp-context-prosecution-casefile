package uk.gov.moj.cpp.prosecution.casefile.it;

import org.hamcrest.CoreMatchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateSjpProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.ResolveCaseErrorsHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.QueryHelper.verifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.assertErrorsExpected;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.assertErrorsExpectedForCivil;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.assertExpctedErrorPayload;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.assertNoErrorsExpected;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.getCustomComparator;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrorsEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrorsForDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCasesAreEmptyCollection;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.replaceExpectedValues;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForGenericOffence;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceLocationRequired;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesWithBackDuty;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubNonSjpProsecutors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.INVALID_DEFENDANT_INDIVIDUAL_POST_CODE;

public class ValidationErrorIT extends BaseIT {

    private static final String CASE_MARKER_CODE = "YO";
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private String caseUrn;
    private String defendantId1;
    private String defendantId2;
    private String defendantId3;
    private String offenceId1;
    private String offenceId2;
    private UUID externalId;

    final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper = new InitiateSjpProsecutionHelper();
    final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();

    @BeforeAll
    public static void setupOnce() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
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
    public void shouldRaiseValidationErrorWhenInvalidInitiationCode() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-initiation-code.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());


        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final String expectedPayload = readFile("expected/invalid_initiation_code_problem.json");
        final Optional<JsonEnvelope> caseValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(caseValidationFailedEvent.isPresent(), is(true));

        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });
        assertNotNull(caseValidationFailedEvent);
        assertEquals(expectedPayload, caseValidationFailedEvent.get().payloadAsJsonObject().get("problems").toString(), LENIENT);

        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidInitiationCode.json");
        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].errors[displayName=CASE_INITIATION_CODE_INVALID].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[bailStatus=C].id", (o1, o2) -> true)

        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", arrayValueMatcher)
        ));

    }

    @Test
    public void shouldRaiseValidationErrorWhenInvalidOffenceAlcoholLevelMethod() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-alocohol-level-method.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidAlcoholLevelMethod.json").replace("OFFENCE_ID1", offenceId1);
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED });

        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization(format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s]", offenceId1), (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].errors[displayName=DEFENDANT_CUSTODY_STATUS_INVALID].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", arrayValueMatcher)
        ));
    }

    @Test
    public void shouldRaiseValidationErrorWhenInvalidCustodyStatus() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-custody-status.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/invalid_custody_status_problem.json", privateEvent.get());
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        final String expectedErrorsPayload = readFile("expected/expected_case_invalid_custody_status_errors.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "A", "2015-04-04", "032575aa-85e7-11e9-bc42-526af7764f64", "CASE_MARKER_IS_INVALID", "DEFENDANT_CUSTODY_STATUS_INVALID"));

    }

    @Test
    public void shouldRaiseValidationErrorWhenInvalidPncId() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-pncid.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/invalid_pncid_problem.json", privateEvent.get());
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        queryAndVerifyDefendantErrors(caseId, "pncIdentifier", "SF20/123456M", "INVALID_PNC_ID");
    }

    @Test
    public void shouldRaiseValidationErrorWhenInvalidPostCode() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-with-invalid-postcode.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/invalid_postcode_problem.json", privateEvent.get());

        queryAndVerifyDefendantErrors(caseId, "individual_personalInformation_address_postcode", "CRO 2QX", INVALID_DEFENDANT_INDIVIDUAL_POST_CODE.name());
    }

    @Test
    public void shouldRaiseValidationErrorWhenInvalidCroNumber() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-cro-number.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));

        assertErrorsExpected("expected/invalid_cro_number_problem.json", privateEvent.get());
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        queryAndVerifyDefendantErrors(caseId, "croNumber", "SF20/123456MM", "INVALID_CRO_NUMBER");
    }

    @Test
    public void shouldRaiseValidationErrorInvalidWhenStatementOfFacts() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        //initiateCCProsecutionHelper.thenPrivateEventShouldBeRaised();
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        final String expectedErrorsPayload = readFile("expected/expected_case_invalid_statement_of_facts.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "C", "2015-04-04", "032575aa-85e7-11e9-bc42-526af7764f64", "CASE_MARKER_IS_INVALID", "STATEMENT_OF_FACTS_REQUIRED"));

    }

    @Test
    public void shouldNotRaiseValidationErrorWhenStatementOfFactsFromMCCChannelWithSummonsTypeAsMCA() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), "M", "MCC");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        assertNoErrorsExpected(initiateCCProsecutionHelper);

        queryAndVerifyCasesAreEmptyCollection(caseId);
    }

    @Test
    public void shouldNotRaiseValidationErrorWhenStatementOfFactsFromMCCChannelWithSummonsTypeAsWitness() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), "W", "MCC");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        assertNoErrorsExpected(initiateCCProsecutionHelper);

        queryAndVerifyCasesAreEmptyCollection(caseId);
    }

    @Test
    public void shouldNotRaiseValidationErrorWhenStatementOfFactsFromCPPIChannel() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), "W", "CPPI");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        assertNoErrorsExpected(initiateCCProsecutionHelper);

        queryAndVerifyCasesAreEmptyCollection(caseId);
    }

    @Test
    public void shouldRaiseValidationErrorWhenStatementOfFactsFromCPPIChannelWithSummonsTypeAsMCA() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), "M", "CPPI"); //For MCA summon type, code is 'M'

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));

        final String defendantErrors = privateEvent.get().payloadAsJsonObject().get("defendantErrors").toString();
        final JSONArray defendantErrorsArray = new JSONArray(defendantErrors);
        final String actualPayload = defendantErrorsArray.getJSONObject(0).getJSONArray("problems").toString();
        final String expectedErrorsPayload = readFile("expected/invalid_statement_of_facts_problem.json");

        assertEquals(expectedErrorsPayload, actualPayload, LENIENT);

        queryAndVerifyCasesAreEmptyCollection(caseId);
    }

    @Test
    public void shouldRaiseValidationErrorInvalidWhenStatementOfFactsWelsh() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-statement-of-facts-welsh.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/invalid_statement_of_facts_welsh_problem.json", privateEvent.get());

        final String expectedErrorsPayload = readFile("expected/case_invalid_statement_of_facts_welsh.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "C", "2015-04-04", "032575aa-85e7-11e9-bc42-526af7764f64", "CASE_MARKER_IS_INVALID", "STATEMENT_OF_FACTS_WELSH_REQUIRED"));

    }

    @Test
    public void shouldRaiseValidationErrorInvalidBailConditions() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-bail-conditions.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpected("expected/invalid_bail_conditions_problem.json", privateEvent.get());
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED });

        final String expectedErrorsPayload = readFile("expected/case_invalid_bail_condition.json");
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "B", "2015-04-04", "", "BAIL_CONDITIONS_REQUIRED", "CASE_MARKER_IS_INVALID"));
    }

    @Test
    public void shouldRaiseValidationErrorInvalidAlcoholDrugLevelMethod() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-alcohol-drug-level-method.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));

        assertExpctedErrorPayload(readFile("expected/invalid_alcohol_drug_level_method_problem.json").replace("OFFENCE_ID1", this.offenceId1), privateEvent.get());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidAlcoholLevelInfo.json").replace("OFFENCE_ID1", this.offenceId1);
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization(format("cases[0].defendants[bailStatus=C].offences[id=%s].errors[displayName=ALCOHOL_DRUG_LEVEL_AMOUNT_MISSING].version", offenceId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[bailStatus=C].offences[id=%s].errors[displayName=ALCOHOL_DRUG_LEVEL_METHOD_MISSING].version",offenceId1), (o1, o2) -> true),
                new Customization("cases[0].defendants[bailStatus=C].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", arrayValueMatcher)
        ));


        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);

        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-alcohol-errors.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);

    }


    @Test
    public void shouldSubmitErrorCorrectionsAndRaiseCCCaseReceived() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);

        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));

        assertErrorsExpected("expected/defendant_validation_error_problems_for_correction.json", defendantValidationFailedEvent.get());

        final Optional<JsonEnvelope> caseValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(caseValidationFailedEvent.isPresent(), is(true));
        assertErrorsExpected("expected/case_validation_error_problems_for_correction-cps-organisation.json", caseValidationFailedEvent.get());
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{ PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED, PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED });

        final String expectedErrorsPayload = readFile("expected/invalid_case_should_receive.json");
        final String expectedErrorsPayloadWithReplaceValues = replaceExpectedValues(expectedErrorsPayload);

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayloadWithReplaceValues, getCustomComparator(caseId.toString(), "B", LocalDate.now().plusDays(5).format(DATE_FORMAT), "2013c181-7458-4fb2-a637-b596fbe0e792",
                "CASE_INITIATION_CODE_INVALID", "ARREST_DATE_IN_FUTURE", "CHARGE_DATE_IN_FUTURE", "DEFENDANT_SECONDARY_EMAIL_ADDRESS_INVALID", "DEFENDANT_NATIONALITY_INVALID", "DEFENDANT_PRIMARY_EMAIL_ADDRESS_INVALID"));


        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);

        final String ccExpectedPayload = readFile("expected/initiate_cc_expected_output_after_error_correction.json");
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);

    }

    @Test
    public void laidDateAndArrestDateValidationForCivilCase() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-civil-laid-date-arrest-date.json");
        final String ccPayLoad = replaceValuesForOffenceLaidDateAndArrestDate(staticPayLoad, caseId.toString());
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpectedForCivil("expected/defendant_validation_error_problems_for_laid_date_arrest_date.json", privateEvent.get());

    }

    @Test
    public void shouldRaiseValidationErrorInvalidOffenceCodeMethod() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-alcohol-drug-level-method.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));

        assertExpctedErrorPayload(readFile("expected/invalid_alcohol_drug_level_method_problem.json").replace("OFFENCE_ID1", this.offenceId1)
                , privateEvent.get());

        final String expectedErrorsPayload = readFile("expected/case_invalid_offence.json").replace("OFFENCE_ID1", this.offenceId1);
        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, getCustomComparator(caseId.toString(), "C", "05-04-2015", this.offenceId1, "ALCOHOL_DRUG_LEVEL_AMOUNT_MISSING", "ALCOHOL_DRUG_LEVEL_METHOD_MISSING"));
    }

    @Test
    public void shouldSubmitErrorCorrectionsForCorporateDefendantAndRaiseCCCaseReceived() {

        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-corporate-cc-prosecution-with-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);

        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));

        assertErrorsExpected("expected/organisation_defendant_validation_error_problems.json", defendantValidationFailedEvent.get());

        final Optional<JsonEnvelope> caseValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        assertThat(caseValidationFailedEvent.isPresent(), is(true));

        assertErrorsExpected("expected/case_validation_error_problems_for_correction.json", caseValidationFailedEvent.get());
        //This stub was removed when running all tests
        stubOffencesForOffenceCode();

        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors-for-corporate.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);

        final String ccExpectedPayload = readFile("expected/initiate_cc_corporate_defendant_expected_output_after_error_correction.json");
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);
    }


    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasOffenceCodeErrors() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-code-error.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidOffenceCode.json").replace("OFFENCE_ID1", offenceId1);

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].id", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].errors[displayName=DEFENDANT_CUSTODY_STATUS_INVALID].version", (o1, o2) -> true),
                new Customization(format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s].errors[errorValue=1].id", offenceId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s].errors[errorValue=1].version", offenceId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s].errors[errorValue=XX].id", offenceId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s].errors[errorValue=XX].version", offenceId1), (o1, o2) -> true),
                new Customization("cases[0].defendants[bailStatus=C].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", arrayValueMatcher)
        ));
    }

    @Test
    public void shouldNOTRaiseValidationErrorWhenPayloadHasOffenceLocationErrors() {

        stubOffencesForOffenceLocationRequired();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-offence-location.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidOffenceLocation.json");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        queryAndVerifyCaseErrorsEmpty(caseId, expectedErrorsPayload, new CustomComparator(LENIENT,
                new Customization("cases", (o1, o2) -> true)));
    }

    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasDefendantDOBErrors() {

        stubOffencesForOffenceLocationRequired();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-defendant-dob.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidDefendantDOB.json");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].errors[displayName=DEFENDANT_DOB_IN_FUTURE].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(LENIENT,
                new Customization("cases", arrayValueMatcher)
        ));

    }

    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasDefendantAdditionalNationality() {

        stubOffencesForOffenceLocationRequired();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-additional-nationality.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidAdditionalNationality.json");

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);


        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].errors[displayName=DEFENDANT_ADDITIONAL_NATIONALITY_INVALID].version", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(LENIENT,
                new Customization("cases", arrayValueMatcher)
        ));
    }

    @Test
    public void shouldRaiseValidationErrorWhenBackDutyIsMissing() {
        stubOffencesWithBackDuty();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-when-backduty-is-missing.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> actualEnvelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_REJECTED);
        assertThat(actualEnvelope.isPresent(), is(true));

        final String expectedErrorsPayload = readFile("expected/backduty_field_method_problem.json");
        JSONObject expectedJson = new JSONObject(expectedErrorsPayload);

        JSONObject actualValidationError = new JSONObject(actualEnvelope.get().payloadAsJsonObject().toString())
                .getJSONArray("defendantErrors")
                .getJSONObject(0)
                .getJSONArray("problems")
                .getJSONObject(0);

        int numValues = actualValidationError.getJSONArray("values").length();
        for (int i = 0; i < numValues; i++) {
            // IDs are generated on the fly
            actualValidationError.getJSONArray("values").getJSONObject(i).remove("id");
        }

        assertThat("Validation Error is as expected", expectedJson.similar(actualValidationError));
        stubOffencesForOffenceCode();

    }

    @Test
    public void shouldRaiseValidationErrorWhenBackDutyFromDateIsAfterToDateForSJPProsecution() {
        stubOffencesWithBackDuty();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-when-backduty-fromdate-is-missing.json");

        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));

        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED);
        assertThat(jsonEnvelope.isPresent(), is(true));


        final String expectedValidationErrorPayload = readFile("expected/backduty_from_date_method_problem.json");

        JSONObject actualValidationError = new JSONObject(jsonEnvelope.get().payloadAsJsonObject().toString())
                .getJSONArray("errors")
                .getJSONObject(0);

        JSONObject expectedJson = new JSONObject(expectedValidationErrorPayload);

        int numValues = actualValidationError.getJSONArray("values").length();
        for (int i = 0; i < numValues; i++) {
            // IDs are generated on the fly
            actualValidationError.getJSONArray("values").getJSONObject(i).remove("id");
        }
        assertThat("Validation Error is as expected", expectedJson.similar(actualValidationError));
        stubOffencesForOffenceCode();


    }

    @Test
    public void shouldRaiseValidationErrorWhenSjpISNotValid() {

        stubNonSjpProsecutors();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-when-prosecutor-is-invalid.json");

        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));

        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED);
        assertThat(jsonEnvelope.isPresent(), is(true));

        final String expectedErrorsPayload = readFile("expected/sjp_flag_invalid_problem.json");
        final String actualPayload = jsonEnvelope.get().payloadAsJsonObject().get("errors").toString();
        assertEquals(expectedErrorsPayload, actualPayload, LENIENT);

    }

    @Test
    public void shouldNotRaiseValidationErrorWhenSjpISIsValid() {

        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-when-prosecutor-is-valid.json");
        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());
        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));
        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(jsonEnvelope.isPresent(), is(true));

        final String actualPayload = jsonEnvelope.get().payloadAsJsonObject().get("errors") != null ?
                jsonEnvelope.get().payloadAsJsonObject().get("errors").toString() : "";
        if (null != actualPayload || !actualPayload.isEmpty()) {
            assertThat(actualPayload, not(CoreMatchers.containsString("PROSECUTOR_NOT_RECOGNISED_AS_AN_AUTHORISED_SJP_PROSECUTOR")));
        }

    }


    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasOffenceLocationErrorsForSJPProsecution() {
        stubOffencesForOffenceLocationRequired("stub-data/referencedataoffences.offences-with-offence-location-required.json");
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-with-invalid-offence-location.json");

        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));

        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED);
        assertThat(jsonEnvelope.isPresent(), is(true));

        final String expectedErrorsPayload = replaceValues(readFile("expected/expected_sjp_case_errors_for_invalid_offence_location.json"), caseId.toString());
        final String actualPayload = jsonEnvelope.get().payloadAsJsonObject().get("errors").toString();
        assertEquals(expectedErrorsPayload, actualPayload, LENIENT);
        stubOffencesForOffenceCodeList();

    }

    @Test
    public void shouldRaiseValidationErrorWhenPayloadHasGenericOffenceCode() {
        stubOffencesForGenericOffence();
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-with-generic-offence-code.json");

        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());

        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));


        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED);
        assertThat(jsonEnvelope.isPresent(), is(true));

        final String expectedErrorsPayload = replaceValues(readFile("expected/expected_sjp_case_errors_for_generic_offence_code.json"), caseId.toString());
        final String actualPayload = jsonEnvelope.get().payloadAsJsonObject().get("errors").toString();
        assertEquals(expectedErrorsPayload, actualPayload, LENIENT);
        stubOffencesForOffenceCode();
    }

    @Test
    public void shouldNotRaiseValidationErrorWhenPayloadHasGenericAlteredOffenceCode() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-sjp-prosecution-with-generic-altered-offence-code.json");
        final String sjpPayLoad = replaceValues(staticPayLoad, caseId.toString());
        initiateSjpProsecutionHelper.initiateSjpProsecution(FileUtil.readJson(sjpPayLoad));
        final Optional<JsonEnvelope> jsonEnvelope = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(jsonEnvelope.isPresent(), is(true));

        assertNull(jsonEnvelope.get().payloadAsJsonObject().get("errors"));
    }

    @Test
    public void shouldSubmitOnlyErrorCorrectionsAndRaiseForDefendantShouldClearFromBusinessError() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-hearing-date-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);

        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = resolveCaseErrorsHelper.getDefendantValidationFailedEvent();
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));
        assertErrorsExpected("expected/defendant_validation_error_problems_for_hearing_date_correction.json", defendantValidationFailedEvent.get());

        final String expectedErrorsPayload = readFile("expected/expected_case_errors_with_invalid_hearing_date.json")
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3);


        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", getArrayValueMatcher())
        ));


        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors-one-hearing-date.json")
                .replace("CASE_ID", caseId.toString())
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DATE_OF_HEARING", LocalDate.now().plusMonths(2).format(DATE_FORMAT));

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());


        final String expectedErrorsPayload1 = readFile("expected/expected_case_errors_with_invalid_hearing_date_with_two_def.json")
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_ID3", this.defendantId3);


        queryAndVerifyCaseErrorsForDefendants(caseId, expectedErrorsPayload1, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", getArrayValueMatcher())
        ));


    }

    private ArrayValueMatcher getArrayValueMatcher() {
        return new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE].version", defendantId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_IN_THE_PAST].version", defendantId1), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE].version", defendantId2), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_IN_THE_PAST].version", defendantId2), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_EARLIER_THAN_OFFENCE_COMMITTED_DATE].version", defendantId3), (o1, o2) -> true),
                new Customization(format("cases[0].defendants[id=%s].errors[displayName=DATE_OF_HEARING_IN_THE_PAST].version", defendantId3), (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[bailStatus=C].id", (o1, o2) -> true)
        ));
    }

    private String replaceValues(final String payload, final String caseId) {
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
                .replace("SUMMONS_CODE", "M")
                .replace("OFFENCE_CHARGE_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }

    private String replaceValuesForOffenceLaidDateAndArrestDate(final String payload, final String caseId) {
        return payload
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT))
                .replace("OFFENCE_LAID_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT))
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("INITIATION_CODE", "O")
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("DATE_OF_HEARING", LocalDates.to(LocalDate.now().plusMonths(2)));

    }

    private String replaceValues(final String payload, final String caseId, final String summonsCode, final String channel) {
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
                .replace("CHANNEL_TYPE", channel)
                .replace("SUMMONS_CODE", summonsCode)
                .replace("OFFENCE_CHARGE_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }


    private void queryAndVerifyDefendantErrors(final UUID caseId, final String fieldName, final String errorValue, final String displayName) {
        verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases[0].id", equalTo(caseId.toString())),
                withJsonPath("$.cases[0].defendants[0].errors[0].fieldName", is(fieldName)),
                withJsonPath("$.cases[0].defendants[0].errors[0].errorValue", is(errorValue)),
                withJsonPath("$.cases[0].defendants[0].errors[0].displayName", is(displayName)))
        );
    }
}
