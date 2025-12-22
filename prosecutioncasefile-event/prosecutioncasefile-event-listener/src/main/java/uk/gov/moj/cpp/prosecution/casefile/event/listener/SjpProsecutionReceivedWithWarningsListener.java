package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedWithWarningsToCase;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SjpProsecutionReceivedWithWarningsListener {
    @Inject
    private ProsecutionReceivedWithWarningsToCase prosecutionReceivedWithWarningsToCaseConverter;

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Handles("prosecutioncasefile.events.sjp-prosecution-received-with-warnings")
    public void prosecutionReceived(final Envelope<SjpProsecutionReceivedWithWarnings> envelope) {
        final SjpProsecutionReceivedWithWarnings sjpProsecutionReceived = envelope.payload();
        final CaseDetails caseDetails = prosecutionReceivedWithWarningsToCaseConverter.convert(sjpProsecutionReceived);
        businessValidationErrorRepository.deleteByCaseId(caseDetails.getCaseId());
        businessValidationErrorCaseDetailsRepository.deleteByCaseId(caseDetails.getCaseId());
        caseDetailsRepository.save(caseDetails);
    }
}
