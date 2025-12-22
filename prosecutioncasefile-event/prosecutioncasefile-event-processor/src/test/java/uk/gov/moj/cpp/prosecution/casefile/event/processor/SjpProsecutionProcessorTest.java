package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;

public class SjpProsecutionProcessorTest {

    /**
     * Specific tests for each handler into their specific classes:
     * {@link SjpProsecutionProcessor#handleSjpProsecutionReceived} -> {@link SjpProsecutionReceivedProcessorTest}
     * {@link SjpProsecutionProcessor#handleSjpProsecutionInitiated} -> {@link SjpProsecutionInitiatedProcessorTest}
     */

    @Test
    public void shouldHandleSjpProsecutionEvents() {
        assertThat(SjpProsecutionProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleSjpProsecutionReceived").thatHandles("prosecutioncasefile.events.sjp-prosecution-received"))
                .with(method("handleSjpProsecutionInitiated").thatHandles("prosecutioncasefile.events.sjp-prosecution-initiated"))
                .with(method("handleSjpProsecutionReceivedWithWarnings").thatHandles("prosecutioncasefile.events.sjp-prosecution-received-with-warnings"))
                .with(method("handleSjpProsecutionInitiatedWithWarnings").thatHandles("prosecutioncasefile.events.sjp-prosecution-initiated-with-warnings"))
        );
    }

}