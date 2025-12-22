package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;

import javax.json.JsonObject;
import javax.json.JsonValue;

public interface ValidationRule {
    MatchedDefendant validate(final JsonObject input, final JsonValue matchedObject);
}
