package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PDF_MIME_TYPE;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.PENDING_MATERIAL_EXPIRATION_PROCESS_NAME;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.buildAddMaterialCommandPayloadForCpsCaseDocument;
import static uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper.uploadFile;

import com.jayway.awaitility.Awaitility;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.prosecution.casefile.helper.ActivitiHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.AddMaterialHelper;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilteredCaseIT extends BaseIT {

    private final static String SJPN_DOCUMENT_TYPE_LOWER_CASE = "sjpn";
    private static final String PUBLIC_SPI_CASE_FILTERED_EVENT_PAYLOAD = "{\"caseId\": \"%s\"}";
    public static final String PROSECUTIONCASEFILE_EVENTS_CASE_FILTERED = "prosecutioncasefile.events.case-filtered";

    private UUID caseId;

    private final UUID submissionId = randomUUID();

    private final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME)
            .withEventNames(PROSECUTIONCASEFILE_EVENTS_CASE_FILTERED)
            .getMessageConsumerClient();

    @BeforeEach
    public void setup() {
        caseId = randomUUID();
    }

    @Test
    public void shouldCancelTimerWhenCaseIsFiltered() throws Exception {
        final UUID fileStoreId = uploadFile(PDF_MIME_TYPE);

        final JsonObject addMaterialCommandPayload = buildAddMaterialCommandPayloadForCpsCaseDocument(fileStoreId, SJPN_DOCUMENT_TYPE_LOWER_CASE);

        final AddMaterialHelper addMaterialHelper = new AddMaterialHelper();

        // when CPS material is received before case
        addMaterialHelper.addCpsMaterial(caseId, submissionId, addMaterialCommandPayload);
        ActivitiHelper.pollUntilProcessExists(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, fileStoreId.toString());
        ActivitiHelper.pollUntilProcessExists(BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, fileStoreId.toString());


        // And case is filtered then event "prosecutioncasefile.events.case-filtered" should be raised
        final String payload = String.format(PUBLIC_SPI_CASE_FILTERED_EVENT_PAYLOAD, caseId);
        postPublicCaseFilteredMessageToTopicAndVerify(payload, of(caseId.toString()));

        // And timer should be cancelled
        ActivitiHelper.pollUntilProcessDeleted(PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, fileStoreId.toString(), "Timeout cancelled");
        ActivitiHelper.pollUntilProcessDeleted(BULKSCAN_PENDING_MATERIAL_EXPIRATION_PROCESS_NAME, fileStoreId.toString(), "Timeout cancelled");
    }


    @Test
    public void shouldRaiseCaseFilteredEventWhenCaseFilterCommandReceived() {

        final String payload = String.format(PUBLIC_SPI_CASE_FILTERED_EVENT_PAYLOAD, caseId);
        postPublicCaseFilteredMessageToTopicAndVerify(payload,  of(caseId.toString()));

    }

    private void postPublicCaseFilteredMessageToTopicAndVerify(final String payload, final Optional<String> caseId) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        sendPublicEvent("public.stagingprosecutorsspi.event.prosecution-case-filtered", stringToJsonObjectConverter.convert(payload));

        if (caseId.isPresent()) {
            Awaitility.await().timeout(30, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .pollDelay(1, TimeUnit.SECONDS)
                    .until(
                            () -> messageConsumerClient.retrieveMessage(10000L).orElse(null),
                            containsString(caseId.get()));
        } else {
            final String eventPayload = messageConsumerClient.retrieveMessage(10000L).orElse(null);
            assertThat("prosecutioncasefile.events.case-filtered message not found in prosecutioncasefile.event topic", eventPayload, notNullValue());

        }
    }
}
