package uk.gov.moj.cpp.prosecution.casefile.domain;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;


@SuppressWarnings("squid:S2384")
public class DefendantWithReferenceData {

    private final Defendant defendant;
    private final ReferenceDataVO referenceDataVO;
    private final CaseDetails caseDetails;

    private  final boolean isMCCWithListNewHearing;
    private final boolean isMCC ;
    private final boolean isInactiveMigratedCase;



    public DefendantWithReferenceData(final Defendant defendant, ReferenceDataVO referenceDataVO, CaseDetails caseDetails) {
        this.defendant = defendant;
        this.referenceDataVO = referenceDataVO;
        this.caseDetails = caseDetails;
        this.isMCCWithListNewHearing = false;
        this.isMCC = false;
        this.isInactiveMigratedCase = false;
    }

    public DefendantWithReferenceData(final Defendant defendant, ReferenceDataVO referenceDataVO, CaseDetails caseDetails, boolean  isMCCWithListNewHearing, boolean isMCC, final boolean isInactiveMigratedCase) {
        this.defendant = defendant;
        this.referenceDataVO = referenceDataVO;
        this.caseDetails = caseDetails;
        this.isMCCWithListNewHearing = isMCCWithListNewHearing;
        this.isMCC = isMCC;
        this.isInactiveMigratedCase = isInactiveMigratedCase;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public CaseDetails getCaseDetails() {
        return caseDetails;
    }

    public boolean isMCCWithListNewHearing() {
        return isMCCWithListNewHearing;
    }

    public boolean isInactiveMigratedCase() {
        return isInactiveMigratedCase;
    }

    public boolean isMCC() {
        return isMCC;
    }
}
