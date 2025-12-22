package uk.gov.moj.cpp.prosecution.casefile.validation;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import java.util.List;

public class Problems {

    private Problems() {
    }

    public static Problem newProblem(final ProblemCode code, final ProblemValue... values) {
        return new Problem(code.name(), asList(values));
    }

    public static Problem newProblem(final ProblemCode code, final String valueKey, final Object value) {
        return new Problem(code.name(), asList(new ProblemValue(null, valueKey, value.toString())));
    }

    public static Problem newProblem(final ProblemCode code, final List<ProblemValue> values) {
        return new Problem(code.name(), values);
    }
}
