package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;

import javax.inject.Inject;
import java.util.Objects;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionReceivedWithWarningListener {

    @Inject
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;

    @Handles("prosecutioncasefile.events.cc-case-received-with-warnings")
    public void prosecutionCCCaseReceivedWithWarning(final Envelope<CcCaseReceivedWithWarnings> envelope) {
        final CcCaseReceivedWithWarnings caseReceivedWithWarnings = envelope.payload();

        final Prosecution prosecution = caseReceivedWithWarnings.getProsecutionWithReferenceData().getProsecution();

        if(Objects.nonNull(prosecution) && prosecution.getChannel() == MCC) {
            final CaseDetails caseDetails = prosecutionReceivedToCaseConverter.convert(prosecution);
            caseDetailsRepository.save(caseDetails);
            businessValidationErrorRepository.deleteByCaseId(caseDetails.getCaseId());
            businessValidationErrorCaseDetailsRepository.deleteByCaseId(caseDetails.getCaseId());
        }
    }



}
