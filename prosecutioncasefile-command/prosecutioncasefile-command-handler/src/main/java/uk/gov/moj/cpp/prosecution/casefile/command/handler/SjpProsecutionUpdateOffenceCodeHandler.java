package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.SjpProsecutionUpdateOffenceCode;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class SjpProsecutionUpdateOffenceCodeHandler extends BaseProsecutionCaseFileHandler {

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Handles("prosecutioncasefile.command.sjp-prosecution-update-offence-code")
    public void handleInitiateSjpProsecutionWithReferenceData(final Envelope<SjpProsecutionUpdateOffenceCode> envelope) throws EventStreamException {
        final SjpProsecutionUpdateOffenceCode payload = envelope.payload();

        appendEventsToStream(
                payload.getCaseId(),
                envelope,
                prosecutionCaseFile -> prosecutionCaseFile.updateOffenceCode(payload.getOffenceCode(), payload.getOffenceReferenceData(), jsonObjectToObjectConverter, objectToJsonObjectConverter));
    }


}
