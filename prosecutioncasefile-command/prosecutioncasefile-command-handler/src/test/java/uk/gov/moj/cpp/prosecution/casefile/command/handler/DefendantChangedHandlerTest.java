package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.readJson;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.test.utils.FileResourceObjectMapper;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.CaseDefendantChangedCommand;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.UpdateCaseWithDefendant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDefendantChanged;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantChangedHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory
            .createEnveloperWithEvents(
                    CaseDefendantChanged.class);

    @InjectMocks
    private DefendantChangedHandler defendantChangedHandler;

    private ProsecutionCaseFile aggregate = new ProsecutionCaseFile();

    private static final UUID DEFENDANT_ID = fromString("3781b95e-113a-46bb-8f94-d5859eb12865");
    private static final UUID CASE_ID = fromString("3781b95e-113a-46bb-8f94-d5859eb12866");


    private static final CcCaseReceived ccCaseReceivedWithLastName = CcCaseReceived.ccCaseReceived()
            .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(Prosecution.prosecution()
                    .withCaseDetails(CaseDetails.caseDetails()
                            .withCaseId(CASE_ID)
                            .build())
                    .withDefendants(Arrays.asList(Defendant.defendant()
                            .withId(DEFENDANT_ID.toString())
                            .withIndividual(Individual.individual().
                                    withPersonalInformation(PersonalInformation.personalInformation()
                                            .withLastName("lastName")
                                            .withTitle("DR")
                                            .build())
                                    .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation().build())
                                    .build())
                            .build()))
                    .build()))
            .build();

    private static final CcCaseReceived ccCaseReceivedWithDateOfBirth = CcCaseReceived.ccCaseReceived()
            .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(Prosecution.prosecution()
                    .withCaseDetails(CaseDetails.caseDetails()
                            .withCaseId(CASE_ID)
                            .build())
                    .withDefendants(Arrays.asList(Defendant.defendant()
                            .withId(DEFENDANT_ID.toString())
                            .withIndividual(Individual.individual().
                                    withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                            .withDateOfBirth(LocalDate.of(2010, 2, 1))
                                            .build())
                                    .withPersonalInformation(PersonalInformation.personalInformation()
                                            .withTitle("DR")
                                            .build())
                                    .build())
                            .build()))
                    .build()))
            .build();

    private static final CcCaseReceived ccCaseReceivedWithMandatoryFields = CcCaseReceived.ccCaseReceived()
            .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(Prosecution.prosecution()
                    .withCaseDetails(CaseDetails.caseDetails()
                            .withCaseId(CASE_ID)
                            .build())
                    .withDefendants(Arrays.asList(Defendant.defendant()
                            .withId(DEFENDANT_ID.toString())
                            .withIndividual(Individual.individual()
                                    .withPersonalInformation(PersonalInformation.personalInformation().withTitle("DR").build())
                                    .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation().build())
                                    .build())
                            .build()))
                    .build()))
            .build();

    private static final CcCaseReceived ccCaseReceivedWithOrganisationDefendant = CcCaseReceived.ccCaseReceived()
            .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(Prosecution.prosecution()
                    .withCaseDetails(CaseDetails.caseDetails()
                            .withCaseId(CASE_ID)
                            .build())
                    .withDefendants(Arrays.asList(Defendant.defendant()
                            .withId(DEFENDANT_ID.toString())
                            .withOrganisationName("Org")
                            .build()))
                    .build()))
            .build();


    @Test
    public void shouldHaveAHandlerForAcceptCourtReferralCommand() {
        assertThat(defendantChangedHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleCaseDefendantChanged")
                        .thatHandles("prosecutioncasefile.command.case-defendant-changed")
                ));
    }

    @Test
    public void shouldHandleTheAcceptCourtReferralWhenTheCaseIsReferredToCourt() throws EventStreamException, IOException {

        when(eventSource.getStreamById(DEFENDANT_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);

        final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

        final CaseDefendantChangedCommand caseDefendantChangedCommand =
                handlerTestHelper.convertFromFile("json/defendant-changed.json", CaseDefendantChangedCommand.class);

        final Envelope<CaseDefendantChangedCommand> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.case-defendant-changed"), caseDefendantChangedCommand);

        defendantChangedHandler.handleCaseDefendantChanged(envelope);

        matchEvent(verifyAppendAndGetArgumentFrom(eventStream),
                "prosecutioncasefile.event.case-defendant-changed",
                () -> readJson("json/defendant-changed.json", JsonValue.class));
    }

    @Test
    public void shouldUpdateAggregateWhenDefendantChanged() throws EventStreamException, IOException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        testDefendantChanged(ccCaseReceivedWithMandatoryFields, "json/update-case-with-defendant.json");
    }

    @Test
    public void shouldUpdateAggregateWhenDefendantChangedButOldDefendantHasOnlyDateOfBirth() throws EventStreamException, IOException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        testDefendantChanged(ccCaseReceivedWithDateOfBirth, "json/update-case-with-defendant.json");
    }

    @Test
    public void shouldUpdateAggregateWhenDefendantChangedButOldDefendantHasOnlyPersonalInformation() throws EventStreamException, IOException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        testDefendantChanged(ccCaseReceivedWithLastName, "json/update-case-with-defendant.json");
    }

    @Test
    public void shouldUpdateAggregateWhenDefendantChangedButONewDefendantDoesNotHaveDateOfBirth() throws EventStreamException, IOException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        testDefendantChanged(ccCaseReceivedWithDateOfBirth, "json/update-case-with-defendant-without-dateofbirth.json");
    }

    @Test
    public void shouldUpdateAggregateWhenOrganisationDefendantChanged() throws EventStreamException, IOException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ProsecutionCaseFile.class)).thenReturn(aggregate);
        testOrganisationDefendantChanged(ccCaseReceivedWithOrganisationDefendant, "json/update-case-with-defendant-organisation.json");
    }

    private void testDefendantChanged(final CcCaseReceived ccCaseReceivedWithMandatoryFields, final String eventFile) throws IOException, EventStreamException {
        createCaseAndUpdateDefendant(ccCaseReceivedWithMandatoryFields, eventFile);

        assertThat(aggregate.getDefendants().size(), is(1));
        if(eventFile.equals("json/update-case-with-defendant-without-dateofbirth.json")){
            assertThat(aggregate.getDefendants().get(0).getIndividual().getSelfDefinedInformation().getDateOfBirth().toString(), is("2010-02-01"));
        }else{
            assertThat(aggregate.getDefendants().get(0).getIndividual().getSelfDefinedInformation().getDateOfBirth().toString(), is("2010-01-01"));
        }

        assertThat(aggregate.getDefendants().get(0).getIndividual().getPersonalInformation().getTitle(), is("DR"));
        assertThat(aggregate.getDefendants().get(0).getIndividual().getPersonalInformation().getFirstName(), is("updatedName"));
        assertThat(aggregate.getDefendants().get(0).getIndividual().getPersonalInformation().getGivenName2(), is("givenName2"));
        assertThat(aggregate.getDefendants().get(0).getIndividual().getPersonalInformation().getGivenName3(), is("givenName3"));
        assertThat(aggregate.getDefendants().get(0).getIndividual().getPersonalInformation().getLastName(), is("Kane Junior"));
    }

    private void testOrganisationDefendantChanged(final CcCaseReceived ccCaseReceivedWithMandatoryFields, final String eventFile) throws IOException, EventStreamException {
        createCaseAndUpdateDefendant(ccCaseReceivedWithMandatoryFields, eventFile);

        assertThat(aggregate.getDefendants().size(), is(1));

        assertThat(aggregate.getDefendants().get(0).getOrganisationName(), is("org2"));
    }

    private void createCaseAndUpdateDefendant(final CcCaseReceived ccCaseReceivedWithMandatoryFields, final String eventFile) throws IOException, EventStreamException {
        aggregate.apply(ccCaseReceivedWithMandatoryFields);

        final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

        final UpdateCaseWithDefendant caseDefendantChangedCommand =
                handlerTestHelper.convertFromFile(eventFile, UpdateCaseWithDefendant.class);

        final Envelope<UpdateCaseWithDefendant> envelope =
                envelopeFrom(metadataFor("prosecutioncasefile.command.update-case-with-defendant"), caseDefendantChangedCommand);

        defendantChangedHandler.handleUpdateCaseWithDefendant(envelope);
    }

}