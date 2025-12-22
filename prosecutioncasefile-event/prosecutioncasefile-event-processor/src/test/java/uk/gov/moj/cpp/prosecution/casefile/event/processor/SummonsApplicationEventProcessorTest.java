package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval.defendantsParkedForSummonsApplicationApproval;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;

import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.DefendantsParkedToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;

import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsApplicationEventProcessorTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Mock
    private DefendantsParkedToCourtApplicationProceedingsConverter defendantsParkedToCourtApplicationProceedingsConverter;

    @Mock
    private Envelope<DefendantsParkedForSummonsApplicationApproval> envelope;

    @Mock
    private InitiateCourtApplicationProceedings outgoingPayload;

    private Metadata metadata;

    @InjectMocks
    private SummonsApplicationEventProcessor target;
    private UUID caseId;
    private UUID applicationId;
    private String prosecutorCaseReference;

    public static Stream<Arguments> nonMccChannels() {
        return Stream.of(
                Arguments.of(CPPI),
                Arguments.of(SPI)
        );
    }

    @BeforeEach
    public void setup() {
        metadata = getMetaData();
        caseId = randomUUID();
        applicationId = randomUUID();
        prosecutorCaseReference = RandomStringUtils.random(10);

    }

    @Test
    public void shouldIssueCommandToProgressionWhenDefendantsParkedForSummonsApplicationApprovalAndEmitPublicEvemtForMCC() {
        given(envelope.metadata()).willReturn(metadata);
        final DefendantsParkedForSummonsApplicationApproval payload = getPayload(MCC);
        given(envelope.payload()).willReturn(payload);
        given(defendantsParkedToCourtApplicationProceedingsConverter.convert(payload, metadata)).willReturn(outgoingPayload);

        target.handleDefendantsParkedForSummonsApplicationApproval(envelope);

        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());
        final Envelope<InitiateCourtApplicationProceedings> firstResultEnvelope = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(firstResultEnvelope.metadata().name(), is("progression.initiate-court-proceedings-for-application"));
        assertThat(firstResultEnvelope.payload(), is(outgoingPayload));

        final Envelope<ManualCaseReceived> secondResultEnvelope = envelopeArgumentCaptor.getAllValues().get(1);
        assertThat(secondResultEnvelope.metadata().name(), is("public.prosecutioncasefile.manual-case-received"));
        assertThat(secondResultEnvelope.payload().getApplicationId(), is(applicationId));
        assertThat(secondResultEnvelope.payload().getCaseId(), is(caseId));
        assertThat(secondResultEnvelope.payload().getProsecutorCaseReference(), is(prosecutorCaseReference));
    }

    @MethodSource("nonMccChannels")
    @ParameterizedTest
    public void shouldIssueCommandToProgressionWhenDefendantsParkedForSummonsApplicationApprovalAndNoPublicEventEmittedForNonMCCChannels(final Channel channel) {
        given(envelope.metadata()).willReturn(metadata);
        final DefendantsParkedForSummonsApplicationApproval payload = getPayload(channel);
        given(envelope.payload()).willReturn(payload);
        given(defendantsParkedToCourtApplicationProceedingsConverter.convert(payload, metadata)).willReturn(outgoingPayload);

        target.handleDefendantsParkedForSummonsApplicationApproval(envelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope<InitiateCourtApplicationProceedings> firstResultEnvelope = envelopeArgumentCaptor.getAllValues().get(0);
        assertThat(firstResultEnvelope.metadata().name(), is("progression.initiate-court-proceedings-for-application"));
        assertThat(firstResultEnvelope.payload(), is(outgoingPayload));
    }

    private Metadata getMetaData() {
        return metadataBuilder()
                .createdAt(now())
                .withCausation(randomUUID())
                .withName("prosecutioncasefile.events.defendants-parked-for-summons-application-approval")
                .withId(randomUUID())
                .build();
    }

    private DefendantsParkedForSummonsApplicationApproval getPayload(final Channel channel) {
        final CaseDetails caseDetails = caseDetails().withCaseId(caseId).withProsecutorCaseReference(prosecutorCaseReference).build();
        final Prosecution prosecution = prosecution()
                .withChannel(channel)
                .withCaseDetails(caseDetails)
                .build();
        return defendantsParkedForSummonsApplicationApproval()
                .withApplicationId(applicationId)
                .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(prosecution))
                .build();
    }

}