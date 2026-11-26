package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant.matchedDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue.problemValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.helper.ValidationRuleHelper.getValueAsString;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.ASN;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.DEFENDANT_ID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.MATCHING_ID;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsnValidationRule implements ValidationRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsnValidationRule.class);

    @Override
    public MatchedDefendant validate(final JsonObject cpsDefendant, final JsonValue pcfDefendant) {
        final String asnToMatch = cpsDefendant.getString(ASN, "");
        LOGGER.info("Executing Rule: ASN matching rule for: {}", asnToMatch);

        final List<JsonObject> caseDefendantList = ((JsonObject) pcfDefendant)
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class);

        final List<String> matchedObjectAsn = caseDefendantList
                .stream()
                .filter(defendant -> defendant.containsKey(ASN))
                .map(o -> getValueAsString(o, ASN))
                .collect(toList());

        final MatchedDefendant.Builder matchedDefendantBuilder = matchedDefendant()
                .withMatchingId(fromString(cpsDefendant.getString(MATCHING_ID)));
        if (matchedObjectAsn.contains(cpsDefendant.getString(ASN, ""))) {
            final Optional<JsonObject> defendantOptional = getDefendantByAsn(((JsonObject) pcfDefendant), asnToMatch);
            final String defendantId = defendantOptional.map(defendant -> defendant.getString(DEFENDANT_ID)).orElse(null);
            matchedDefendantBuilder.withDefendantId(defendantId != null ? fromString(defendantId) : null);
            matchedDefendantBuilder.withIsContinueMatching(false);
        } else {
            final boolean isContinueMatching = caseDefendantList.size() == matchedObjectAsn.size();
            matchedDefendantBuilder.withIsContinueMatching(!isContinueMatching);

            LOGGER.info("Matching defendant on ASN failed so creating <INVALID_DEFENDANTS_PROVIDED> problem for: {}", asnToMatch);
            matchedDefendantBuilder
                    .withProblems(asList(Problem.problem()
                            .withCode(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode())
                            .withValues(asList(problemValue()
                                    .withKey(ASN)
                                    .withValue(asnToMatch)
                                    .build()))
                            .build()))
                    .build();
        }

        return matchedDefendantBuilder.build();
    }

    public static Optional<JsonObject> getDefendantByAsn(final JsonObject matchedObject, final String asnKey) {

        return matchedObject
                .getJsonArray(DEFENDANTS)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(defendant -> defendant.containsKey("asn") && defendant.getString("asn").equals(asnKey))
                .findFirst();
    }

}
