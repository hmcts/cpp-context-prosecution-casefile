package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.util.FileUtil;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.response.CaseDetailsView;
import uk.gov.moj.cpp.prosecutioncasefile.query.view.service.CaseDetailsService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.SubmitApplication;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;


@ExtendWith(MockitoExtension.class)
public class SubmitCCApplicationApiTest {
    private static final Logger LOGGER = getLogger(SubmitCCApplicationApiTest.class);

    private static final UUID MASTER_DEFENDANT_1 = fromString("b44fa9bb-dc36-4375-83a9-ff1bc4cd4374");
    private static final UUID MASTER_DEFENDANT_2 = fromString("1f1fba3c-34ee-454e-b3be-5fb845b2de4a");
    private static final String CASE_URN = "CN12345";
    private static final UUID CASE_ID = randomUUID();
    private static final UUID applicationId = randomUUID();
    private static final UUID boxHearingRequestId = randomUUID();

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private CaseDetailsService caseDetailsService;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private SubmitCCApplicationApi submitCCApplicationApi;

    public static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Test
    public void shouldInitialCourtProceedingsForCourtApplication() throws IOException {
        final JsonEnvelope commandEnvelope = buildSubmitApplicationEnvelope();
        SubmitApplication submitApplication = new ObjectMapperProducer().objectMapper().readValue(commandEnvelope.payloadAsJsonObject().getJsonObject("submitApplication").toString(), SubmitApplication.class);

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), eq(SubmitApplication.class))).thenReturn(submitApplication);
        when(caseDetailsService.findAllCaseByProsecutionReferenceIds(any())).thenReturn(getMockCaseDetailsView());
        when(requester.requestAsAdmin(any(Envelope.class), eq(JsonObject.class))).thenReturn(getResponseEnvelopeFromProgression());
        when(referenceDataQueryService.retrieveApplicationTypes()).thenReturn(getMockApplicationType());
        when(referenceDataQueryService.retrieveCourtCentreDetails(anyString())).thenReturn(getMockCourtroomReferenceData());

        when(jsonObjectToObjectConverter.convert(any(), eq(ProsecutionCase.class)))
                .thenReturn(ProsecutionCase.prosecutionCase()
                        .withId(CASE_ID)
                        .withDefendants(Arrays.asList(
                                Defendant.defendant().withMasterDefendantId(MASTER_DEFENDANT_1).build(),
                                Defendant.defendant().withMasterDefendantId(MASTER_DEFENDANT_2).build()
                        ))
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN(CASE_URN)
                                .build())
                        .withIsCivil(true)
                        .build());


        submitCCApplicationApi.submitCCApplication(commandEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope newCommand = envelopeCaptor.getValue();

        assertThat(newCommand.metadata().name(), is("prosecutioncasefile.command.submit-application"));
        assertThat(newCommand.payload(), notNullValue());
    }

    private Optional<OrganisationUnitWithCourtroomReferenceData> getMockCourtroomReferenceData() {
        return ofNullable(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build());
    }

    private List<CourtApplicationType> getMockApplicationType() {
        return singletonList(CourtApplicationType.courtApplicationType()
                .withId(randomUUID())
                .withCode("CJ03564")
                .withType("Application to amend a community order on change of residence")
                .withLegislation("In accordance with paragraph 16 of Schedule 8 to the Criminal Justice Act 2003.")
                .withCategoryCode("CO")
                .withJurisdiction(Jurisdiction.EITHER)
                .withLinkType(LinkType.LINKED)
                .withAppealFlag(true)
                .withSummonsTemplateType(SummonsTemplateType.BREACH)
                .withHearingCode("APL")
                .withValidFrom("2019-04-01")
                .withValidTo("2099-12-01")
                .withApplicantAppellantFlag(true)
                .withPleaApplicableFlag(true)
                .withOffenceActiveOrder(OffenceActiveOrder.OFFENCE)
                .withCommrOfOathFlag(true)
                .withBreachType(BreachType.GENERIC_BREACH)
                .withCourtOfAppealFlag(true)
                .withCourtExtractAvlFlag(true)
                .withListingNotifTemplate("Template1,Template2")
                .withBoxworkNotifTemplate("Template1,Template2")
                .withTypeWelsh("Sample Text")
                .withLegislationWelsh("Sample Text")
                .withProsecutorThirdPartyFlag(true)
                .withSpiOutApplicableFlag(true)
                .withResentencingActivationCode("CJO3523")
                .withPrefix("Resentenced")
                .build());
    }

    public static JsonEnvelope buildSubmitApplicationEnvelope() {
        JsonObject payload = null;
        try {
            payload = getJsonObjectFromResource("json/submit-application.json");
        } catch (IOException e) {
            LOGGER.error("error while parsing submit-application json");
        }

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("prosecutioncasefile.command.submit-application")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    public static Envelope<JsonObject> getResponseEnvelopeFromProgression() {
        JsonObject responseJsonObject = null;
        try {
            responseJsonObject = getJsonObjectFromResource("json/prosecutionCaseQueryMultipleDefendantsResponse.json");
        } catch (IOException e) {
            LOGGER.error("error while parsing prosecution json");
        }
        return envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.submit-application"), responseJsonObject);
    }

    public static JsonObject getJsonObjectFromResource(final String path) throws IOException {
        return stringToJsonObjectConverter.convert(FileUtil.resourceToString(path));
    }

    private List<CaseDetailsView> getMockCaseDetailsView() {
        final CaseDetails caseDetails1 = new CaseDetails();
        caseDetails1.setCaseId(randomUUID());

        final CaseDetails caseDetails2 = new CaseDetails();
        caseDetails2.setCaseId(randomUUID());

        return asList(new CaseDetailsView(caseDetails1), new CaseDetailsView(caseDetails2));
    }

}