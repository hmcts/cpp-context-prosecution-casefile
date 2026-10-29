package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ApproveCaseDefendantsAsSummonsApplicationApproved;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_CIVIL_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_FOR_SUMMONS;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.ApproveCaseDefendantsAsSummonsApplicationApproved.approveCaseDefendantsAsSummonsApplicationApproved;

/**
 * Reproduces the ste19 failure for large civil group uploads without a database:
 * "FATAL: terminating connection due to idle-in-transaction timeout".
 *
 * A simulated clock stands in for wall time. Reading an aggregate counts as event-store activity
 * (it opens the connection inside the JMS transaction); each reference data enrichment advances the
 * clock as a REST call per case would. Appending after the connection has been idle longer than the
 * Postgres limit fails the same way the real event store does.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GroupCaseIdleInTransactionTest {

    private static final long IDLE_IN_TRANSACTION_TIMEOUT_MILLIS = 60_000;
    private static final long REFERENCE_DATA_CALL_MILLIS_PER_CASE = 200;
    private static final int GROUP_SIZE = 1000;
    private static final String COURT_HEARING_LOCATION = "C55BN00";

    private final AtomicLong clock = new AtomicLong();
    private final AtomicLong lastEventStoreActivity = new AtomicLong(-1);

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private Instance<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers;

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    private GroupCasesReferenceDataEnricher groupCasesReferenceDataEnricher;

    @Mock
    private DefendantRefDataEnricher defendantRefDataEnricher;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private GroupProsecutionHandler groupProsecutionHandler;

    @InjectMocks
    private SummonsApplicationHandler summonsApplicationHandler;

    private final UUID groupId = randomUUID();

    @BeforeEach
    public void setUp() {
        when(groupCasesReferenceDataEnrichers.iterator()).thenAnswer(i -> List.of(groupCasesReferenceDataEnricher).iterator());
        when(defendantRefDataEnrichers.iterator()).thenAnswer(i -> List.of(defendantRefDataEnricher).iterator());
        when(caseRefDataEnrichers.iterator()).thenAnswer(i -> Collections.emptyIterator());
        doAnswer(i -> advanceClockForCases(i.getArgument(0))).when(groupCasesReferenceDataEnricher).enrich(anyList());
        doAnswer(i -> advanceClockForCases(i.getArgument(0))).when(defendantRefDataEnricher).enrich(anyList());
        when(referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(COURT_HEARING_LOCATION))
                .thenReturn(Optional.of(OrganisationUnitWithCourtroomReferenceData.organisationUnitWithCourtroomReferenceData().build()));
    }

    @Test
    public void shouldReceiveLargeCivilGroupWithoutHoldingTheEventStoreConnectionIdle() throws EventStreamException {
        final EventStream groupStream = eventStoreStream(groupId);
        aggregateRead(groupStream, GroupProsecutionCaseFile.class, new GroupProsecutionCaseFile());

        assertDoesNotThrow(() -> groupProsecutionHandler.handleInitiateGroupProsecutionWithReferenceData(
                envelopeFrom(metadataWithRandomUUID("prosecutioncasefile.command.initiate-group-prosecution-with-reference-data"),
                        groupProsecutionList(INITIATION_CODE_CIVIL_CASE))));
    }

    @Test
    public void shouldApproveLargeSummonsGroupWithoutHoldingTheEventStoreConnectionIdle() throws EventStreamException {
        final UUID masterCaseId = randomUUID();
        final UUID applicationId = randomUUID();

        final ProsecutionCaseFile masterCase = mock(ProsecutionCaseFile.class);
        when(masterCase.getGroupId()).thenReturn(groupId);
        aggregateRead(eventStoreStream(masterCaseId), ProsecutionCaseFile.class, masterCase);

        final GroupProsecutionCaseFile parkedGroup = new GroupProsecutionCaseFile();
        parkedGroup.apply(GroupCasesParkedForApproval.groupCasesParkedForApproval()
                .withApplicationId(applicationId)
                .withGroupProsecutionList(groupProsecutionList(INITIATION_CODE_FOR_SUMMONS))
                .build());
        aggregateRead(eventStoreStream(groupId), GroupProsecutionCaseFile.class, parkedGroup);

        when(progressionService.getApplicationOnly(applicationId)).thenReturn(CourtApplication.courtApplication()
                .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication().withIsCivil(true).build())
                .withType(CourtApplicationType.courtApplicationType().build())
                .build());

        final Envelope<ApproveCaseDefendantsAsSummonsApplicationApproved> envelope = envelopeFrom(
                metadataWithRandomUUID("prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved"),
                approveCaseDefendantsAsSummonsApplicationApproved()
                        .withCaseId(masterCaseId)
                        .withApplicationId(applicationId)
                        .withSummonsApprovedOutcome(SummonsApprovedOutcome.summonsApprovedOutcome().build())
                        .build());

        assertDoesNotThrow(() -> summonsApplicationHandler.approveCaseDefendantsAsSummonsApplicationApproved(envelope));
    }

    private Object advanceClockForCases(final List<?> cases) {
        clock.addAndGet(cases.size() * REFERENCE_DATA_CALL_MILLIS_PER_CASE);
        return null;
    }

    private EventStream eventStoreStream(final UUID streamId) throws EventStreamException {
        final EventStream stream = mock(EventStream.class);
        when(eventSource.getStreamById(streamId)).thenReturn(stream);
        doAnswer(i -> {
            final long idleMillis = clock.get() - lastEventStoreActivity.get();
            if (lastEventStoreActivity.get() >= 0 && idleMillis > IDLE_IN_TRANSACTION_TIMEOUT_MILLIS) {
                throw new EventStreamException("Exception while reading stream " + streamId
                        + ": FATAL: terminating connection due to idle-in-transaction timeout (idle for " + idleMillis + "ms)");
            }
            final Stream<?> events = i.getArgument(0);
            events.count();
            lastEventStoreActivity.set(clock.get());
            return null;
        }).when(stream).append(any());
        return stream;
    }

    private <T extends Aggregate> void aggregateRead(final EventStream stream, final Class<T> aggregateClass, final T aggregate) {
        when(aggregateService.get(stream, aggregateClass)).thenAnswer(i -> {
            lastEventStoreActivity.set(clock.get());
            return aggregate;
        });
    }

    private GroupProsecutionList groupProsecutionList(final String initiationCode) {
        final List<GroupProsecutionWithReferenceData> cases = new ArrayList<>();
        for (int i = 0; i < GROUP_SIZE; i++) {
            cases.add(groupCase(initiationCode, i == 0, "URN" + i));
        }
        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(cases);
        groupProsecutionList.setChannel(Channel.CIVIL);
        return groupProsecutionList;
    }

    private GroupProsecutionWithReferenceData groupCase(final String initiationCode, final boolean isGroupMaster, final String prosecutorCaseReference) {
        final ReferenceDataVO referenceDataVO = new ReferenceDataVO();
        referenceDataVO.setInitiationTypes(asList(initiationCode));

        final GroupProsecutionWithReferenceData groupCase = new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMaster(isGroupMaster)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(randomUUID())
                        .withInitiationCode(initiationCode)
                        .withSummonsCode("A")
                        .withProsecutorCaseReference(prosecutorCaseReference)
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
        groupCase.setReferenceDataVO(referenceDataVO);
        return groupCase;
    }
}
