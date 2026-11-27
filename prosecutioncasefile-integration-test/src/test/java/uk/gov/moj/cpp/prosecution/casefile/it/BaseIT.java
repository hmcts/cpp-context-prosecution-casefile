package uk.gov.moj.cpp.prosecution.casefile.it;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.createCommonMockEndpoints;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetParentBundleSection;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetReferenceDataBySectionCode;

@ExtendWith(JmsResourceManagementExtension.class)
public class BaseIT {
    public static final String CONTEXT_NAME = "prosecutioncasefile";

    private static AtomicBoolean atomicBoolean = new AtomicBoolean();

    private JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider()
            .getMessageProducerClient();

    @BeforeAll
    public static void baseSetupOnce() throws Throwable {
        if (!atomicBoolean.get()) {
            atomicBoolean.set(true);

            WireMock.configureFor(System.getProperty("INTEGRATION_HOST_KEY", "localhost"), 8080);
            WireMock.reset();
            createCommonMockEndpoints();
            stubGetReferenceDataBySectionCode();
            stubGetOrganisationUnits();
            stubGetParentBundleSection();
        }
    }

    protected void sendPublicEvent(final String name, final String payloadResource, final String... placeholders) {
        final JsonEnvelope publicEvent = envelopeFrom(
                metadataWithRandomUUID(name),
                readJsonResource(payloadResource, (Object[]) placeholders));

        publicMessageProducerClient.sendMessage(name, publicEvent);
    }

    protected void sendPublicEvent(final String name, final JsonEnvelope publicEvent) {
        publicMessageProducerClient.sendMessage(name, publicEvent);
    }

    protected void sendPublicEvent(final String name, final JsonObject publicEvent) {
        publicMessageProducerClient.sendMessage(name, publicEvent);
    }

}
