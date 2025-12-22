package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncasefile.command.handler.RecordDecisionToReferCaseForCourtHearingSaved;
import uk.gov.moj.cpp.resulting.event.PublicDecisionToReferCaseForCourtHearingSaved;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ResultingPublicEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    ResultingPublicEventProcessor sut;

    @Captor
    private ArgumentCaptor<Envelope<RecordDecisionToReferCaseForCourtHearingSaved>> captor;

    final UUID caseId = UUID.randomUUID();
    final UUID decisionId = UUID.randomUUID();
    final ZonedDateTime decisionSavedAt = ZonedDateTime.now();
    final Integer estimatedHearingDuration = 2;
    final UUID hearingTypeId = UUID.randomUUID();
    final String listingNotes = "Do not have necessary information";
    final UUID referralReasonId = UUID.randomUUID();
    final UUID sessionId = UUID.randomUUID();

    @Test
    public void shouldHaveAHandlerToProcessCaseReferredToCourtEvent() {
        assertThat(ResultingPublicEventProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(method("handleDecisionToReferCaseForCourtHearingSaved")
                        .thatHandles("public.resulting.decision-to-refer-case-for-court-hearing-saved"))
        );
    }

    @Test
    public void shouldProcessCaseReferredToCourtEvent() {
        // given
        final Metadata metadata = metadataBuilder()
                .withName("public.resulting.case-referred-to-court")
                .withId(randomUUID())
                .build();

        final Envelope<PublicDecisionToReferCaseForCourtHearingSaved> envelope = prepareDecisionToReferCaseForCourtHearingSavedEnvelope(metadata);

        // when
        sut.handleDecisionToReferCaseForCourtHearingSaved(envelope);

        // then
        verify(sender).send(captor.capture());
        Envelope<RecordDecisionToReferCaseForCourtHearingSaved> acceptCourtReferralEnvelope = captor.getValue();

        RecordDecisionToReferCaseForCourtHearingSaved recordDecisionToReferCaseForCourtHearingSaved = acceptCourtReferralEnvelope.payload();
        // think a way with out transforming
        PublicDecisionToReferCaseForCourtHearingSaved caseReferredToCourt = PublicDecisionToReferCaseForCourtHearingSaved
                .publicDecisionToReferCaseForCourtHearingSaved()
                .withCaseId(recordDecisionToReferCaseForCourtHearingSaved.getCaseId())
                .withDecisionSavedAt(recordDecisionToReferCaseForCourtHearingSaved.getDecisionSavedAt())
                .withEstimatedHearingDuration(recordDecisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())
                .withHearingTypeId(recordDecisionToReferCaseForCourtHearingSaved.getHearingTypeId())
                .withListingNotes(recordDecisionToReferCaseForCourtHearingSaved.getListingNotes())
                .withReferralReasonId(recordDecisionToReferCaseForCourtHearingSaved.getReferralReasonId())
                .withSessionId(recordDecisionToReferCaseForCourtHearingSaved.getSessionId())
                .build();

        reflectionEquals(caseReferredToCourt, envelope.payload());

    }

    private Envelope<PublicDecisionToReferCaseForCourtHearingSaved> prepareDecisionToReferCaseForCourtHearingSavedEnvelope(final Metadata metadata) {
        final PublicDecisionToReferCaseForCourtHearingSaved caseReferredToCourt = PublicDecisionToReferCaseForCourtHearingSaved
                .publicDecisionToReferCaseForCourtHearingSaved()
                .withCaseId(caseId)
                .withDecisionId(decisionId)
                .withDecisionSavedAt(decisionSavedAt)
                .withEstimatedHearingDuration(estimatedHearingDuration)
                .withHearingTypeId(hearingTypeId)
                .withListingNotes(listingNotes)
                .withReferralReasonId(referralReasonId)
                .withSessionId(sessionId)
                .build();
        return envelopeFrom(metadata, caseReferredToCourt);
    }

}