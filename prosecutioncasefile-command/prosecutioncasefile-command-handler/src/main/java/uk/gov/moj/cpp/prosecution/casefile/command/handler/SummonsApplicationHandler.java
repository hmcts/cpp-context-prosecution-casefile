package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails.summonsApplicationApprovedDetails;
import static uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationRejectedDetails.summonsApplicationRejectedDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ApproveCaseDefendantsAsSummonsApplicationApproved;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectCaseDefendantsAsSummonsApplicationRejected;

import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.json.JsonValue;

@ServiceComponent(COMMAND_HANDLER)
public class SummonsApplicationHandler extends BaseProsecutionCaseFileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsApplicationHandler.class);

    @Inject
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Inject
    private Instance<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers;

    @Inject
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Inject
    private ProgressionService progressionService;

    @Handles("prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved")
    public void approveCaseDefendantsAsSummonsApplicationApproved(final Envelope<ApproveCaseDefendantsAsSummonsApplicationApproved> envelope) throws EventStreamException {
        final ApproveCaseDefendantsAsSummonsApplicationApproved commandPayload = envelope.payload();
        final UUID caseId = commandPayload.getCaseId();
        final UUID applicationId = commandPayload.getApplicationId();

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);

        LOGGER.info("prosecutioncasefile.command.record-group-id-for-summons-application caseId: {}, applicationId: {}, groupId: {}", caseId, applicationId, prosecutionCaseFile.getGroupId());
        final CourtApplication summonsApplication = progressionService.getApplicationOnly(applicationId);
        final Boolean isCivil = summonsApplication.getCourtCivilApplication().getIsCivil();
        if (isNull(prosecutionCaseFile.getGroupId())) {
            final Stream<Object> events = prosecutionCaseFile.approveCaseDefendants(
                    summonsApplicationApprovedDetails()
                            .withApplicationId(applicationId)
                            .withCaseId(caseId)
                            .withSummonsApprovedOutcome(commandPayload.getSummonsApprovedOutcome())
                            .build(),
                    newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), isCivil);

            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
        } else {
            final EventStream eventStreamForGroupCases = eventSource.getStreamById(prosecutionCaseFile.getGroupId());
            final GroupProsecutionCaseFile groupProsecutionCaseFile = aggregateService.get(eventStreamForGroupCases, GroupProsecutionCaseFile.class);
            final Stream<Object> events = groupProsecutionCaseFile.approveGroupProsecution(newArrayList(groupCasesReferenceDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()));
            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
            eventStreamForGroupCases.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
        }
    }

    @Handles("prosecutioncasefile.command.reject-case-defendants-as-summons-application-rejected")
    public void rejectCaseDefendantsAsSummonsApplicationRejected(final Envelope<RejectCaseDefendantsAsSummonsApplicationRejected> envelope) throws EventStreamException {
        final RejectCaseDefendantsAsSummonsApplicationRejected commandPayload = envelope.payload();
        final UUID caseId = commandPayload.getCaseId();
        final UUID applicationId = commandPayload.getApplicationId();

        final EventStream eventStream = eventSource.getStreamById(caseId);
        final ProsecutionCaseFile prosecutionCaseFile = aggregateService.get(eventStream, ProsecutionCaseFile.class);
        LOGGER.info("prosecutioncasefile.command.reject-case-defendants-as-summons-application-rejected caseId: {}, applicationId: {}, groupId: {}", caseId, applicationId, prosecutionCaseFile.getGroupId());

        if (isNull(prosecutionCaseFile.getGroupId())) {
            final Stream<Object> events = prosecutionCaseFile.rejectCaseDefendants(
                    summonsApplicationRejectedDetails()
                            .withApplicationId(applicationId)
                            .withCaseId(caseId)
                            .withSummonsRejectedOutcome(commandPayload.getSummonsRejectedOutcome())
                            .build());

            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
            eventStream.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
        } else {
            final EventStream eventStreamForGroupCases = eventSource.getStreamById(prosecutionCaseFile.getGroupId());
            final GroupProsecutionCaseFile groupProsecutionCaseFile = aggregateService.get(eventStreamForGroupCases, GroupProsecutionCaseFile.class);
            final Stream<Object> events = groupProsecutionCaseFile.rejectGroupProsecution();
            final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(envelope.metadata(), JsonValue.NULL);
            eventStreamForGroupCases.append(events.map(toEnvelopeWithMetadataFrom(jsonEnvelope)));
        }
    }
}
