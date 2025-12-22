package uk.gov.moj.cpp.prosecution.casefile.helper;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseErrorDetails;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class QueryHelper {

    public static final int TIMEOUT = 30;

    public static ResponseData verifyCaseErrors(final UUID caseId, final Matcher<ReadContext> matcher) {

        return poll(getCaseErrorDetails(caseId.toString()))
                .timeout(20L, SECONDS)
                .pollInterval(1L, SECONDS)
                .until(status().is(OK),
                        payload().isJson(matcher)
                );
    }

    public static String pollForResponse(final String path, final String mediaType, final String userId, final ResponseStatusMatcher responseStatusMatcher, final Matcher... payloadMatchers) {

        return poll(requestParams(getReadUrl(path), mediaType)
                .withHeader(USER_ID, userId).build())
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .until(
                        responseStatusMatcher,
                        payload().isJson(allOf(payloadMatchers))
                )
                .getPayload();
    }
}
