package uk.gov.moj.cpp.prosecution.casefile.command.api;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.util.FileUtil.resourceToString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.command.api.service.CaseDetailsEnrichmentService;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateGroupProsecution;

import java.util.UUID;

import javax.enterprise.inject.Instance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class InitiateGroupProsecutionApiTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
    private static final String INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA_COMMAND = "prosecutioncasefile.command.initiate-group-prosecution-with-reference-data";
    private static final String POLICE_SYSTEM_ID = "00301PoliceCaseSystem";

    @Captor
    private ArgumentCaptor<Envelope<GroupProsecutionList>> envelopeArgumentCaptor;

    @InjectMocks
    private InitiateGroupProsecutionApi initiateGroupProsecutionApi;
    @Mock
    private Sender sender;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;
    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;
    @Mock
    private CaseDetailsEnrichmentService caseDetailsEnrichmentService;

    @Test
    public void shouldSendReceiveGroupProsecutionWithReferenceDataCommandWithCorrectPayload() throws Exception {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222"))
                .withInitiationCode("C")
                .withPoliceSystemId(POLICE_SYSTEM_ID)
                .build();
        when(this.caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);
        when(this.referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(singletonList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("N")
                .build()));

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();
        when(this.referenceDataQueryService.getProsecutorsByOuCode(any(), any())).thenReturn(prosecutorsReferenceData);

        final InitiateGroupProsecution initiateGroupProsecution = initiateGroupProsecutionPayloadFromFile("json/initiateGroupProsecution.json", "MCC");
        final Envelope<InitiateGroupProsecution> envelope = envelope(initiateGroupProsecution);

        this.initiateGroupProsecutionApi.initiateGroupProsecution(envelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        final Envelope<GroupProsecutionList> groupProsecutionDetailsEnvelope = this.envelopeArgumentCaptor.getValue();
        assertThat(groupProsecutionDetailsEnvelope.metadata().name(), is(INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA_COMMAND));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getCaseDetails().getCaseId(), is(caseDetails.getCaseId()));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getPaymentReference(), is(initiateGroupProsecution.getGroupProsecutions().get(0).getPaymentReference()));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getDefendants().size(), is(1));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(groupProsecutionDetailsEnvelope.payload().getExternalId(), is(notNullValue()));
    }

    @Test
    public void shouldGetProsecutorByIdWhenOUCodeIsNull() throws Exception {
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withCaseId(UUID.fromString("51cac7fb-387c-4d19-9c80-8963fa8cf222"))
                .withPoliceSystemId(POLICE_SYSTEM_ID)
                .build();
        when(this.caseDetailsEnrichmentService.enrichCaseDetails(any(), any())).thenReturn(caseDetails);

        when(this.referenceDataQueryService.retrieveOffenceData(any(), any())).thenReturn(singletonList(OffenceReferenceData.offenceReferenceData()
                .withLocationRequired("N")
                .build()));

        final ProsecutorsReferenceData prosecutorsReferenceData = new ProsecutorsReferenceData.Builder()
                .withShortName("OWTW")
                .build();

        final InitiateGroupProsecution initiateGroupProsecution = initiateGroupProsecutionPayloadFromFile("json/initiateGroupProsecution.json", "MCC");
        final Envelope<InitiateGroupProsecution> envelope = envelope(initiateGroupProsecution);

        this.initiateGroupProsecutionApi.initiateGroupProsecution(envelope);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());
        final Envelope<GroupProsecutionList> groupProsecutionDetailsEnvelope = this.envelopeArgumentCaptor.getValue();
        assertThat(groupProsecutionDetailsEnvelope.metadata().name(), is(INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA_COMMAND));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getCaseDetails().getCaseId(), is(caseDetails.getCaseId()));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getPaymentReference(), is(initiateGroupProsecution.getGroupProsecutions().get(0).getPaymentReference()));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getDefendants().size(), is(1));
        assertThat(groupProsecutionDetailsEnvelope.payload().getGroupProsecutionWithReferenceDataList().get(0).getGroupProsecution().getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(groupProsecutionDetailsEnvelope.payload().getExternalId(), is(notNullValue()));
    }

    private Envelope<InitiateGroupProsecution> envelope(final InitiateGroupProsecution initiateGroupProsecution) {
        return envelopeFrom(metadataBuilder().withName("Command").withId(randomUUID()), initiateGroupProsecution);
    }

    private InitiateGroupProsecution initiateGroupProsecutionPayloadFromFile(final String inputPayloadFile, final String channel) throws Exception {
        return OBJECT_MAPPER.readValue(resourceToString(inputPayloadFile, channel), InitiateGroupProsecution.class);
    }
}
