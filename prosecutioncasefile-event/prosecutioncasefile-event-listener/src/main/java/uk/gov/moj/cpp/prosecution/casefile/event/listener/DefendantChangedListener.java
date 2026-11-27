package uk.gov.moj.cpp.prosecution.casefile.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.PersonalInformationToPersonalInformationDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.ProsecutionReceivedToCase;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.BusinessValidationErrorRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.CaseDetailsRepository;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.repository.DefendantRepository;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDefendantChanged;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantChangedListener {

    @Inject
    private ProsecutionReceivedToCase prosecutionReceivedToCaseConverter;

    @Inject
    private CaseDetailsRepository caseDetailsRepository;

    @Inject
    private BusinessValidationErrorRepository businessValidationErrorRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetails;

    @Handles("prosecutioncasefile.event.case-defendant-changed")
    public void caseDefendantChanged(final Envelope<CaseDefendantChanged> envelope) {
        final CaseDefendantChanged caseDefendantChanged = envelope.payload();
        final PersonalInformation personalInformation = caseDefendantChanged.getPersonDetails();
        final DefendantDetails defendantDetails = defendantRepository.findBy(caseDefendantChanged.getDefendantId().toString());
        if (nonNull(defendantDetails)) {
            final PersonalInformationDetails personalInformationDetails = personalInformationToPersonalInformationDetails.convert(personalInformation);
            personalInformationDetails.setPersonalInformationId(defendantDetails.getPersonalInformation().getPersonalInformationId());
            defendantDetails.setPersonalInformation(personalInformationDetails);
            if (nonNull(caseDefendantChanged.getDateOfBirth()) && nonNull(defendantDetails.getSelfDefinedInformation())) {
                defendantDetails.getSelfDefinedInformation().setDateOfBirth(caseDefendantChanged.getDateOfBirth());
            }
            defendantRepository.save(defendantDetails);
        }
    }
}
