package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.inject.Instance;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_CIVIL_CASE;

@ExtendWith(MockitoExtension.class)
public class GroupProsecutionHandlerTest {

    private static final String INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA = "prosecutioncasefile.command.initiate-group-prosecution-with-reference-data";
    private static final String COURT_HEARING_LOCATION = "C55BN00";

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream eventStream;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private Instance<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers;

    @Mock
    private GroupCasesReferenceDataEnricher groupCasesReferenceDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private GroupProsecutionHandler groupProsecutionHandler;

    @Test
    public void shouldHandleInitiateGroupProsecutionWithReferenceData() {
        assertThat(groupProsecutionHandler, isHandler(COMMAND_HANDLER)
                .with(method("handleInitiateGroupProsecutionWithReferenceData")
                        .thatHandles(INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA)
                ));
    }

    @Test
    public void shouldEnrichGroupCasesBeforeLoadingTheGroupEventStream() throws EventStreamException {
        final UUID groupId = randomUUID();
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(List.of(buildMasterCase(groupId)));
        groupProsecutionList.setChannel(Channel.CIVIL);

        when(groupCasesReferenceDataEnrichers.iterator()).thenReturn(List.of(groupCasesReferenceDataEnricher).iterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(COURT_HEARING_LOCATION))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));
        when(eventSource.getStreamById(groupId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, GroupProsecutionCaseFile.class)).thenReturn(new GroupProsecutionCaseFile());

        groupProsecutionHandler.handleInitiateGroupProsecutionWithReferenceData(
                Envelope.envelopeFrom(metadataWithRandomUUID(INITIATE_GROUP_PROSECUTION_WITH_REFERENCE_DATA), groupProsecutionList));

        final InOrder inOrder = inOrder(groupCasesReferenceDataEnricher, eventSource, aggregateService, eventStream);
        inOrder.verify(groupCasesReferenceDataEnricher).enrich(anyList());
        inOrder.verify(eventSource).getStreamById(groupId);
        inOrder.verify(aggregateService).get(eventStream, GroupProsecutionCaseFile.class);
        inOrder.verify(eventStream).append(any());
    }

    private GroupProsecutionWithReferenceData buildMasterCase(final UUID groupId) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(asList(INITIATION_CODE_CIVIL_CASE));

        final GroupProsecutionWithReferenceData masterCase = new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMaster(true)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withInitiationCode(INITIATION_CODE_CIVIL_CASE)
                        .withProsecutorCaseReference("URN1")
                        .build())
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                        .withIndividual(Individual.individual()
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation()
                                        .withDateOfBirth(LocalDate.now().minusYears(30))
                                        .build())
                                .build())
                        .withOffences(asList(Offence.offence()
                                .withOffenceId(randomUUID())
                                .withOffenceCode("998A")
                                .withChargeDate(LocalDate.now().minusDays(2))
                                .withOffenceCommittedDate(LocalDate.now().minusDays(2))
                                .withStatementOfFacts("statements")
                                .build()))
                        .withInitialHearing(InitialHearing.initialHearing()
                                .withCourtHearingLocation(COURT_HEARING_LOCATION)
                                .withDateOfHearing(LocalDate.now().plusDays(2).toString())
                                .withTimeOfHearing("09:05:01.001")
                                .build())
                        .build()))
                .build());
        masterCase.setReferenceDataVO(referenceDataVO);
        return masterCase;
    }
}
