package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;


import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant.matchedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem.problem;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.INVALID_DEFENDANTS_PROVIDED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.CPS_DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.MATCHING_ID;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpsDefendantIdValidationRule implements ValidationRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsDefendantIdValidationRule.class);

    @Override
    public MatchedDefendant validate(final JsonObject cpsDefendant, final JsonValue cpsDefendantIdRuleInput) {

        final String cpsDefendantId = cpsDefendant.getString(CPS_DEFENDANT_ID);
        LOGGER.info("Executing Rule: CPS Defendant Id matching rule for CPS Defendant Id: {}", cpsDefendantId);

        final JsonArray defendantIdsList = ((JsonObject) cpsDefendantIdRuleInput).getJsonArray("cpsDefendantIdList");
        final boolean areAllDefendantsHaveCpsIds = ((JsonObject) cpsDefendantIdRuleInput).getBoolean("areAllDefendantsHaveCpsIds");

        final List<JsonObject> defendantIds = defendantIdsList.getValuesAs(JsonObject.class);

        final Optional<JsonObject> foundDefendant = defendantIds.stream()
                .filter(defendantIdsAsJsonObject -> (defendantIdsAsJsonObject).getString(CPS_DEFENDANT_ID).equals(cpsDefendantId))
                .findFirst();

        final MatchedDefendant.Builder matchedDefendantBuilder = matchedDefendant();

        if (foundDefendant.isPresent()) {
            final UUID defendantId = fromString(foundDefendant.get().getString(DEFENDANT_ID));

            LOGGER.info("Found matching defendantId: {} for cpsDefendantId: {}", defendantId, cpsDefendantId);
            matchedDefendantBuilder
                    .withMatchingId(fromString(cpsDefendant.getString(MATCHING_ID)))
                    .withDefendantId(defendantId)
                    .withIsContinueMatching(false);
        } else {
            matchedDefendantBuilder.withIsContinueMatching(!areAllDefendantsHaveCpsIds);

            LOGGER.info("No matching defendantId for cpsDefendantId: {} found", cpsDefendantId);
            matchedDefendantBuilder
                    .withProblems(asList(problem()
                            .withCode(INVALID_DEFENDANTS_PROVIDED.getCode())
                            .withValues(asList(problemValue()
                                    .withKey(CPS_DEFENDANT_ID)
                                    .withValue(cpsDefendantId)
                                    .build()))
                            .build()))
                    .build();
        }

        return matchedDefendantBuilder.build();
    }
}
