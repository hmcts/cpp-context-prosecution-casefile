package uk.gov.moj.cpp.prosecution.casefile.event;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantProblemsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("prosecutioncasefile.events.sjp-validation-failed")
@SuppressWarnings("squid:S2384")
public class SjpValidationFailed {

    private final Prosecution prosecution;

    private final List<Problem> caseProblems;

    private final List<DefendantProblemsVO> defendantProblemsVO;

    private final ReferenceDataVO referenceDataVO;

    private final InitialHearing initialHearing;

    public SjpValidationFailed(Prosecution prosecution, List<Problem> caseProblems, List<DefendantProblemsVO> defendantProblemsVO, ReferenceDataVO referenceDataVO, final InitialHearing initialHearing) {
        this.prosecution = prosecution;
        this.caseProblems = caseProblems;
        this.defendantProblemsVO = defendantProblemsVO;
        this.referenceDataVO = referenceDataVO;
        this.initialHearing = initialHearing;
    }

    public Prosecution getProsecution() {
        return prosecution;
    }

    public List<Problem> getCaseProblems() {
        return caseProblems;
    }

    public List<DefendantProblemsVO> getDefendantProblemsVO() {
        return defendantProblemsVO;
    }

    public ReferenceDataVO getReferenceDataVO() {
        return referenceDataVO;
    }

    public InitialHearing getInitialHearing() {
        return initialHearing;
    }

}
