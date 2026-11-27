package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.UUID.fromString;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.BusinessValidationErrorDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorCaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class SummonsApplicationApprovalListener {

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private BusinessValidationErrorCaseDetailsRepository businessValidationErrorCaseDetailsRepository;


    @Handles("prosecutioncasefile.events.defendants-parked-for-summons-application-approval")
    public void processApplicationApprovalRequest(final Envelope<DefendantsParkedForSummonsApplicationApproval> envelope) {
        final DefendantsParkedForSummonsApplicationApproval payload = envelope.payload();
        final UUID caseId = payload.getProsecutionWithReferenceData().getProsecution().getCaseDetails().getCaseId();
        businessValidationErrorRepository.deleteByCaseIdAndDefendantIdIsNull(caseId);
        final List<Defendant> defendants = payload.getProsecutionWithReferenceData().getProsecution().getDefendants();
        if (isNotEmpty(defendants)) {
            defendants.forEach(d -> businessValidationErrorRepository.deleteByDefendantId(fromString(d.getId())));
        }
        deleteErrorCaseDetails(caseId);
    }
    public void deleteErrorCaseDetails(UUID caseId) {
        final List<BusinessValidationErrorDetails> errorDetails = businessValidationErrorRepository.findByCaseId(caseId);
        if (errorDetails.isEmpty()) {
            businessValidationErrorCaseDetailsRepository.deleteByCaseId(caseId);
        }
    }
}
