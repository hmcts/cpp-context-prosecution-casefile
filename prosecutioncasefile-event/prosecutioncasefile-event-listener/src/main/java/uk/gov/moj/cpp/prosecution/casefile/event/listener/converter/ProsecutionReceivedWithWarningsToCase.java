package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;


public class ProsecutionReceivedWithWarningsToCase implements Converter<SjpProsecutionReceivedWithWarnings, CaseDetails> {

    @Inject
    private DefendantToDefendantDetails defendantToDefendantDetail;

    @Inject
    private CaseDetailsToCivilFees caseDetailsToCivilFees;

    @Override
    public CaseDetails convert(final SjpProsecutionReceivedWithWarnings prosecutionReceived) {

        return new CaseDetails(
                prosecutionReceived.getProsecution().getCaseDetails().getCaseId(),
                prosecutionReceived.getProsecution().getCaseDetails().getProsecutorCaseReference(),
                prosecutionReceived.getProsecution().getCaseDetails().getProsecutor().getInformant(),
                prosecutionReceived.getProsecution().getCaseDetails().getProsecutor().getProsecutingAuthority(),
                prosecutionReceived.getProsecution().getCaseDetails().getOriginatingOrganisation(),
                Collections.singleton(defendantToDefendantDetail.convert(prosecutionReceived.getProsecution().getDefendants().get(0))),
                getCivilFees(prosecutionReceived.getProsecution())
        );
    }

   private Set<CivilFees> getCivilFees(final Prosecution prosecution) {
       return caseDetailsToCivilFees.convert(prosecution.getCaseDetails());
   }
}

