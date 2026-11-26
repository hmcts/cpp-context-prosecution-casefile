package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnlinePcqVisited;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PleadOnlineHandler extends BaseProsecutionCaseFileHandler {

    private static final String STREAM_ID = "caseId";

    @Inject
    private Clock clock;


    @Handles("prosecutioncasefile.command.plead-online")
    public void pleadOnline(final Envelope<PleadOnline> envelope) throws EventStreamException {
        final PleadOnline pleadOnline = envelope.payload();
        final UUID userId = getUserId(envelope);

        appendEventsToStream(pleadOnline.getCaseId(), envelope, prosecutionCaseFile ->
                prosecutionCaseFile.pleadOnline(pleadOnline.getCaseId(), pleadOnline, clock.now(), userId));
    }

    @Handles("prosecutioncasefile.command.plead-online-pcq-visited")
    public void pleadOnlinePcqVisited(final Envelope<PleadOnlinePcqVisited> envelope) throws EventStreamException {
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = envelope.payload();
        final UUID userId = getUserId(envelope);

        appendEventsToStream(pleadOnlinePcqVisited.getCaseId(), envelope, prosecutionCaseFile ->
                prosecutionCaseFile.pleadOnlinePcqVisited(pleadOnlinePcqVisited.getCaseId(), pleadOnlinePcqVisited, clock.now(), userId));
    }


    protected UUID getCaseId(final JsonObject payload) {
        return UUID.fromString(payload.getString(STREAM_ID));
    }

    protected UUID getUserId(final Envelope<?> command) {
        return command.metadata().userId().map(UUID::fromString).orElse(null);
    }
}
