package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public interface ValidationRule {
    MatchedDefendant validate(final JsonObject input, final JsonValue matchedObject);
}
