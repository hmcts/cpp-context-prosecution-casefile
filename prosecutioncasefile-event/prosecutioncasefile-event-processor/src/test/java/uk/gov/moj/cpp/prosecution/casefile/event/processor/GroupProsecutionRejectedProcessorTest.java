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
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem.caseProblem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;

@ExtendWith(MockitoExtension.class)
public class GroupProsecutionRejectedProcessorTest {
    private final static String PROBLEM_CODE = "code";
    private final static String OTHER_PROBLEM_CODE = "other_code";
    private final static String URN_1 = "URN1";
    private final static String URN_2 = "URN2";
    private final static String PROBLEM_VALUE_KEY_1 = "value_key_1";
    private final static String PROBLEM_VALUE_KEY_2 = "value_key_2";
    private final static String PROBLEM_VALUE_1 = "value_1";
    private final static String PROBLEM_VALUE_2 = "value_2";
    private final static String GROUP_PROBLEM_CODE = "group_code";

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

        // caseErrors is now one CaseProblem per case, and each must keep its own prosecutorCaseReference
        final List<CaseProblem> publishedCaseErrors = publicGroupProsecutionRejected.getCaseErrors();
        assertThat(publishedCaseErrors, hasSize(2));
        assertThat(publishedCaseErrors.get(0).getProsecutorCaseReference(), is(URN_1));
        assertThat(publishedCaseErrors.get(0).getProblems(), is(getProblems(PROBLEM_CODE)));
        assertThat(publishedCaseErrors.get(1).getProsecutorCaseReference(), is(URN_2));
        assertThat(publishedCaseErrors.get(1).getProblems(), is(getProblems(OTHER_PROBLEM_CODE)));

        // groupCaseErrors is a straight passthrough too, and its wrapper carries no prosecutorCaseReference
        assertThat(publicGroupProsecutionRejected.getGroupCaseErrors(), is(envelope.payload().getGroupCaseErrors()));
        final List<CaseProblem> publishedGroupCaseErrors = publicGroupProsecutionRejected.getGroupCaseErrors();
        assertThat(publishedGroupCaseErrors, hasSize(1));
        assertThat(publishedGroupCaseErrors.get(0).getProsecutorCaseReference(), is(nullValue()));
        assertThat(publishedGroupCaseErrors.get(0).getProblems(), is(getProblems(GROUP_PROBLEM_CODE)));
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
                        .withCaseErrors(getCaseProblems())
                        .withGroupCaseErrors(getGroupCaseProblems())
                        .build()
        );

    }

    private List<CaseProblem> getCaseProblems() {
        final List<CaseProblem> caseProblems = new ArrayList<>();
        caseProblems.add(caseProblem()
                .withProsecutorCaseReference(URN_1)
                .withProblems(getProblems(PROBLEM_CODE))
                .build());
        caseProblems.add(caseProblem()
                .withProsecutorCaseReference(URN_2)
                .withProblems(getProblems(OTHER_PROBLEM_CODE))
                .build());
        return caseProblems;
    }

    private List<CaseProblem> getGroupCaseProblems() {
        final List<CaseProblem> groupCaseProblems = new ArrayList<>();
        groupCaseProblems.add(caseProblem()
                .withProblems(getProblems(GROUP_PROBLEM_CODE))
                .build());
        return groupCaseProblems;
    }

    private List<Problem> getProblems(final String problemCode) {
        List<Problem> problemList = new ArrayList<>();
        problemList.add(Problem.problem()
                .withCode(problemCode)
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