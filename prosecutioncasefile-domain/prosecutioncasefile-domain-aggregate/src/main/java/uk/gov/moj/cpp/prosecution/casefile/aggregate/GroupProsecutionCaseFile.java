package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.stream.Stream.builder;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.buildDefendantWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem.caseProblem;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSummonsApplicationRejected;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"java:S1068", "java:S3740"})
public class GroupProsecutionCaseFile implements Aggregate {

    private static final long serialVersionUID = 7500527201134194465L;

    public static final String INITIATION_CODE_CIVIL_CASE = "O";
    public static final String INITIATION_CODE_FOR_SUMMONS = "S";

    private List<GroupProsecution> groupProsecutions;
    private UUID externalId;
    private Channel channel;
    // reference data looked up when a summons group was parked, reused on approval; null for groups
    // not parked for approval and for aggregates restored from a snapshot taken before it was kept
    private GroupProsecutionList parkedGroupProsecutionList;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(GroupCasesReceived.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                            .map(GroupProsecutionWithReferenceData::getGroupProsecution)
                            .toList();
                    this.externalId = e.getGroupProsecutionList().getExternalId();
                    this.channel = e.getGroupProsecutionList().getChannel();
                    this.parkedGroupProsecutionList = null;
                }),
                when(GroupProsecutionRejected.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutions();
                    this.externalId = e.getExternalId();
                    this.channel = e.getChannel();
                    this.parkedGroupProsecutionList = null;
                }),
                when(GroupCasesParkedForApproval.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                            .map(GroupProsecutionWithReferenceData::getGroupProsecution)
                            .toList();
                    this.externalId = e.getGroupProsecutionList().getExternalId();
                    this.channel = e.getGroupProsecutionList().getChannel();
                    this.parkedGroupProsecutionList = e.getGroupProsecutionList();
                }),
                otherwiseDoNothing());
    }

    /**
     * Applies the events decided by {@link GroupProsecutionReceiver#evaluate}, which runs before
     * this aggregate is loaded so the event-store connection is not held during enrichment.
     */
    public Stream<Object> receiveGroupProsecution(final List<Object> events) {
        return apply(events.stream());
    }

    /**
     * Reuses the reference data looked up when the group was parked, so approving a large group makes no
     * REST calls while the event-store connection is open. Enriches again only when that data isn't held.
     */
    public Stream<Object> approveGroupProsecution(final List<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers){
        final GroupProsecutionList groupProsecutionList = this.parkedGroupProsecutionList != null
                ? this.parkedGroupProsecutionList
                : enrichGroupProsecutions(groupCasesReferenceDataEnrichers, defendantRefDataEnrichers);

        return apply(builder().add(GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(groupProsecutionList).build())
                .build());
    }

    private GroupProsecutionList enrichGroupProsecutions(final List<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers) {
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = this.groupProsecutions.stream()
                .map(GroupProsecutionWithReferenceData::new)
                .toList();

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = groupProsecutionWithReferenceDataList.stream()
                .map(groupProsecutionWithReferenceData -> convertToProsecutionWithReferenceData(groupProsecutionWithReferenceData, this.channel, this.externalId))
                .toList();

        groupCasesReferenceDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceDataList));


        buildDefendantsWithReferenceDataByCaseId(groupProsecutionWithReferenceDataList, this.channel, this.externalId, defendantRefDataEnrichers);

        return new GroupProsecutionList(groupProsecutionWithReferenceDataList, this.externalId, this.channel);
    }


    public Stream<Object> rejectGroupProsecution() {
        final Stream.Builder<Object> builder = builder();
        builder.add(GroupSummonsApplicationRejected.groupSummonsApplicationRejected()
                .withGroupId(this.groupProsecutions.get(0).getGroupId())
                .withChannel(this.channel)
                .withExternalId(this.externalId)
                .build());
        return apply(builder.build());
    }

    public Stream<Object> rejectGroupProsecution(final List<Problem> problems) {
        return apply(builder().add(buildGroupProsecutionRejected(problems)).build());
    }

    private GroupProsecutionRejected buildGroupProsecutionRejected(final List<Problem> problems) {
        return GroupProsecutionRejected.groupProsecutionRejected()
                .withGroupProsecutions(this.groupProsecutions)
                .withChannel(this.channel)
                .withExternalId(this.externalId)
                .withGroupCaseErrors(toGroupCaseErrors(problems))
                .build();
    }

    /**
     * Group-level problems aren't tied to any one case, so prosecutorCaseReference is always null here —
     * unlike caseProblems, which are grouped per case reference. A null input (no problems given, e.g. the
     * no-arg rejectGroupProsecution()) returns null so the field is omitted from the event rather than
     * published as an empty list, preserving the previous wire shape for that path.
     */
    static List<CaseProblem> toGroupCaseErrors(final List<Problem> groupProblems) {
        if (groupProblems == null) {
            return null;
        }
        if (groupProblems.isEmpty()) {
            return new ArrayList<>();
        }
        final List<CaseProblem> groupCaseErrors = new ArrayList<>();
        groupCaseErrors.add(caseProblem()
                .withProblems(groupProblems)
                .withProsecutorCaseReference(null)
                .build());
        return groupCaseErrors;
    }

    static Map<UUID, DefendantsWithReferenceData> buildDefendantsWithReferenceDataByCaseId(final List<GroupProsecutionWithReferenceData> groupProsecutions, final Channel channel, final UUID externalId, final List<DefendantRefDataEnricher> defendantRefDataEnrichers) {
        final List<DefendantsWithReferenceData> defendantsWithReferenceDataList = groupProsecutions.stream()
                .map(prosecutionWithReferenceData -> buildDefendantWithReferenceData(convertToProsecutionWithReferenceData(prosecutionWithReferenceData, channel, externalId), defendantRefDataEnrichers))
                .toList();

        defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceDataList));

        final Map<UUID, DefendantsWithReferenceData> defendantsWithReferenceDataMap = new HashMap<>();
        defendantsWithReferenceDataList.forEach(defendantsWithReferenceData -> {
            final UUID prosecutionCaseId = defendantsWithReferenceData.getCaseDetails().getCaseId();
            if (!defendantsWithReferenceDataMap.containsKey(prosecutionCaseId)) {
                defendantsWithReferenceDataMap.put(prosecutionCaseId, defendantsWithReferenceData);
            }
        });
        return defendantsWithReferenceDataMap;
    }

    static ProsecutionWithReferenceData convertToProsecutionWithReferenceData(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final Channel channel, final UUID externalId) {
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails())
                .withDefendants(groupProsecutionWithReferenceData.getGroupProsecution().getDefendants())
                .withChannel(channel)
                .withIsCivil(groupProsecutionWithReferenceData.getGroupProsecution().getIsCivil())
                .build();

        return new ProsecutionWithReferenceData(prosecution, groupProsecutionWithReferenceData.getReferenceDataVO(), externalId);
    }

    public Stream<Object> acceptGroupCases(final UUID groupId) {
        final Stream.Builder<Object> builder = builder();
        builder.accept(GroupCasesCreatedSuccessfully.groupCasesCreatedSuccessfully()
                .withExternalId(this.externalId)
                .withGroupId(groupId)
                .build()
        );
        return apply(builder.build());
    }
}
