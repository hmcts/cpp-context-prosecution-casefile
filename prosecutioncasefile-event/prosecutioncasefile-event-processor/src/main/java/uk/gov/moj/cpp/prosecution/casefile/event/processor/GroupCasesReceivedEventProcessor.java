package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved.publicGroupSubmissionApproved;

import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToCreateGroupCaseRequestConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToInitiateCourtProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.CaseManagementService;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class GroupCasesReceivedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCasesReceivedEventProcessor.class);
    public static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES = "progression.initiate-court-proceedings-for-group-cases";
    private static final String PUBLIC_EVENT_PROSECUTIONCASEFILE_GROUP_SUBMISSION_APPROVED = "public.prosecutioncasefile.group-submission-approved";
    private static final String SUMMONS_INITIATION_CODE = "S";
    public static final String CREATE_GROUP_CASE_FEATURE = "create-or-result-bulk-complaints-lists";

    @Inject
    private Sender sender;

    @Inject
    private GroupCasesReceivedToInitiateCourtProceedingsConverter groupCasesReceivedToInitiateCourtProceedingsConverter;

    @Inject
    private GroupCasesReceivedToCreateGroupCaseRequestConverter groupCasesReceivedToCreateGroupCaseRequestConverter;

    @Inject
    private CaseManagementService caseManagementService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("prosecutioncasefile.events.group-cases-received")
    public void handleGroupCasesReceived(final Envelope<GroupCasesReceived> envelope) {

        final GroupCasesReceived payload = envelope.payload();

        // CAD-1850: case management records the group before anything else is sent. If it fails the
        // exception propagates, nothing below is sent, and the event is redelivered then dead-lettered.
        // A redelivery after success gets 409 from case management, which counts as created.
        // CAD-1874: only when the toggle is on; otherwise go straight to Progression as before.
        if (isCreateGroupCaseEnabled(payload.getGroupProsecutionList().getExternalId())) {
            groupCasesReceivedToCreateGroupCaseRequestConverter.convert(payload)
                    .ifPresent(request -> caseManagementService.createGroupCase(request, envelope.metadata()));
        }

        emitGroupSubmissionApprovedIfCivilSummons(payload.getGroupProsecutionList(), envelope);

        LOGGER.info("Posting {} for submission id {}  and calling {} ", PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES, payload.getGroupProsecutionList().getExternalId(), PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES)
                .build();

        final Envelope<InitiateCourtProceedingsForGroupCases> commandEnvelope = envelopeFrom(metadata, this.groupCasesReceivedToInitiateCourtProceedingsConverter.convert(payload));
        this.sender.send(commandEnvelope);
    }

    /**
     * Release toggle for the case management call (CAD-1874). A failed toggle lookup counts as off,
     * so a feature store problem never blocks a group submission.
     */
    private boolean isCreateGroupCaseEnabled(final UUID externalId) {
        try {
            if (featureControlGuard.isFeatureEnabled(CREATE_GROUP_CASE_FEATURE)) {
                return true;
            }
            LOGGER.info("Feature {} is off, not creating group case in case management for submission id {}", CREATE_GROUP_CASE_FEATURE, externalId);
        } catch (final RuntimeException e) {
            LOGGER.warn("Feature {} lookup failed, treating it as off and not creating group case in case management for submission id {}", CREATE_GROUP_CASE_FEATURE, externalId, e);
        }
        return false;
    }

    private void emitGroupSubmissionApprovedIfCivilSummons(final GroupProsecutionList groupProsecutionList, final Envelope<?> envelope) {
        final Optional<GroupProsecution> masterCase = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .map(GroupProsecutionWithReferenceData::getGroupProsecution)
                .filter(GroupProsecution::getIsGroupMaster)
                .findFirst();

        final String masterCaseInitiationCode = masterCase.map(groupProsecution -> groupProsecution.getCaseDetails().getInitiationCode()).orElse(null);

        if (CIVIL == groupProsecutionList.getChannel() && SUMMONS_INITIATION_CODE.equals(masterCaseInitiationCode)) {
            final UUID groupId = masterCase.map(GroupProsecution::getGroupId).orElse(null);

            final Metadata publicEventMetadata = metadataFrom(envelope.metadata())
                    .withName(PUBLIC_EVENT_PROSECUTIONCASEFILE_GROUP_SUBMISSION_APPROVED)
                    .build();

            final PublicGroupSubmissionApproved publicGroupSubmissionApproved = publicGroupSubmissionApproved()
                    .withGroupId(groupId)
                    .withExternalId(groupProsecutionList.getExternalId())
                    .build();

            sender.send(envelopeFrom(publicEventMetadata, publicGroupSubmissionApproved));
        }
    }
}