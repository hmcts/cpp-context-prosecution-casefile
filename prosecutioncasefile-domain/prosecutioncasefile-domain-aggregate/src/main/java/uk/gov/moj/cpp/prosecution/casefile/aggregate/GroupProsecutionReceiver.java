package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.builder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.validateDefendantErrors;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_CIVIL_CASE;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.INITIATION_CODE_FOR_SUMMONS;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.buildDefendantsWithReferenceDataByCaseId;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.convertToProsecutionWithReferenceData;
import static uk.gov.moj.cpp.prosecution.casefile.aggregate.GroupProsecutionCaseFile.toGroupCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem.caseProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getCaseValidationRulesForCivil;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getGroupCasesValidationRules;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesReceived;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.GroupCasesReferenceDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupProsecutionRejected;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enriches and validates a received group prosecution and decides which events it raises.
 * Reads no aggregate state and uses no event-store connection, so it can run before the
 * group aggregate is loaded; the events are then applied with
 * {@link GroupProsecutionCaseFile#receiveGroupProsecution(List)}.
 */
@SuppressWarnings({"java:S3740"})
public final class GroupProsecutionReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupProsecutionReceiver.class);

    private GroupProsecutionReceiver() {
    }

    public static List<Object> evaluate(final GroupProsecutionList groupProsecutionList, final List<GroupCasesReferenceDataEnricher> groupCasesReferenceDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService) {

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
            return emptyList();
        }

        final List<Problem> groupCaseProblems = validate(groupProsecutionList, referenceDataQueryService, getGroupCasesValidationRules());

        LOGGER.info("groupCaseProblems validated for submissionId {} with count {} ", groupProsecutionList.getExternalId(), groupCaseProblems.size());

        final List<CaseProblem> caseProblems = new ArrayList<>();

        final List<ProsecutionWithReferenceData> prosecutionWithReferenceDataList = groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .map(groupProsecutionWithReferenceData -> convertToProsecutionWithReferenceData(groupProsecutionWithReferenceData, groupProsecutionList.getChannel(), groupProsecutionList.getExternalId()))
                .toList();

        groupCasesReferenceDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceDataList));

        prosecutionWithReferenceDataList.forEach(prosecutionWithReferenceData -> {
            final List<Problem> problemsForCase = validate(prosecutionWithReferenceData, referenceDataQueryService, getCaseValidationRulesForCivil(prosecutionWithReferenceData.getProsecution().getCaseDetails().getInitiationCode()));
            if (isNotEmpty(problemsForCase)) {
                caseProblems.add(caseProblem()
                        .withProblems(problemsForCase)
                        .withProsecutorCaseReference(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference())
                        .build());
            }
        });
        LOGGER.info("caseProblems validated for submissionId {} with count {} ", groupProsecutionList.getExternalId(), caseProblems.size());
        final List<DefendantProblem> defendantProblems = validateDefendants(groupProsecutionList, defendantRefDataEnrichers, referenceDataQueryService, builder, isCivil);

        final boolean hasErrors = isNotEmpty(groupCaseProblems) || isNotEmpty(caseProblems) || isNotEmpty(defendantProblems);

        if (hasErrors) {
            return builder.add(GroupProsecutionRejected.groupProsecutionRejected()
                    .withGroupProsecutions(groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream().map(GroupProsecutionWithReferenceData::getGroupProsecution).toList())
                    .withChannel(groupProsecutionList.getChannel())
                    .withGroupCaseErrors(toGroupCaseErrors(groupCaseProblems))
                    .withCaseErrors(caseProblems)
                    .withDefendantErrors(defendantProblems)
                    .withExternalId(groupProsecutionList.getExternalId()).build())
                    .build().toList();
        }
        LOGGER.info("defendantProblems validated for submission Id {} with count {} ", groupProsecutionList.getExternalId(), defendantProblems.size());
        if (Objects.equals(receivedInitiationCode, INITIATION_CODE_FOR_SUMMONS)) {
            LOGGER.info("GroupCasesParkedForApproval for groupId {} and submissionId {} ", masterCaseData.get().getGroupProsecution().getGroupId(), groupProsecutionList.getExternalId());
            return builder.add(GroupCasesParkedForApproval.groupCasesParkedForApproval()
                    .withApplicationId(UUID.randomUUID())
                    .withGroupProsecutionList(groupProsecutionList).build())
                    .build().toList();

        } else if (Objects.equals(receivedInitiationCode, INITIATION_CODE_CIVIL_CASE)) {
            LOGGER.info("GroupCasesReceived for groupId {}  with  submissionId {} and masterCaseId {}", masterCaseData.get().getGroupProsecution().getGroupId(), groupProsecutionList.getExternalId(), masterCaseData.get().getGroupProsecution().getCaseDetails().getCaseId());
            return builder.add(GroupCasesReceived.groupCasesReceived()
                    .withGroupProsecutionList(groupProsecutionList)
                    .withDefendantWarnings(defendantProblems).build())
                    .build().toList();
        }

        return emptyList();
    }

    private static List<DefendantProblem> validateDefendants(final GroupProsecutionList groupProsecutionList, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService, final Stream.Builder builder, final Boolean isCivil) {
        final Map<UUID, DefendantsWithReferenceData> defendantsWithReferenceDataMap = buildDefendantsWithReferenceDataByCaseId(groupProsecutionList.getGroupProsecutionWithReferenceDataList(), groupProsecutionList.getChannel(), groupProsecutionList.getExternalId(), defendantRefDataEnrichers);

        return groupProsecutionList.getGroupProsecutionWithReferenceDataList().stream()
                .flatMap(groupProsecutionWithReferenceData -> {
                    final DefendantsWithReferenceData defendantsWithReferenceData = defendantsWithReferenceDataMap.get(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails().getCaseId());
                    final List<DefendantProblem> errors = validateDefendantErrors(groupProsecutionWithReferenceData.getGroupProsecution().getCaseDetails(), groupProsecutionList.getChannel(), defendantsWithReferenceData, referenceDataQueryService, builder, Boolean.TRUE, false, false, isCivil);
                    return errors.stream();
                }).toList();
    }
}
