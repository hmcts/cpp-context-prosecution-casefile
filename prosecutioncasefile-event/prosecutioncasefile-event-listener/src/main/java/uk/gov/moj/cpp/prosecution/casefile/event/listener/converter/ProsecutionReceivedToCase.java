package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;

import javax.inject.Inject;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProsecutionReceivedToCase implements Converter<Prosecution, CaseDetails> {

    @Inject
    private DefendantToDefendantDetails defendantToDefendantDetail;

    @Inject
    private CaseDetailsToCivilFees caseDetailsToCivilFees;

    @Override
    public CaseDetails convert(final Prosecution prosecution) {

        return new CaseDetails(
                prosecution.getCaseDetails().getCaseId(),
                prosecution.getCaseDetails().getProsecutorCaseReference(),
                prosecution.getCaseDetails().getProsecutor().getInformant(),
                prosecution.getCaseDetails().getProsecutor().getProsecutingAuthority(),
                prosecution.getCaseDetails().getOriginatingOrganisation(),
                getDefendantDetails(prosecution.getDefendants()),
                getCivilFees(prosecution)
        );
    }

    private Set<CivilFees> getCivilFees(final Prosecution prosecution) {
        return caseDetailsToCivilFees.convert(prosecution.getCaseDetails());
    }

    private Set<DefendantDetails> getDefendantDetails(final List<Defendant> defendants) {
        return defendants.stream().map(defendantToDefendantDetail::convert).collect(Collectors.toSet());
    }
}
