package uk.gov.moj.cpp.prosecution.casefile.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionUpdateOffenceCodeRequestReceived;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SjpProsecutionUpdateOffenceCodeListener {

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Handles("prosecutioncasefile.events.sjp-prosecution-update-offence-code-request-received")
    public void prosecutionReceived(final Envelope<SjpProsecutionUpdateOffenceCodeRequestReceived> envelope) {
        final SjpProsecutionUpdateOffenceCodeRequestReceived event = envelope.payload();

        final CaseDetails existingCase = caseDetailsRepository.findBy(event.getCaseId());
        existingCase.getDefendants()
                .forEach(d -> {
                    d.getOffences().forEach(o -> {
                        o.setOffenceCode(getNewOffenceCode(d.getDefendantId(), o.getOffenceId(), event));
                    });
                });

        caseDetailsRepository.save(existingCase);
    }

    private String getNewOffenceCode(final String defendantId, final UUID offenceId, final SjpProsecutionUpdateOffenceCodeRequestReceived event) {
        return event
                .getDefendants()
                .stream()
                .filter(d -> defendantId.equals(d.getId()))
                .map(d ->
                        d.getOffences()
                                .stream()
                                .filter(o -> o.getOffenceId().equals(offenceId))
                                .findFirst().orElseThrow())
                .findFirst().orElseThrow().getOffenceCode();
    }
}
