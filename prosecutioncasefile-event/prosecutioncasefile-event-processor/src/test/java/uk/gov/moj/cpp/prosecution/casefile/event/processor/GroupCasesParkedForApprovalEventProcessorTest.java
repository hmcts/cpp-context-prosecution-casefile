package uk.gov.moj.cpp.prosecution.casefile.event.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.converter.GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter;
import uk.gov.moj.cpp.prosecution.casefile.event.processor.service.DocumentGeneratorService;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.GroupCasesParkedForApprovalEventProcessor.PROGRESSION_COMMAND_ADD_COURT_DOCUMENT;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.GroupCasesParkedForApprovalEventProcessor.PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.GroupCasesParkedForApprovalEventProcessor.PROSECUTIONCASEFILE_COMMAND_RECORD_GROUP_ID_FOR_SUMMONS_APPLICATION;


@ExtendWith(MockitoExtension.class)
public class GroupCasesParkedForApprovalEventProcessorTest {

    @Captor
    private ArgumentCaptor<Envelope<Object>> senderArgCaptor;

    @Mock
    private GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter converter;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private GroupCasesParkedForApprovalEventProcessor groupCasesParkedForApprovalEventProcessor;

    @Test
    void shouldHandleGroupCasesParkedForApproval() {
        final GroupCasesParkedForApproval groupCasesParkedForApproval = createGroupCasesParkedForApproval();

        final Envelope<GroupCasesParkedForApproval> requestEnvelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.events.group-cases-parked-for-approval"),
                groupCasesParkedForApproval);

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = createInitiateCourtApplicationProceedings();

        when(converter.convert(eq(groupCasesParkedForApproval), any())).thenReturn(initiateCourtApplicationProceedings);
        when(documentGeneratorService.generateGroupCasesSummonsDocument(any(), any(), any())).thenReturn("FileName");
        groupCasesParkedForApprovalEventProcessor.handleGroupCasesParkedForApproval(requestEnvelope);

        verify(sender, times(3)).send(senderArgCaptor.capture());

        final Envelope<Object> initiateCourtApplicationProceedingsEnvelope = senderArgCaptor.getAllValues().get(0);
        assertThat(initiateCourtApplicationProceedingsEnvelope.metadata().name(), is(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_FOR_APPLICATION));
        assertThat(initiateCourtApplicationProceedingsEnvelope.payload().getClass().getName(), is(InitiateCourtApplicationProceedings.class.getName()));

        final Envelope<Object> recordGroupIdCommandEnvelope = senderArgCaptor.getAllValues().get(1);
        assertThat(recordGroupIdCommandEnvelope.metadata().name(), is(PROSECUTIONCASEFILE_COMMAND_RECORD_GROUP_ID_FOR_SUMMONS_APPLICATION));

        final Envelope<Object> addCourtDocumentEnvelope = senderArgCaptor.getAllValues().get(2);
        assertThat(addCourtDocumentEnvelope.metadata().name(), is(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT));

    }

    private static GroupCasesParkedForApproval createGroupCasesParkedForApproval() {
        return GroupCasesParkedForApproval.groupCasesParkedForApproval()
                .withApplicationId(randomUUID())
                .withGroupProsecutionList(new GroupProsecutionList(asList(
                        new GroupProsecutionWithReferenceData(
                                GroupProsecution.groupProsecution()
                                        .withIsGroupMaster(true)
                                        .withGroupId(randomUUID())
                                        .withPaymentReference("PAYMENTREF")
                                        .withCaseDetails(CaseDetails.caseDetails()
                                                .withCaseId(randomUUID())
                                                .withDateReceived(LocalDate.now())
                                                .withFeeStatus("OUTSTANDING")

                                                .withProsecutor(Prosecutor.prosecutor()
                                                        .withProsecutingAuthority("TFL")
                                                        .build())
                                                .build())
                                        .withDefendants(singletonList(Defendant.defendant()
                                                .withIndividual(Individual.individual()
                                                        .withPersonalInformation(PersonalInformation.personalInformation()
                                                                .build())
                                                        .build())
                                                .withOffences(singletonList(Offence.offence()
                                                        .withOffenceWording("Offence Wording")
                                                        .withOffenceTitle("Offence Title")
                                                        .withOffenceCode("Offence Code")
                                                        .withReferenceData(OffenceReferenceData.offenceReferenceData()
                                                                .withTitle("Offence Title")
                                                                .withLegislation("Legislation")
                                                                .build())
                                                        .build()))
                                                .build()))
                        .build()))))
                .build();
    }

    private static InitiateCourtApplicationProceedings createInitiateCourtApplicationProceedings() {
        return InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(CourtApplication.courtApplication().build())
                .build();
    }
}