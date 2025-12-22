package uk.gov.moj.cpp.prosecution.casefile.command.handler;


import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class GroupProsecutionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupProsecutionHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers;

    @Inject
    private ReferenceDataQueryService referenceDataQueryService;


    @Handles("prosecutioncasefile.command.initiate-group-prosecution-with-reference-data")
    public void handleInitiateGroupProsecutionWithReferenceData(final Envelope<GroupProsecutionList> envelope) throws EventStreamException {
        final GroupProsecutionList groupProsecutionList = envelope.payload();
        final UUID groupId = groupProsecutionList.getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getGroupId();
        LOGGER.info("prosecutioncasefile.command.initiate-group-prosecution-with-reference-data for submission id {} and GroupId {}", groupProsecutionList.getExternalId(), groupId);
        final EventStream eventStream = this.eventSource.getStreamById(groupId);
        final GroupProsecutionCaseFile groupProsecution = this.aggregateService.get(eventStream, GroupProsecutionCaseFile.class);
        final Stream<Object> events = groupProsecution.receiveGroupProsecution(groupProsecutionList,  newArrayList(this.groupCasesReferenceDataEnrichers.iterator()), newArrayList(this.defendantRefDataEnrichers.iterator()), this.referenceDataQueryService);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
        eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
    }
}
