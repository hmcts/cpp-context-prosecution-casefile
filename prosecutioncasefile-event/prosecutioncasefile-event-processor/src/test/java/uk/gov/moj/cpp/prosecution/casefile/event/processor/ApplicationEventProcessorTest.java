package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.ApplicationEventProcessor.PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.CaseReceivedHelper.getPayloadAsJsonObject;

import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.BoxHearingRequest;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ApplicationAcceptedToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.ApplicationParameters;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.utils.EnvelopeHelper;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationValidationFailed;

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
public class ApplicationEventProcessorTest {

    private static final String POCA_SUCCESS_EMAIL = "4a40c000-2ac4-4464-bf7c-f944b31a67f4";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<InitiateCourtApplicationProceedings>> initiateCourtApplicationProceedingsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<SubmitApplicationValidationFailed>> submitApplicationValidationFailedArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope> captorAdmin;

    @Mock
    private ApplicationAcceptedToCourtApplicationProceedingsConverter converter;

    @Mock
    private Envelope<SubmitApplicationAccepted> exceptedEnvelope;

    @Mock
    private Envelope<SubmitApplicationValidationFailed> validationFailedEnvelope;

    @Mock
    private InitiateCourtApplicationProceedings outgoingPayload;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private EnvelopeHelper envelopeHelper;

    @InjectMocks
    private ApplicationEventProcessor applicationEventProcessor;

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private UUID applicationId;
    private UUID boxHearingRequestId;

    @BeforeEach
    public void setup() {
        initMocks(this);
        applicationId = randomUUID();
        boxHearingRequestId = randomUUID();
    }

    @Test
    public void shouldIssueCommandToProgressionWhenSubmitCourtApplicationAccepted() {
        given(exceptedEnvelope.metadata()).willReturn(getMetaData("prosecutioncasefile.events.submit-application-accepted"));
        final SubmitApplicationAccepted payload = getPayload();
        given(exceptedEnvelope.payload()).willReturn(payload);
        given(converter.convert(payload)).willReturn(outgoingPayload);

        applicationEventProcessor.handleSubmitApplicationAccepted(exceptedEnvelope);

        verify(sender).send(initiateCourtApplicationProceedingsArgumentCaptor.capture());
        final Envelope<InitiateCourtApplicationProceedings> firstResultEnvelope = initiateCourtApplicationProceedingsArgumentCaptor.getAllValues().get(0);
        assertThat(firstResultEnvelope.metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION));
        assertThat(firstResultEnvelope.payload(), is(outgoingPayload));
    }

    @Test
    public void shouldRaisePublicEventWhenSubmitApplicationValidationFailed() {
        given(validationFailedEnvelope.metadata()).willReturn(getMetaData("prosecutioncasefile.events.submit-application-validation-failed"));
        final SubmitApplicationValidationFailed submitApplicationValidationFailed = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject("prosecutioncasefile.events.submit-application-validation-failed.json"), SubmitApplicationValidationFailed.class);
        given(validationFailedEnvelope.payload()).willReturn(submitApplicationValidationFailed);

        applicationEventProcessor.handleSubmitApplicationValidationFailed(validationFailedEnvelope);

        verify(sender).send(submitApplicationValidationFailedArgumentCaptor.capture());
        final Envelope<SubmitApplicationValidationFailed> publicEventEnvelope = submitApplicationValidationFailedArgumentCaptor.getAllValues().get(0);
        assertThat(publicEventEnvelope.metadata().name(), is("public.prosecutioncasefile.submit-application-validation-failed"));
        assertThat(publicEventEnvelope.payload(), is(submitApplicationValidationFailed));
    }

    @Test
    public void shouldRaiseSendEmailNotificationPublicEventWhenSubmitApplicationValidationFailed() {
        given(validationFailedEnvelope.metadata()).willReturn(getMetaData("notificationnotify.send-email-notification"));

        final SubmitApplicationValidationFailed submitApplicationValidationFailed = jsonObjectToObjectConverter.convert(getPayloadAsJsonObject("prosecutioncasefile.events.submit-application-validation-failed-with-emailDetails.json"), SubmitApplicationValidationFailed.class);

        given(validationFailedEnvelope.payload()).willReturn(submitApplicationValidationFailed);

        when(applicationParameters.getEmailTemplateId()).thenReturn(POCA_SUCCESS_EMAIL);

        final Envelope envelope = envelopeFrom(getMetaData("notificationnotify.send-email-notification"), null);

        when(envelopeHelper.withMetadataInPayloadForEnvelope(any())).thenReturn(envelope);

        applicationEventProcessor.handleSubmitApplicationValidationFailed(validationFailedEnvelope);

        verify(sender).sendAsAdmin(captorAdmin.capture());
        verify(sender, never()).send(any());


        final Envelope<JsonObject> emailNotificationPublicEventEnvelope = captorAdmin.getAllValues().get(0);
        assertThat(emailNotificationPublicEventEnvelope.metadata().name(), is("notificationnotify.send-email-notification"));
    }

    private Metadata getMetaData(final String name) {
        return metadataBuilder()
                .createdAt(now())
                .withCausation(randomUUID())
                .withName(name)
                .withId(randomUUID())
                .build();
    }

    private SubmitApplicationAccepted getPayload() {
        return SubmitApplicationAccepted.submitApplicationAccepted()
                .withCourtApplication(CourtApplication.courtApplication().withId(applicationId).build())
                .withBoxHearingRequest(BoxHearingRequest.boxHearingRequest().withId(boxHearingRequestId).build())
                .build();
    }

}