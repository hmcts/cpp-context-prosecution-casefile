package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.lang.String.format;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_ADDED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_PENDING;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_PENDING_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED_V2;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_UPLOAD_CASE_DOCUMENT_RECORDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_BULK_SCAN_MATERIAL_REJECTED_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_CASE_IS_EJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_CASE_REFERRED_TO_COURT_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_DOCUMENT_REVIEW;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_MATERIAL_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_MATERIAL_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_SJP_CASE_ASSIGNED_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_SJP_CASE_UNASSIGNED_EVENT;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.UPLOAD_FILE_URL;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.ADD_COURT_DOCUMENT_COMMAND;
import static uk.gov.moj.cpp.prosecution.casefile.stub.SjpStub.UPLOAD_CASE_DOCUMENT_COMMAND;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.stub.SjpStub;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.jayway.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;

public class AddMaterialHelper extends AbstractTestHelper {

    public final static String PDF_MIME_TYPE = "application/pdf";
    public final static String PENDING_MATERIAL_EXPIRATION_PROCESS_NAME = "pendingMaterialExpiration";
    public final static String BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME = "bulkScanMaterialExpiration";

    public AddMaterialHelper() {
        createPrivateConsumerForMultipleSelectors(
                EVENT_SELECTOR_MATERIAL_ADDED,
                EVENT_SELECTOR_MATERIAL_ADDED_V2,
                EVENT_SELECTOR_MATERIAL_ADDED_WITH_WARNINGS,
                EVENT_SELECTOR_MATERIAL_PENDING,
                EVENT_SELECTOR_MATERIAL_PENDING_V2,
                EVENT_SELECTOR_MATERIAL_REJECTED_WITH_WARNINGS,
                EVENT_SELECTOR_MATERIAL_REJECTED,
                EVENT_SELECTOR_MATERIAL_REJECTED_V2,
                EVENT_SELECTOR_UPLOAD_CASE_DOCUMENT_RECORDED);

        createPublicConsumerForMultipleSelectors(
                PUBLIC_MATERIAL_REJECTED,
                PUBLIC_MATERIAL_ADDED,
                PUBLIC_CASE_REFERRED_TO_COURT_EVENT,
                PUBLIC_SJP_CASE_ASSIGNED_EVENT,
                PUBLIC_SJP_CASE_UNASSIGNED_EVENT,
                PUBLIC_BULK_SCAN_MATERIAL_REJECTED_EVENT,
                PUBLIC_CASE_IS_EJECTED,
                PUBLIC_DOCUMENT_REVIEW,
                PUBLIC_MATERIAL_DOCUMENT_BUNDLE_ARRIVED_FOR_UNBUNDLE
        );
    }

    public void addMaterial(final UUID caseId, final UUID submissionId, final JsonObject addMaterialRequest) {
        SjpStub.stubForUploadCaseDocumentCommand(caseId.toString(), addMaterialRequest.getJsonObject("material").getString("documentType"));

        makePostCall(getWriteUrl(format("/cases/%s/material", caseId.toString())),
                "application/vnd.prosecutioncasefile.add-material+json",
                createObjectBuilder(addMaterialRequest)
                        .add(JsonEnvelope.METADATA, JsonObjects.createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("name", "prosecutioncasefile.add-material")
                                .add("submissionId", submissionId.toString())
                                .build()
                        )
                        .build().toString());
    }

    public void addMaterials(final UUID caseId, final UUID submissionId, final JsonObject addMaterialRequest) {
        addMaterialRequest.getJsonArray("materials").getValuesAs(JsonObject.class).stream()
                .map(jsonObject -> jsonObject.getString("documentType"))
                .distinct()
                .forEach(documentType -> SjpStub.stubForUploadCaseDocumentCommand(caseId.toString(), documentType));

        makePostCall(getWriteUrl(format("/cases/%s/material", caseId.toString())),
                "application/vnd.prosecutioncasefile.add-materials+json",
                createObjectBuilder(addMaterialRequest)
                        .add(JsonEnvelope.METADATA, JsonObjects.createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("name", "prosecutioncasefile.add-materials")
                                .add("submissionId", submissionId.toString())
                                .build()
                        )
                        .build().toString());
    }

    public void addMaterialV2(final UUID caseId, final UUID submissionId, final JsonObject addMaterialRequest) {
        SjpStub.stubForUploadCaseDocumentCommand(caseId.toString(), addMaterialRequest.getString("materialType"));

        makePostCall(getWriteUrl(format("/cases/%s/material", caseId)),
                "application/vnd.prosecutioncasefile.add-material-v2+json",
                createObjectBuilder(addMaterialRequest)
                        .add(JsonEnvelope.METADATA, JsonObjects.createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("name", "prosecutioncasefile.add-material-v2")
                                .add("submissionId", submissionId.toString())
                                .build()
                        )
                        .build().toString());
    }

    public void addApplicationMaterialV2(final UUID applicationId, final UUID submissionId, final JsonObject addMaterialRequest) {
        SjpStub.stubForUploadCaseDocumentCommand(applicationId.toString(), addMaterialRequest.getString("materialType"));

        makePostCall(getWriteUrl(format("/applications/%s/material", applicationId.toString())),
                "application/vnd.prosecutioncasefile.add-application-material-v2+json",
                createObjectBuilder(addMaterialRequest)
                        .add(JsonEnvelope.METADATA, JsonObjects.createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("name", "prosecutioncasefile.add-application-material-v2")
                                .add("submissionId", submissionId.toString())
                                .build()
                        )
                        .build().toString());
    }

    public void addCpsMaterial(final UUID caseId, final UUID submissionId, final JsonObject addCpsMaterialRequest) {
        SjpStub.stubForUploadCaseDocumentCommand(caseId.toString(), addCpsMaterialRequest.getJsonObject("material").getString("documentType"));

        makePostCall(getWriteUrl(format("/cases/%s/material", caseId.toString())),
                "application/vnd.prosecutioncasefile.add-cps-material+json",
                createObjectBuilder(addCpsMaterialRequest)
                        .add(JsonEnvelope.METADATA, JsonObjects.createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .add("name", "prosecutioncasefile.add-cps-material")
                                .add("submissionId", submissionId.toString())
                                .build()
                        )
                        .build().toString());
    }

    public void verifyUploadCaseDocumentCalled(final UUID caseId, final String documentType) {
        await().until(() -> !findAll(postRequestedFor(urlPathMatching(String.format(UPLOAD_CASE_DOCUMENT_COMMAND, caseId, documentType)))).isEmpty());
    }

    public void verifyUploadMaterialCalled(final String fileStoreId) {
        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_SECS, TimeUnit.SECONDS)
                .pollDelay(POLL_DELAY_SECS, TimeUnit.SECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(UPLOAD_FILE_URL))
                                .withRequestBody(containing(fileStoreId))).size(),
                        CoreMatchers.is(1));
    }

    public void verifyAddCourtDocumentCalled(final String materialId) {
        Awaitility.await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(POLL_INTERVAL_SECS, TimeUnit.SECONDS)
                .pollDelay(POLL_DELAY_SECS, TimeUnit.SECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlPathMatching(ADD_COURT_DOCUMENT_COMMAND + materialId))).size(),
                        CoreMatchers.is(1));
    }

    public static JsonObject buildAddMaterialCommandPayloadForCpsCaseDocument(final UUID fileStoreId, final String documentType) {
        return readJsonResource("stub-data/prosecutioncasefile.add-material_for_cps_case_document.json", fileStoreId, documentType);
    }

    public static UUID uploadFile(final String mimeType) throws Exception {
        return FileServiceHelper.create("XVBN22.pdf", mimeType, getSystemResourceAsStream("materials/XVBN22.pdf"));
    }

    public static String getEventName(final JsonEnvelope event) {
        return Optional.ofNullable(event)
                .map(jsonEnvelope -> jsonEnvelope.metadata().name())
                .orElseThrow(() -> new AssertionError("No event retrieved"));
    }

}

