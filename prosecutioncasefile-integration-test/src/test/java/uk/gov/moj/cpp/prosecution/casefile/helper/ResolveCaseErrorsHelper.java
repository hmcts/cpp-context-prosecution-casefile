package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_PASSED;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class ResolveCaseErrorsHelper extends AbstractTestHelper {

    private final InitiateCCProsecutionHelper initiateCCProsecutionHelper;

    public ResolveCaseErrorsHelper(final InitiateCCProsecutionHelper initiateCCProsecutionHelper) {
        createPrivateConsumerForMultipleSelectors(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED,
                EVENT_SELECTOR_DEFENDANT_VALIDATION_PASSED,
                EVENT_SELECTOR_CASE_VALIDATION_FAILED,
                EVENT_SELECTOR_CC_PROSECUTION_RECEIVED,
                EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);

        this.initiateCCProsecutionHelper = initiateCCProsecutionHelper;
    }

    public void initiateCCProsecution(final String ccProsecution) {
        initiateCCProsecutionHelper.initiateCCProsecution(ccProsecution);
    }


    public void submitErrorCorrections(final String errorCorrections, final String caseId) {
        final String urlSuffix = format("/cases/%s/resolve-errors", caseId);
        makePostCall(getWriteUrl(urlSuffix),
                "application/vnd.prosecutioncasefile.command.resolve-errors+json",
                errorCorrections);
    }

    public Optional<JsonEnvelope> getPrivateEvent(UUID caseId) {
        Matcher<Object> matcher = isJson(Matchers.allOf(
                withJsonPath("$.prosecutionWithReferenceData.prosecution.caseDetails.caseId", is(caseId.toString()))));

        return retrieveMessageWithMatchers(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, matcher);

    }

    public Optional<JsonEnvelope> getDefendantValidationFailedEvent() {
        return retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED);
    }

    public Optional<JsonEnvelope> getDefendantValidationPassedEvent() {
        return retrieveEvent(EVENT_SELECTOR_DEFENDANT_VALIDATION_PASSED);
    }

    public Optional<JsonEnvelope> getCaseValidationFailedEvent() {
        return retrieveEvent(EVENT_SELECTOR_CASE_VALIDATION_FAILED);
    }

    public Optional<JsonEnvelope> getApplicationCreationRequested() {
        return retrieveEvent(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);
    }
}
