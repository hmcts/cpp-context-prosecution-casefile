package uk.gov.moj.cpp.prosecution.casefile.it;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseErrorDetails;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.createCommonMockEndpoints;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubProsecutors;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

public class SpiAddDefendantsValidationErrorIT extends BaseIT {

    private static final UUID CASE_ID = randomUUID();
    private String defendantId;
    private String offenceId1;
    private String defendantId_2;
    private String defendantId_3;
    private String offenceId2;
    private String caseUrn;
    private UUID externalId;

    @BeforeEach
    public void setUp() {
        defendantId = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        defendantId_3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        caseUrn = randomAlphanumeric(10);
        externalId = randomUUID();
    }

    @BeforeAll
    public static void setUpOnce() {
        stubGetCaseMarkersWithCode("ABC");
    }


    @Test
    public void verifySpiAddNewDefendants() {

        final String initiationCode = "C";
        initiateCCProsecution(initiationCode);

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();

        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-add-defendants-with-invalid-court-hearing.json");
        final String ccPayLoad = staticPayLoad
                .replace("CASE-ID", CASE_ID.toString())
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId_3)
                .replace("DEFENDANT_REFERENCE1", this.defendantId_3)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("INITIATION_CODE", initiationCode)
                .replace("EXTERNAL_ID", randomUUID().toString());
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
        assertThat(privateEvent.isPresent(), is(true));
        assertErrorsExpectations("expected/expected-add-defendant-to-courts-error-output.json", privateEvent.get());

        final String expectedErrorsPayload = readFile("expected/case_defendant_oucode_errors.json");
        queryAndVerifyCaseErrors(CASE_ID, expectedErrorsPayload, getCustomComparator(CASE_ID.toString()));

    }

    private void assertErrorsExpectations(final String filename, final JsonEnvelope actualValidationErrors) {
        final String expectedErrorsPayload = readFile(filename);
        final String actualPayload = actualValidationErrors.payloadAsJsonObject().get("problems").toString();
        JSONAssert.assertEquals(expectedErrorsPayload, actualPayload, STRICT);
        assertThat(actualValidationErrors.payloadAsJsonObject().getString("policeSystemId"), is("00101PoliceCaseSystem"));
        assertThat(actualValidationErrors.payloadAsJsonObject().getString("urn"), is(caseUrn));
    }

    private void initiateCCProsecution(final String initiationCode) {
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        final String caseMarkerCode = "ABC";
        final String ccPayLoad = staticPayLoad
                .replace("CASE-ID", CASE_ID.toString())
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("DEFENDANT_ID2", this.defendantId_2)
                .replace("CASE_MARKER", caseMarkerCode)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("INITIATION_CODE", initiationCode)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID", this.externalId.toString());
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();
    }

    private CustomComparator getCustomComparator(final String caseId) {
        return new CustomComparator(STRICT,
                new Customization("cases[0].defendants[0].errors[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[1].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[2].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[3].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[4].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[5].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[6].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[7].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[8].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[9].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[10].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[11].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[12].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[13].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[14].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].errors[15].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].id", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].offences[0].errors[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].offences[0].errors[1].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].offences[0].errors[2].version", (o1, o2) -> true),
                new Customization("cases[0].defendants[0].offences[0].id", (o1, o2) -> true),
                new Customization("cases[0].id", (o1, o2) -> o1.equals(caseId)),
                new Customization("cases[0].urn", (o1, o2) -> o1.equals(caseUrn)),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].errors[0].version", (o1, o2) -> true),
                new Customization("cases[0].errors[1].version", (o1, o2) -> true),
                new Customization("cases[0].caseMarkersErrors[0].version", (o1, o2) -> true),
                new Customization("cases[0].caseMarkersErrors[1].version", (o1, o2) -> true),
                new Customization("cases[0].errorCaseDetails", (o1, o2) -> true)
        );
    }

    public void queryAndVerifyCaseErrors(final UUID caseId, final String expectedCaseErrorsPayload, final JSONComparator customComparator) {

        final ResponseData responseData = poll(getCaseErrorDetails(caseId.toString())).until(status().is(OK),
                payload().isJson(Matchers.allOf(
                        withJsonPath("$.cases[0].id", equalTo(caseId.toString())),
                        withJsonPath("$.cases[0].errorDescription", containsString("Error"))

                ))
        );

        assertEquals(expectedCaseErrorsPayload, responseData.getPayload(), customComparator);
    }
}
