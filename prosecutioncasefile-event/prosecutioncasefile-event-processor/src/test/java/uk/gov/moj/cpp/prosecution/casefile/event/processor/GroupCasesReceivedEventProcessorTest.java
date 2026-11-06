package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.GroupCasesReceivedEventProcessor.PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution.groupProsecution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToInitiateCourtProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class GroupCasesReceivedEventProcessorTest {
    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<InitiateCourtProceedingsForGroupCases>> senderArgCaptor;

    @Captor
    private ArgumentCaptor<Envelope> genericSenderArgCaptor;

    @Mock
    private GroupCasesReceivedToInitiateCourtProceedingsConverter groupCasesReceivedToInitiateCourtProceedingsConverter;

    @InjectMocks
    private GroupCasesReceivedEventProcessor groupCasesReceivedEventProcessor;

    @Test
    public void shouldHandleGroupCasesReceived() {
        final GroupCasesReceived groupCasesReceived = createGroupCasesReceived();
        final Envelope<GroupCasesReceived> requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-cases-received"),
                groupCasesReceived);
        final InitiateCourtProceedingsForGroupCases initiateCourtProceedingsForGroupCases = createInitiateCourtProceedingsForGroupCases();
        when(groupCasesReceivedToInitiateCourtProceedingsConverter.convert(any())).thenReturn(initiateCourtProceedingsForGroupCases);
        groupCasesReceivedEventProcessor.handleGroupCasesReceived(requestEnvelope);
        verify(sender, times(1)).send(senderArgCaptor.capture());

        final Envelope<InitiateCourtProceedingsForGroupCases> initiateCourtProceedingsForGroupCasesEnvelope = senderArgCaptor.getValue();
        assertThat(initiateCourtProceedingsForGroupCasesEnvelope.metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES));
        assertThat(initiateCourtProceedingsForGroupCasesEnvelope.payload().getClass().getName(), is(InitiateCourtProceedingsForGroupCases.class.getName()));
    }

    @Test
    public void shouldEmitGroupSubmissionApprovedPublicEventForCivilSummonsGroup() {
        final UUID externalId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final GroupCasesReceived groupCasesReceived = GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(buildCivilSummonsGroupProsecutionList(groupId, externalId))
                .build();
        final Envelope<GroupCasesReceived> requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-cases-received"),
                groupCasesReceived);
        when(groupCasesReceivedToInitiateCourtProceedingsConverter.convert(any())).thenReturn(createInitiateCourtProceedingsForGroupCases());

        groupCasesReceivedEventProcessor.handleGroupCasesReceived(requestEnvelope);

        verify(sender, times(2)).send(genericSenderArgCaptor.capture());

        final Envelope<PublicGroupSubmissionApproved> submissionApprovedEnvelope = genericSenderArgCaptor.getAllValues().get(0);
        assertThat(submissionApprovedEnvelope.metadata().name(), is("public.prosecutioncasefile.group-submission-approved"));
        final PublicGroupSubmissionApproved payload = submissionApprovedEnvelope.payload();
        assertThat(payload.getGroupId(), is(groupId));
        assertThat(payload.getExternalId(), is(externalId));

        assertThat(genericSenderArgCaptor.getAllValues().get(1).metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES));
    }

    @Test
    public void shouldNotEmitGroupSubmissionApprovedPublicEventForNonSummonsCivilGroup() {
        final GroupCasesReceived groupCasesReceived = GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(buildGroupProsecutionListWithChannelAndInitiationCode(CIVIL, "O"))
                .build();
        final Envelope<GroupCasesReceived> requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-cases-received"),
                groupCasesReceived);
        when(groupCasesReceivedToInitiateCourtProceedingsConverter.convert(any())).thenReturn(createInitiateCourtProceedingsForGroupCases());

        groupCasesReceivedEventProcessor.handleGroupCasesReceived(requestEnvelope);

        verify(sender, times(1)).send(genericSenderArgCaptor.capture());
        assertThat(genericSenderArgCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES));
    }

    @Test
    public void shouldNotEmitGroupSubmissionApprovedPublicEventForNonCivilSummonsGroup() {
        final GroupCasesReceived groupCasesReceived = GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(buildGroupProsecutionListWithChannelAndInitiationCode(SPI, "S"))
                .build();
        final Envelope<GroupCasesReceived> requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-cases-received"),
                groupCasesReceived);
        when(groupCasesReceivedToInitiateCourtProceedingsConverter.convert(any())).thenReturn(createInitiateCourtProceedingsForGroupCases());

        groupCasesReceivedEventProcessor.handleGroupCasesReceived(requestEnvelope);

        verify(sender, times(1)).send(genericSenderArgCaptor.capture());
        assertThat(genericSenderArgCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_GROUP_CASES));
    }

    private static GroupProsecutionList buildCivilSummonsGroupProsecutionList(final UUID groupId, final UUID externalId) {
        final GroupProsecution masterCase = groupProsecution()
                .withGroupId(groupId)
                .withIsGroupMaster(true)
                .withCaseDetails(caseDetails().withInitiationCode("S").build())
                .build();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = Arrays.asList(new GroupProsecutionWithReferenceData(masterCase));
        return new GroupProsecutionList(groupProsecutionWithReferenceDataList, externalId, CIVIL);
    }

    private static GroupProsecutionList buildGroupProsecutionListWithChannelAndInitiationCode(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel channel, final String initiationCode) {
        final GroupProsecution masterCase = groupProsecution()
                .withGroupId(UUID.randomUUID())
                .withIsGroupMaster(true)
                .withCaseDetails(caseDetails().withInitiationCode(initiationCode).build())
                .build();
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = Arrays.asList(new GroupProsecutionWithReferenceData(masterCase));
        return new GroupProsecutionList(groupProsecutionWithReferenceDataList, UUID.randomUUID(), channel);
    }

    private static GroupCasesReceived createGroupCasesReceived() {
        return GroupCasesReceived.groupCasesReceived().withGroupProsecutionList(new GroupProsecutionList(Arrays.asList(), UUID.randomUUID()))
                .build();
    }

    private static InitiateCourtProceedingsForGroupCases createInitiateCourtProceedingsForGroupCases() {
        return InitiateCourtProceedingsForGroupCases.initiateCourtProceedingsForGroupCases()
                .build();
    }
}