package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData.offenceReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality.referenceDataCountryNationality;
import static uk.gov.moj.cpp.prosecution.casefile.util.FileUtil.resourceToString;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.IdGenerationService;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ReferenceDataCountryNationality;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.util.List;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(MockitoExtension.class)
public class InitiateSjpProsecutionApiTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    private static final String UUID_IN_TEST = "7f83640a-7bea-4670-af5e-19c1f945b27b";

    private static final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private InitiateSjpProsecutionApi.IdGenerator idGenerator;

    @Mock
    private IdGenerationService idGenerationService;

    @InjectMocks
    private InitiateSjpProsecutionApi initiateSjpProsecutionApi;

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    @Captor
    private ArgumentCaptor<Envelope<ProsecutionWithReferenceData>> envelopeCaptor;

    private static List<ReferenceDataCountryNationality> referenceDataCountryNationalities() {
        return singletonList(referenceDataCountryNationality()
                .withCjsCode("1")
                .withIsoCode("GBR")
                .build()
        );
    }

    private static List<OffenceReferenceData> referenceDataOffences() {
        return singletonList(offenceReferenceData()
                .withCjsOffenceCode("OFCODE12")
                .withOffenceId(UUID.fromString("462204a9-a728-4161-8e53-22241fadf0a0"))
                .build()
        );
    }

    private static Metadata metadataFor(final String commandName) {
        return metadataBuilder()
                .withName(commandName)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())

                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withSessionId(randomUUID().toString())
                .withCausation(randomUUID(), randomUUID())
                .build();
    }

    @Test
    public void shouldHandleProsecutionCaseFileCommandInitiateSjpProsecution() {
        assertThat(InitiateSjpProsecutionApi.class, isHandlerClass(COMMAND_API)
                .with(
                        method("initiateSjpProsecution")
                                .thatHandles("prosecutioncasefile.command.initiate-sjp-prosecution")
                )
        );
    }

    @Test
    public void shouldInitiateSjpProsecutionForIndividual() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("Y")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecution.json", "json/initiate-sjp-prosecution-with-reference-data.json", "SPI");
    }

    @Test
    public void shouldInitiateSjpProsecutionForOrganization() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("Y")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionOrganisation.json", "json/initiate-sjp-prosecution-with-reference-data-organisation.json", "SPI");
    }

    @Test
    public void shouldSetBlankEmailsToNull() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("Y")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithBlankEmails.json", "json/initiate-sjp-prosecution-with-reference-data-with-blank-emails.json", "SPI");
    }

    @Test
    public void shouldInitiateSjpProsecutionWithBlankContactDetails() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("Y")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithBlankContactDetails.json", "json/initiate-sjp-prosecution-with-reference-data-with-blank-contact-details.json", "SPI");
    }

    @Test
    public void shouldInitiateSjpProsecutionWithGivenOffenceLocationWhenChannelIsMCC() throws Exception {
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithGivenOffenceLocation.json", "json/initiate-sjp-prosecution-with-given-offence-location-mcc.json", "MCC");
    }


    @Test
    public void shouldInitiateSjpProsecutionWithBlankOffenceLocationWhenChannelIsMCC() throws Exception {
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithBlankOffenceLocation.json", "json/initiate-sjp-prosecution-with-blank-offence-location-mcc.json", "MCC");
    }

    @Test
    public void shouldInitiateSjpProsecutionWithDefaultOffenceLocationWhenChannelIsSPIAndLocationRequired() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("Y")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithBlankOffenceLocation.json", "json/initiate-sjp-prosecution-with-default-offence-location-spi.json", "SPI");
    }

    @Test
    public void shouldInitiateSjpProsecutionWithEmptyOffenceLocationWhenChannelIsSPIAndLocationNotRequired() throws Exception {
        when(referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(asList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("N")
                .build()));
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithBlankOffenceLocation.json", "json/initiate-sjp-prosecution-with-empty-offence-location-spi.json", "SPI");
    }

    @Test
    public void shouldInitiateSjpProsecutionWithGivenOffenceLocationWhenChannelIsSPI() throws Exception {
        shouldInitiateSjpProsecution("json/initiateSjpProsecutionWithGivenOffenceLocation.json", "json/initiate-sjp-prosecution-with-given-offence-location-spi.json", "SPI");
    }

    private void shouldInitiateSjpProsecution(final String inputPayloadPath, final String expectedPayloadPath, final String channel) throws Exception {
        final InitiateProsecution inputPayload = initiateSjpProsecutionPayloadFromFile(inputPayloadPath, channel);
        final Envelope<InitiateProsecution> receivedEnvelope = envelopeFrom(metadataFor("prosecutioncasefile.command.initiate-sjp-prosecution"), inputPayload);

        final List<ReferenceDataCountryNationality> referenceDataCountryNationalities = referenceDataCountryNationalities();

        when(caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(CaseDetails.caseDetails().build());
        when(idGenerator.generateId()).thenReturn(UUID.fromString(UUID_IN_TEST));

        initiateSjpProsecutionApi.initiateSjpProsecution(receivedEnvelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<ProsecutionWithReferenceData> sentEnvelope = envelopeCaptor.getValue();

        assertThat(sentEnvelope.metadata().name(), is("prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data"));
        assertThat(sentEnvelope.metadata().id(), is(receivedEnvelope.metadata().id()));
        assertThat(sentEnvelope.metadata().userId(), is(receivedEnvelope.metadata().userId()));

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        assertEquals(getExpectedJson(expectedPayloadPath, channel).toString(), objectToJsonObjectConverter.convert(sentEnvelope.payload()).toString(), new CustomComparator(LENIENT,
                new Customization("id", (o1, o2) -> true)
        ));
    }

    private InitiateProsecution initiateSjpProsecutionPayloadFromFile(final String inputPayloadFile, final String channel) throws Exception {
        return OBJECT_MAPPER.readValue(resourceToString(inputPayloadFile, channel), InitiateProsecution.class);
    }

    private JsonObject getExpectedJson(final String filePath, final String channel) throws Exception {
        return OBJECT_MAPPER.readValue(resourceToString(filePath, channel), JsonObject.class);
    }
}
