package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem.caseProblem;
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
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CcProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicSubmissionRejected;
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
    private final static String CIVIL_PROSECUTOR_CASE_REFERENCE = "TVL54321";

    @Mock
    private Sender sender;

    @InjectMocks
    private ProsecutionRejectedProcessor prosecutionRejectedProcessor;

    @Captor
    private ArgumentCaptor<Envelope<PublicProsecutionRejected>> publicEventCaptor;

    @Captor
    private ArgumentCaptor<Envelope> publicCivilEventCaptor;

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
        // non-civil channels never populate civilCaseErrors on the private event
        return getCcProsecutionRejectedEnvelope(channel, new ArrayList<>());
    }

    private Envelope<CcProsecutionRejected> getCcProsecutionRejectedEnvelope(final Channel channel, final List<CaseProblem> civilCaseErrors) {
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
                new CcProsecutionRejected(getProblems(), civilCaseErrors, defendantErrors, randomUUID(), prosecution));
    }

    private List<ProblemValue> getProblemValues() {
        return asList(
                problemValue().withKey(PROBLEM_VALUE_KEY_1).withValue(PROBLEM_VALUE_1).build(),
                problemValue().withKey(PROBLEM_VALUE_KEY_2).withValue(PROBLEM_VALUE_2).build());
    }

    @Test
    public void shouldEmitPublicEventWhenCCProsecutionRejectedWhenChannelCivil() {
        final List<CaseProblem> civilCaseErrors = singletonList(caseProblem()
                .withProblems(getProblems())
                .withProsecutorCaseReference(CIVIL_PROSECUTOR_CASE_REFERENCE)
                .build());
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CIVIL, civilCaseErrors);

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender, times(2)).send(this.publicCivilEventCaptor.capture());
        final Envelope<PublicCivilProsecutionRejected> publicCivilEventCaptorValue = publicCivilEventCaptor.getAllValues().get(0);

        assertThat(publicCivilEventCaptorValue.metadata().name(), is("public.prosecutioncasefile.civil-prosecution-rejected"));

        final PublicCivilProsecutionRejected expectedSentPayload =
                PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                        .withCaseId(envelope.payload().getProsecution().getCaseDetails().getCaseId())
                        .withCaseErrors(envelope.payload().getCivilCaseErrors())
                        .withDefendantErrors(envelope.payload().getDefendantErrors())
                        .withExternalId(envelope.payload().getExternalId())
                        .withChannel(envelope.payload().getProsecution().getChannel())
                        .build();

        assertThat(publicCivilEventCaptorValue.payload(), equalTo(expectedSentPayload));

        // the civil branch must read civilCaseErrors (List<CaseProblem>), not the legacy caseErrors field
        final List<CaseProblem> publishedCaseErrors = publicCivilEventCaptorValue.payload().getCaseErrors();
        assertThat(publishedCaseErrors, hasSize(1));
        assertThat(publishedCaseErrors.get(0).getProsecutorCaseReference(), is(CIVIL_PROSECUTOR_CASE_REFERENCE));
        assertThat(publishedCaseErrors.get(0).getProblems(), is(getProblems()));
    }

    @Test
    public void shouldWrapLegacyCaseErrorsWhenCCProsecutionRejectedWhenChannelCivilHasNoCivilCaseErrors() {
        // channel == CIVIL does not guarantee isCivil == true (a real, independently-set combination),
        // and replay of historical cc-prosecution-rejected events never populates civilCaseErrors at
        // all — either way the legacy caseErrors must not be silently dropped from the public event.
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CIVIL, null);

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender, times(2)).send(this.publicCivilEventCaptor.capture());
        final PublicCivilProsecutionRejected publishedPayload = (PublicCivilProsecutionRejected) publicCivilEventCaptor.getAllValues().get(0).payload();

        final List<CaseProblem> caseErrors = publishedPayload.getCaseErrors();
        assertThat(caseErrors, hasSize(1));
        assertThat(caseErrors.get(0).getProsecutorCaseReference(), is(envelope.payload().getProsecution().getCaseDetails().getProsecutorCaseReference()));
        assertThat(caseErrors.get(0).getProblems(), is(envelope.payload().getCaseErrors()));
        assertThat(publishedPayload.getDefendantErrors(), hasSize(1));
    }

    @Test
    public void shouldPreferCivilCaseErrorsOverLegacyCaseErrorsWhenBothPresent() {
        // civilCaseErrors, when present, is the authoritative source for a civil rejection raised
        // after this change — the legacy caseErrors fallback must not override it.
        final List<CaseProblem> civilCaseErrors = singletonList(caseProblem()
                .withProsecutorCaseReference("URN-CIVIL")
                .withProblems(getProblems())
                .build());
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CIVIL, civilCaseErrors);

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender, times(2)).send(this.publicCivilEventCaptor.capture());
        final PublicCivilProsecutionRejected publishedPayload = (PublicCivilProsecutionRejected) publicCivilEventCaptor.getAllValues().get(0).payload();

        assertThat(publishedPayload.getCaseErrors(), is(civilCaseErrors));
    }

    @Test
    public void shouldEmitEmptyCaseErrorsWhenNeitherCivilNorLegacyCaseErrorsArePresent() {
        // caseErrors is a REQUIRED property of public.prosecutioncasefile.civil-prosecution-rejected,
        // so a civil rejection driven purely by defendant-level problems must still publish an empty
        // array rather than a null that would fail outgoing schema validation.
        final Prosecution prosecution = prosecution()
                .withChannel(CIVIL)
                .withCaseDetails(caseDetails()
                        .withCaseId(caseId)
                        .withProsecutor(prosecutor().build())
                        .withProsecutorCaseReference("TVL12345")
                        .build())
                .withDefendants(ImmutableList.of(defendant().build()))
                .build();
        final List<DefendantProblem> defendantErrors = singletonList(defendantProblem()
                .withProsecutorDefendantReference("Defendant1")
                .withProblems(getProblems())
                .build());
        final Envelope<CcProsecutionRejected> envelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.cc-prosecution-rejected"),
                new CcProsecutionRejected(null, null, defendantErrors, randomUUID(), prosecution));

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender, times(2)).send(this.publicCivilEventCaptor.capture());
        final PublicCivilProsecutionRejected publishedPayload = (PublicCivilProsecutionRejected) publicCivilEventCaptor.getAllValues().get(0).payload();

        assertThat(publishedPayload.getCaseErrors(), is(notNullValue()));
        assertThat(publishedPayload.getCaseErrors(), is(empty()));
        assertThat(publishedPayload.getDefendantErrors(), hasSize(1));
    }

    @Test
    public void shouldEmitSubmissionRejectedPublicEventWhenCCProsecutionRejectedWhenChannelCivil() {
        final Envelope<CcProsecutionRejected> envelope = getCcProsecutionRejectedEnvelope(CIVIL, new ArrayList<>());

        prosecutionRejectedProcessor.handleCCProsecutionRejected(envelope);

        verify(this.sender, times(2)).send(this.publicCivilEventCaptor.capture());
        final PublicSubmissionRejected publishedPayload = (PublicSubmissionRejected) publicCivilEventCaptor.getAllValues().get(1).payload();

        assertThat(publicCivilEventCaptor.getAllValues().get(1).metadata().name(), is("public.prosecutioncasefile.submission-rejected"));
        assertThat(publishedPayload.getCaseId(), is(envelope.payload().getProsecution().getCaseDetails().getCaseId()));
        assertThat(publishedPayload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(publishedPayload.getChannel(), is(CIVIL));
    }

}