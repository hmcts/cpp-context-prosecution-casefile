package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import java.util.List;


@SuppressWarnings("squid:S2384")
public class DefendantsWithReferenceData {

    private String prosecutionAuthorityShortName;
    private List<Defendant> defendants;
    private CaseDetails caseDetails;
    private ReferenceDataVO referenceDataVO = new ReferenceDataVO();

    private boolean isCivil;

    public boolean isCivil() {
        return isCivil;
    }

    public void setCivil(final boolean civil) {
        isCivil = civil;
    }

    public DefendantsWithReferenceData(final List<Defendant> defendants) {
        this.defendants = defendants;
    }

    public DefendantsWithReferenceData(final List<Defendant> defendants, final String prosecutionAuthorityShortName) {
        this.defendants = defendants;
        this.prosecutionAuthorityShortName = prosecutionAuthorityShortName;
    }

    public String getProsecutionAuthorityShortName() {
        return prosecutionAuthorityShortName;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(final List<Defendant> defendants) {
        this.defendants = defendants;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public void setReferenceDataVO(final ReferenceDataVO referenceDataVO) {
        this.referenceDataVO = referenceDataVO;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final CaseDetails caseDetails) {
        this.caseDetails = caseDetails;
    }
}
