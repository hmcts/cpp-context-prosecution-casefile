package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;

import javax.inject.Inject;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupProsecutionRejectedProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupProsecutionRejectedProcessor.class);

    private static final String PUBLIC_GROUP_PROSECUTION_REJECTED_EVENT = "public.prosecutioncasefile.group-prosecution-rejected";

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.group-prosecution-rejected")
    public void handleGroupProsecutionRejected(final Envelope<GroupProsecutionRejected> groupProsecutionRejectedEvent) {
        final GroupProsecutionRejected prosecutionRejected = groupProsecutionRejectedEvent.payload();
        LOGGER.info("Raising public.prosecutioncasefile.group-prosecution-rejected for submission id {}",prosecutionRejected.getExternalId());
        final UUID groupId = prosecutionRejected.getGroupProsecutions().get(0).getGroupId();
        sender.send(envelopeFrom(
                metadataFrom(groupProsecutionRejectedEvent.metadata()).withName(PUBLIC_GROUP_PROSECUTION_REJECTED_EVENT),
                PublicGroupProsecutionRejected.publicGroupProsecutionRejected()
                        .withGroupId(groupId)
                        .withCaseErrors(prosecutionRejected.getCaseErrors())
                        .withGroupCaseErrors(prosecutionRejected.getGroupCaseErrors())
                        .withDefendantErrors(prosecutionRejected.getDefendantErrors())
                        .withExternalId(prosecutionRejected.getExternalId())
                        .withChannel(prosecutionRejected.getChannel())
                        .build()));

    }

}