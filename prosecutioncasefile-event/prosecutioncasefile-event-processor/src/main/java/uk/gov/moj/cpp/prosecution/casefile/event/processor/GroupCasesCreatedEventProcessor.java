package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded.groupSubmissionSucceeded;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupCasesCreatedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCasesCreatedEventProcessor.class.getCanonicalName());

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.group-cases-created-successfully")
    public void handleGroupCasesCreated(final Envelope<GroupCasesCreatedSuccessfully> envelope) {
        final GroupCasesCreatedSuccessfully groupCasesCreatedSuccessfully = envelope.payload();
        LOGGER.info("prosecutioncasefile.events.group-cases-created-successfully for Group with id '{}'", groupCasesCreatedSuccessfully.getGroupId());

        final MetadataBuilder builder = metadataFrom(envelope.metadata());
        final Metadata metadata = builder.withName("public.prosecutioncasefile.group-submission-succeeded").build();

        final GroupSubmissionSucceeded payload = groupSubmissionSucceeded()
                .withGroupId(groupCasesCreatedSuccessfully.getGroupId())
                .withExternalId(groupCasesCreatedSuccessfully.getExternalId())
                .build();

        sender.send(envelopeFrom(metadata, payload));
    }
}
