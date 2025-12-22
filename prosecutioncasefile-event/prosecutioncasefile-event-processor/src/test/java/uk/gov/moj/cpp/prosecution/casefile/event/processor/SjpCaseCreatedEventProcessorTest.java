package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully.sjpCaseCreatedSuccessfully;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfullyWithWarnings.sjpCaseCreatedSuccessfullyWithWarnings;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfullyWithWarnings;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SjpCaseCreatedEventProcessorTest {

    private static final String SJP_CASE_CREATED_SUCCESSFULLY = "sjp-case-created-successfully";
    private static final String PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";

    private static final String SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS = "prosecutioncasefile.events.sjp-case-created-successfully-with-warnings";
    private static final String PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    private static final Channel CHANNEL = randomEnum(Channel.class).next();

    @InjectMocks
    private SjpCaseCreatedEventProcessor sjpCaseCreatedEventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionSubmissionSucceeded>> captor;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionSubmissionSucceededWithWarnings>> captorWithWarnings;

    @Test
    public void shouldHandleSjpProsecutionCreatedWithWarningsEvent() {
        final UUID id = randomUUID();
        final Envelope<SjpCaseCreatedSuccessfullyWithWarnings> envelope = buildSjpCaseCreatedSuccessfullyWithWarningsEnvelope(id);

        sjpCaseCreatedEventProcessor.handleSjpProsecutionCreatedWithWarnings(envelope);

        verify(sender).send(captorWithWarnings.capture());

        final Envelope<ProsecutionSubmissionSucceededWithWarnings> receivedEnvelope = captorWithWarnings.getValue();
        final ProsecutionSubmissionSucceededWithWarnings payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS));
        assertThat(payload.getCaseId(), is(id));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(payload.getChannel(), is(CHANNEL));
    }

    @Test
    public void shouldHandleSjpProsecutionCreatedEvent() {
        final UUID id = randomUUID();
        final Envelope<SjpCaseCreatedSuccessfully> envelope = buildSjpCaseCreatedSuccessfullyEnvelope(id);

        sjpCaseCreatedEventProcessor.handleSjpProsecutionCreated(envelope);

        verify(sender).send(captor.capture());

        final Envelope<ProsecutionSubmissionSucceeded> receivedEnvelope = captor.getValue();
        final ProsecutionSubmissionSucceeded payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(PROSECUTION_SUBMISSION_SUCCEEDED));
        assertThat(payload.getCaseId(), is(id));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(payload.getChannel(), is(CHANNEL));
    }

    private Envelope<SjpCaseCreatedSuccessfullyWithWarnings> buildSjpCaseCreatedSuccessfullyWithWarningsEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS)
                .withId(randomUUID())
                .build();

        final SjpCaseCreatedSuccessfullyWithWarnings sjpCaseCreatedSuccessfullyWithWarnings = sjpCaseCreatedSuccessfullyWithWarnings()
                .withCaseId(generatedRandomUUID)
                .withExternalId(randomUUID())
                .withChannel(CHANNEL)
                .build();

        return envelopeFrom(metadata, sjpCaseCreatedSuccessfullyWithWarnings);
    }

    private Envelope<SjpCaseCreatedSuccessfully> buildSjpCaseCreatedSuccessfullyEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(SJP_CASE_CREATED_SUCCESSFULLY)
                .withId(randomUUID())
                .build();

        final SjpCaseCreatedSuccessfully sjpCaseCreatedSuccessfully = sjpCaseCreatedSuccessfully()
                .withCaseId(generatedRandomUUID)
                .withExternalId(randomUUID())
                .withChannel(CHANNEL)
                .build();

        return envelopeFrom(metadata, sjpCaseCreatedSuccessfully);
    }
}