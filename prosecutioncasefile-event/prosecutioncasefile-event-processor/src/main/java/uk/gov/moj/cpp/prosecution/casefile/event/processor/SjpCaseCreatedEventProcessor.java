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
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfullyWithWarnings;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class SjpCaseCreatedEventProcessor {

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.sjp-case-created-successfully")
    public void handleSjpProsecutionCreated(final Envelope<SjpCaseCreatedSuccessfully> envelope) {
        final SjpCaseCreatedSuccessfully sjpCaseCreatedSuccessfully = envelope.payload();
        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        final Metadata metadata = builder.withName("public.prosecutioncasefile.prosecution-submission-succeeded").build();
        final ProsecutionSubmissionSucceeded payload = prosecutionSubmissionSucceeded()
                .withCaseId(sjpCaseCreatedSuccessfully.getCaseId())
                .withExternalId(sjpCaseCreatedSuccessfully.getExternalId())
                .withChannel(sjpCaseCreatedSuccessfully.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

    @Handles("prosecutioncasefile.events.sjp-case-created-successfully-with-warnings")
    public void handleSjpProsecutionCreatedWithWarnings(final Envelope<SjpCaseCreatedSuccessfullyWithWarnings> envelope) {
        final SjpCaseCreatedSuccessfullyWithWarnings caseCreatedSuccessfullyWithWarnings = envelope.payload();
        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        final Metadata metadata = builder.withName("public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings").build();

        final ProsecutionSubmissionSucceededWithWarnings payload = prosecutionSubmissionSucceededWithWarnings()
                .withCaseId(caseCreatedSuccessfullyWithWarnings.getCaseId())
                .withWarnings(caseCreatedSuccessfullyWithWarnings.getWarnings())
                .withExternalId(caseCreatedSuccessfullyWithWarnings.getExternalId())
                .withChannel(caseCreatedSuccessfullyWithWarnings.getChannel())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }

}
