package uk.gov.moj.cpp.prosecution.casefile.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyIterator;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.prosecution.casefile.test.utils.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.ApproveCaseDefendantsAsSummonsApplicationApproved.approveCaseDefendantsAsSummonsApplicationApproved;
import static uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectCaseDefendantsAsSummonsApplicationRejected.rejectCaseDefendantsAsSummonsApplicationRejected;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCivilApplication;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecution.casefile.aggregate.ProsecutionCaseFile;
import uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails;
import uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationRejectedDetails;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.ApproveCaseDefendantsAsSummonsApplicationApproved;
import uk.gov.moj.cps.prosecutioncasefile.command.handler.RejectCaseDefendantsAsSummonsApplicationRejected;

import java.util.UUID;

import javax.enterprise.inject.Instance;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SummonsApplicationHandlerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final Boolean PERSONAL_SERVICE = BOOLEAN.next();
    private static final Boolean SUMMONS_SUPPRESSED = BOOLEAN.next();
    private static final String PROSECUTOR_COST = "£600";
    private static final String REJECTION_REASON_1 = STRING.next();
    private static final String REJECTION_REASON_2 = STRING.next();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseFile aggregate;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Captor
    private ArgumentCaptor<SummonsApplicationApprovedDetails> summonsApplicationApprovedArgumentCaptor;

    @Captor
    private ArgumentCaptor<SummonsApplicationRejectedDetails> summonsApplicationRejectedArgumentCaptor;

    @InjectMocks
    private final SummonsApplicationHandler summonsApplicationHandler = new SummonsApplicationHandler();

    @Test
    public void shouldHandleApproveCaseDefendantsAsSummonsApplicationApproved() {
        assertThat(summonsApplicationHandler, isHandler(COMMAND_HANDLER)
                .with(method("approveCaseDefendantsAsSummonsApplicationApproved")
                        .thatHandles("prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved")
                ));
    }

    @Test
    public void shouldCallAggregateToApproveCaseDefendants() throws EventStreamException {
        given(eventSource.getStreamById(CASE_ID)).willReturn(eventStream);
        given(aggregateService.get(eventStream, ProsecutionCaseFile.class)).willReturn(aggregate);
        given(caseRefDataEnrichers.iterator()).willReturn(emptyIterator());
        given(defendantRefDataEnrichers.iterator()).willReturn(emptyIterator());
        given(progressionService.getApplicationOnly(any())).willReturn(CourtApplication.courtApplication()
                        .withCourtCivilApplication(CourtCivilApplication.courtCivilApplication().withIsCivil(true).build())
                .withType(CourtApplicationType.courtApplicationType().build()).build());
        given(aggregate.approveCaseDefendants(any(SummonsApplicationApprovedDetails.class), anyList(), anyList(), anyBoolean())).willReturn(empty());

        final Envelope<ApproveCaseDefendantsAsSummonsApplicationApproved> envelope = envelopeFrom(metadataFor(
                "prosecutioncasefile.command.approve-case-defendants-as-summons-application-approved"), buildSummonsApplicationApprovedCommandPayload());

        summonsApplicationHandler.approveCaseDefendantsAsSummonsApplicationApproved(envelope);

        verify(aggregate).approveCaseDefendants(summonsApplicationApprovedArgumentCaptor.capture(), eq(newArrayList(emptyIterator())), eq(newArrayList(emptyIterator())), eq(true));
        final SummonsApplicationApprovedDetails result = summonsApplicationApprovedArgumentCaptor.getValue();
        assertThat(result.getCaseId(), is(CASE_ID));
        assertThat(result.getApplicationId(), is(APPLICATION_ID));
        assertThat(result.getSummonsApprovedOutcome().getPersonalService(), is(PERSONAL_SERVICE));
        assertThat(result.getSummonsApprovedOutcome().getSummonsSuppressed(), is(SUMMONS_SUPPRESSED));
        assertThat(result.getSummonsApprovedOutcome().getProsecutorCost(), is(PROSECUTOR_COST));
    }

    @Test
    public void shouldCallAggregateToRejectCaseDefendants() throws EventStreamException {
        given(eventSource.getStreamById(CASE_ID)).willReturn(eventStream);
        given(aggregateService.get(eventStream, ProsecutionCaseFile.class)).willReturn(aggregate);
        given(aggregate.rejectCaseDefendants(any(SummonsApplicationRejectedDetails.class))).willReturn(empty());
        final Envelope<RejectCaseDefendantsAsSummonsApplicationRejected> envelope = envelopeFrom(metadataFor(
                "prosecutioncasefile.command.reject-case-defendants-as-summons-application-rejected"), buildSummonsApplicationRejectedCommandPayload());

        summonsApplicationHandler.rejectCaseDefendantsAsSummonsApplicationRejected(envelope);

        verify(aggregate).rejectCaseDefendants(summonsApplicationRejectedArgumentCaptor.capture());
        final SummonsApplicationRejectedDetails result = summonsApplicationRejectedArgumentCaptor.getValue();
        assertThat(result.getCaseId(), is(CASE_ID));
        assertThat(result.getApplicationId(), is(APPLICATION_ID));
        assertThat(result.getSummonsRejectedOutcome().getReasons(), hasItems(REJECTION_REASON_1, REJECTION_REASON_2));
    }

    private ApproveCaseDefendantsAsSummonsApplicationApproved buildSummonsApplicationApprovedCommandPayload() {
        return approveCaseDefendantsAsSummonsApplicationApproved()
                .withApplicationId(APPLICATION_ID)
                .withCaseId(CASE_ID)
                .withSummonsApprovedOutcome(SummonsApprovedOutcome.summonsApprovedOutcome()
                        .withPersonalService(PERSONAL_SERVICE)
                        .withSummonsSuppressed(SUMMONS_SUPPRESSED)
                        .withProsecutorCost(PROSECUTOR_COST)
                        .build())
                .build();
    }

    private RejectCaseDefendantsAsSummonsApplicationRejected buildSummonsApplicationRejectedCommandPayload() {
        return rejectCaseDefendantsAsSummonsApplicationRejected()
                .withApplicationId(APPLICATION_ID)
                .withCaseId(CASE_ID)
                .withSummonsRejectedOutcome(summonsRejectedOutcome().withReasons(ImmutableList.of(REJECTION_REASON_1, REJECTION_REASON_2)).build())
                .build();
    }
}