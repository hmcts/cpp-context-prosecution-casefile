package uk.gov.moj.cpp.prosecution.casefile.query.api.utils;

import javax.json.JsonObject;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.List;

public class OffenceHelper {

    private static final List<String> summaryModeOfTrials = asList("STRAFF", "SNONIMP", "SIMP");

    public boolean isOffenceSummaryType(final JsonObject offenceDefinition) {
        if (isNull(offenceDefinition)) {
            return true;
        }
        return !summaryModeOfTrials.contains(ofNullable(offenceDefinition.getString("modeOfTrial", null)).orElse(EMPTY).toUpperCase());
    }
}
