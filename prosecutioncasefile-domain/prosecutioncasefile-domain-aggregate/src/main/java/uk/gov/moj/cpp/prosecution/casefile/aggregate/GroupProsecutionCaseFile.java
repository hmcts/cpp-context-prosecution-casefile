package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Optional.*;
import static java.util.stream.Stream.builder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.buildDefendantWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.validateDefendantErrors;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getGroupCasesValidationRules;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupCasesCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"java:S1068", "java:S3740"})
public class GroupProsecutionCaseFile implements Aggregate {

    private static final long serialVersionUID = 7500527201134194465L;

    public static final String INITIATION_CODE_CIVIL_CASE = "O";
    public static final String INITIATION_CODE_FOR_SUMMONS = "S";
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupProsecutionCaseFile.class);

    private List<GroupProsecution> groupProsecutions;
    private UUID externalId;
    private Channel channel;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(GroupCasesReceived.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                            .map(GroupProsecutionWithReferenceData::getGroupProsecution)
                            .toList();
                    this.externalId = e.getGroupProsecutionList().getExternalId();
                    this.channel = e.getGroupProsecutionList().getChannel();
                }),
                when(GroupProsecutionRejected.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutions();
                    this.externalId = e.getExternalId();
                    this.channel = e.getChannel();
                }),
                when(GroupCasesParkedForApproval.class).apply(e -> {
                    this.groupProsecutions = e.getGroupProsecutionList().getGroupProsecutionWithReferenceDataList().stream()
                            .map(GroupProsecutionWithReferenceData::getGroupProsecution)
                            .toList();
                    this.externalId = e.getGroupProsecutionList().getExternalId();
                    this.channel = e.getGroupProsecutionList().getChannel();

                }),
                otherwiseDoNothing());
    }

    public Stream<Object> receiveGroupProsecution(final GroupProsecutionList groupProsecutionList, final List<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService) {

        final Stream.Builder<Object> builder = builder();

        final Optional<GroupProsecutionWithReferenceData> masterCaseData = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .filter(p -> p.getGroupProsecution().getIsGroupMaster())
                .findFirst();
        final Boolean isCivil = masterCaseData.isPresent() && ofNullable(masterCaseData.get().getGroupProsecution().getIsCivil()).orElse(false);
        String receivedInitiationCode = null;
        if (masterCaseData.isPresent() && masterCaseData.get().getGroupProsecution() != null &&
                masterCaseData.get().getGroupProsecution().getCaseDetails() != null &&
                masterCaseData.get().getGroupProsecution().getCaseDetails().getInitiationCode() != null) {

            receivedInitiationCode = masterCaseData.get().getGroupProsecution().getCaseDetails().getInitiationCode();
            LOGGER.info("receivedInitiationCode {} for submissionId {}  ", receivedInitiationCode, groupProsecutionList.getExternalId());
        } else {
            LOGGER.error("Cannot proceed as group cases need to have a master case.");
            return builder.build();
        }

        final List<Problem> groupCaseProblems = validate(groupProsecutionList, referenceDataQueryService, getGroupCasesValidationRules());

        LOGGER.info("groupCaseProblems validated for submissionId {} with count {} ", groupProsecutionList.getExternalId(), groupCaseProblems.size());

        final List<Problem> caseProblems = new ArrayList<>();

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .map(groupProsecutionWithReferenceData -> convertToProsecutionWithReferenceData(groupProsecutionWithReferenceData, groupProsecutionList.getChannel(), groupProsecutionList.getExternalId()))
                .toList();

        groupCasesReferenceDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceDataList));

        prosecutionWithReferenceDataList.forEach(
                prosecutionWithReferenceData ->
                    caseProblems.addAll(validate(prosecutionWithReferenceData, referenceDataQueryService, getCaseValidationRulesForCivil(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode())))
        );
        LOGGER.info("caseProblems validated for submissionId {} with count {} ", groupProsecutionList.getExternalId(), caseProblems.size());
        final List<DefendantProblem> defendantProblems = validateDefendants(groupProsecutionList, defendantRefDataEnrichers, referenceDataQueryService, builder,isCivil);

        final boolean hasErrors = isNotEmpty(groupCaseProblems) || isNotEmpty(caseProblems) || isNotEmpty(defendantProblems);

        if (hasErrors) {
            return apply(builder.add(GroupProsecutionRejected.groupProsecutionRejected()
                    .withGroupProsecutions(groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream().map(GroupProsecutionWithReferenceData::getGroupProsecution).toList())
                    .withChannel(groupProsecutionList.getChannel())
                    .withGroupCaseErrors(groupCaseProblems)
                    .withCaseErrors(caseProblems)
                    .withDefendantErrors(defendantProblems)
                    .withExternalId(groupProsecutionList.getExternalId()).build())
                    .build());
        }
        LOGGER.info("defendantProblems validated for submission Id {} with count {} ", groupProsecutionList.getExternalId(), defendantProblems.size());
        if (Objects.equals(receivedInitiationCode, INITIATION_CODE_FOR_SUMMONS)) {
            LOGGER.info("GroupCasesParkedForApproval for groupId {} and submissionId {} ", masterCaseData.get().getGroupProsecution().getGroupId(), groupProsecutionList.getExternalId());
            return apply(builder.add(GroupCasesParkedForApproval.groupCasesParkedForApproval()
                    .withApplicationId(UUID.randomUUID())
                    .withGroupProsecutionList(groupProsecutionList).build())
                    .build());

        } else if (Objects.equals(receivedInitiationCode, INITIATION_CODE_CIVIL_CASE)) {
            LOGGER.info("GroupCasesReceived for groupId {}  with  submissionId {} and masterCaseId {}", masterCaseData.get().getGroupProsecution().getGroupId(), groupProsecutionList.getExternalId(), masterCaseData.get().getGroupProsecution().getCaseDetails().getCaseId());
            return apply(builder.add(GroupCasesReceived.groupCasesReceived()
                    .withGroupProsecutionList(groupProsecutionList)
                    .withDefendantWarnings(defendantProblems).build())
                    .build());
        }

        return Stream.empty();

    }

    public Stream<Object> approveGroupProsecution(final List<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers){
        final List<GroupProsecutionWithReferenceData> groupProsecutionWithReferenceDataList = this.groupProsecutions.stream()
                .map(GroupProsecutionWithReferenceData::new)
                .toList();

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = groupProsecutionWithReferenceDataList.stream()
                .map(groupProsecutionWithReferenceData -> convertToProsecutionWithReferenceData(groupProsecutionWithReferenceData, this.channel, this.externalId))
                .toList();

        groupCasesReferenceDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceDataList));


        buildDefendantsWithReferenceDataByCaseId(groupProsecutionWithReferenceDataList, this.channel, this.externalId, defendantRefDataEnrichers);

        final GroupProsecutionList groupProsecutionList = new GroupProsecutionList(groupProsecutionWithReferenceDataList, this.externalId, this.channel);

        return apply(builder().add(GroupCasesReceived.groupCasesReceived()
                .withGroupProsecutionList(groupProsecutionList).build())
                .build());
    }


    public Stream<Object> rejectGroupProsecution() {
        return rejectGroupProsecution(null);
    }

    public Stream<Object> rejectGroupProsecution(final List<Problem> problems) {
        return apply(builder().add(GroupProsecutionRejected.groupProsecutionRejected()
                        .withGroupProsecutions(this.groupProsecutions)
                        .withChannel(this.channel)
                        .withExternalId(this.externalId)
                        .withGroupCaseErrors(problems)
                        .build())
                .build()
        );
    }

    private List<DefendantProblem> validateDefendants(final GroupProsecutionList groupProsecutionList, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService, final Stream.Builder builder,final Boolean isCivil) {
        final Map<UUID, DefendantsWithReferenceData> defendantsWithReferenceDataMap = buildDefendantsWithReferenceDataByCaseId(groupProsecutionList.getGroupProsecutionWithReferenceDataList(), groupProsecutionList.getChannel(), groupProsecutionList.getExternalId(), defendantRefDataEnrichers);

        return groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .flatMap(groupProsecutionWithReferenceData -> {
                    final DefendantsWithReferenceData defendantsWithReferenceData = defendantsWithReferenceDataMap.get(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails().getCaseId());
                    final List<DefendantProblem> errors = validateDefendantErrors(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails(), groupProsecutionList.getChannel(), defendantsWithReferenceData, referenceDataQueryService, builder, Boolean.TRUE, false, isCivil);
                    return errors.stream();
                }).toList();
    }

    private Map<UUID, DefendantsWithReferenceData> buildDefendantsWithReferenceDataByCaseId(final List<GroupProsecutionWithReferenceData> groupProsecutions, final Channel channel, final UUID externalId, final List<DefendantRefDataEnricher> defendantRefDataEnrichers) {
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

    private ProsecutionWithReferenceData convertToProsecutionWithReferenceData(final GroupProsecutionWithReferenceData groupProsecutionWithReferenceData, final Channel channel, final UUID externalId) {
        final Prosecution prosecution = Prosecution.prosecution()
                .withCaseDetails(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails())
                .withDefendants(groupProsecutionWithReferenceData.getGroupProsecution().getDefendants())
                .withChannel(channel)
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
