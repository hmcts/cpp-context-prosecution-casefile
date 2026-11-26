package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseDetailsBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_INITIATED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EXTERNAL_COMMAND_SJP_CREATE_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;

import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.test.utils.core.http.ResponseData;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class InitiateSjpProsecutionHelper extends AbstractTestHelper {


    public InitiateSjpProsecutionHelper() {
        createPrivateConsumerForMultipleSelectors(
                EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED,
                EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS,
                EVENT_SELECTOR_SJP_PROSECUTION_REJECTED,
                EVENT_SELECTOR_SJP_PROSECUTION_INITIATED,
                EVENT_SELECTOR_SJP_VALIDATION_FAILED,
                EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS,
                EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS,
                EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED,
                EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY
        );


        createPublicConsumerForMultipleSelectors(
                EXTERNAL_COMMAND_SJP_CREATE_CASE,
                PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED,
                PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED,
                PUBLIC_PROSECUTIONCASEFILE_SJP_VALIDATION_FAILED,
                PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS,
                PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED
        );

    }

    public void initiateSjpProsecution(final JsonObject initiateSjpProsecutionRequest) {
        makePostCall(getWriteUrl("/initiate-sjp-prosecution"),
                "application/vnd.prosecutioncasefile.command.initiate-sjp-prosecution+json",
                initiateSjpProsecutionRequest.toString());
    }

    public void updateOffenceCode(final String caseId, JsonObject payload) {
        makePostCall(getWriteUrl(String.format("/cases/%s", caseId)),
                "application/vnd.prosecutioncasefile.sjp-prosecution-update-offence-code+json",
                payload.toString());
    }


    public void verifyCaseDetails(final JsonObject request,
                                  final UUID caseId,
                                  final String caseUrn,
                                  final String defendantId,
                                  final LocalDate dateOfBirth,
                                  final String offenceId,
                                  final LocalDate chargeDate,
                                  final LocalDate offenceCommitedDate) {
        verifyCaseDetails(request, caseId, caseUrn, defendantId, dateOfBirth, offenceId, chargeDate,
                offenceCommitedDate, "expected/case-details.json");
    }

    public void verifyCaseDetailsWithoutDefendantTitle(final JsonObject request,
                                                       final UUID caseId,
                                                       final String caseUrn,
                                                       final String defendantId,
                                                       final LocalDate dateOfBirth,
                                                       final String offenceId,
                                                       final LocalDate chargeDate,
                                                       final LocalDate offenceCommitedDate) {
        verifyCaseDetails(request, caseId, caseUrn, defendantId, dateOfBirth, offenceId, chargeDate,
                offenceCommitedDate, "expected/case-details-without-defendant-title.json");
    }

    public void verifyCaseDetailsWithoutPostcode(final JsonObject request,
                                                 final UUID caseId,
                                                 final String caseUrn,
                                                 final String defendantId,
                                                 final LocalDate dateOfBirth,
                                                 final String offenceId,
                                                 final LocalDate chargeDate,
                                                 final LocalDate offenceCommitedDate) {
        verifyCaseDetails(request, caseId, caseUrn, defendantId, dateOfBirth, offenceId, chargeDate,
                offenceCommitedDate, "expected/case-details-without-postcode.json");
    }

    public void verifyCaseDetailsWithAsnAndPncIdentifier(final JsonObject request,
                                                         final UUID caseId,
                                                         final String caseUrn,
                                                         final String defendantId,
                                                         final LocalDate dateOfBirth,
                                                         final String offenceId,
                                                         final LocalDate chargeDate,
                                                         final LocalDate offenceCommitedDate) {
        verifyCaseDetails(request, caseId, caseUrn, defendantId, dateOfBirth, offenceId, chargeDate,
                offenceCommitedDate, "expected/case-details-with-asn-and-pnc-identifier.json");
    }

    private void verifyCaseDetails(final JsonObject request,
                                   final UUID caseId,
                                   final String caseUrn,
                                   final String defendantId,
                                   final LocalDate dateOfBirth,
                                   final String offenceId,
                                   final LocalDate chargeDate,
                                   final LocalDate offenceCommitedDate,
                                   final String caseDetailsResource) {

        final ResponseData responseData = poll(getCaseDetailsBuilder(caseId.toString()))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(matchCaseDetailsFields(request))
                );

        final JsonObject jsonObject = JsonObjects.createObjectBuilder(readJsonResource(caseDetailsResource, caseId, caseUrn, defendantId, dateOfBirth.toString(), chargeDate.toString(), offenceCommitedDate.toString(), offenceId)).build();
        assertEquals(jsonObject.toString(), responseData.getPayload(), LENIENT);
    }


    public void verifyCaseDetailsForMCC(final JsonObject request,
                                        final UUID caseId,
                                        final String caseUrn,
                                        final String defendantId,
                                        final LocalDate dateOfBirth,
                                        final String offenceId,
                                        final String prosecutorDefendantRef,
                                        final LocalDate chargeDate,
                                        final LocalDate offenceCommitedDate) {
        final ResponseData responseData = poll(getCaseDetailsBuilder(caseId.toString()))
                .until(
                        status().is(OK),
                        payload()
                                .isJson(matchCaseDetailsFieldsForMCC(request))
                );

        final JsonObject jsonObject = JsonObjects.createObjectBuilder(readJsonResource("expected/case-details-mcc.json", caseId, caseUrn, defendantId,
                prosecutorDefendantRef, dateOfBirth.toString(), chargeDate.toString(), offenceCommitedDate.toString(), offenceId)).build();

        assertEquals(responseData.getPayload(), jsonObject.toString(), JSONCompareMode.LENIENT);
    }

    private Matcher<ReadContext> matchCaseDetailsFields(final JsonObject request) {
        final JsonObject caseDetails = request.getJsonObject("caseDetails");
        final JsonObject defendant = request.getJsonArray("defendants").getJsonObject(0);

        return allOf(
                withJsonPath("$.caseId", getMatcherStringFromJsonObject(caseDetails, "caseId")),
                withJsonPath("$.prosecutionCaseReference", getMatcherStringFromJsonObject(caseDetails, "prosecutorCaseReference")),
                withJsonPath("$.prosecutionAuthority", getMatcherStringFromJsonObject(caseDetails, "prosecutor", "prosecutingAuthority")),
                withJsonPath("$.defendants", hasSize(1)),
                withJsonPath("$.defendants[0].nationalInsuranceNumber", getMatcherStringFromJsonObject(defendant, "individual", "nationalInsuranceNumber")));
    }

    private Matcher<ReadContext> matchCaseDetailsFieldsForMCC(final JsonObject request) {
        final JsonObject caseDetails = request.getJsonObject("caseDetails");

        return allOf(
                withJsonPath("$.caseId", getMatcherStringFromJsonObject(caseDetails, "caseId")),
                withJsonPath("$.prosecutionCaseReference", getMatcherStringFromJsonObject(caseDetails, "prosecutorCaseReference")),
                withJsonPath("$.prosecutionAuthority", getMatcherStringFromJsonObject(caseDetails, "prosecutor", "prosecutingAuthority")),
                withJsonPath("$.defendants", hasSize(1)));
    }


    private Matcher<String> getMatcherStringFromJsonObject(final JsonObject jsonObject, final String... field) {
        return is(JsonObjects.getString(jsonObject, field).get());
    }

}
