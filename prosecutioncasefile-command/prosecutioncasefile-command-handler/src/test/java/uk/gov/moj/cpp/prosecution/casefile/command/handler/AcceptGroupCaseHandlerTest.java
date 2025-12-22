package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcceptGroupCaseHandlerTest {

    @InjectMocks
    private AcceptGroupCaseHandler acceptGroupCaseHandler;

    @Test
    public void shouldHandleAcceptGroupCas() {
        assertThat(acceptGroupCaseHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleAcceptGroupCases")
                        .thatHandles("prosecutioncasefile.command.handler.accept-group-cases")
                ));
    }
}
