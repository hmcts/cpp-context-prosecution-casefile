package uk.gov.moj.cpp.prosecutioncasefile.query.view.response;

import javax.json.JsonArray;

@SuppressWarnings("squid:S2384")
public class ErrorCaseDetails {

    private JsonArray defendants;

    public ErrorCaseDetails(final JsonArray defendants) {
        this.defendants = defendants;
    }

    public JsonArray getDefendants() {
        return defendants;
    }
}
