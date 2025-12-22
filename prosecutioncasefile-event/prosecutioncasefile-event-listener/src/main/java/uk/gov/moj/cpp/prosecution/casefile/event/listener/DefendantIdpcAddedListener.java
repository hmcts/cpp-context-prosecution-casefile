package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantIdpcAddedListener {

    @Inject
    private DefendantRepository defendantRepository;

    @Handles("prosecutioncasefile.events.defendant-idpc-added")
    public void caseDocumentAdded(final Envelope<DefendantIdpcAdded> envelope) {
        final DefendantIdpcAdded defendantIdpcAdded = envelope.payload();
        final UUID materialId = defendantIdpcAdded.getCaseDocument().getMaterialId();
        final DefendantDetails defendantDetails = defendantRepository.findBy(defendantIdpcAdded.getDefendantId().toString());
        defendantDetails.setIdpcMaterialId(materialId);
        defendantRepository.save(defendantDetails);
    }

}
