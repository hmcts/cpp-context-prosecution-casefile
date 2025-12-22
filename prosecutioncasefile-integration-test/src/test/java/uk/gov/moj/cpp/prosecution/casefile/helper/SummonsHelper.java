package uk.gov.moj.cpp.prosecution.casefile.helper;

import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

public class SummonsHelper extends AbstractTestHelper {

    public SummonsHelper() {
        createPrivateConsumer(EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY);

        createPublicConsumer(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
    }

    public Optional<JsonEnvelope> getPrivateEvent() {
        return retrieveEvent(EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY);
    }

    public Optional<JsonEnvelope> getPublicEvent() {
        return retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
    }
}
