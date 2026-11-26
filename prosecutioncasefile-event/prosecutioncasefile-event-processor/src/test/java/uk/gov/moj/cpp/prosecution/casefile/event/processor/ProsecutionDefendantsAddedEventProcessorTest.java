package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionCaseFileDefendantToDefenceDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ProsecutionToCCAddDefendantConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefenceDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.AcceptCase;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CheckPendingEventsForNewDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionSubmissionSucceededWithWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionDefendantsAddedEventProcessorTest {

    private static final String PROGRESSION_ADD_DEFENDANT_TO_COURTS = "progression.add-defendants-to-court-proceedings";
    private static final String PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH = "prosecutioncasefile.handler.case-updated-initiate-idpc-match";
    private static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED = "public.prosecutioncasefile.prosecution-defendants-added";
    private static final String PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED = "prosecutioncasefile.events.prosecution-defendants-added";
    private static final String PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED = "public.prosecutioncasefile.prosecution-submission-succeeded";
    public static final String PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS = "prosecutioncasefile.command.check-pending-events-for-new-defendants";

    private static final UUID CASE_ID = randomUUID();

    @InjectMocks
    private ProsecutionDefendantsAddedEventProcessor prosecutionDefendantsAddedEventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProsecutionToCCAddDefendantConverter prosecutionToCCAddDefendantConverter;

    @Mock
    private ProsecutionCaseFileDefendantToDefenceDefendantConverter prosecutionCaseFileDefendantToDefenceDefendantConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionDefendantsAdded prosecutionDefendantsAdded;

    @Mock
    private AddDefendantsToCourtProceedings addDefendantsToCourtProceedings;

    @Mock
    private JsonObject addDefendantsToCourProceedingsJsonObject;

    @Mock
    private List<DefenceDefendant> defenceDefendants;

    @Captor
    private ArgumentCaptor<Envelope> jsonEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Mock
    private EnvelopeHelper envelopeHelper;

    private Envelope<ProsecutionDefendantsAdded> envelope;

    @BeforeEach
    public void setup() {

        envelope = Envelope.envelopeFrom(
                metadataBuilder().withName(PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED).withId(randomUUID()),
                prosecutionDefendantsAdded);

        when(prosecutionToCCAddDefendantConverter.convert(prosecutionDefendantsAdded)).thenReturn(addDefendantsToCourtProceedings);
        when(objectToJsonObjectConverter.convert(addDefendantsToCourtProceedings)).thenReturn(addDefendantsToCourProceedingsJsonObject);
        when(prosecutionDefendantsAdded.getCaseId()).thenReturn(CASE_ID);
        when(prosecutionDefendantsAdded.getExternalId()).thenReturn(randomUUID());
        when(prosecutionDefendantsAdded.getChannel()).thenReturn(SPI);
        when(prosecutionCaseFileDefendantToDefenceDefendantConverter.convert(prosecutionDefendantsAdded.getDefendants())).thenReturn(defenceDefendants);
    }

    @Test
    public void shouldSendAddDefendantsToCourtProceedingsCommandToProgression() {
        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(jsonEnvelopeArgumentCaptor.capture());
        final Envelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getAllValues().get(0);
        assertThat(jsonEnvelope.metadata().name(), is(PROGRESSION_ADD_DEFENDANT_TO_COURTS));
        assertThat(jsonEnvelope.payload(), is(addDefendantsToCourProceedingsJsonObject));

    }

    @Test
    public void shouldSendCaseUpdatedInitiateIdpcMatchCommandWhenChannelIsSpi() {
        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(envelopeArgumentCaptor.capture());

        AcceptCase requestPayload= (AcceptCase) getRequestPayload(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH);
        assertThat(requestPayload.getCaseId(), is(CASE_ID));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));
    }

    @Test
    public void shouldSendProsecutionSubmissionSucceededPublicEventWhenWarningsAreNull() {
        when(prosecutionDefendantsAdded.getDefendantWarnings()).thenReturn(null);
        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(envelopeArgumentCaptor.capture());

        ProsecutionSubmissionSucceeded requestPayload
                = (ProsecutionSubmissionSucceeded) getRequestPayload(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
        assertThat(requestPayload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(requestPayload.getCaseId(), is(CASE_ID));
        assertThat(requestPayload.getChannel(), is(SPI));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));

    }

    @Test
    public void shouldSendProsecutionSubmissionSucceededPublicEventWhenWarningsAreEmpty() {
        when(prosecutionDefendantsAdded.getDefendantWarnings()).thenReturn(emptyList());

        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(envelopeArgumentCaptor.capture());

        ProsecutionSubmissionSucceeded requestPayload
                = (ProsecutionSubmissionSucceeded) getRequestPayload(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
        assertThat(requestPayload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(requestPayload.getCaseId(), is(CASE_ID));
        assertThat(requestPayload.getChannel(), is(SPI));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));

    }

    @Test
    public void shouldSendProsecutionSubmissionSucceededWithWarningsPublicEvent() {
        when(prosecutionDefendantsAdded.getDefendantWarnings()).thenReturn(singletonList(any(DefendantProblem.class)));

        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(envelopeArgumentCaptor.capture());
        ProsecutionSubmissionSucceededWithWarnings requestPayload
                = (ProsecutionSubmissionSucceededWithWarnings) getRequestPayload(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
        assertThat(requestPayload.getExternalId(), is(envelope.payload().getExternalId()));
        assertThat(requestPayload.getCaseId(), is(CASE_ID));
        assertThat(requestPayload.getChannel(), is(SPI));
        assertFalse(envelope.payload().getDefendantWarnings().isEmpty());
        assertThat(requestPayload.getDefendantWarnings(), is(envelope.payload().getDefendantWarnings()));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));

    }

    @Test
    public void shouldNotSendCaseUpdatedInitiateIdpcMatchCommandWhenChannelIsNotSpi() {
        when(prosecutionDefendantsAdded.getChannel()).thenReturn(CPPI);

        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(4)).send(envelopeArgumentCaptor.capture());
        Optional<Envelope> envelope = getEnvelope(PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH);
        assertThat(envelope.isPresent(), is(false));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));

    }

    @Test
    public void shouldSendProsecutionDefendantsAddedPublicEvent() {
        prosecutionDefendantsAddedEventProcessor.handleProsecutionDefendantsAdded(envelope);

        verify(sender, times(5)).send(envelopeArgumentCaptor.capture());

        uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.ProsecutionDefendantsAdded requestPayload
                = (uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.ProsecutionDefendantsAdded) getRequestPayload(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED);
        assertThat(requestPayload.getDefendants(), is(defenceDefendants));
        assertThat(requestPayload.getChannel(), is(SPI));

        CheckPendingEventsForNewDefendants checkPendingEventsForNewDefendants= (CheckPendingEventsForNewDefendants) getRequestPayload(PROSECUTIONCASEFILE_COMMAND_CHECK_PENDING_EVENTS_FOR_NEW_DEFENDANTS);
        assertThat(checkPendingEventsForNewDefendants.getCaseId(), is(CASE_ID));

    }

    private Object getRequestPayload(String eventName) {
        Envelope matchingEnvelope = getEnvelope(eventName).get();
        return matchingEnvelope.payload();
    }

    private <T> Optional<Envelope> getEnvelope(String eventName) {
        return envelopeArgumentCaptor.getAllValues().stream()
                .filter(Objects::nonNull)
                .filter(request -> eventName.equals(request.metadata().name()))
                .findFirst();
    }


}