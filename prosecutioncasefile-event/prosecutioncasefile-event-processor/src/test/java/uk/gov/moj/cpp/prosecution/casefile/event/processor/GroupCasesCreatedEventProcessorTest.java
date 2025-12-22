package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully.groupCasesCreatedSuccessfully;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;

import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class GroupCasesCreatedEventProcessorTest {

    private static final String GROUP_CASES_CREATED_SUCCESSFULLY = "prosecutioncasefile.events.group-cases-created-successfully";
    private static final String GROUP_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.group-submission-succeeded";

    @InjectMocks
    private GroupCasesCreatedEventProcessor groupCasesCreatedEventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<GroupSubmissionSucceeded>> groupSubmissionSucceededCaptor;

    @Test
    public void shouldHandleGroupCaseCreatedEvent() {
        final UUID id = randomUUID();
        final Envelope<GroupCasesCreatedSuccessfully> envelope = buildGroupCaseCreatedSuccessfullyEnvelope(id);

        groupCasesCreatedEventProcessor.handleGroupCasesCreated(envelope);

        verify(sender).send(groupSubmissionSucceededCaptor.capture());

        final Envelope<GroupSubmissionSucceeded> receivedEnvelope = groupSubmissionSucceededCaptor.getValue();
        final GroupSubmissionSucceeded payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(GROUP_SUBMISSION_SUCCEEDED));
        assertThat(payload.getGroupId(), is(id));
    }

    private Envelope<GroupCasesCreatedSuccessfully> buildGroupCaseCreatedSuccessfullyEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(GROUP_CASES_CREATED_SUCCESSFULLY)
                .withId(randomUUID())
                .build();

        final GroupCasesCreatedSuccessfully groupCasesCreatedSuccessfully = groupCasesCreatedSuccessfully()
                .withGroupId(generatedRandomUUID)
                .build();

        return envelopeFrom(metadata, groupCasesCreatedSuccessfully);
    }
}
