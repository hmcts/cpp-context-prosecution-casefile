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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.InitiateCourtProceedingsForGroupCases;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesReceivedToInitiateCourtProceedingsConverter;

import java.util.Arrays;
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

    private static GroupCasesReceived createGroupCasesReceived() {
        return GroupCasesReceived.groupCasesReceived().withGroupProsecutionList(new GroupProsecutionList(Arrays.asList(), UUID.randomUUID()))
                .build();
    }

    private static InitiateCourtProceedingsForGroupCases createInitiateCourtProceedingsForGroupCases() {
        return InitiateCourtProceedingsForGroupCases.initiateCourtProceedingsForGroupCases()
                .build();
    }
}