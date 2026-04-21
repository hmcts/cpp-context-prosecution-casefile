package uk.gov.moj.cpp.prosecution.casefile.helper;

import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_DETAILS_UPDATED;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

public class AppplicationProceedingsEditedHelper extends AbstractTestHelper {

    public AppplicationProceedingsEditedHelper() {
        createPrivateConsumer(EVENT_SELECTOR_CASE_DETAILS_UPDATED);
    }

    public Optional<JsonEnvelope> getPrivateEvent() {
        return retrieveEvent(EVENT_SELECTOR_CASE_DETAILS_UPDATED);
    }
}
