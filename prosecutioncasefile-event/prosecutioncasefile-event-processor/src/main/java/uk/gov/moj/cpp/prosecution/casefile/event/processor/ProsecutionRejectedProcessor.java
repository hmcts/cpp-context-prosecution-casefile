package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived.manualCaseReceived;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionRejected.publicProsecutionRejected;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CcProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ManualCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ProsecutionRejectedProcessor {

    private static final String PUBLIC_PROSECUTION_REJECTED_EVENT = "public.prosecutioncasefile.prosecution-rejected";
    private static final String PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED = "public.prosecutioncasefile.manual-case-received";
    private static final String PUBLIC_CIVIL_PROSECUTION_REJECTED_EVENT = "public.prosecutioncasefile.civil-prosecution-rejected";

    @Inject
    private Sender sender;

    @Handles("prosecutioncasefile.events.sjp-prosecution-rejected")
    public void handleSjpProsecutionRejected(final Envelope<SjpProsecutionRejected> sjpProsecutionRejectedEvent) {
        final SjpProsecutionRejected privateSjpProsecutionRejected = sjpProsecutionRejectedEvent.payload();
        final Prosecution prosecution = privateSjpProsecutionRejected.getProsecution();

        if (prosecution.getChannel() == MCC) {
            final UUID caseId = prosecution.getCaseDetails().getCaseId();
            final String caseURN = prosecution.getCaseDetails().getProsecutorCaseReference();
            final List<Problem> errors = privateSjpProsecutionRejected.getErrors();
            emitMCCPublicEvent(sjpProsecutionRejectedEvent.metadata(), caseId, caseURN, errors);
        } else {
            sender.send(envelopeFrom(
                    metadataFrom(sjpProsecutionRejectedEvent.metadata()).withName(PUBLIC_PROSECUTION_REJECTED_EVENT),
                    publicProsecutionRejected()
                            .withErrors(privateSjpProsecutionRejected.getErrors())
                            .withCaseId(privateSjpProsecutionRejected.getProsecution().getCaseDetails().getCaseId())
                            .withExternalId(privateSjpProsecutionRejected.getExternalId())
                            .withChannel(privateSjpProsecutionRejected.getProsecution().getChannel())
                            .build()));
        }
    }

    @Handles("prosecutioncasefile.events.cc-prosecution-rejected")
    public void handleCCProsecutionRejected(final Envelope<CcProsecutionRejected> ccProsecutionRejectedEnvelope) {
        final CcProsecutionRejected ccProsecutionRejected = ccProsecutionRejectedEnvelope.payload();
        final Prosecution prosecution = ccProsecutionRejected.getProsecution();

        if (prosecution.getChannel() == MCC) {
            final UUID caseId = prosecution.getCaseDetails().getCaseId();
            final String caseURN = prosecution.getCaseDetails().getProsecutorCaseReference();
            final List<Problem> errors = ccProsecutionRejected.getCaseErrors();
            if (isNotEmpty(ccProsecutionRejected.getDefendantErrors())) {
                final List<Problem> defendantErrors = ccProsecutionRejected.getDefendantErrors().stream()
                        .map(DefendantProblem::getProblems)
                        .flatMap(Collection::stream)
                        .collect(toList());
                errors.addAll(defendantErrors);
            }
            emitMCCPublicEvent(ccProsecutionRejectedEnvelope.metadata(), caseId, caseURN, errors);
        } else if (prosecution.getChannel() == CIVIL) {
            sender.send(envelopeFrom(
                    metadataFrom(ccProsecutionRejectedEnvelope.metadata()).withName(PUBLIC_CIVIL_PROSECUTION_REJECTED_EVENT),
                    PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                            .withCaseId(ccProsecutionRejected.getProsecution().getCaseDetails().getCaseId())
                            .withCaseErrors(ccProsecutionRejected.getCaseErrors())
                            .withDefendantErrors(ccProsecutionRejected.getDefendantErrors())
                            .withExternalId(ccProsecutionRejected.getExternalId())
                            .withChannel(ccProsecutionRejected.getProsecution().getChannel())
                            .build()
            ));
        } else {
            sender.send(envelopeFrom(
                    metadataFrom(ccProsecutionRejectedEnvelope.metadata()).withName(PUBLIC_PROSECUTION_REJECTED_EVENT),
                    publicProsecutionRejected()
                            .withCaseId(ccProsecutionRejected.getProsecution().getCaseDetails().getCaseId())
                            .withCaseErrors(ccProsecutionRejected.getCaseErrors())
                            .withDefendantErrors(ccProsecutionRejected.getDefendantErrors())
                            .withExternalId(ccProsecutionRejected.getExternalId())
                            .withChannel(ccProsecutionRejected.getProsecution().getChannel())
                            .build()
            ));
        }
    }

    private void emitMCCPublicEvent(Metadata metadata, UUID caseId, String prosecutorCaseReference, List<Problem> errors) {
        final ManualCaseReceived manualCaseReceived = manualCaseReceived()
                .withCaseId(caseId)
                .withProsecutorCaseReference(prosecutorCaseReference)
                .withErrors(errors)
                .build();

        final Metadata sjpCaseMetadata = metadataFrom(metadata)
                .withName(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED)
                .build();

        sender.send(envelopeFrom(sjpCaseMetadata, manualCaseReceived));
    }

}