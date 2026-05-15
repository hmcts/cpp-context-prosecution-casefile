package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleaType.GUILTY;
import static uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline.pleadOnline;
import static uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnlinePcqVisited.pleadOnlinePcqVisited;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.command.api.validator.PleadOnlineValidator;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnline;
import uk.gov.moj.cps.prosecutioncasefile.command.api.PleadOnlinePcqVisited;

import java.util.UUID;

import javax.json.JsonObject;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class PleadOnlineApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private PleadOnlineApi pleadOnlineApi;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private PleadOnlineValidator pleadOnlineValidator;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope<PleadOnline>> envelopeWithPleadOnlineArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PleadOnlinePcqVisited>> envelopeWithPleadOnlinePcqVisitedArgumentCaptor;


    @Test
    public void shouldReturnPleadOnline() {

        final UUID offenceId = randomUUID();
        final UUID caseId = randomUUID();

        final PleadOnline pleadOnline = pleadOnline()
                .withCaseId(caseId)
                .withOffences(asList(offence()
                        .withId(offenceId.toString())
                        .withPlea(GUILTY)
                        .build()))

                .build();

        final Envelope<PleadOnline> command = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), pleadOnline);
        final Envelope<JsonObject> result = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), createObjectBuilder()
                .build());

        final JsonObject jsonObject = createObjectBuilder().add("personalDetails", createObjectBuilder()
                .add("address", createObjectBuilder()
                        .add("postcode", "EA1 L11")
                        .build()))
                .build();

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(result);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(jsonObject);
        pleadOnlineApi.pleadOnline(command);

        verify(sender).send(envelopeWithPleadOnlineArgumentCaptor.capture());
        final Envelope<PleadOnline> envelopeArgumentCaptorValue = envelopeWithPleadOnlineArgumentCaptor.getValue();

        assertThat(envelopeArgumentCaptorValue.metadata().name(), is("prosecutioncasefile.command.plead-online"));
    }

    @Test
    public void shouldReturnPleadOnline_WhenSJP() {

        final UUID offenceId = randomUUID();
        final UUID caseId = randomUUID();

        final PleadOnline pleadOnline = pleadOnline()
                .withCaseId(caseId)
                .withInitiationCode(InitiationCode.J)
                .withOffences(asList(offence()
                        .withId(offenceId.toString())
                        .withPlea(GUILTY)
                        .build()))
                .build();

        final Envelope<PleadOnline> command = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), pleadOnline);
        final Envelope<JsonObject> result = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), createObjectBuilder().build());

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(result);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        pleadOnlineApi.pleadOnline(command);

        verify(sender).send(envelopeWithPleadOnlineArgumentCaptor.capture());
        final Envelope<PleadOnline> envelopeArgumentCaptorValue = envelopeWithPleadOnlineArgumentCaptor.getValue();

        assertThat(envelopeArgumentCaptorValue.metadata().name(), is("prosecutioncasefile.command.plead-online"));
    }

    @Test
    public void pleadOnlinePCQVisited() {

        final PleadOnlinePcqVisited pleadOnlinePcqVisited = pleadOnlinePcqVisited()
                .build();
        final Envelope<PleadOnlinePcqVisited> command = envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), pleadOnlinePcqVisited);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        pleadOnlineApi.pleadOnlinePCQVisited(command);

        verify(sender).send(envelopeWithPleadOnlinePcqVisitedArgumentCaptor.capture());
        final Envelope<PleadOnlinePcqVisited> envelopeArgumentCaptorValue = envelopeWithPleadOnlinePcqVisitedArgumentCaptor.getValue();

        assertThat(envelopeArgumentCaptorValue.metadata().name(), is("prosecutioncasefile.command.plead-online-pcq-visited"));

    }


    private Envelope<PleadOnline> envelope(final PleadOnline pleadOnline) {
        return envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), pleadOnline);
    }
}