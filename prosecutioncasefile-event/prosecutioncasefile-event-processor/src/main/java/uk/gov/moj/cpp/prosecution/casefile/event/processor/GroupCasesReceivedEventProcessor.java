package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved.publicGroupSubmissionApproved;

import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToInitiateCourtProceedingsConverter;
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

    @Inject
    private Sender sender;

    @Inject
    private GroupCasesReceivedToInitiateCourtProceedingsConverter groupCasesReceivedToInitiateCourtProceedingsConverter;

    @Handles("prosecutioncasefile.events.group-cases-received")
    public void handleGroupCasesReceived(final Envelope<GroupCasesReceived> envelope) {

        final GroupCasesReceived payload = envelope.payload();

        emitGroupSubmissionApprovedIfCivilSummons(payload.getGroupProsecutionList(), envelope);

        LOGGER.info("Posting {} for submission id {}  and calling {} ", PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES, payload.getGroupProsecutionList().getExternalId(), PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES)
                .build();

        final Envelope<InitiateCourtProceedingsForGroupCases> commandEnvelope = envelopeFrom(metadata, this.groupCasesReceivedToInitiateCourtProceedingsConverter.convert(payload));
        this.sender.send(commandEnvelope);
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