package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

@ExtendWith(MockitoExtension.class)
public class GroupProsecutionHandlerTest {

    @InjectMocks
    private GroupProsecutionHandler groupProsecutionHandler;

    @Test
    public void shouldHandleInitiateGroupProsecutionWithReferenceData() {
        assertThat(groupProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleInitiateGroupProsecutionWithReferenceData")
                        .thatHandles("prosecutioncasefile.command.initiate-group-prosecution-with-reference-data")
                ));
    }
}