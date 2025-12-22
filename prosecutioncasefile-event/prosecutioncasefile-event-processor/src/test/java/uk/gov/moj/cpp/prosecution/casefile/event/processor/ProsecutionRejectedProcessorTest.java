package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem.defendantProblem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived.manualCaseReceived;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionRejected.publicProsecutionRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected.sjpProsecutionRejected;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CcProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected;

import java.util.ArrayList;
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
public class ProsecutionRejectedProcessorTest {

    private final static String PROBLEM_CODE = "code";
    private final static String PROBLEM_VALUE_KEY_1 = "value_key_1";
    private final static String PROBLEM_VALUE_KEY_2 = "value_key_2";
    private final static String PROBLEM_VALUE_1 = "value_1";
    private final static String PROBLEM_VALUE_2 = "value_2";

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionRejectedProcessor prosecutionRejectedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<PublicProsecutionRejected>> publicEventCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PublicCivilProsecutionRejected>> publicCivilEventCaptor;

    @Captor
    private ArgumentCaptor<Envelope<ManualCaseReceived>> publicMCCEventCaptor;

    private final UUID caseId = randomUUID();

    @Test
    public void shouldEmitPublicEventWhenSjpProsecutionRejected() {
        final Envelope<SjpProsecutionRejected> envelope = buildSjpProsecutionRejectedEnvelope(CPPI);

        prosecutionRejectedProcessor.handleSjpProsecutionRejected(envelope);

        verify(this.sender).send(this.publicEventCaptor.capture());
        final Envelope<PublicProsecutionRejected> publicEventCaptorValue = publicEventCaptor.getValue();

        assertThat(publicEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.prosecution-rejected"));

        final PublicProsecutionRejected expectedSentPayload =
                publicProsecutionRejected()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withErrors(envelope.payload().getErrors())
                        .withExternalId(envelope.payload().getExternalId())
                        .withChannel(envelope.payload().getProsecution().getChannel())
                        .build();

        assertThat(publicEventCaptorValue.payload(), equalTo(expectedSentPayload));
    }

    @Test
    public void shouldEmitPublicEventWhenCCProsecutionRejectedWhenChannelCPPI() {
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CPPI);

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender).send(this.publicEventCaptor.capture());
        final Envelope<PublicProsecutionRejected> publicEventCaptorValue = publicEventCaptor.getValue();

        assertThat(publicEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.prosecution-rejected"));

        final PublicProsecutionRejected expectedSentPayload =
                publicProsecutionRejected()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withCaseErrors(envelope.payload().getCaseErrors())
                        .withDefendantErrors(envelope.payload().getDefendantErrors())
                        .withExternalId(envelope.payload().getExternalId())
                        .withChannel(envelope.payload().getProsecution().getChannel())
                        .build();

        assertThat(publicEventCaptorValue.payload(), equalTo(expectedSentPayload));
    }

    @Test
    public void shouldEmitPublicEventWhenSjpProsecutionRejectedForMCC() {
        final Envelope<SjpProsecutionRejected> envelope = buildSjpProsecutionRejectedEnvelope(MCC);
        final ManualCaseReceived expectedManualCaseReceived =
                manualCaseReceived()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withProsecutorCaseReference(envelope.payload().getProsecution().getCaseDetails().getProsecutorCaseReference())
                        .withErrors(envelope.payload().getErrors())
                        .build();

        prosecutionRejectedProcessor.handleSjpProsecutionRejected(envelope);

        verify(this.sender).send(this.publicMCCEventCaptor.capture());
        final Envelope<ManualCaseReceived> publicEventCaptorValue = publicMCCEventCaptor.getValue();
        assertThat(publicEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.manual-case-received"));
        assertThat(publicEventCaptorValue.payload(), is(expectedManualCaseReceived));
    }

    @Test
    public void shouldEmitPublicEventWhenCCProsecutionRejectedWhenChanneMCC() {
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(MCC);
        final List<Problem> expectedProblems = newArrayList();
        expectedProblems.addAll(envelope.payload().getCaseErrors());
        envelope.payload().getDefendantErrors().forEach(defendantProblem -> expectedProblems.addAll(defendantProblem.getProblems()));

        final ManualCaseReceived expectedManualCaseReceived =
                manualCaseReceived()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withProsecutorCaseReference(envelope.payload().getProsecution().getCaseDetails().getProsecutorCaseReference())
                        .withErrors(expectedProblems)
                        .build();

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender).send(this.publicMCCEventCaptor.capture());
        final Envelope<ManualCaseReceived> publicEventCaptorValue = publicMCCEventCaptor.getValue();

        assertThat(publicEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.manual-case-received"));
        assertThat(publicEventCaptorValue.payload(), is(expectedManualCaseReceived));
    }

    private Envelope<SjpProsecutionRejected> buildSjpProsecutionRejectedEnvelope(final Channel channel) {
        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.sjp-prosecution-rejected"),
                sjpProsecutionRejected()
                        .withProsecution(prosecution()
                                .withChannel(channel)
                                .withCaseDetails(caseDetails()
                                        .withCaseId(caseId)
                                        .withProsecutor(prosecutor().build())
                                        .withProsecutorCaseReference("TVL12345")
                                        .build())
                                .withDefendants(singletonList(defendant().build()))
                                .build())
                        .withErrors(getProblems())
                        .withExternalId(randomUUID())
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

    private Envelope<CcProsecutionRejected> getCcProsecutionRejectedEnvelope(final Channel channel) {
        final Prosecution prosecution = prosecution()
                .withChannel(channel)
                .withCaseDetails(caseDetails()
                        .withCaseId(caseId)
                        .withProsecutor(prosecutor().build())
                        .withProsecutorCaseReference("TVL12345")
                        .build())
                .withDefendants(ImmutableList.of(defendant().build()))
                .build();

        final List<DefendantProblem> defendantErrors = new ArrayList<>();
        defendantErrors.add(defendantProblem()
                .withProsecutorDefendantReference("Defendant1")
                .withProblems(getProblems())
                .build());

        return envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.cc-prosecution-rejected"),
                new CcProsecutionRejected(getProblems(), defendantErrors, randomUUID(), prosecution));
    }

    private List<ProblemValue> getProblemValues() {
        return asList(
                problemValue().withKey(PROBLEM_VALUE_KEY_1).withValue(PROBLEM_VALUE_1).build(),
                problemValue().withKey(PROBLEM_VALUE_KEY_2).withValue(PROBLEM_VALUE_2).build());
    }

    @Test
    public void shouldEmitPublicEventWhenCCProsecutionRejectedWhenChannelCivil() {
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CIVIL);

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender).send(this.publicCivilEventCaptor.capture());
        final Envelope<PublicCivilProsecutionRejected> publicCivilEventCaptorValue = publicCivilEventCaptor.getValue();

        assertThat(publicCivilEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.civil-prosecution-rejected"));

        final PublicCivilProsecutionRejected expectedSentPayload =
                PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withCaseErrors(envelope.payload().getCaseErrors())
                        .withDefendantErrors(envelope.payload().getDefendantErrors())
                        .withExternalId(envelope.payload().getExternalId())
                        .withChannel(envelope.payload().getProsecution().getChannel())
                        .build();

        assertThat(publicCivilEventCaptorValue.payload(), equalTo(expectedSentPayload));
    }

}