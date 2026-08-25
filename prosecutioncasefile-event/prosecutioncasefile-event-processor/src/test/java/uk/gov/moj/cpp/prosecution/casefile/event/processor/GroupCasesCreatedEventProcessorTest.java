package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully.groupCasesCreatedSuccessfully;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupSubmissionApproved;

import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class GroupCasesCreatedEventProcessorTest {

    private static final String GROUP_CASES_CREATED_SUCCESSFULLY = "prosecutioncasefile.events.group-cases-created-successfully";
    private static final String GROUP_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.group-submission-succeeded";
    private static final String GROUP_SUBMISSION_APPROVED = "public.prosecutioncasefile.group-submission-approved";
    private static final String SUMMONS_INITIATION_CODE = "S";
    private static final String NON_SUMMONS_INITIATION_CODE = "O";

    @InjectMocks
    private GroupCasesCreatedEventProcessor groupCasesCreatedEventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> groupSubmissionSucceededCaptor;

    @Test
    public void shouldHandleGroupCaseCreatedEvent() {
        final UUID id = randomUUID();
        final Envelope<GroupCasesCreatedSuccessfully> envelope = buildGroupCaseCreatedSuccessfullyEnvelope(id, SPI, NON_SUMMONS_INITIATION_CODE);

        groupCasesCreatedEventProcessor.handleGroupCasesCreated(envelope);

        verify(sender, times(1)).send(groupSubmissionSucceededCaptor.capture());

        final Envelope<GroupSubmissionSucceeded> receivedEnvelope = groupSubmissionSucceededCaptor.getAllValues().get(0);
        final GroupSubmissionSucceeded payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(GROUP_SUBMISSION_SUCCEEDED));
        assertThat(payload.getGroupId(), is(id));
    }

    @Test
    public void shouldEmitGroupSubmissionApprovedPublicEvent() {
        final UUID id = randomUUID();
        final Envelope<GroupCasesCreatedSuccessfully> envelope = buildGroupCaseCreatedSuccessfullyEnvelope(id, CIVIL, SUMMONS_INITIATION_CODE);

        groupCasesCreatedEventProcessor.handleGroupCasesCreated(envelope);

        verify(sender, times(2)).send(groupSubmissionSucceededCaptor.capture());

        final Envelope<PublicGroupSubmissionApproved> receivedEnvelope = groupSubmissionSucceededCaptor.getAllValues().get(1);
        final PublicGroupSubmissionApproved payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(GROUP_SUBMISSION_APPROVED));
        assertThat(payload.getGroupId(), is(id));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
    }

    @Test
    public void shouldNotEmitGroupSubmissionApprovedForNonSummonsCivilGroup() {
        final Envelope<GroupCasesCreatedSuccessfully> envelope = buildGroupCaseCreatedSuccessfullyEnvelope(randomUUID(), CIVIL, NON_SUMMONS_INITIATION_CODE);

        groupCasesCreatedEventProcessor.handleGroupCasesCreated(envelope);

        verify(sender, times(1)).send(groupSubmissionSucceededCaptor.capture());
        assertThat(groupSubmissionSucceededCaptor.getValue().metadata().name(), is(GROUP_SUBMISSION_SUCCEEDED));
    }

    @Test
    public void shouldNotEmitGroupSubmissionApprovedForNonCivilSummonsGroup() {
        final Envelope<GroupCasesCreatedSuccessfully> envelope = buildGroupCaseCreatedSuccessfullyEnvelope(randomUUID(), SPI, SUMMONS_INITIATION_CODE);

        groupCasesCreatedEventProcessor.handleGroupCasesCreated(envelope);

        verify(sender, times(1)).send(groupSubmissionSucceededCaptor.capture());
        assertThat(groupSubmissionSucceededCaptor.getValue().metadata().name(), is(GROUP_SUBMISSION_SUCCEEDED));
    }

    private Envelope<GroupCasesCreatedSuccessfully> buildGroupCaseCreatedSuccessfullyEnvelope(final UUID generatedRandomUUID, final Channel channel, final String initiationCode) {
        final Metadata metadata = metadataBuilder()
                .withName(GROUP_CASES_CREATED_SUCCESSFULLY)
                .withId(randomUUID())
                .build();

        final GroupCasesCreatedSuccessfully groupCasesCreatedSuccessfully = groupCasesCreatedSuccessfully()
                .withGroupId(generatedRandomUUID)
                .withExternalId(randomUUID())
                .withChannel(channel)
                .withInitiationCode(initiationCode)
                .build();

        return envelopeFrom(metadata, groupCasesCreatedSuccessfully);
    }
}
