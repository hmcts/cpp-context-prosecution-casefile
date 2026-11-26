package uk.gov.moj.cpp.prosecution.casefile.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.ResolveCaseErrorsHelper;

import javax.json.JsonObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.QueryHelper.verifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

public class ResolvedCasesIT extends BaseIT {

    private static final String CASE_MARKER_CODE = "YO";
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private String caseUrn;
    private String defendantId1;
    private String defendantId2;
    private String defendantId_3;
    private String offenceId1;
    private String offenceId2;
    private UUID externalId;

    @BeforeAll
    public static void setUpClass() {
        stubWiremocks();
    }

    private static void stubWiremocks() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubGetOrganisationUnitWithOneCourtroom();
        new DatabaseCleaner().cleanViewStoreTables("prosecutioncasefile", "offence");
    }

    @BeforeEach
    public void setUp() {
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId_3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        externalId = randomUUID();
    }

    @Test
    public void shouldSubmitErrorCorrectionsAndRaiseCCCaseRecieved() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = resolveCaseErrorsHelper.getDefendantValidationFailedEvent();
        final Optional<JsonEnvelope> caseValidationFailedEvent = resolveCaseErrorsHelper.getCaseValidationFailedEvent();
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));
        assertThat(caseValidationFailedEvent.isPresent(), is(true));

        assertErrorsExpectations("expected/defendant_validation_error_problems_for_correction.json", defendantValidationFailedEvent.get());
        assertErrorsExpectations("expected/case_validation_error_problems_for_correction-cps-organisation.json", caseValidationFailedEvent.get());

        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);

        final String ccExpectedPayload = readFile("expected/initiate_cc_expected_output_after_error_correction.json");
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);

    }

    @Test
    @Disabled
    public void shouldSubmitErrorCorrectionsAndRaiseCCCaseReceivedAndAddSecondDefendantWithError() {
        final UUID caseId = randomUUID();
        final String hearingDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        final String firstStaticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-hearing-date-for-resolving.json");
        final String firstPayLoad = replaceValues(firstStaticPayLoad, caseId.toString());

        // Create the case with past hearing date
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(firstPayLoad);

        stubGetOrganisationUnits();

        // Resolve the errors
        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors-valid-hearing-date.json")
                .replace("%DATE_OF_HEARING%", hearingDate)
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);

        final String ccExpectedPayload = readFile("expected/initiate_cc_expected_output_after_error_correct-hearing-date.json")
                .replace("%DATE_OF_HEARING%", hearingDate);
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);

        final String secondStaticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-subsequent-cc-prosecution-with-invalid-hearing-date-for-resolving.json");
        final String secondPayload = replaceValues(secondStaticPayLoad, caseId.toString());

        initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);

        resolveCaseErrorsHelper.initiateCCProsecution(secondPayload);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));

        assertErrorsExpectations("expected/defendant_add_validation_error_problems_with_hearingdate_for_correction.json", defendantValidationFailedEvent.get());

        final String errorCorrectionPayLoadSecondDefendant = readFile("command-json/prosecutioncasefile.command.resolve-error-hearing-date-defendant.json")
                .replace("DEF_ID", this.defendantId_3)
                .replace("OFFENCE_ID", this.offenceId1);

        // Resolve error
        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoadSecondDefendant, caseId.toString());
        final String ccExpectedPayloadSecondDefendant = readFile("expected/initiate_cc_add-second-defendant-expected_output_after_error_corrections.json")
                .replace("%DATE_OF_HEARING%", hearingDate);
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayloadSecondDefendant);
    }

    @Test
    public void shouldSubmitErrorCorrectionsForDefendantLevelFirstAndCaseLevelNextAndGenerateApplicationRequest() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-for-resolving-authority.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        assertErrorsExpectations("expected/defendant_validation_error_problems_for_correction_for_summons_case.json", resolveCaseErrorsHelper.getDefendantValidationFailedEvent().get());
        assertErrorsExpectations("expected/case_validation_error_problems_for_correction_authority.json", resolveCaseErrorsHelper.getCaseValidationFailedEvent().get());

        final String defendantErrorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-defendant-errors.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(defendantErrorCorrectionPayLoad, caseId.toString());
        assertNotNull(resolveCaseErrorsHelper.getDefendantValidationPassedEvent().get().payloadAsJsonObject());
        assertErrorsExpectations("expected/case_validation_error_problems_for_correction_authority.json", resolveCaseErrorsHelper.getCaseValidationFailedEvent().get());

//        verifyCaseErrors(caseId, allOf(
//                withJsonPath("$.cases", not(empty())),
//                withJsonPath("$.cases[0].errors", hasSize(1)),
//                withJsonPath("$.cases[0].caseMarkersErrors", hasSize(2)),
//                withJsonPath("$.cases[0].defendants", empty())
//        ));

        final String caseErrorCorrectionPayload = readFile("command-json/prosecutioncasefile.command.resolve-errors-authority.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(caseErrorCorrectionPayload, caseId.toString());
        assertNotNull(resolveCaseErrorsHelper.getDefendantValidationPassedEvent().get().payloadAsJsonObject());

        final JsonObject eventJsonPayload = resolveCaseErrorsHelper.getApplicationCreationRequested().get().payloadAsJsonObject();
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "prosecution", "caseDetails", "caseId"}).get(), is(caseId.toString()));
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "prosecution", "caseDetails", "prosecutor", "prosecutingAuthority"}).get(), is("GAFTL00"));
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "referenceDataVO", "prosecutorsReferenceData", "oucode"}).get(), is("GAFTL00"));

        verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases", empty())
        ));
    }

    @Test
    public void shouldSubmitErrorCorrectionsForCaseLevelFirstAndDefendantLevelNextAndGenerateApplicationRequest() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-for-resolving-authority.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        assertErrorsExpectations("expected/defendant_validation_error_problems_for_correction_for_summons_case.json", resolveCaseErrorsHelper.getDefendantValidationFailedEvent().get());
        assertErrorsExpectations("expected/case_validation_error_problems_for_correction_authority.json", resolveCaseErrorsHelper.getCaseValidationFailedEvent().get());

        final String caseErrorCorrectionPayload = readFile("command-json/prosecutioncasefile.command.resolve-errors-authority.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(caseErrorCorrectionPayload, caseId.toString());
        final JsonObject caseValidationFailedPayload = resolveCaseErrorsHelper.getCaseValidationFailedEvent().get().payloadAsJsonObject();
        assertThat(caseValidationFailedPayload.getJsonArray("problems"), empty());
        assertErrorsExpectations("expected/defendant_validation_error_problems_for_correction_for_summons_case.json", resolveCaseErrorsHelper.getDefendantValidationFailedEvent().get());

//        verifyCaseErrors(caseId, allOf(
//                withJsonPath("$.cases", not(empty())),
//                withJsonPath("$.cases[0].errors", empty()),
//                withJsonPath("$.cases[0].caseMarkersErrors", empty()),
//                withJsonPath("$.cases[0].defendants[0].errors", hasSize(3))
//        ));

        final String defendantErrorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-defendant-errors.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(defendantErrorCorrectionPayLoad, caseId.toString());
        assertNotNull(resolveCaseErrorsHelper.getDefendantValidationPassedEvent().get().payloadAsJsonObject());

        final JsonObject eventJsonPayload = resolveCaseErrorsHelper.getApplicationCreationRequested().get().payloadAsJsonObject();
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "prosecution", "caseDetails", "caseId"}).get(), is(caseId.toString()));
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "prosecution", "caseDetails", "prosecutor", "prosecutingAuthority"}).get(), is("GAFTL00"));
        assertThat(JsonObjects.getString(eventJsonPayload, new String[]{"prosecutionWithReferenceData", "referenceDataVO", "prosecutorsReferenceData", "oucode"}).get(), is("GAFTL00"));

        verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases", empty())
        ));

    }

    @Test
    public void shouldSubmitErrorCorrectionsAndRaiseCCCaseReceivedForUnknownNationality() {
        final UUID caseId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString());

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        assertErrorsExpectations("expected/defendant_validation_error_problems_for_correction.json", resolveCaseErrorsHelper.getDefendantValidationFailedEvent().get());
        assertErrorsExpectations("expected/case_validation_error_problems_for_correction-cps-organisation.json", resolveCaseErrorsHelper.getCaseValidationFailedEvent().get());


        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors-unknown-nationality.json")
                .replace("DEF_ID", this.defendantId1)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        resolveCaseErrorsHelper.getPrivateEvent(caseId);


        final String ccExpectedPayload = readFile("expected/initiate_cc_expected_output_after_error_correction-unknown-nationality.json");
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);

    }

    @Test
    public void shouldResolveCaseWhenInvalidOffenceAlcoholLevelMethodAndErrorCorrectionSubmitted() {
        final UUID caseId = randomUUID();
        String validPayload = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-valid-alocohol-level-method.json");
        final String invalidAlcoholMethodPayload = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-invalid-alocohol-level-method.json");


        validPayload = replaceValues(validPayload, caseId.toString());
        final String expectedErrorsPayload = readFile("expected/expected_case_errors_whenInvalidAlcoholLevelMethod.json").replace("OFFENCE_ID1", offenceId1).replace("Smith","Walace");;
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        // send valid SPI case with one defendant
        initiateCCProsecutionHelper.initiateCCProsecution(validPayload);
        final Optional<JsonEnvelope> ccProsecutionReceivedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED);
        assertThat(ccProsecutionReceivedEvent.isPresent(), is(true));

        // send second SPI case with different defendant and invalid alcohol method
        final String invalidAlcoholMethodPayload1 = replaceValues(
                invalidAlcoholMethodPayload.replace("DEFENDANT_ID1", this.defendantId2).replace("Smith","Walace")
                , caseId.toString());
        initiateCCProsecutionHelper.initiateCCProsecution(invalidAlcoholMethodPayload1);

        final Optional<JsonEnvelope> defendantValidationFailedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(defendantValidationFailedEvent.isPresent(), is(true));

        // send second SPI case with second defendant and invalid alcohol method. Only change defendant id
        final String invalidAlcoholMethodPayload2 = replaceValues(
                invalidAlcoholMethodPayload.replace("DEFENDANT_ID1", this.defendantId_3)
                , caseId.toString());

        initiateCCProsecutionHelper.initiateCCProsecution(invalidAlcoholMethodPayload2);

        final Optional<JsonEnvelope> caseReceivedWithDuplicateDefendantsEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS);
        assertThat(caseReceivedWithDuplicateDefendantsEvent.isPresent(), is(true));


        final ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization(String.format("cases[0].defendants[chargeDate=2015-04-04].offences[id=%s]", offenceId1), (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].errors[displayName=ALCOHOL_DRUG_LEVEL_METHOD_INVALID].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[chargeDate=2015-04-04].id", (o1, o2) -> true)
        ));

//        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
//                new Customization("cases", arrayValueMatcher)
//        ));

        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);
        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-alcohol-errors.json")
                .replace("DEF_ID", defendantId2)
                .replace("OFFENCE_ID", this.offenceId1);

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());
        final Optional<JsonEnvelope> defendantAddedEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_ADDED);
        assertThat(defendantAddedEvent.isPresent(), is(true));
        assertThat(defendantAddedEvent.get().payloadAsJsonObject().get("defendants").toString(), containsString(this.defendantId2));
    }

    private String replaceValues(final String payload, final String caseId) {
        return payload
                .replace("CASE-ID", caseId)
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("DATE_OF_HEARING", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replace("OFFENCE_CHARGE_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }

    private String replaceExpectedValues(final String payload) {
        return payload
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("OFFENCE_CHARGE_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }

    private void assertErrorsExpectations(final String filename, final JsonEnvelope actualValidationErrors) {
        final String expectedErrorsPayload = readFile(filename);
        final String expectedErrorsPayloadWithReplaceValues = replaceExpectedValues(expectedErrorsPayload);
        final String actualPayload = actualValidationErrors.payloadAsJsonObject().get("problems").toString();
        assertEquals(expectedErrorsPayloadWithReplaceValues, actualPayload, STRICT);
    }

}