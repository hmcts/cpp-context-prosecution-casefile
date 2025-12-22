package uk.gov.moj.cpp.prosecution.casefile.domain;

import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;

import java.util.List;


public class DefendantProblemsVO {

    private final Defendant defendant;

    private final List<Problem> problems;

    public DefendantProblemsVO(final Defendant defendant, final List<Problem> problems) {
        this.defendant = defendant;
        this.problems = unmodifiableList(problems);
    }

    public Defendant getDefendant() {
        return defendant;
    }

    public List<Problem> getProblems() {
        return unmodifiableList(problems);
    }
}