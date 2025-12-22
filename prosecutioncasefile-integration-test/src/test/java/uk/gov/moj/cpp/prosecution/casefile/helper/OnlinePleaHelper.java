package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import org.hamcrest.core.Is;

public class OnlinePleaHelper extends AbstractTestHelper {

    public static final String PUBLIC_PROSECUTIONCASEFILE_SJP_PLEAD_ONLINE = "public.prosecutioncasefile.sjp-plead-online";

    public OnlinePleaHelper(final String expectedPrivateEventName) {
        createPrivateConsumerForMultipleSelectors(
                expectedPrivateEventName);

        createPublicConsumerForMultipleSelectors(PUBLIC_PROSECUTIONCASEFILE_SJP_PLEAD_ONLINE);
    }

    public void submitPlea(final String payload, final UUID caseId, final UUID defendantId) {
        makePostCall(getWriteUrl(format("/cases/%s/defendants/%s/plead-online", caseId, defendantId)),
                "application/vnd.prosecutioncasefile.plead-online+json",
                payload);
    }

    public void submitPleaPcqVisited(final String payload, final UUID caseId, final UUID defendantId, final UUID pcqId) {
        makePostCall(getWriteUrl(format("/cases/%s/defendants/%s/pcq/%s/plead-online", caseId, defendantId, pcqId)),
                "application/vnd.prosecutioncasefile.plead-online-pcq-visited+json",
                payload);
    }

    public void thenOnlinePleaSubmittedPrivateEventShouldBeRaised(final UUID caseId) {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent("prosecutioncasefile.events.online-plea-submitted");
        assertThat(jsonEnvelope.isPresent(), is(true));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
    }

    public void thenOnlinePleaPcqVisitedSubmittedPrivateEventShouldBeRaised(final UUID caseId, final UUID defendantId, final UUID pcqId, final String type) {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent("prosecutioncasefile.events.online-plea-pcq-visited-submitted");
        assertThat(jsonEnvelope.isPresent(), is(true));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getJsonObject("pleadOnlineVisited").getString("defendantId"), is(defendantId.toString()));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getJsonObject("pleadOnlineVisited").getString("pcqId"), is(pcqId.toString()));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getJsonObject("pleadOnlineVisited").getString("type"), is(type));
    }

    public void verifyPublicEventRaisedForSJPReceived(final UUID caseId, final UUID defendantId) {
        assertThat(retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_SJP_PLEAD_ONLINE).get(), jsonEnvelope(
                metadata().withName(PUBLIC_PROSECUTIONCASEFILE_SJP_PLEAD_ONLINE),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", Is.is(caseId.toString())),
                        withJsonPath("$.defendantId", Is.is(defendantId.toString()))))));
    }

}
