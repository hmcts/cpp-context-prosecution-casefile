package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SjpProsecutionReceivedListener {

    @Inject
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Handles("prosecutioncasefile.events.sjp-prosecution-received")
    public void prosecutionReceived(final Envelope<SjpProsecutionReceived> envelope) {
        final SjpProsecutionReceived sjpProsecutionReceived = envelope.payload();
        final CaseDetails caseDetails = prosecutionReceivedToCaseConverter.convert(sjpProsecutionReceived.getProsecution());

        businessValidationErrorRepository.deleteByCaseId(caseDetails.getCaseId());
        businessValidationErrorCaseDetailsRepository.deleteByCaseId(caseDetails.getCaseId());
        final CaseDetails existingCase = caseDetailsRepository.findBy(caseDetails.getCaseId());

        // Deltaspike bug - It can't update existing child records
        if (existingCase != null){
            existingCase.getDefendants().stream().forEach(defendant -> defendantRepository.remove(defendant));
            caseDetailsRepository.remove(existingCase);
        }

        caseDetailsRepository.save(caseDetails);
    }
}
