package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode.J;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaPcqVisitedSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaSubmitted;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
public class OnlinePleaEventProcessor {

    private static final String SJP_TYPE = "SJP";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OnlinePleaEventProcessor.class);


    @Handles("prosecutioncasefile.events.online-plea-submitted")
    public void handleOnlinePleaSubmitted(final Envelope<OnlinePleaSubmitted> envelope) {
        final OnlinePleaSubmitted onlinePleaSubmitted = envelope.payload();

        final MetadataBuilder builder = metadataFrom(envelope.metadata());

        if (isSjpCase(onlinePleaSubmitted)) {
            final Metadata metadata = builder.withName("public.prosecutioncasefile.sjp-plead-online").build();
            final PleadOnline payload = getPleadOnlineForSjp(onlinePleaSubmitted);
            LOGGER.info("Sending online plea to SJP for CaseId: {} and DefendantId: {}", payload.getCaseId(), payload.getDefendantId());
            sender.send(envelopeFrom(metadata, payload));
        } else { //CC case
            final Metadata metadata = builder.withName("progression.plead-online").build();
            sender.send(envelopeFrom(metadata, getPleadOnlineForProgression(onlinePleaSubmitted)));
        }
    }

    @Handles("prosecutioncasefile.events.online-plea-pcq-visited-submitted")
    public void handleOnlinePleaPcqVisitedSubmitted(final Envelope<OnlinePleaPcqVisitedSubmitted> envelope) {
        final OnlinePleaPcqVisitedSubmitted onlinePleaPcqVisitedSubmitted = envelope.payload();

        final MetadataBuilder builder = metadataFrom(envelope.metadata());
        final PleadOnlinePcqVisited pleadOnlineVisited = onlinePleaPcqVisitedSubmitted.getPleadOnlineVisited();

        if (SJP_TYPE.equals(pleadOnlineVisited.getType())) {
            // call sjp command
            final Metadata metadata = builder.withName("sjp.plead-online-pcq-visited").build();
            sender.send(envelopeFrom(metadata, pleadOnlineVisited));
        } else {
            // call progression command
            final Metadata metadata = builder.withName("progression.plead-online-pcq-visited").build();
            sender.send(envelopeFrom(metadata, pleadOnlineVisited));
        }
    }

    private PleadOnline getPleadOnlineForSjp(final OnlinePleaSubmitted onlinePleaSubmitted) {
        return PleadOnline.pleadOnline()
                .withValuesFrom(onlinePleaSubmitted.getPleadOnline())
                .withInitiationCode(null)
                .withUrn(null)
                .withOffences(getOffences(onlinePleaSubmitted.getPleadOnline().getOffences()))
                .build();
    }

    @SuppressWarnings("squid:S1168")
    private List<Offence> getOffences(List<Offence> offences) {
        if (isNull(offences)) {
            return null;
        }
        return offences.stream()
                .map(offence -> Offence.offence()
                        .withValuesFrom(offence)
                        .withTitle(null)
                        .build())
                .collect(Collectors.toList());
    }

    private PleadOnline getPleadOnlineForProgression(final OnlinePleaSubmitted onlinePleaSubmitted) {
        return PleadOnline.pleadOnline()
                .withValuesFrom(onlinePleaSubmitted.getPleadOnline())
                .withInitiationCode(null)
                .build();
    }

    private boolean isSjpCase(final OnlinePleaSubmitted onlinePleaSubmitted) {
        return J == onlinePleaSubmitted.getPleadOnline().getInitiationCode();
    }

}
