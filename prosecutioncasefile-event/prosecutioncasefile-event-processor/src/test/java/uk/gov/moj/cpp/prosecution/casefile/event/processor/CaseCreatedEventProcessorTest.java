package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully.caseCreatedSuccessfully;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfullyWithWarnings.caseCreatedSuccessfullyWithWarnings;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfullyWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseCreatedEventProcessorTest {

    private static final String CASE_CREATED_SUCCESSFULLY = "prosecutioncasefile.events.case-created-successfully";
    private static final String CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS = "prosecutioncasefile.events.case-created-successfully-with-warnings";
    private static final String PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    private static final String CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.civil.prosecution-submission-succeeded";
    private static final String PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS = "public.prosecutioncasefile.prosecution-submission-succeeded-with-warnings";
    private static final String DEFENDANT_REFERENCE = "TF12345";
    private final static String PROBLEM_VALUE_KEY_1 = "daysOverdue";
    private final static String PROBLEM_VALUE_KEY_2 = "daysOverdue";
    private final static String PROBLEM_VALUE_1 = "4";
    private final static String PROBLEM_VALUE_2 = "9";
    private final static String PROBLEM_CODE = "OFFENCE_OUT_OF_TIME";
    private final static Channel CHANNEL = randomEnum(Channel.class).next();

    @InjectMocks
    private CaseCreatedEventProcessor caseCreatedEventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionSubmissionSucceeded>> captor;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionSubmissionSucceededWithWarnings>> captorWithWarnings;

    @Test
    public void shouldHandleProsecutionCreatedEvent() {
        final UUID id = randomUUID();
        final Envelope<CaseCreatedSuccessfully> envelope = buildCaseCreatedSuccessfullyEnvelope(id);

        caseCreatedEventProcessor.handleProsecutionCreated(envelope);

        verify(sender).send(captor.capture());

        final Envelope<ProsecutionSubmissionSucceeded> receivedEnvelope = captor.getValue();
        final ProsecutionSubmissionSucceeded payload = receivedEnvelope.payload();
        if(CIVIL == CHANNEL){
            assertThat(receivedEnvelope.metadata().name(), is(CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED));
        } else {
            assertThat(receivedEnvelope.metadata().name(), is(PROSECUTION_SUBMISSION_SUCCEEDED));
        }

        assertThat(payload.getCaseId(), is(id));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(payload.getChannel(), is(CHANNEL));
    }

    @Test
    public void shouldEmitCivilProsecutionSubmissionSucceededPublicEvent() {

        final UUID id = randomUUID();
        final Envelope<CaseCreatedSuccessfully> envelope = buildCivilCaseCreatedSuccessfullyEnvelope(id);

        caseCreatedEventProcessor.handleProsecutionCreated(envelope);

        verify(sender).send(captor.capture());

        final Envelope<ProsecutionSubmissionSucceeded> receivedEnvelope = captor.getValue();
        final ProsecutionSubmissionSucceeded payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(CIVIL_PROSECUTION_SUBMISSION_SUCCEEDED));
        assertThat(payload.getCaseId(), is(id));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(payload.getChannel(), is(CIVIL));
    }


    @Test
    public void shouldHandleProsecutionCreatedEventWithWarnings() {
        final UUID id = randomUUID();
        final Envelope<CaseCreatedSuccessfullyWithWarnings> envelope = buildCaseCreatedSuccessfullyWithWarningsEnvelope(id);

        caseCreatedEventProcessor.handleProsecutionCreatedWithWarnings(envelope);

        verify(sender).send(captorWithWarnings.capture());

        final Envelope<ProsecutionSubmissionSucceededWithWarnings> receivedEnvelope = captorWithWarnings.getValue();
        final ProsecutionSubmissionSucceededWithWarnings payload = receivedEnvelope.payload();
        assertThat(receivedEnvelope.metadata().name(), is(PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS));
        assertThat(payload.getCaseId(), is(id));
        assertThat(payload.getDefendantWarnings().size(), is(1));
        assertThat(payload.getDefendantWarnings().get(0).getProsecutorDefendantReference(), is(DEFENDANT_REFERENCE));
        assertThat(payload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(payload.getChannel(), is(CHANNEL));
    }

    private Envelope<CaseCreatedSuccessfully> buildCaseCreatedSuccessfullyEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(CASE_CREATED_SUCCESSFULLY)
                .withId(randomUUID())
                .build();

        final CaseCreatedSuccessfully caseCreatedSuccessfully = caseCreatedSuccessfully()
                .withCaseId(generatedRandomUUID)
                .withExternalId(randomUUID())
                .withChannel(CHANNEL)
                .build();

        return envelopeFrom(metadata, caseCreatedSuccessfully);
    }

    private Envelope<CaseCreatedSuccessfully> buildCivilCaseCreatedSuccessfullyEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(CASE_CREATED_SUCCESSFULLY)
                .withId(randomUUID())
                .build();

        final CaseCreatedSuccessfully caseCreatedSuccessfully = caseCreatedSuccessfully()
                .withCaseId(generatedRandomUUID)
                .withExternalId(randomUUID())
                .withChannel(CIVIL)
                .build();

        return envelopeFrom(metadata, caseCreatedSuccessfully);
    }

    private Envelope<CaseCreatedSuccessfullyWithWarnings> buildCaseCreatedSuccessfullyWithWarningsEnvelope(final UUID generatedRandomUUID) {
        final Metadata metadata = metadataBuilder()
                .withName(CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS)
                .withId(randomUUID())
                .build();

        final CaseCreatedSuccessfullyWithWarnings caseCreatedSuccessfullyWithWarnings = caseCreatedSuccessfullyWithWarnings()
                .withCaseId(generatedRandomUUID)
                .withDefendantWarnings(ImmutableList.of(DefendantProblem.defendantProblem()
                        .withProblems(getProblems())
                        .withProsecutorDefendantReference(DEFENDANT_REFERENCE)
                        .build()))
                .withExternalId(randomUUID())
                .withChannel(CHANNEL)
                .build();

        return envelopeFrom(metadata, caseCreatedSuccessfullyWithWarnings);
    }

    private List<Problem> getProblems() {
        return singletonList(Problem.problem()
                .withCode(PROBLEM_CODE)
                .withValues(getProblemValues())
                .build());
    }

    private List<ProblemValue> getProblemValues() {
        return asList(
                problemValue().withKey(PROBLEM_VALUE_KEY_1).withValue(PROBLEM_VALUE_1).build(),
                problemValue().withKey(PROBLEM_VALUE_KEY_2).withValue(PROBLEM_VALUE_2).build());
    }
}
