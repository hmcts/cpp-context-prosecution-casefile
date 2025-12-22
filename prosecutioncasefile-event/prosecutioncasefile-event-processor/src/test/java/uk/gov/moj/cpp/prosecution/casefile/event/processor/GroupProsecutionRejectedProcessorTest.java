package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;

@ExtendWith(MockitoExtension.class)
public class GroupProsecutionRejectedProcessorTest {
    private final static String PROBLEM_CODE = "code";
    private final static String PROBLEM_VALUE_KEY_1 = "value_key_1";
    private final static String PROBLEM_VALUE_KEY_2 = "value_key_2";
    private final static String PROBLEM_VALUE_1 = "value_1";
    private final static String PROBLEM_VALUE_2 = "value_2";

    private final static Channel CHANNEL = Channel.CIVIL;
    private final static UUID EXTERNAL_ID = randomUUID();

    @Captor
    private ArgumentCaptor<Envelope<PublicGroupProsecutionRejected>> senderArgCaptor;

    @Mock
    private Sender sender;

    @InjectMocks
    private GroupProsecutionRejectedProcessor groupProsecutionRejectedProcessor;

    @Test
    public void shouldHandleGroupProsecutionRejected() {
        final UUID groupId = randomUUID();
        final Envelope<GroupProsecutionRejected> envelope = buildGroupProsecutionRejectedEnvelope(groupId);
        groupProsecutionRejectedProcessor.handleGroupProsecutionRejected(envelope);
        verify(sender, times(1)).send(senderArgCaptor.capture());
        final Envelope<PublicGroupProsecutionRejected> publicEvent = senderArgCaptor.getValue();
        final PublicGroupProsecutionRejected publicGroupProsecutionRejected = publicEvent.payload();
        assertThat(publicGroupProsecutionRejected.getGroupId(), is(groupId));
        assertThat(publicGroupProsecutionRejected.getChannel(), is(CHANNEL));
        assertThat(publicGroupProsecutionRejected.getExternalId(), is(EXTERNAL_ID));
        assertThat(publicGroupProsecutionRejected.getCaseErrors(), is(envelope.payload().getCaseErrors()));
    }

    private Envelope<GroupProsecutionRejected> buildGroupProsecutionRejectedEnvelope(final UUID groupId) {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-prosecution-rejected"),
                GroupProsecutionRejected.groupProsecutionRejected()
                        .withGroupProsecutions( asList(
                                GroupProsecution.groupProsecution()
                                        .withGroupId(groupId)
                                        .build()))
                        .withChannel(CHANNEL)
                        .withExternalId(EXTERNAL_ID)
                        .withCaseErrors(getProblems())
                        .build()
        );

    }

    private List<Problem> getProblems() {
        List<Problem> problemList = new ArrayList<>();
        problemList.add(Problem.problem()
                .withCode(PROBLEM_CODE)
                .withValues(getProblemValues())
                .build());
        return problemList;
    }

    private List<ProblemValue> getProblemValues() {
        return asList(
                problemValue().withKey(PROBLEM_VALUE_KEY_1).withValue(PROBLEM_VALUE_1).build(),
                problemValue().withKey(PROBLEM_VALUE_KEY_2).withValue(PROBLEM_VALUE_2).build());
    }
}