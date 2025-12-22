package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded.prosecutionSubmissionSucceeded;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings.prosecutionSubmissionSucceededWithWarnings;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfullyWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class CaseCreatedEventProcessor {

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.case-created-successfully")
    public void handleProsecutionCreated(final Envelope<CaseCreatedSuccessfully> envelope) {
        final CaseCreatedSuccessfully caseCreatedSuccessfully = envelope.payload();

        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        final Metadata metadata = getMetadata(builder, caseCreatedSuccessfully.getChannel());
        final ProsecutionSubmissionSucceeded payload = prosecutionSubmissionSucceeded()
                .withCaseId(caseCreatedSuccessfully.getCaseId())
                .withExternalId(caseCreatedSuccessfully.getExternalId())
                .withChannel(caseCreatedSuccessfully.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

    @Handles("prosecutioncasefile.events.case-created-successfully-with-warnings")
    public void handleProsecutionCreatedWithWarnings(final Envelope<CaseCreatedSuccessfullyWithWarnings> envelope) {
        final CaseCreatedSuccessfullyWithWarnings caseCreatedSuccessfullyWithWarnings = envelope.payload();

        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        final Metadata metadata = builder.withName("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings").build();
        final ProsecutionSubmissionSucceededWithWarnings payload = prosecutionSubmissionSucceededWithWarnings()
                .withCaseId(caseCreatedSuccessfullyWithWarnings.getCaseId())
                .withWarnings(caseCreatedSuccessfullyWithWarnings.getWarnings())
                .withDefendantWarnings(caseCreatedSuccessfullyWithWarnings.getDefendantWarnings())
                .withExternalId(caseCreatedSuccessfullyWithWarnings.getExternalId())
                .withChannel(caseCreatedSuccessfullyWithWarnings.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

    private Metadata getMetadata(final MetadataBuilder builder, final Channel channel) {
        if (Channel.CIVIL == channel) {
            return builder.withName("public.prosecutioncasefile.civil.prosecution-submission-succeeded").build();
        } else {
            return builder.withName("public.prosecutioncasefile.prosecution-submission-succeeded").build();
        }
    }


}
