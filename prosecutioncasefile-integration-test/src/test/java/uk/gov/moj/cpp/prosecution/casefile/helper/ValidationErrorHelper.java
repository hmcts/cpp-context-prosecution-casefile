package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseErrorDetails;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.QueryHelper.verifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.ResponseData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONArray;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

public class ValidationErrorHelper {


    private static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");

    public static CustomComparator getCustomComparator(final String caseId, final String bailStatus, final String chargeDate, final String offenceId, final String... displayNames) {
        List<Customization> customizationList = new ArrayList<>();

        for (String name : displayNames) {
            customizationList.add(new Customization(String.format("cases[0].defendants[bailStatus=%s].errors[displayName=%s].version", bailStatus, name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].defendants[id=%s].errors[displayName=%s].version", bailStatus, name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].defendants[chargeDate=%s].errors[displayName=%s].version", chargeDate, name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].defendants[bailStatus=%s].offences[id=%s].errors[displayName=%s].version", bailStatus, offenceId, name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].defendants[chargeDate=%s].offences[id=%s].errors[displayName=%s].version", chargeDate, offenceId, name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].caseMarkersErrors[displayName=%s].version", name), (o1, o2) -> true));
            customizationList.add(new Customization(String.format("cases[0].errors[displayName=%s].version", name), (o1, o2) -> true));
        }
        customizationList.add(new Customization("cases[0].id", (o1, o2) -> true));
        customizationList.add(new Customization("cases[0].urn", (o1, o2) -> true));
        customizationList.add(new Customization("cases[0].version", (o1, o2) -> true));
        customizationList.add(new Customization("cases[0].caseMarkersErrors[errorValue=BCD].version", (o1, o2) -> true));
        customizationList.add(new Customization("cases[0].caseMarkersErrors[errorValue=ABC].version", (o1, o2) -> true));
        customizationList.add(new Customization(String.format("cases[0].defendants[bailStatus=%s].id", bailStatus), (o1, o2) -> true));
        customizationList.add(new Customization(String.format("cases[0].defendants[chargeDate=%s].id", chargeDate), (o1, o2) -> true));

        CustomComparator customComparator = new CustomComparator(LENIENT, customizationList.toArray(new Customization[0]));

        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(customComparator);

        return new CustomComparator(LENIENT,
                new Customization("cases", arrayValueMatcher)
        );
    }

    public static String replaceExpectedValues(final String payload) {
        return payload
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusMonths(2).format(DATE_FORMAT))
                .replace("OFFENCE_CHARGE_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }

    public static String replaceExpectedValuesForCivil(final String payload) {
        return payload
                .replace("OFFENCE_LAID_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT))
                .replace("OFFENCE_ARREST_DATE", LocalDate.now().plusDays(5).format(DATE_FORMAT));
    }

    public static void assertErrorsExpected(final String filename, final JsonEnvelope actualValidationErrors) {
        final String expectedErrorsPayload = readFile(filename);
        final String expectedErrorsPayloadWithReplaceValues = replaceExpectedValues(expectedErrorsPayload);
        final String actualPayload = actualValidationErrors.payloadAsJsonObject().get("problems").toString();
        assertEquals(expectedErrorsPayloadWithReplaceValues, actualPayload, LENIENT);
    }

    public static void assertErrorsExpectedForCivil(final String filename, final JsonEnvelope actualValidationErrors) {
        final String expectedErrorsPayload = readFile(filename);
        final String expectedErrorsPayloadWithReplaceValues = replaceExpectedValuesForCivil(expectedErrorsPayload);
        final String defendantErrors = actualValidationErrors.payloadAsJsonObject().get("defendantErrors").toString();
        final JSONArray defendantErrorsArray = new JSONArray(defendantErrors);
        final String actualPayload = defendantErrorsArray.getJSONObject(0).getJSONArray("problems").toString();
        assertEquals(expectedErrorsPayloadWithReplaceValues, actualPayload, LENIENT);
    }



    public static void assertExpctedErrorPayload(final String expectedCaseErrorsPayload, final JsonEnvelope actualValidationErrors) {
        final String expectedErrorsPayloadWithReplaceValues = replaceExpectedValues(expectedCaseErrorsPayload);
        final String actualPayload = actualValidationErrors.payloadAsJsonObject().get("problems").toString();
        assertEquals(expectedErrorsPayloadWithReplaceValues, actualPayload, LENIENT);
    }

    public static void assertNoErrorsExpected(final InitiateCCProsecutionHelper initiateCCProsecutionHelper) {
        final Optional<JsonEnvelope> envelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_REJECTED);
        assertThat(envelope.isPresent(), is(true));

        final String defendantErrors = envelope.get().payloadAsJsonObject().get("defendantErrors").toString();
        final JSONArray defendantErrorsArray = new JSONArray(defendantErrors);
        assertThat(defendantErrorsArray.isNull(0), is(true));
    }

    public static void queryAndVerifyCaseErrors(final UUID caseId, final String expectedCaseErrorsPayload, final JSONComparator customComparator) {

        final ResponseData responseData = poll(getCaseErrorDetails(caseId.toString()))
                .timeout(20L, SECONDS)
                .pollInterval(1L, SECONDS)
                .until(status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.cases[0].id", equalTo(caseId.toString())),
                                withJsonPath("$.cases[0].errorDescription", containsString("Error"))

                        ))
                );
        assertEquals(expectedCaseErrorsPayload, responseData.getPayload(), customComparator);
    }

    public static void queryAndVerifyCaseErrorsEmpty(final UUID caseId, final String expectedCaseErrorsPayload, final JSONComparator customComparator) {

        final ResponseData responseData = verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases", notNullValue())
        ));

        assertEquals(expectedCaseErrorsPayload, responseData.getPayload(), customComparator);
    }

    public static void queryAndVerifyCasesAreEmptyCollection(final UUID caseId) {

        verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases", empty())
        ));
    }

    public static void queryAndVerifyCaseErrorsForDefendants(final UUID caseId, final String expectedCaseErrorsPayload, final JSONComparator customComparator) {

        final ResponseData responseData = verifyCaseErrors(caseId, allOf(
                withJsonPath("$.cases[0].id", equalTo(caseId.toString())),
                withJsonPath("$.cases[0].errorDescription", containsString("Error")),
                withJsonPath("$.cases[0].defendants[*]", hasSize(2))
        ));

        assertEquals(expectedCaseErrorsPayload, responseData.getPayload(), customComparator);
    }


}
