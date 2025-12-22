package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.fail;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.ApplicationAcceptedToCourtApplicationProceedingsConverter;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmitApplicationAccepted;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;

public class MapperBase {
    protected ApplicationAcceptedToCourtApplicationProceedingsConverter subject;
    private UUID applicationId;
    private UUID boxHearingRequestId;
    protected SubmitApplicationAccepted sourceSubmitApplication;
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @BeforeEach
    public void init() {
        applicationId = randomUUID();
        boxHearingRequestId = randomUUID();
        JsonObject jsonObject = generateSubmitApplicationPayload();
        sourceSubmitApplication = new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, SubmitApplicationAccepted.class);
        subject = new ApplicationAcceptedToCourtApplicationProceedingsConverter();
    }

    private JsonObject generateSubmitApplicationPayload() {
        String payloadStr = getStringFromResource()
                .replace("%APPLICATION_ID%", applicationId.toString())
                .replace("%BOX_HEARING_ID%", boxHearingRequestId.toString());
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private static String getStringFromResource() {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource("submit-application.json"), Charset.defaultCharset());
        } catch (final Exception e) {
            fail("Error consuming file from location " + "submit-application.json");
        }
        return request;
    }
}
