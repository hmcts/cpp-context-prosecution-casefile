package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_RESOLVED_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.ResolveCaseErrorsHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class GetCountsCasesErrorsIT extends BaseIT {
    private static final String CASE_MARKER_CODE = "YO";
    private static final String GET_COUNTS_CASES_ERRORS = "application/vnd.prosecutioncasefile.query.counts-cases-errors+json";
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    private static String caseUrn;
    private static String defendantId1;
    private static String defendantId2;
    private static String offenceId1;
    private static String offenceId2;

    @BeforeAll
    public static void setUpDataOnce() throws JMSException {
        stubWiremocks();
        setUpData();
    }

    public static void setUpData() throws JMSException {
        cleanTables();
        String courtLocation = "C55BN00";
        String caseType = "R";

        final JmsMessageConsumerClient caseValidationFailedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(EVENT_SELECTOR_CASE_VALIDATION_FAILED)
                .getMessageConsumerClient();


        produceAndSaveBusinessValidationErrors(courtLocation, caseType, 10);

        caseType = "H";
        produceAndSaveBusinessValidationErrors(courtLocation, caseType, 20);

        courtLocation = "B01BH00";
        produceAndSaveBusinessValidationErrors(courtLocation, caseType, 5);

        for (int i = 0; i < 35; i++) {
            final JsonPath jsonPath = retrieveMessage(caseValidationFailedConsumer);
            if (jsonPath == null) {
                fail("Expected message to emit on the topic :" + EVENT_SELECTOR_CASE_VALIDATION_FAILED);
            }
        }

        produceResolvedCases(courtLocation, caseType);

    }

    @Test
    public void shouldReturnAllErrorsWithoutParameters() {
        final String expectedErrorsPayload = readFile("expected/expected_counts_case_errors_without_any_filtering.json");
        queryandVerifyCountsCasesErrors(new HashMap<>(), expectedErrorsPayload);
    }

    @Test
    public void shouldReturnAllErrorsWithAllParameters() {
        final HashMap<String, String> param = new HashMap<String, String>();
        param.put("courtLocation", "C55BN00");
        param.put("caseType", "R");

        final String expectedErrorsPayloadAllFilter = readFile("expected/expected_counts_case_errors_with_all_filtering.json");
        queryandVerifyCountsCasesErrors(param, expectedErrorsPayloadAllFilter);
    }


    @Test
    public void shouldReturnAllErrorsWithCaseTypeParameterOnly() {
        final HashMap<String, String> param = new HashMap<String, String>();
        param.put("caseType", "H");
        final String expectedErrorsPayloadFilter = readFile("expected/expected_counts_case_errors_with_casetype_filtering.json");
        queryandVerifyCountsCasesErrors(param, expectedErrorsPayloadFilter);
    }

    @Test
    public void shouldReturnAllErrorsWithCourtLocationParameterOnly() {
        final HashMap<String, String> param = new HashMap<String, String>();
        param.put("courtLocation", "B01BH00");
        final String expectedErrorsPayloadFilter = readFile("expected/expected_counts_case_errors_with_courtlocation_filtering.json");
        queryandVerifyCountsCasesErrors(param, expectedErrorsPayloadFilter);
    }

    private static void stubWiremocks() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
    }


    private static String replaceValues(final String payload, final String caseId, final String courtLocation, final String caseType, final UUID externalId) {
        return payload
                .replace("CASE-ID", caseId)
                .replace("CASE-URN", caseUrn)
                .replace("EXTERNAL_ID", externalId.toString())
                .replace("DEFENDANT_ID1", defendantId1)
                .replace("DEFENDANT_ID2", defendantId2)
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("OFFENCE_ID1", offenceId1)
                .replace("OFFENCE_ID2", offenceId2)
                .replace("DATE_OF_HEARING", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("COURT-LOCATION", courtLocation)
                .replace("CASE-TYPE", caseType);
    }


    private static RequestParamsBuilder getCountsCasesErrors(HashMap<String, String> queryParams) {
        final String PREFIX = "/counts/cases-errors";
        final StringBuilder builder = new StringBuilder(PREFIX);

        if (!queryParams.isEmpty()) {
            builder.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                builder.append(entry.getKey() + "=" + entry.getValue());
                builder.append("&");
            }
        }
        return requestParams(getReadUrl(format("%s", builder.toString())), GET_COUNTS_CASES_ERRORS)
                .withHeader(HeaderConstants.USER_ID, USER_ID);
    }

    private void queryandVerifyCountsCasesErrors(HashMap<String, String> queryParameters, final String expectedCountsPayload) {

        final ResponseData responseData = poll(getCountsCasesErrors(queryParameters)).until(status().is(OK));
        assertEquals(expectedCountsPayload, responseData.getPayload(), new CustomComparator(STRICT, new Customization("casesWithOutstandingErrors", (o1, o2) -> true)));
    }


    private static void produceAndSaveBusinessValidationErrors(final String courtLocation, final String caseType, final int caseCount) {
        for (int i = 0; i < caseCount; i++) {
            final UUID caseId = randomUUID();
            final UUID externalId = randomUUID();
            caseUrn = randomAlphanumeric(10);
            defendantId1 = randomUUID().toString();
            defendantId2 = randomUUID().toString();
            offenceId1 = randomUUID().toString();
            offenceId2 = randomUUID().toString();

            final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-code-error-for-count.json");

            final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), courtLocation, caseType, externalId);

            final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
            initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        }
    }


    private static void produceResolvedCases(final String courtLocation, final String caseType) throws JMSException {
        final UUID caseId = randomUUID();
        final UUID externalId = randomUUID();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-corporate-cc-prosecution-with-error-for-resolving.json");

        final String ccPayLoad = replaceValues(staticPayLoad, caseId.toString(), courtLocation, caseType, externalId);

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final ResolveCaseErrorsHelper resolveCaseErrorsHelper = new ResolveCaseErrorsHelper(initiateCCProsecutionHelper);

        final JmsMessageConsumerClient caseValidationFailedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(EVENT_SELECTOR_CASE_VALIDATION_FAILED)
                .getMessageConsumerClient();

        resolveCaseErrorsHelper.initiateCCProsecution(ccPayLoad);

        final JsonPath jsonPath = retrieveMessage(caseValidationFailedConsumer);
        if (jsonPath == null) {
            fail("Expected message to emit on the topic :" + EVENT_SELECTOR_CASE_VALIDATION_FAILED);
        }


        final String errorCorrectionPayLoad = readFile("command-json/prosecutioncasefile.command.resolve-errors-for-corporate.json")
                .replace("DEF_ID", defendantId1)
                .replace("OFFENCE_ID", offenceId1);

        final JmsMessageConsumerClient resolvedCaseConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
                .withEventNames(EVENT_SELECTOR_RESOLVED_CASE)
                .getMessageConsumerClient();

        resolveCaseErrorsHelper.submitErrorCorrections(errorCorrectionPayLoad, caseId.toString());

        final JsonPath jsonPathResolvedCase = retrieveMessage(resolvedCaseConsumer);
        if (jsonPathResolvedCase == null) {
            fail("Expected message to emit on the topic :" + EVENT_SELECTOR_RESOLVED_CASE);
        }

        assertThat(jsonPathResolvedCase.get("caseId"), is(caseId.toString()));

        final String ccExpectedPayload = readFile("expected/initiate_cc_corporate_expected_output_after_error_correction.json");

        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, ccExpectedPayload);

    }


    private static void cleanTables() {
        final DatabaseCleaner cleaner = new DatabaseCleaner();
        cleaner.cleanViewStoreTables("prosecutioncasefile", "business_validation_errors");
        cleaner.cleanViewStoreTables("prosecutioncasefile", "resolved_cases");
    }

}