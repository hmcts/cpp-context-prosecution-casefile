package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_DOCUMENT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.UPLOAD_CASE_DOCUMENT_COMMAND_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.UPLOAD_FILE_URL;

import java.util.UUID;
import java.util.concurrent.Callable;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONObject;

public class AddIDPCMaterialHelper extends AbstractTestHelper {

    public AddIDPCMaterialHelper() {
        createPrivateConsumerForMultipleSelectors(
                EVENT_SELECTOR_IDPC_MATERIAL_RECEIVED,
                EVENT_SELECTOR_CASE_DOCUMENT_ADDED,
                EVENT_SELECTOR_IDPC_DEFENDANT_MATCH_PENDING,
                EVENT_SELECTOR_IDPC_DEFENDANT_MATCHED,
                EVENT_SELECTOR_DEFENDANT_IDPC_ALREADY_EXITS
        );

        createPublicConsumerForMultipleSelectors(PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_IDPC_ADDED);
    }

    public void addIDPCMaterial(final UUID caseId, final JsonObject addIDPCMaterialRequest) {
        makePostCall(getWriteUrl(format("/cases/%s/material", caseId.toString())),
                "application/vnd.prosecutioncasefile.add-idpc-material+json",
                addIDPCMaterialRequest.toString());
    }


    public JSONObject verifyUploadFileCalledAndGetPayload(final String fileServiceId) throws Exception {

        final Matcher commandMatcher = hasJsonPath("$.fileServiceId", Matchers.equalTo(fileServiceId));

        final Callable<JSONObject> getMaterialId = () -> findAll(postRequestedFor(urlPathMatching(UPLOAD_FILE_URL))
                .withHeader(CONTENT_TYPE, equalTo(UPLOAD_CASE_DOCUMENT_COMMAND_TYPE)))
                .stream()
                .map(LoggedRequest::getBodyAsString)
                .filter(commandMatcher::matches)
                .findFirst()
                .map(JSONObject::new)
                .orElse(null);

        await().until(getMaterialId, notNullValue());

        return getMaterialId.call();
    }

//    public JsonEnvelope getPrivateEvent() {
//        return getEventFromQueue(privateEventsConsumer);
//    }
//
//    public JsonEnvelope getPublicEvent() {
//        return getEventFromQueue(publicEventsConsumer);
//    }
}

