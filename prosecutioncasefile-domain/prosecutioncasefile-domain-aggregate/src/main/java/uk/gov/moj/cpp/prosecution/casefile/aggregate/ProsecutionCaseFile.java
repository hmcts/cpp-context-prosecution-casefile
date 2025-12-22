package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.disjoint;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.builder;
import static java.util.stream.Stream.of;
import static javax.json.Json.createArrayBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections.ListUtils.union;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.json.schemas.prosecutioncasefile.events.CaseReceivedWithDuplicateDefendants.caseReceivedWithDuplicateDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.CaseType.CC;
import static uk.gov.moj.cpp.prosecution.casefile.CaseType.SJP;
import static uk.gov.moj.cpp.prosecution.casefile.CaseType.UNKNOWN;
import static uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper.*;
import static uk.gov.moj.cpp.prosecution.casefile.ValidationHelper.buildCaseValidationFailedEvent;
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.PROBLEM_CODE_DOCUMENT_NOT_MATCHED;
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.SOURCE_CPS_FOR_PUBLIC_EVENTS;
import static uk.gov.moj.cpp.prosecution.casefile.domain.DomainConstants.getListOfAllowedDocumentTypes;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived.ccCaseReceived;
import static uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings.ccCaseReceivedWithWarnings;
import static uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval.defendantsParkedForSummonsApplicationApproval;
import static uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded.prosecutionDefendantsAdded;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.MCC;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATED_PROSECUTION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.MATERIAL_EXPIRED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.SUMMONS_APPLICATION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationRuleExecutor.validate;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getCaseValidationRules;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.CcProsecutionValidationRuleProvider.getDefendantValidationRules;
import static uk.gov.moj.cpp.prosecution.casefile.validation.provider.SjpProsecutionWarningRuleProvider.getWarningRules;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.BulkscanMaterialRejected.bulkscanMaterialRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CcProsecutionRejected.ccProsecutionRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched.idpcDefendantMatched;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialReceived.idpcMaterialReceived;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected.idpcMaterialRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded.materialAdded;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected.materialRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported.prosecutionCaseUnsupported;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected.summonsApplicationRejected;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.UploadCaseDocumentRecorded.uploadCaseDocumentRecorded;

import uk.gov.justice.core.courts.CourtDocumentAdded;
import uk.gov.justice.core.courts.SummonsApprovedOutcome;
import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.prosecution.casefile.AssociateIdpcToDefendantHelper;
import uk.gov.moj.cpp.prosecution.casefile.CaseType;
import uk.gov.moj.cpp.prosecution.casefile.DocumentDetails;
import uk.gov.moj.cpp.prosecution.casefile.ProsecutionCaseFileHelper;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocument;
import uk.gov.moj.cpp.prosecution.casefile.domain.CaseDocumentWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantProblemsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationApprovedDetails;
import uk.gov.moj.cpp.prosecution.casefile.domain.SummonsApplicationRejectedDetails;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseEjected;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceived;
import uk.gov.moj.cpp.prosecution.casefile.event.CcCaseReceivedWithWarnings;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantIdpcAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.event.ProsecutionDefendantsAdded;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpCaseAssigned;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpCaseUnAssigned;
import uk.gov.moj.cpp.prosecution.casefile.event.SjpValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.AddMaterialSubmissionV2;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CmsDocumentIdentifier;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtDocument;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DocumentCategory;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.context.ReferenceDataValidationContext;
import uk.gov.moj.cpp.prosecution.casefile.validation.provider.MaterialValidationRuleProvider;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfullyWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDefendantChanged;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseDocumentReviewRequired;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseFiltered;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseReferredToCourtRecorded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseUpdatedWithDefendant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DefendantsReceivedNotAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DocumentType;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupIdRecordedForSummonsApplication;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatchPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcDefendantMatched;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IdpcMaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAdded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialAddedWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPending;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialPendingV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.MaterialRejectedV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaPcqVisitedSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ResolvedCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfullyWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiated;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionInitiatedWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceivedWithWarnings;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionUpdateOffenceCodeRequestReceived;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SummonsApplicationRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ValidationCompleted;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonParser;

@SuppressWarnings("java:S4738")
public class ProsecutionCaseFile implements Aggregate {
    private static final long serialVersionUID = 8155753984190031446L;
    private static final String EXPIRED_AT = "expiredAt";
    private static final String SUMMONS_INITIATION_CODE = "S";
    private static final String SJP_INITIATION_CODE = "J";
    public static final String METLI = "GALMT00";
    public static final String GM_00001 = "GM00001";

    private final List<MaterialPending> pendingMaterials = new ArrayList<>();
    private final List<MaterialPendingV2> pendingMaterialsV2 = new ArrayList<>();
    private final List<MaterialAddedV2> pendingMaterialsForCourtDocumentUpload = new ArrayList<>();
    private final List<MaterialAddedWithWarnings> pendingMaterialsWithWarningsForCourtDocumentUpload = new ArrayList<>();
    private final List<IdpcDefendantMatchPending> pendingIdpcMaterials = new ArrayList<>();
    private final List<String> defendantsWithIdpc = new ArrayList<>();
    private final List<Defendant> defendantsWithHeldAfterCaseReceived = new ArrayList<>();
    private final Map<UUID, List<UUID>> externalIdToDefendantsMap = new HashMap<>();
    private final Map<UUID, List<UUID>> applicationIdToDefendantIdsMap = new HashMap<>();
    private final Map<UUID, List<UUID>> rejectedApplicationIdToDefendantIdsMap = new HashMap<>();
    private boolean prosecutionAccepted;
    private boolean prosecutionReceived;
    private boolean prosecutionFoundWithErrors;
    private CaseType caseType = UNKNOWN;
    private Channel channel;
    private UUID caseId;
    private String prosecutorCaseReference;
    private List<Defendant> defendants = new ArrayList<>();
    private List<Defendant> rejectedApplicationDefendants = new ArrayList<>();
    private List<Problem> warnings = new ArrayList<>();
    private List<DefendantProblem> defendantWarnings = new ArrayList<>();
    private final Map<String, UUID> validDefendantIds = new HashMap<>();
    private CaseDetails caseDetails;
    private boolean caseReferredToCourt;
    private UUID referralReasonId;
    private transient JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private transient ObjectToJsonObjectConverter objectToJsonObjectConverter;
    private boolean allErrorsFixed = true;
    private boolean isCaseAssigned;
    private boolean isCaseEjected;
    private boolean isCaseFiltered;
    private String initiationCode;
    private boolean isSummonsCaseRejected;
    private UUID groupId;

    private static final String OFFENCE_CODE = "GM00001";
    public static final String OFFENCES_CHARGE_DATE = "offence_chargeDate";
    public static final String POSTING_DATE = "postingDate";

    @SuppressWarnings({"squid:S3655", "squid:MethodCyclomaticComplexity", "squid:S3776"})
    public Stream<Object> receiveSjpProsecution(ProsecutionWithReferenceData prosecutionWithReferenceData, final List<CaseRefDataEnricher> caseRefDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService) {
        final Builder<Object> builder = builder();
        final ProsecutionWithReferenceData finalProsecutionWithReferenceData = prosecutionWithReferenceData;
        caseRefDataEnrichers.forEach(x -> x.enrich(finalProsecutionWithReferenceData));
        final ReferenceDataVO referenceDataVO = prosecutionWithReferenceData.getReferenceDataVO();
        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final Channel prosecutionChannel = prosecution.getChannel();
        if (SPI.equals(prosecutionChannel) && (this.prosecutionReceived || prosecution.getDefendants().size() > 1)) {
            builder.accept(prosecutionCaseUnsupported()
                    .withChannel(SPI)
                    .withErrorMessage("Multiple Defendants Found")
                    .withExternalId(prosecutionWithReferenceData.getExternalId())
                    .withPoliceSystemId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId())
                    .withUrn(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference())
                    .build());
            return apply(builder.build());
        }

        if (nonNull(this.caseDetails)) {
            prosecutionWithReferenceData = updateInitiationCodeForNewDefendantsForSpi(prosecutionWithReferenceData);
        }
        final ReferenceDataValidationContext referenceDataValidationContext = ReferenceDataValidationContext.newInstance(
                referenceDataVO.getOffenceReferenceData(),
                referenceDataVO.getCountryNationalityReferenceData()
        );
        final DefendantWithReferenceData defendantWithReferenceData = new DefendantWithReferenceData(prosecution.getDefendants().stream().findAny().get(), prosecutionWithReferenceData.getReferenceDataVO(), prosecution.getCaseDetails());
        final String caseInitiationCode = prosecution.getCaseDetails().getInitiationCode() == null ? SJP_INITIATION_CODE : prosecution.getCaseDetails().getInitiationCode();
        final DefendantsWithReferenceData defendantsWithReferenceData = referenceDataVO.getProsecutorsReferenceData() == null ?
                new DefendantsWithReferenceData(singletonList(prosecution.getDefendants().get(0))) :
                new DefendantsWithReferenceData(singletonList(prosecution.getDefendants().get(0)), referenceDataVO.getProsecutorsReferenceData().getShortName());
        defendantsWithReferenceData.setReferenceDataVO(referenceDataVO);
        defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceData));
        final List<Problem> caseProblems = validate(prosecutionWithReferenceData, referenceDataQueryService, getCaseValidationRules(caseInitiationCode));
        if (this.prosecutionReceived) {
            caseProblems.add(newProblem(DUPLICATED_PROSECUTION, "urn", prosecution.getCaseDetails().getProsecutorCaseReference()));
        }
        final Boolean isCivil = prosecution.getIsCivil();
        final List<Problem> defendantProblems = validate(defendantWithReferenceData, referenceDataQueryService, getDefendantValidationRules(caseInitiationCode, prosecutionChannel,isCivil));
        final List<Problem> rejections = newArrayList(concat(caseProblems, defendantProblems));
        if (!rejections.isEmpty()) {
            builder.accept(new SjpProsecutionRejected(rejections, prosecutionWithReferenceData.getExternalId(), prosecution));
            if (SPI.equals(prosecutionChannel)) {
                final DefendantProblemsVO defendantProblemsVO = new DefendantProblemsVO(defendantWithReferenceData.getDefendant(), defendantProblems);

                final InitialHearing initialHearing = defendantWithReferenceData.getDefendant() != null ? defendantWithReferenceData.getDefendant().getInitialHearing() : null;
                builder.accept(new SjpValidationFailed(prosecution, caseProblems, singletonList(defendantProblemsVO), referenceDataVO, initialHearing));
            }
        } else {
            final List<Problem> validationWarnings = validate(
                    prosecution.getDefendants().get(0),
                    referenceDataValidationContext,
                    getWarningRules());
            if (!validationWarnings.isEmpty()) {
                builder.accept(new SjpProsecutionReceivedWithWarnings(prosecutionWithReferenceData.getExternalId(), prosecution, validationWarnings));
            } else {
                builder.accept(new SjpProsecutionReceived(prosecutionWithReferenceData.getExternalId(), prosecution));
            }
        }
        return apply(builder.build());
    }

    public Stream<Object> pleadOnline(final UUID caseId, final PleadOnline pleadOnline, final ZonedDateTime createdOn, final UUID userId) {
        return apply(of(OnlinePleaSubmitted.onlinePleaSubmitted()
                .withCaseId(caseId)
                .withPleadOnline(pleadOnline)
                .withCreatedBy(userId)
                .withReceivedDateTime(createdOn)
                .build()));
    }

    public Stream<Object> pleadOnlinePcqVisited(final UUID caseId, final PleadOnlinePcqVisited pleadOnlinePcqVisited, final ZonedDateTime createdOn, final UUID userId) {

        return apply(of(OnlinePleaPcqVisitedSubmitted.onlinePleaPcqVisitedSubmitted()
                .withCaseId(caseId)
                .withPleadOnlineVisited(pleadOnlinePcqVisited)
                .withCreatedBy(userId)
                .withReceivedDateTime(createdOn)
                .build()));
    }

    public Stream<Object> receiveErrorCorrections(final JsonObject correctedFields, final ObjectToJsonObjectConverter objectToJsonObjectConverter, final JsonObjectToObjectConverter jsonObjectToObjectConverter, final List<CaseRefDataEnricher> caseRefDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService) {

        final Builder<Object> builder = builder();
        Stream<Object> eventStream = Stream.empty();

        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
        if (prosecutionFoundWithErrors) {
            final JsonObject caseDetailsJsonObject = objectToJsonObjectConverter.convert(caseDetails);
            updateCaseCorrections(correctedFields, caseDetailsJsonObject);

            final List<JsonObject> updatedDefendantsJsonList = updateDefendantCorrections(correctedFields, defendants);
            updatedDefendantsJsonList.stream().map(x -> jsonObjectToObjectConverter.convert(x, Defendant.class)).forEach(x -> replaceErrorCorrectedDefendant(x, defendants));
            final Prosecution prosecution = prosecution()
                    .withCaseDetails(caseDetails)
                    .withChannel(channel)
                    .withDefendants(copyOf(defendants))
                    .build();

            final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution);
            prosecutionWithReferenceData.setExternalId(getExternalIdFromDefendants(defendants));
            if (caseType == SJP) {
                eventStream = receiveSjpProsecution(prosecutionWithReferenceData, caseRefDataEnrichers, defendantRefDataEnrichers, referenceDataQueryService);
            } else {
                eventStream = receiveCCCase(prosecutionWithReferenceData, caseRefDataEnrichers, defendantRefDataEnrichers, referenceDataQueryService, true);
            }
        } else if (!defendantsWithHeldAfterCaseReceived.isEmpty()) {
            final List<JsonObject> updatedDefendantsJsonList = updateDefendantCorrections(correctedFields, defendantsWithHeldAfterCaseReceived);
            updatedDefendantsJsonList.stream().map(x -> jsonObjectToObjectConverter.convert(x, Defendant.class)).forEach(x -> replaceErrorCorrectedDefendant(x, defendantsWithHeldAfterCaseReceived));
            final DefendantsWithReferenceData defendantsWithReferenceData = new DefendantsWithReferenceData(defendantsWithHeldAfterCaseReceived);
            defendantsWithReferenceData.setCaseDetails(this.caseDetails);
            defendantRefDataEnrichers.forEach(x -> x.enrich(defendantsWithReferenceData));
            eventStream = addErrorCorrectedDefendantsForSPI(caseDetails.getCaseId(), getExternalIdFromDefendants(defendantsWithReferenceData.getDefendants()), defendantsWithReferenceData, referenceDataQueryService,false);
        }
        final List<Object> events = eventStream.collect(toList());
        final Stream<Object> resolvedCase = createResolveCase(builder, events, caseDetails.getInitiationCode());
        if (resolvedCase != null) {
            return resolvedCase;
        }
        return Stream.concat(events.stream(), apply(builder.add(new ValidationCompleted(caseId)).build()));
    }

    private Stream<Object> createResolveCase(final Builder<Object> builder, final List<Object> events, final String caseType) {
        if (resolveCaseHasNoErrors(events)) {
            final String courtLocation = retrieveCourtLocation(defendants);
            final ResolvedCase resolvedCase = new ResolvedCase(caseId, caseType, courtLocation, "North West");
            return Stream.concat(events.stream(), apply(builder.add(new ValidationCompleted(caseId)).add(resolvedCase).build()));
        }
        return null;
    }

    private String retrieveCourtLocation(final List<Defendant> defendants) {
        final List<String> courtLocations = new ArrayList<>();
        defendants.forEach(defendant -> courtLocations.add(defendant.getInitialHearing().getCourtHearingLocation()));
        return courtLocations.get(0);
    }

    private boolean resolveCaseHasNoErrors(final List<Object> eventStream) {
        eventStream.forEach(event -> {
            if (event instanceof CaseValidationFailed ||
                    event instanceof SjpValidationFailed ||
                    event instanceof DefendantValidationFailed) {
                allErrorsFixed = false;
            }
        });
        return allErrorsFixed;
    }

    private void replaceErrorCorrectedDefendant(final Defendant defendant, final List<Defendant> defendantList) {
        final Optional<Defendant> erroredDefendant = defendantList.stream().filter(d -> d.getId().equals(defendant.getId())).findAny();
        erroredDefendant.ifPresent(defendantList::remove);
        defendantList.add(defendant);
    }

    private void updateCaseCorrections(final JsonObject correctedFields, final JsonObject jsonObject) {
        final Optional<JsonArray> caseCorrections = ofNullable(correctedFields.getJsonArray("errors"));
        final com.google.gson.JsonObject mutableJsonObject = new JsonParser().parse(jsonObject.toString()).getAsJsonObject();
        caseCorrections.ifPresent(x -> x.forEach(i -> {
            final JsonObject errorField = (JsonObject) i;
            final String fieldName = errorField.containsKey(ProsecutionCaseFileHelper.FIELD_NAME) ? errorField.getString(ProsecutionCaseFileHelper.FIELD_NAME) : null;
            final String value = errorField.containsKey(ProsecutionCaseFileHelper.FIELD_VALUE) ? errorField.getString(ProsecutionCaseFileHelper.FIELD_VALUE) : null;
            final String id = errorField.containsKey("id") ? errorField.getString("id") : null;
            addJsonProperty(mutableJsonObject, value, fieldName, id);
        }));
        try (JsonReader jsonReader = Json.createReader(new StringReader(mutableJsonObject.getAsJsonObject().toString()))) {
            final JsonObject updatedJsonObject = jsonReader.readObject();
            this.caseDetails = jsonObjectToObjectConverter.convert(updatedJsonObject, CaseDetails.class);
        }
    }

    private List<JsonObject> updateDefendantCorrections(final JsonObject correctedFields, final List<Defendant> defendantList) {
        final Optional<JsonArray> defendantsCorrections = ofNullable(correctedFields.getJsonArray("defendants"));
        return defendantsCorrections.orElseGet(() -> createArrayBuilder().build()).stream().map(i -> {
            final JsonObject jsonObject = (JsonObject) i;
            final Defendant defendant = defendantList.stream().filter(d -> d.getId().equals(jsonObject.getString("id"))).findAny().get();
            final com.google.gson.JsonObject mutableJsonObject = new JsonParser().parse(objectToJsonObjectConverter.convert(defendant).toString()).getAsJsonObject();
            final Optional<JsonArray> allErrors = ofNullable(jsonObject.getJsonArray("errors"));
            allErrors.orElseGet(() -> createArrayBuilder().build()).forEach(x ->
                    {
                        final JsonObject errorField = (JsonObject) x;
                        final String fieldName = errorField.containsKey(ProsecutionCaseFileHelper.FIELD_NAME) ? errorField.getString(ProsecutionCaseFileHelper.FIELD_NAME) : null;
                        final String value = errorField.containsKey(ProsecutionCaseFileHelper.FIELD_VALUE) ? errorField.getString(ProsecutionCaseFileHelper.FIELD_VALUE) : null;
                        final String id = errorField.containsKey("id") ? errorField.getString("id") : null;
                        addJsonProperty(mutableJsonObject, value, fieldName, id);
                        if(!mutableJsonObject.has(POSTING_DATE) && fieldName.equalsIgnoreCase(OFFENCES_CHARGE_DATE)){
                            mutableJsonObject.addProperty(POSTING_DATE, value);
                        }
                    }
            );

            try (JsonReader jsonReader = Json.createReader(new StringReader(mutableJsonObject.getAsJsonObject().toString()))) {
                return jsonReader.readObject();
            }
        }).collect(toList());
    }


    @SuppressWarnings({"squid:S1172", "squid:S3776"})
    public Stream<Object> receiveCCCase(final ProsecutionWithReferenceData receivedProsecutionWithReferenceData, final List<CaseRefDataEnricher> caseRefDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final ReferenceDataQueryService referenceDataQueryService) {
        return receiveCCCase(receivedProsecutionWithReferenceData, caseRefDataEnrichers, defendantRefDataEnrichers, referenceDataQueryService, false);
    }

    @SuppressWarnings({"squid:S1172", "squid:S3776", "squid:MethodCyclomaticComplexity"})
    private Stream<Object> receiveCCCase(final ProsecutionWithReferenceData receivedProsecutionWithReferenceData,
                                         final List<CaseRefDataEnricher> caseRefDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers,
                                         final ReferenceDataQueryService referenceDataQueryService, final boolean errorCorrection) {
        final Builder<Object> builder = builder();
        ProsecutionWithReferenceData prosecutionWithReferenceData = getDeduplicatedProsecution(setCivilFees(receivedProsecutionWithReferenceData), builder, errorCorrection);
        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final Channel prosecutionChannel = prosecution.getChannel();
        final String receivedInitiationCode = prosecution.getCaseDetails().getInitiationCode();
        final UUID externalId = prosecutionWithReferenceData.getExternalId();

        final boolean noDefendantsParkedForSummonsApplicationApproval = this.applicationIdToDefendantIdsMap.isEmpty();
        final boolean noErrorsAndNotSeenBefore = SUMMONS_INITIATION_CODE.equals(receivedInitiationCode) ? !this.prosecutionFoundWithErrors && noDefendantsParkedForSummonsApplicationApproval :
                !this.prosecutionFoundWithErrors && !this.prosecutionReceived;
        final boolean correctingKnownErrors = this.prosecutionFoundWithErrors && errorCorrection;
        final boolean firstMessageFromSpi = SPI.equals(prosecutionChannel) && (correctingKnownErrors || noErrorsAndNotSeenBefore);
        final boolean messageFromCppiOrMccOrCivil = (MCC.equals(prosecutionChannel) || CPPI.equals(prosecutionChannel) || CIVIL.equals(prosecutionChannel));

        if (isEmpty(prosecutionWithReferenceData.getProsecution().getDefendants())) {
            return apply(builder.build());
        }

        if (SPI.equals(prosecutionChannel) && !firstMessageFromSpi && shouldCaseBeRejectedBasedOnInitiationCode(receivedInitiationCode)) {
            return builder.add(prosecutionCaseUnsupported()
                    .withChannel(SPI)
                    .withErrorMessage(String.format("Original Case with initiation code %s has received a case update with different case initiation code %s", this.initiationCode, receivedInitiationCode))
                    .withExternalId(prosecutionWithReferenceData.getExternalId())
                    .withPoliceSystemId(prosecutionWithReferenceData.getProsecution().getCaseDetails().getPoliceSystemId())
                    .withUrn(prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference())
                    .build()).build();
        }
        if (!firstMessageFromSpi) {
            prosecutionWithReferenceData = updateInitiationCodeForNewDefendantsForSpi(prosecutionWithReferenceData);
        }

        final ProsecutionWithReferenceData finalProsecutionWithReferenceData = prosecutionWithReferenceData;
        caseRefDataEnrichers.forEach(x -> x.enrich(finalProsecutionWithReferenceData));
        final DefendantsWithReferenceData defendantsWithReferenceData = buildDefendantWithReferenceData(prosecutionWithReferenceData, defendantRefDataEnrichers);

        final Boolean isCivil = Optional.ofNullable(receivedProsecutionWithReferenceData.getProsecution().getIsCivil()).orElse(false);

        final List<Problem> caseProblems = validate(prosecutionWithReferenceData, referenceDataQueryService, getCaseValidationRules(receivedInitiationCode));
        boolean isMCCWithListNewHearing = MCC.equals(prosecutionChannel) && Objects.nonNull(prosecutionWithReferenceData.getProsecution().getListNewHearing());
        final List<DefendantProblem> defendantErrors = validateDefendantErrors(prosecution.getCaseDetails(), prosecutionChannel, defendantsWithReferenceData, referenceDataQueryService, builder, Boolean.FALSE, isMCCWithListNewHearing, isCivil);

        if (messageFromCppiOrMccOrCivil && prosecutionReceived) {
            caseProblems.add(newProblem(DUPLICATED_PROSECUTION, "urn", prosecution.getCaseDetails().getProsecutorCaseReference()));
        }

        final boolean hasNoErrors = isEmpty(caseProblems) && isEmpty(defendantErrors);

        if (hasNoErrors && (firstMessageFromSpi || messageFromCppiOrMccOrCivil)) {
            return processWithoutProblems(prosecutionWithReferenceData, defendantsWithReferenceData, builder);
        }

        if (SPI.equals(prosecutionChannel)) {
            if (!firstMessageFromSpi) {
                return addDefendants(caseId, externalId, defendantsWithReferenceData, defendantErrors, builder);
            }
            return apply(builder.add(buildCaseValidationFailedEvent(prosecution, externalId, caseProblems, defendantsWithReferenceData)).build());
        }

        return apply(builder.add(ccProsecutionRejected()
                        .withCaseErrors(caseProblems)
                        .withProsecution(prosecution)
                        .withDefendantErrors(defendantErrors)
                        .withExternalId(externalId).build())
                .build());
    }

    private boolean shouldCaseBeRejectedBasedOnInitiationCode(final String receivedInitiationCode) {
        return (this.initiationCode.equals(SJP_INITIATION_CODE) || receivedInitiationCode.equals(SJP_INITIATION_CODE))
                && !receivedInitiationCode.equals(this.initiationCode);
    }


    private ProsecutionWithReferenceData updateInitiationCodeForNewDefendantsForSpi(final ProsecutionWithReferenceData prosecutionWithReferenceData) {
        final List<Defendant> newDefendants = prosecutionWithReferenceData.getProsecution().getDefendants()
                .stream()
                .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                        .withInitiationCode(getDefendantInitiationCode(defendant)).build()).collect(toList());

        final Prosecution updatedProsecution = prosecution().withValuesFrom(prosecutionWithReferenceData.getProsecution()).withDefendants(newDefendants).build();
        return new ProsecutionWithReferenceData(updatedProsecution, prosecutionWithReferenceData.getReferenceDataVO(), prosecutionWithReferenceData.getExternalId());
    }

    private String getDefendantInitiationCode(final Defendant defendant) {
        if (!this.defendants.isEmpty() && Objects.equals(this.defendants.get(0).getId(), defendant.getId())) {
            return defendants.get(0).getInitiationCode();
        }
        return defendant.getInitiationCode();
    }

    private ProsecutionWithReferenceData getDeduplicatedProsecution(final ProsecutionWithReferenceData prosecutionWithReferenceData,
                                                                    final Builder<Object> builder,
                                                                    final boolean errorCorrection) {
        final Channel prosecutionChannel = prosecutionWithReferenceData.getProsecution().getChannel();
        final String prosecutionCaseReference = prosecutionWithReferenceData.getProsecution().getCaseDetails().getProsecutorCaseReference();

        if (errorCorrection || !SPI.equals(prosecutionChannel)) {
            return prosecutionWithReferenceData;
        }

        final List<Defendant> payloadDefendants = prosecutionWithReferenceData.getProsecution().getDefendants();
        final Set<String> seenDefendants = this.defendants.stream().map(Defendant::getProsecutorDefendantReference).collect(toSet());

        seenDefendants.addAll(this.defendantsWithHeldAfterCaseReceived.stream().map(Defendant::getProsecutorDefendantReference).collect(toSet()));

        final List<Defendant> newDefendants = new ArrayList<>();
        final List<Defendant> duplicateDefendants = new ArrayList<>();


        for (final Defendant payloadDefendant : payloadDefendants) {
            if (isDuplicateDefendant(payloadDefendant, this.defendants, prosecutionCaseReference) || seenDefendants.contains(payloadDefendant.getProsecutorDefendantReference())) {
                duplicateDefendants.add(payloadDefendant);
            } else {
                newDefendants.add(payloadDefendant);
                seenDefendants.add(payloadDefendant.getProsecutorDefendantReference());
            }
        }

        if (!duplicateDefendants.isEmpty()) {
            builder.accept(caseReceivedWithDuplicateDefendants()
                    .withDefendants(duplicateDefendants)
                    .withCaseId(caseId)
                    .build());
            final Prosecution updatedProsecution = prosecution().withValuesFrom(prosecutionWithReferenceData.getProsecution()).withDefendants(newDefendants).build();
            return new ProsecutionWithReferenceData(updatedProsecution, prosecutionWithReferenceData.getReferenceDataVO(), prosecutionWithReferenceData.getExternalId());
        }
        return prosecutionWithReferenceData;
    }

    @SuppressWarnings({"squid:S1172", "squid:S3776", "squid:MethodCyclomaticComplexity"})
    private boolean isDuplicateDefendant(Defendant newDefendant, List<Defendant> existingDefendants, final String prosecutionCaseReference) {
        for (final Defendant existingDefendant : existingDefendants) {
            if (this.prosecutorCaseReference != null && this.prosecutorCaseReference.equals(prosecutionCaseReference)) {
                if (newDefendant.getAsn() != null && existingDefendant.getAsn() != null &&
                        Objects.equals(newDefendant.getAsn(), existingDefendant.getAsn())) {
                    return true;
                }

                if (isDuplicateDefendantBasedOnDefendantDetails(newDefendant, existingDefendant)) {
                    return true;
                }

                if (isDuplicateDefendantBasedOnNamesWithoutDOB(newDefendant, existingDefendant)) {
                    return true;
                }

                if (isDuplicateBasedOnLastNameAndDOB(newDefendant, existingDefendant)) {
                    return true;
                }

                if (isDuplicateBasedOnFirstNameExistence(newDefendant, existingDefendant)) {
                    return false;
                }
            }
        }
        return false;
    }

    @SuppressWarnings({"squid:S1172", "squid:S3776", "squid:MethodCyclomaticComplexity"})
    private static boolean isDuplicateDefendantBasedOnDefendantDetails(final Defendant newDefendant,
                                                                       final Defendant existingDefendant) {
        return isDuplicateBasedOnOrganisationName(newDefendant, existingDefendant) || isDuplicateBasedOnPersonalInformation(newDefendant, existingDefendant);
    }

    private static boolean isDuplicateBasedOnOrganisationName(final Defendant newDefendant,
                                                              final Defendant existingDefendant) {
        return newDefendant.getOrganisationName() != null && existingDefendant.getOrganisationName() != null &&
                newDefendant.getOrganisationName().equalsIgnoreCase(existingDefendant.getOrganisationName());
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private static boolean isDuplicateBasedOnPersonalInformation(final Defendant newDefendant,
                                                                 final Defendant existingDefendant) {
        if (newDefendant.getIndividual() == null || existingDefendant.getIndividual() == null) {
            return false;
        }

        final PersonalInformation newPersonalInfo = newDefendant.getIndividual().getPersonalInformation();
        final PersonalInformation existingPersonalInfo = existingDefendant.getIndividual().getPersonalInformation();

        if (newPersonalInfo == null || existingPersonalInfo == null) {
            return false;
        }

        final boolean firstNameMatches = newPersonalInfo.getFirstName() != null &&
                newPersonalInfo.getFirstName().equalsIgnoreCase(existingPersonalInfo.getFirstName());
        final boolean lastNameMatches = newPersonalInfo.getLastName() != null &&
                newPersonalInfo.getLastName().equalsIgnoreCase(existingPersonalInfo.getLastName());

        if (firstNameMatches && lastNameMatches) {
            final SelfDefinedInformation newSelfDefinedInfo = newDefendant.getIndividual().getSelfDefinedInformation();
            final SelfDefinedInformation existingSelfDefinedInfo = existingDefendant.getIndividual().getSelfDefinedInformation();

            if (newSelfDefinedInfo == null || existingSelfDefinedInfo == null) {
                return false;
            }

            final LocalDate newDOB = newSelfDefinedInfo.getDateOfBirth();
            final LocalDate existingDOB = existingSelfDefinedInfo.getDateOfBirth();

            return newDOB != null && newDOB.equals(existingDOB);
        }
        return false;
    }

    private static boolean isDuplicateDefendantBasedOnNamesWithoutDOB(final Defendant newDefendant,
                                                                      final Defendant existingDefendant) {
        if (hasValidPersonalInformation(newDefendant) && hasValidPersonalInformation(existingDefendant)) {
            final PersonalInformation newPersonalInfo = newDefendant.getIndividual().getPersonalInformation();
            final PersonalInformation existingPersonalInfo = existingDefendant.getIndividual().getPersonalInformation();

            final boolean firstNameMatches = newPersonalInfo.getFirstName() != null &&
                    newPersonalInfo.getFirstName().equalsIgnoreCase(existingPersonalInfo.getFirstName());
            final boolean lastNameMatches = newPersonalInfo.getLastName() != null &&
                    newPersonalInfo.getLastName().equalsIgnoreCase(existingPersonalInfo.getLastName());

            if (firstNameMatches && lastNameMatches) {
                final LocalDate newDOB = newDefendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();
                final LocalDate existingDOB = existingDefendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();

                return newDOB == null && existingDOB == null;
            }
        }
        return false;
    }

    private static boolean isDuplicateBasedOnLastNameAndDOB(final Defendant newDefendant,
                                                            final Defendant existingDefendant) {
        if (hasValidPersonalInformation(newDefendant) && hasValidPersonalInformation(existingDefendant)) {
            final PersonalInformation newPersonalInfo = newDefendant.getIndividual().getPersonalInformation();
            final PersonalInformation existingPersonalInfo = existingDefendant.getIndividual().getPersonalInformation();

            final boolean firstNameMissing = newPersonalInfo.getFirstName() == null || existingPersonalInfo.getFirstName() == null;
            final boolean lastNameMatches = newPersonalInfo.getLastName() != null &&
                    newPersonalInfo.getLastName().equalsIgnoreCase(existingPersonalInfo.getLastName());

            if (firstNameMissing && lastNameMatches) {
                final LocalDate newDOB = newDefendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();
                final LocalDate existingDOB = existingDefendant.getIndividual().getSelfDefinedInformation().getDateOfBirth();

                return newDOB != null && newDOB.equals(existingDOB);
            }
        }
        return false;
    }

    private static boolean isDuplicateBasedOnFirstNameExistence(final Defendant newDefendant,
                                                                final Defendant existingDefendant) {
        if (hasValidPersonalInformation(newDefendant) && hasValidPersonalInformation(existingDefendant)) {
            final PersonalInformation newPersonalInfo = newDefendant.getIndividual().getPersonalInformation();
            final PersonalInformation existingPersonalInfo = existingDefendant.getIndividual().getPersonalInformation();

            final boolean newFirstNameExists = newPersonalInfo.getFirstName() != null;
            final boolean existingFirstNameExists = existingPersonalInfo.getFirstName() != null;

            final boolean lastNameMatches = newPersonalInfo.getLastName() != null &&
                    newPersonalInfo.getLastName().equalsIgnoreCase(existingPersonalInfo.getLastName());

            return newFirstNameExists != existingFirstNameExists && lastNameMatches;
        }
        return false;
    }


    private static boolean hasValidPersonalInformation(final Defendant defendant) {
        return defendant.getIndividual() != null &&
                defendant.getIndividual().getPersonalInformation() != null &&
                defendant.getIndividual().getSelfDefinedInformation() != null;
    }

    private Stream<Object> processWithoutProblems(final ProsecutionWithReferenceData prosecutionWithReferenceData,
                                                  final DefendantsWithReferenceData defendantsWithReferenceData,
                                                  final Builder<Object> builder) {
        final Prosecution prosecution = prosecutionWithReferenceData.getProsecution();
        final String incomingInitiationCode = prosecution.getCaseDetails().getInitiationCode();
        final Channel prosecutionChannel = prosecution.getChannel();
        final List<DefendantProblem> defendantWarningsForIncomingMessage = prosecutionChannel == SPI ? List.of() : validateDefendantWarnings(defendantsWithReferenceData, incomingInitiationCode);

        if (incomingInitiationCode.equals(SUMMONS_INITIATION_CODE)) {
            return apply(builder.add(defendantsParkedForSummonsApplicationApproval()
                    .withApplicationId(randomUUID())
                    .withProsecutionWithReferenceData(prosecutionWithReferenceData)
                    .withDefendantWarnings(defendantWarningsForIncomingMessage)
                    .build()).build());
        }

        if (SPI.equals(prosecutionChannel) || defendantWarningsForIncomingMessage.isEmpty()) {
            return apply(builder.add(ccCaseReceived()
                    .withProsecutionWithReferenceData(prosecutionWithReferenceData)
                    .withId(randomUUID())
                    .build()
            ).build());
        }

        final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = ccCaseReceivedWithWarnings()
                .withProsecutionWithReferenceData(prosecutionWithReferenceData)
                .withDefendantWarnings(defendantWarningsForIncomingMessage)
                .withId(randomUUID())
                .build();
        return apply(builder.add(ccCaseReceivedWithWarnings).build());
    }

    public Stream<Object> associateEnterpriseId(final String enterpriseId) {
        return initiateSjpProsecution(enterpriseId);
    }

    private Stream<Object> addDefendants(final UUID caseId, final UUID externalId, final DefendantsWithReferenceData defendantsWithReferenceData, List<DefendantProblem> defendantProblemList, final Builder<Object> builder) {
        final List<DefendantProblem> defendantWarningsList = channel == CPPI ? validateDefendantWarnings(defendantsWithReferenceData, this.caseDetails.getInitiationCode()) : null;
        final List<Defendant> validDefendantList = channel == SPI ? validDefendants(defendantsWithReferenceData, defendantProblemList) : defendantsWithReferenceData.getDefendants();
        final String initiationCodeForSameCaseWithNewDefendant = defendantsWithReferenceData.getCaseDetails().getInitiationCode();
        if (!validDefendantList.isEmpty()) {
            if (!prosecutionFoundWithErrors) {
                if (isSummonsCase(initiationCodeForSameCaseWithNewDefendant)) {
                    builder.add(defendantsParkedForSummonsApplicationApproval()
                            .withApplicationId(randomUUID())
                            .withProsecutionWithReferenceData(
                                    new ProsecutionWithReferenceData(prosecution()
                                            .withCaseDetails(this.caseDetails)
                                            .withChannel(this.channel)
                                            .withDefendants(validDefendantList)
                                            .build(),
                                            defendantsWithReferenceData.getReferenceDataVO(),
                                            externalId))
                            .build());
                } else {
                    builder.add(prosecutionDefendantsAdded()
                            .withCaseId(caseId)
                            .withExternalId(externalId)
                            .withDefendants(validDefendantList)
                            .withReferenceDataVO(defendantsWithReferenceData.getReferenceDataVO())
                            .withDefendantWarnings(defendantWarningsList)
                            .withChannel(channel)
                            .build());
                }
            } else {
                builder.add(new DefendantsReceivedNotAdded(validDefendantList, externalId));
            }
        }
        return apply(builder.build());
    }

    private boolean isSummonsCase(final String initiationCodeForSameCaseWithNewDefendant) {
        return !this.isSummonsCaseRejected && SUMMONS_INITIATION_CODE.equals(this.caseDetails.getInitiationCode())
                && this.initiationCode.equals(initiationCodeForSameCaseWithNewDefendant);
    }


    private List<Defendant> validDefendants(final DefendantsWithReferenceData defendantsWithReferenceData, final List<DefendantProblem> defendantProblemList) {
        final List<Defendant> validDefendantList = newArrayList();
        for (final Defendant defendant : defendantsWithReferenceData.getDefendants()) {
            final Optional<DefendantProblem> matchingDefendantProblem = defendantProblemList.stream().filter(dpl -> dpl.getProsecutorDefendantReference().equals(defendant.getProsecutorDefendantReference())).findFirst();
            if (!matchingDefendantProblem.isPresent()) {
                validDefendantList.add(defendant);
            }
        }
        return validDefendantList;
    }

    public Stream<Object> addErrorCorrectedDefendantsForSPI(final UUID caseId, final UUID externalId, final DefendantsWithReferenceData defendantsWithReferenceData, final ReferenceDataQueryService referenceDataQueryService,final Boolean isCivil) {
        final Builder<Object> builder = builder();
        final List<DefendantProblem> defendantErrors = validateDefendantErrors(this.caseDetails, SPI, defendantsWithReferenceData, referenceDataQueryService, builder, Boolean.FALSE, false, isCivil);
        return addDefendants(caseId, externalId, defendantsWithReferenceData, defendantErrors, builder);
    }

    private Stream<Object> initiateSjpProsecution(final String enterpriseId) {
        assert defendants.size() == 1; // SJP supports just a single defendant

        final Prosecution prosecution = recreateProsecution(0);
        return apply(of(warnings.isEmpty() ?
                new SjpProsecutionInitiated(enterpriseId, prosecution) :
                new SjpProsecutionInitiatedWithWarnings(enterpriseId, prosecution, warnings)));
    }

    public Stream<Object> acceptCase(final UUID caseId, final List<UUID> defendantIds, final ReferenceDataQueryService referenceDataQueryService) {
        final Builder<Object> builder = builder();

        if (isProsecutionReceived()) {
            final UUID externalId = getExternalIdFromDefendantIds(defendantIds);

            if (this.caseType.equals(CC)) {
                builder.accept(this.warnings.isEmpty() ? new CaseCreatedSuccessfully(caseId, this.channel, externalId) : new CaseCreatedSuccessfullyWithWarnings(caseId, EMPTY_LIST, this.channel, this.defendantWarnings, externalId, this.warnings));
            } else {
                builder.accept(this.warnings.isEmpty() ? new SjpCaseCreatedSuccessfully(caseId, this.channel, externalId) : new SjpCaseCreatedSuccessfullyWithWarnings(caseId, this.channel, externalId, this.warnings));
            }
            this.pendingMaterials.stream()
                    .map(materialPending -> validatePendingMaterial(materialPending, referenceDataQueryService))
                    .forEach(builder);
            this.pendingMaterialsV2.stream()
                    .map(materialPendingV2 -> validatePendingMaterialV2(materialPendingV2, referenceDataQueryService))
                    .forEach(builder);
        }
        return apply(builder.build());
    }

    public Stream<Object> runPendingEvents(final ReferenceDataQueryService referenceDataQueryService) {
        final Builder<Object> builder = builder();

        if (isProsecutionReceived()) {
            this.pendingMaterialsV2.stream()
                    .map(materialPendingV2 -> validatePendingMaterialV2(materialPendingV2, referenceDataQueryService))
                    .forEach(builder);
        }
        return apply(builder.build());
    }

    public Stream<Object> addMaterial(final UUID caseId, final String prosecutingAuthority,
                                      final String prosecutorDefendantId, final Material material,
                                      final ReferenceDataQueryService referenceDataQueryService, final Boolean isCpsCase,
                                      final ZonedDateTime receivedDateTime) {
        final Object event = isProsecutionAccepted()
                ? validateMaterial(caseId, prosecutingAuthority, prosecutorDefendantId, material, referenceDataQueryService, receivedDateTime, isCpsCase)
                : ProsecutionCaseFileHelper.markMaterialAsPending(caseId, prosecutingAuthority, prosecutorDefendantId, material, null, isCpsCase, null);

        return apply(of(event));
    }

    public Stream<Object> addMaterials(final UUID caseId, final String prosecutingAuthority,
                                       final String prosecutorDefendantId, final List<Material> materials,
                                       final ReferenceDataQueryService referenceDataQueryService, final Boolean isCpsCase,
                                       final ZonedDateTime receivedDateTime) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        materials.forEach(material -> {
            final Object event = isProsecutionAccepted()
                    ? validateMaterial(caseId, prosecutingAuthority, prosecutorDefendantId, material, referenceDataQueryService, receivedDateTime, isCpsCase)
                    : ProsecutionCaseFileHelper.markMaterialAsPending(caseId, prosecutingAuthority, prosecutorDefendantId, material, null, isCpsCase, null);
            streamBuilder.add(event);
        });

        return apply(streamBuilder.build());
    }

    public Stream<Object> addMaterialV2(final AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService) {
        final Builder<Object> builder = builder();
        final Object event = isProsecutionAccepted()
                ? validateMaterialV2(addMaterialCommonV2, referenceDataQueryService)
                : ProsecutionCaseFileHelper.markMaterialAsPendingV2(addMaterialCommonV2);

        builder.add(event);
        final DefendantSubject defendantSubject;
        UUID cpDefendantId = null;
        if (event instanceof MaterialAddedWithWarnings) {
            defendantSubject = ((MaterialAddedWithWarnings) event).getProsecutionCaseSubject().getDefendantSubject();
            cpDefendantId = ((MaterialAddedWithWarnings) event).getDefendantId();
        } else if (event instanceof MaterialAddedV2) {
            defendantSubject = ((MaterialAddedV2) event).getProsecutionCaseSubject().getDefendantSubject();
            cpDefendantId = ((MaterialAddedV2) event).getDefendantId();
        } else {
            defendantSubject = null;
        }

        if (defendantSubject != null) {
            final String defendentId = ProsecutionCaseFileHelper.getDefendantId(defendantSubject);
            this.validDefendantIds.put(defendentId, cpDefendantId);
            this.pendingMaterialsV2.stream()
                    .filter(materialPendingV2 -> ProsecutionCaseFileHelper.getDefendantId(materialPendingV2.getProsecutionCaseSubject().getDefendantSubject()).equals(defendentId))
                    .map(materialPendingV2 -> validatePendingMaterialV2(materialPendingV2, referenceDataQueryService))
                    .filter(object -> !(object instanceof MaterialPendingV2))
                    .forEach(builder::add);
        }

        return apply(builder.build());
    }

    public Stream<Object> addCpsMaterial(final UUID caseId, final String prosecutingAuthority, final String prosecutorDefendantId, final Material material, final ReferenceDataQueryService referenceDataQueryService,
                                         final ZonedDateTime receivedDateTime, final DocumentDetails documentDetails) {
        final Stream.Builder<Object> streamBuilder = builder();

        if (isProsecutionAccepted()) {
            final Object resultingEvent = validateMaterialWithDocumentDetails(caseId, prosecutingAuthority, prosecutorDefendantId, material, referenceDataQueryService, receivedDateTime, true, documentDetails);

            streamBuilder.add(resultingEvent);

            if (resultingEvent instanceof MaterialRejected) {
                streamBuilder.add(getReviewEvent(caseId, material, receivedDateTime, documentDetails, extractErrorCode((MaterialRejected) resultingEvent), prosecutingAuthority));
            }
        } else {
            streamBuilder.add(ProsecutionCaseFileHelper.markMaterialAsPending(caseId, prosecutingAuthority, prosecutorDefendantId, material, receivedDateTime, true, documentDetails));
            streamBuilder.add(getReviewEvent(caseId, material, receivedDateTime, documentDetails, singletonList(PROBLEM_CODE_DOCUMENT_NOT_MATCHED), prosecutingAuthority)
            );
        }
        return apply(streamBuilder.build());
    }

    private CaseDocumentReviewRequired getReviewEvent(final UUID caseId, final Material material, final ZonedDateTime receivedDateTime, final DocumentDetails documentDetails, final List<String> errorList, final String prosecutingAuthority) {
        final String prosecutingAuthorityCode = isBlank(prosecutingAuthority) ? getProsecutingAuthority() : prosecutingAuthority;
        return CaseDocumentReviewRequired.caseDocumentReviewRequired()
                .withCaseId(caseId)
                .withCmsDocumentId(documentDetails.getCmsDocumentId())
                .withDocumentType(getListOfAllowedDocumentTypes().contains(material.getDocumentType()) ? DocumentType.valueOf(material.getDocumentType()) : DocumentType.OTHER)
                .withErrorCodes(errorList)
                .withFileStoreId(material.getFileStoreId())
                .withReceivedDateTime(receivedDateTime)
                .withSource(SOURCE_CPS_FOR_PUBLIC_EVENTS)
                .withProsecutingAuthority(prosecutingAuthorityCode)
                .build();
    }

    private String getProsecutingAuthority() {
        if (this.caseDetails != null && this.caseDetails.getProsecutor() != null) {
            return this.caseDetails.getProsecutor().getProsecutingAuthority();
        }
        return null;
    }

    private List<String> extractErrorCode(final MaterialRejected resultingEvent) {
        return resultingEvent.getErrors().stream().map(Problem::getCode).collect(toList());
    }

    public Stream<Object> populateIdpcMaterialReceived(final UUID cmsCaseId,
                                                       final String urn,
                                                       final UUID fileServiceId,
                                                       final String materialType,
                                                       final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant defendant) {
        final Object event = idpcMaterialReceived()
                .withCaseId(cmsCaseId)
                .withCaseUrn(urn)
                .withFileServiceId(fileServiceId)
                .withMaterialType(materialType)
                .withDefendant(defendant)
                .build();
        return apply(of(event));
    }

    @SuppressWarnings("squid:S2250")
    public Stream<Object> addIdpcCaseMaterial(final UUID cmsCaseId,
                                              final String urn,
                                              final UUID fileServiceId,
                                              final String materialType,
                                              final uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendant defendant) {
        final Object event;
        final Optional<Defendant> associatedDefendant = new AssociateIdpcToDefendantHelper(this.caseId, this.defendants).associateDefendant(cmsCaseId, defendant);

        if (associatedDefendant.isPresent() && !defendantsWithIdpc.isEmpty() && defendantsWithIdpc.contains(associatedDefendant.get().getId())) {
            event = ProsecutionCaseFileHelper.populateDefendantIdpcAlreadyExistsEvent(associatedDefendant.get().getId(), fileServiceId, cmsCaseId);
            return apply(of(event));
        }


        if (associatedDefendant.isPresent()) {
            event = populateIdpcDefendantMatchedEvent(cmsCaseId, urn, fileServiceId, materialType, associatedDefendant);
        } else {
            event = ProsecutionCaseFileHelper.populateIdpcDefendantMatchPendingEvent(cmsCaseId, urn, fileServiceId, materialType, defendant);
        }
        return apply(of(event));
    }

    public Stream<Object> caseUpdated() {
        final AssociateIdpcToDefendantHelper helper = new AssociateIdpcToDefendantHelper(this.caseId, this.defendants);
        final List<Object> events = new ArrayList<>();
        for (final IdpcDefendantMatchPending idpcDefendantMatchPending : this.pendingIdpcMaterials) {
            final Optional<Defendant> associatedDefendantOptional = helper.associateDefendant(idpcDefendantMatchPending.getCaseId(), idpcDefendantMatchPending.getDefendant());
            if (associatedDefendantOptional.isPresent()) {
                final Defendant associatedDefendant = associatedDefendantOptional.get();
                events.add(idpcDefendantMatched()
                        .withCaseId(this.caseId)
                        .withCaseUrn(idpcDefendantMatchPending.getCaseUrn())
                        .withFileServiceId(idpcDefendantMatchPending.getFileServiceId())
                        .withMaterialType(idpcDefendantMatchPending.getMaterialType())
                        .withDefendantId(associatedDefendant.getId())
                        .build());

            }
        }
        return events.stream();
    }

    public Stream<Object> addDefendantIdpc(final UUID caseId,
                                           final CaseDocument caseDocument,
                                           final UUID defendantId) {


        return apply(of(new DefendantIdpcAdded(caseId, caseDocument, defendantId)));
    }

    public Stream<Object> expirePendingMaterial(final UUID fileStoreId, final ZonedDateTime expiredAt) {
        final List<Object> expired = raiseMaterialRejectedEvent(fileStoreId, expiredAt);
        final List<Object> expiredV2 = raiseMaterialRejectedEventV2(fileStoreId, expiredAt);
        return apply(Stream.concat(expired.stream(), expiredV2.stream()));
    }

    public Stream<Object> expireBulkScanPendingMaterial(final UUID fileStoreId, final ZonedDateTime expiredAt) {
        final List<Object> expired = raiseBulkScanMaterialRejectedEvent(fileStoreId, expiredAt);
        final List<Object> expiredV2 = raiseBulkScanMaterialRejectedEventV2(fileStoreId, expiredAt);
        return apply(Stream.concat(expired.stream(), expiredV2.stream()));
    }

    private List<Object> raiseBulkScanMaterialRejectedEvent(final UUID fileStoreId, final ZonedDateTime expiredAt) {
        return pendingMaterials.stream()
                .filter(pendingMaterial -> pendingMaterial.getMaterial().getFileStoreId().equals(fileStoreId))
                .map(pendingMaterial -> bulkscanMaterialRejected()
                        .withCaseId(pendingMaterial.getCaseId())
                        .withMaterial(pendingMaterial.getMaterial())
                        .withErrors(singletonList(newProblem(MATERIAL_EXPIRED, EXPIRED_AT, expiredAt)))
                        .build())
                .collect(toList());
    }

    private List<Object> raiseBulkScanMaterialRejectedEventV2(final UUID fileStoreId, final ZonedDateTime expiredAt) {
        return pendingMaterialsV2.stream()
                .filter(pendingMaterial -> pendingMaterial.getMaterial().equals(fileStoreId))
                .map(pendingMaterial -> bulkscanMaterialRejected()
                        .withCaseId(pendingMaterial.getCaseId())
                        .withMaterial(Material.material()
                                .withDocumentType(pendingMaterial.getMaterialType())
                                .withFileStoreId(pendingMaterial.getMaterial())
                                .withFileType(pendingMaterial.getMaterialContentType())
                                .build())
                        .withErrors(singletonList(newProblem(MATERIAL_EXPIRED, EXPIRED_AT, expiredAt)))
                        .build())
                .collect(toList());
    }

    private List<Object> raiseMaterialRejectedEvent(UUID fileStoreId, ZonedDateTime expiredAt) {
        return pendingMaterials.stream()
                .filter(pendingMaterial -> pendingMaterial.getMaterial().getFileStoreId().equals(fileStoreId))
                .map(pendingMaterial -> materialRejected()
                        .withCaseId(pendingMaterial.getCaseId())
                        .withProsecutingAuthority(pendingMaterial.getProsecutingAuthority())
                        .withProsecutorDefendantId(pendingMaterial.getProsecutorDefendantId())
                        .withMaterial(pendingMaterial.getMaterial())
                        .withErrors(singletonList(newProblem(MATERIAL_EXPIRED, EXPIRED_AT, expiredAt)))
                        .withIsCpsCase(pendingMaterial.getIsCpsCase())
                        .build())
                .collect(toList());

    }

    private List<Object> raiseMaterialRejectedEventV2(UUID fileStoreId, ZonedDateTime expiredAt) {
        return pendingMaterialsV2.stream()
                .filter(pendingMaterial -> pendingMaterial.getMaterial().equals(fileStoreId))
                .map(pendingMaterial -> materialRejected()
                        .withCaseId(pendingMaterial.getCaseId())
                        .withProsecutingAuthority(pendingMaterial.getProsecutionCaseSubject().getProsecutingAuthority())
                        .withProsecutorDefendantId(pendingMaterial.getProsecutionCaseSubject().getDefendantSubject().getProsecutorDefendantId())
                        .withMaterial(Material.material()
                                .withDocumentType(pendingMaterial.getMaterialType())
                                .withFileStoreId(pendingMaterial.getMaterial())
                                .withFileType(pendingMaterial.getMaterialContentType())
                                .withIsUnbundledDocument(false)
                                .build())
                        .withErrors(singletonList(newProblem(MATERIAL_EXPIRED, EXPIRED_AT, expiredAt)))
                        .withIsCpsCase(pendingMaterial.getIsCpsCase())
                        .build())
                .collect(toList());

    }

    public Stream<Object> recordUploadCaseDocument(final UUID caseId, final UUID documentId) {
        final Builder<Object> builder = builder();
        if (isProsecutionReceived()) {
            builder.accept((uploadCaseDocumentRecorded()
                    .withCaseId(caseId)
                    .withDocumentId(documentId)
                    .build()));
        }
        return apply(builder.build());
    }

    public Stream<Object> filterCase(final UUID caseId) {
        final Builder<Object> builder = builder();

        final List<Material> materials = pendingMaterials.stream().map(MaterialPending::getMaterial).collect(toList());
        final List<Material> materialsV2 = pendingMaterialsV2.stream().map(pendingMaterial -> Material.material()
                .withDocumentType(pendingMaterial.getMaterialType())
                .withFileStoreId(pendingMaterial.getMaterial())
                .withFileType(pendingMaterial.getMaterialContentType())
                .withIsUnbundledDocument(false)
                .build()).collect(toList());
        materials.addAll(materialsV2);
        builder.accept((CaseFiltered.caseFiltered()
                .withCaseId(caseId)
                .withMaterials(materials)
                .build()));
        return apply(builder.build());
    }


    public Stream<Object> assignCase(UUID caseId) {
        return apply(of(new SjpCaseAssigned(caseId)));
    }


    public Stream<Object> unassignCase(UUID caseId) {
        return apply(of(new SjpCaseUnAssigned(caseId)));
    }


    public Stream<Object> ejectCase(final UUID caseId) {
        return apply(of(new CaseEjected(caseId)));
    }

    @Override
    @SuppressWarnings("squid:S2250")
    public Object apply(final Object event) {
        return match(event).with(
                when(SjpProsecutionReceived.class).apply(e -> {
                    this.caseType = SJP;
                    prosecutionReceived = true;
                    final Prosecution prosecution = e.getProsecution();
                    this.caseDetails = prosecution.getCaseDetails();
                    this.channel = prosecution.getChannel();
                    this.caseId = caseDetails.getCaseId();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.prosecutorCaseReference = caseDetails.getProsecutorCaseReference();
                    this.defendants = singletonList(prosecution.getDefendants().get(0));
                    hydrateExternalIdToDefendantsMap(e.getExternalId(), this.defendants);
                }),
                when(SjpProsecutionReceivedWithWarnings.class).apply(e -> {
                    this.caseType = SJP;
                    prosecutionReceived = true;
                    prosecutionFoundWithErrors = false;
                    final Prosecution prosecution = e.getProsecution();
                    this.caseDetails = prosecution.getCaseDetails();
                    this.channel = prosecution.getChannel();
                    this.caseId = caseDetails.getCaseId();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.prosecutorCaseReference = caseDetails.getProsecutorCaseReference();
                    this.defendants = singletonList(prosecution.getDefendants().get(0));
                    hydrateExternalIdToDefendantsMap(e.getExternalId(), this.defendants);
                    this.warnings = new ArrayList<>(e.getWarnings());
                }),
                when(SjpCaseCreatedSuccessfully.class).apply(e -> {
                    prosecutionAccepted = true;
                    pendingMaterials.clear();
                    pendingMaterialsV2.clear();
                }),
                when(CaseCreatedSuccessfully.class).apply(e -> {
                    prosecutionAccepted = true;
                    pendingMaterials.clear();
                    pendingMaterialsV2.clear();
                }),
                when(CaseCreatedSuccessfullyWithWarnings.class).apply(e -> {
                    prosecutionAccepted = true;
                    pendingMaterials.clear();
                    pendingMaterialsV2.clear();
                }),
                when(SjpCaseCreatedSuccessfullyWithWarnings.class).apply(e -> {
                    prosecutionAccepted = true;
                    pendingMaterials.clear();
                    pendingMaterialsV2.clear();
                }),
                when(MaterialPending.class).apply(pendingMaterials::add),
                when(MaterialPendingV2.class).apply(pendingMaterialsV2::add),
                when(IdpcDefendantMatchPending.class).apply(pendingIdpcMaterials::add),
                when(CaseDocumentReviewRequired.class).apply(caseDocumentReviewRequired -> {
                }),
                when(MaterialRejected.class).apply(e -> pendingMaterials.removeIf(pendingMaterial -> pendingMaterial.getMaterial().equals(e.getMaterial()))),
                when(MaterialRejectedV2.class).apply(e -> pendingMaterialsV2.removeIf(pendingMaterial -> pendingMaterial.getMaterial().equals(e.getMaterial()))),
                when(IdpcMaterialRejected.class).apply(e -> pendingIdpcMaterials.removeIf(pendingMaterial -> pendingMaterial.getFileServiceId().equals(e.getFileServiceId()))),
                when(CaseReferredToCourtRecorded.class).apply(e -> {
                    this.caseReferredToCourt = true;
                    this.referralReasonId = e.getReferralReasonId();
                    this.caseType = CC;
                    this.prosecutionReceived = true;
                }),
                when(CcCaseReceived.class).apply(e -> {
                    this.prosecutionFoundWithErrors = false;
                    this.prosecutionReceived = true;
                    this.caseType = CC;
                    final Prosecution prosecution = e.getProsecutionWithReferenceData().getProsecution();
                    this.caseDetails = prosecution.getCaseDetails();
                    this.channel = prosecution.getChannel();
                    this.caseId = caseDetails.getCaseId();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.prosecutorCaseReference = caseDetails.getProsecutorCaseReference();
                    this.defendants = union(this.defendants, getNewDefendantsFromProsecution(this.defendants, prosecution));
                    hydrateExternalIdToDefendantsMap(e.getProsecutionWithReferenceData().getExternalId(), prosecution.getDefendants());
                    removeDefendantsFromWithheldList(prosecution.getDefendants());
                }),
                when(DefendantsParkedForSummonsApplicationApproval.class).apply(e -> {
                    this.prosecutionFoundWithErrors = false;
                    this.caseType = CC;
                    final Prosecution prosecution = e.getProsecutionWithReferenceData().getProsecution();
                    this.channel = prosecution.getChannel();
                    this.caseDetails = prosecution.getCaseDetails();
                    this.caseId = caseDetails.getCaseId();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.defendantWarnings = union(this.defendantWarnings, isNotEmpty(e.getDefendantWarnings()) ? e.getDefendantWarnings() : emptyList());
                    this.prosecutorCaseReference = caseDetails.getProsecutorCaseReference();
                    this.defendants = union(this.defendants, getNewDefendantsFromProsecution(this.defendants, prosecution));
                    hydrateExternalIdToDefendantsMap(e.getProsecutionWithReferenceData().getExternalId(), prosecution.getDefendants());
                    if (isNotEmpty(e.getDefendantWarnings())) {
                        e.getDefendantWarnings().forEach(defendantWarning -> this.warnings.addAll(defendantWarning.getProblems()));
                    }
                    this.applicationIdToDefendantIdsMap.put(e.getApplicationId(), prosecution.getDefendants().stream().map(Defendant::getId).map(UUID::fromString).collect(toList()));
                }),
                when(CcCaseReceivedWithWarnings.class).apply(e -> {
                    this.caseType = CC;
                    this.prosecutionReceived = true;
                    final Prosecution prosecution = e.getProsecutionWithReferenceData().getProsecution();
                    this.caseDetails = prosecution.getCaseDetails();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.channel = prosecution.getChannel();
                    this.caseId = caseDetails.getCaseId();
                    this.prosecutorCaseReference = caseDetails.getProsecutorCaseReference();
                    this.defendants = union(this.defendants, getNewDefendantsFromProsecution(this.defendants, prosecution));
                    hydrateExternalIdToDefendantsMap(e.getProsecutionWithReferenceData().getExternalId(), prosecution.getDefendants());
                    final List<DefendantProblem> newDefendantsWarnings = getNewDefendantWarningsFromEvent(this.defendantWarnings, e.getDefendantWarnings());
                    this.defendantWarnings = union(this.defendantWarnings, newDefendantsWarnings);
                    if (isNotEmpty(e.getCaseWarnings())) {
                        this.warnings.addAll(e.getCaseWarnings());
                    }
                    if (isNotEmpty(newDefendantsWarnings)) {
                        newDefendantsWarnings.forEach(defendantProblem -> this.warnings.addAll(defendantProblem.getProblems()));
                    }
                }),
                when(ProsecutionDefendantsAdded.class).apply(e -> {
                    this.caseId = e.getCaseId();
                    this.defendants = union(this.defendants, e.getDefendants());
                    hydrateExternalIdToDefendantsMap(e.getExternalId(), e.getDefendants());
                    removeDefendantsFromWithheldList(e.getDefendants());
                }),
                when(IdpcDefendantMatched.class).apply(e -> defendantsWithIdpc.add(e.getDefendantId())),
                when(CaseValidationFailed.class).apply(e -> {
                    final Prosecution prosecution = e.getProsecution();
                    this.caseId = prosecution.getCaseDetails().getCaseId();
                    this.prosecutionFoundWithErrors = true;
                    this.caseDetails = prosecution.getCaseDetails();
                    this.initiationCode = caseDetails.getInitiationCode();
                    this.channel = prosecution.getChannel();
                    // remove from list and update defendants to capture corrected state
                    copyOf(prosecution.getDefendants()).forEach(i -> this.defendants.removeIf(x -> x.getId().equals(i.getId())));
                    this.defendants = union(this.defendants, prosecution.getDefendants());
                    hydrateExternalIdToDefendantsMap(e.getExternalId(), prosecution.getDefendants());
                }),
                when(DefendantValidationFailed.class).apply(e -> {
                    this.caseId = e.getCaseId();
                    if (prosecutionFoundWithErrors) {
                        this.defendants.removeIf(x -> x.getId().equals(e.getDefendant().getId()));
                        this.defendants.add(e.getDefendant());
                    } else {
                        defendantsWithHeldAfterCaseReceived.removeIf(x -> x.getId().equals(e.getDefendant().getId()));
                        defendantsWithHeldAfterCaseReceived.add(e.getDefendant());
                    }
                }),
                when(DefendantsReceivedNotAdded.class).apply(e -> {
                    copyOf(e.getDefendants()).forEach(i -> this.defendants.removeIf(x -> x.getId().equals(i.getId())));
                    this.defendants = union(this.defendants, e.getDefendants());
                    hydrateExternalIdToDefendantsMap(e.getExternalId(), e.getDefendants());
                }),

                when(SjpValidationFailed.class).apply(e -> {
                            this.caseId = e.getProsecution().getCaseDetails().getCaseId();
                            this.prosecutionFoundWithErrors = true;
                            this.caseType = SJP;
                            this.defendants = e.getProsecution().getDefendants();
                            this.caseDetails = e.getProsecution().getCaseDetails();
                            this.channel = e.getProsecution().getChannel();
                        }
                ),
                when(SjpCaseAssigned.class).apply(e -> {
                    this.caseId = e.getCaseId();
                    this.isCaseAssigned = true;
                }),
                when(SjpCaseUnAssigned.class).apply(e -> {
                    this.caseId = e.getCaseId();
                    this.isCaseAssigned = false;
                }),
                when(CaseEjected.class).apply(e -> this.isCaseEjected = true),
                when(CaseFiltered.class).apply(e -> this.isCaseFiltered = true),
                when(SummonsApplicationRejected.class).apply(e -> {
                    final List<String> rejectedDefendants = e.getDefendantIds().stream().map(UUID::toString).collect(toList());
                    this.rejectedApplicationDefendants = union(this.rejectedApplicationDefendants, this.defendants.stream().filter(defendant -> rejectedDefendants.contains(defendant.getId())).collect(toList()));
                    this.defendants.removeIf(defendant -> rejectedDefendants.contains(defendant.getId()));
                    this.rejectedApplicationIdToDefendantIdsMap.put(e.getApplicationId(), this.applicationIdToDefendantIdsMap.get(e.getApplicationId()));
                    this.applicationIdToDefendantIdsMap.remove(e.getApplicationId());
                    this.isSummonsCaseRejected = true;
                    if (this.defendants.isEmpty()) {
                        this.prosecutionReceived = false;
                    }
                }),
                when(MaterialAddedV2.class).apply(this::handleMaterialAddedV2),
                when(MaterialAddedWithWarnings.class).apply(this::handleMaterialAddedWithWarnings),
                when(CourtDocumentAdded.class).apply(e -> {
                    pendingMaterialsForCourtDocumentUpload
                            .removeIf(materialAddedV2 -> materialAddedV2.getMaterial().toString().equals(e.getFileStoreId()));
                    pendingMaterialsWithWarningsForCourtDocumentUpload
                            .removeIf(materialAddedWithWarnings -> materialAddedWithWarnings.getMaterial().toString().equals(e.getFileStoreId()));
                }),
                when(CaseUpdatedWithDefendant.class).apply(this::handleCaseUpdatedWithDefendant),
                when(GroupIdRecordedForSummonsApplication.class).apply(e -> this.groupId = e.getGroupId()),
                when(SjpProsecutionUpdateOffenceCodeRequestReceived.class).apply(e -> {
                    this.defendants = singletonList(e.getDefendants().get(0));
                }),
                otherwiseDoNothing());
    }

    private void handleCaseUpdatedWithDefendant(final CaseUpdatedWithDefendant caseUpdatedWithDefendant) {
        this.defendants = this.defendants.stream().map(defendant -> defendant.getId().equals(caseUpdatedWithDefendant.getDefendantId().toString()) ? convert(defendant, caseUpdatedWithDefendant) : defendant).collect(toList());
    }

    private Defendant convert(final Defendant defendant, final CaseUpdatedWithDefendant caseUpdatedWithDefendant) {
        return Defendant.defendant().withValuesFrom(defendant)
                .withIndividual(ofNullable(defendant.getIndividual())
                        .map(individual -> Individual.individual()
                                .withValuesFrom(individual)
                                .withSelfDefinedInformation(SelfDefinedInformation.selfDefinedInformation().withValuesFrom(individual.getSelfDefinedInformation())
                                        .withDateOfBirth(ofNullable(caseUpdatedWithDefendant.getDateOfBirth()).orElse(individual.getSelfDefinedInformation().getDateOfBirth()))
                                        .build())
                                .withPersonalInformation(ofNullable(caseUpdatedWithDefendant.getPersonDetails())
                                        .orElse(individual.getPersonalInformation()))
                                .build())
                        .orElse(null))
                .withOrganisationName(ofNullable(caseUpdatedWithDefendant.getOrganisationName()).orElse(defendant.getOrganisationName()))
                .build();
    }

    private void handleMaterialAddedV2(final MaterialAddedV2 materialAddedV2) {
        pendingMaterialsForCourtDocumentUpload.add(materialAddedV2);
        ofNullable(materialAddedV2.getProsecutionCaseSubject().getDefendantSubject())
                .ifPresent(defendantSubject -> this.validDefendantIds.put(getDefendantId(defendantSubject), materialAddedV2.getDefendantId()));
        pendingMaterialsV2.removeIf(pendingMaterial -> pendingMaterial.getMaterial().equals(materialAddedV2.getMaterial()));
    }

    private void handleMaterialAddedWithWarnings(final MaterialAddedWithWarnings materialAddedWithWarnings) {
        pendingMaterialsWithWarningsForCourtDocumentUpload.add(materialAddedWithWarnings);
        ofNullable(materialAddedWithWarnings.getProsecutionCaseSubject().getDefendantSubject())
                .ifPresent(defendantSubject -> this.validDefendantIds.put(getDefendantId(defendantSubject), materialAddedWithWarnings.getDefendantId()));
        pendingMaterialsV2.removeIf(pendingMaterial -> pendingMaterial.getMaterial().equals(materialAddedWithWarnings.getMaterial()));
    }

    private List<Defendant> getNewDefendantsFromProsecution(final List<Defendant> existingDefendants, final Prosecution prosecution) {
        if (isNotEmpty(existingDefendants)) {
            return prosecution.getDefendants().stream()
                    .filter(caseDefendant -> isNoneMatch(existingDefendants, caseDefendant))
                    .collect(toList());
        }
        return prosecution.getDefendants();
    }

    private List<DefendantProblem> getNewDefendantWarningsFromEvent(final List<DefendantProblem> existingDefendantWarnings, final List<DefendantProblem> defendantWarningsFromEvent) {
        if (isEmpty(existingDefendantWarnings)) {
            return defendantWarningsFromEvent;
        }
        if (isNotEmpty(defendantWarningsFromEvent)) {
            return defendantWarningsFromEvent.stream()
                    .filter(defendantWarningFromEvent -> isNoneMatch(existingDefendantWarnings, defendantWarningFromEvent))
                    .collect(toList());
        }
        return emptyList();
    }

    private boolean isNoneMatch(final List<DefendantProblem> existingDefendantWarnings, final DefendantProblem defendantWarning) {
        return existingDefendantWarnings.stream().noneMatch(existingDefendantWarning -> isAMatch(defendantWarning, existingDefendantWarning));
    }

    private boolean isNoneMatch(final List<Defendant> existingDefendants, final Defendant defendant) {
        return existingDefendants.stream().noneMatch(existingDefendant -> isAMatch(defendant, existingDefendant));
    }

    private boolean isAMatch(final Defendant defendant1, final Defendant defendant2) {
        if (isBlank(defendant1.getProsecutorDefendantReference()) && isBlank(defendant2.getProsecutorDefendantReference())) {
            return defendant1.getId().equals(defendant2.getId());
        } else if (isBlank(defendant1.getProsecutorDefendantReference()) || isBlank(defendant2.getProsecutorDefendantReference())) {
            return false;
        }
        return defendant1.getProsecutorDefendantReference().equals(defendant2.getProsecutorDefendantReference());
    }

    private boolean isAMatch(final DefendantProblem defendantWarning1, final DefendantProblem defendantWarning2) {
        if (isBlank(defendantWarning1.getProsecutorDefendantReference()) || isBlank(defendantWarning2.getProsecutorDefendantReference())) {
            return false;
        }
        return defendantWarning1.getProsecutorDefendantReference().equals(defendantWarning2.getProsecutorDefendantReference());
    }

    public Stream<Object> recordDecisionToReferCaseForCaseHearing(final UUID caseId, final UUID referralReasonId) {
        return apply(of(new CaseReferredToCourtRecorded(caseId, referralReasonId)));
    }

    public UUID getCaseId() {
        return caseId;
    }

    public boolean isProsecutionAccepted() {
        return prosecutionAccepted;
    }

    /**
     * NOTE that at this stage the prosecution may not have been accepted from SJP. Use {@link
     * this#isProsecutionAccepted()}
     *
     * @return true if a valid prosecution has been received
     */
    public boolean isProsecutionReceived() {
        return prosecutionReceived;
    }

    public String getProsecutorCaseReference() {
        return prosecutorCaseReference;
    }

    public boolean isCaseReferredToCourt() {
        return caseReferredToCourt;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public List<Defendant> getDefendants() {
        return copyOf(defendants);
    }

    public Stream<Object> expirePendingIdpcMaterial(final UUID fileStoreId, final ZonedDateTime expiredAt) {
        final List<Object> expired = pendingIdpcMaterials.stream()
                .filter(pendingIdpcMaterial -> pendingIdpcMaterial.getFileServiceId().equals(fileStoreId))
                .map(pendingIdpcMaterial -> idpcMaterialRejected()
                        .withCaseId(pendingIdpcMaterial.getCaseId())
                        .withFileServiceId(pendingIdpcMaterial.getFileServiceId())
                        .withErrors(singletonList(newProblem(MATERIAL_EXPIRED, EXPIRED_AT, expiredAt)))
                        .build())
                .collect(toList());

        return apply(expired.stream());
    }

    public boolean isCaseAssigned() {
        return isCaseAssigned;
    }

    public boolean isCaseEjected() {
        return isCaseEjected;
    }

    private Object validateMaterial(final UUID caseId, final String prosecutingAuthority, final String prosecutorDefendantId, final Material material, final ReferenceDataQueryService referenceDataQueryService, final ZonedDateTime receivedDateTime, Boolean isCpsCase) {
        return validateMaterialWithDocumentDetails(caseId, prosecutingAuthority, prosecutorDefendantId, material, referenceDataQueryService, receivedDateTime, isCpsCase, null);
    }

    private Object validateMaterialV2(AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService) {
        return validateMaterialWithDocumentDetailsV2(addMaterialCommonV2, referenceDataQueryService);
    }


    @SuppressWarnings("java:S107")
    private Object validateMaterialWithDocumentDetails(final UUID caseId, final String prosecutingAuthority, final String prosecutorDefendantId, final Material material, final ReferenceDataQueryService referenceDataQueryService, final ZonedDateTime receivedDateTime, final Boolean isCpsCase,
                                                       final DocumentDetails documentDetails) {
        final CaseDocumentWithReferenceData caseDocumentWithReferenceData = new CaseDocumentWithReferenceData(referralReasonId, isCaseReferredToCourt(), material,
                prosecutorDefendantId, defendants, material.getDocumentType(), isCaseAssigned(), isCaseEjected());

        final List<Problem> rejections = validate(caseDocumentWithReferenceData, referenceDataQueryService, MaterialValidationRuleProvider.getRejectionRules(caseType));

        String cmsDocumentId = null;
        String sectionCode = null;
        Integer materialType;
        CmsDocumentIdentifier cmsDocumentIdentifier = null;
        if (nonNull(documentDetails)) {
            cmsDocumentId = documentDetails.getCmsDocumentId();
            materialType = documentDetails.getMaterialType();
            sectionCode = documentDetails.getSectionCode();
            cmsDocumentIdentifier = CmsDocumentIdentifier.cmsDocumentIdentifier().withDocumentId(cmsDocumentId).withMaterialType(materialType).build();
        }

        if (rejections.isEmpty()) {
            final MaterialAdded.Builder materialAddedBuilder = materialAdded();

            if (caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData() != null) {
                materialAddedBuilder.withDocumentTypeId(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getId().toString());
                materialAddedBuilder.withDocumentCategory(getDocumentCategory(caseDocumentWithReferenceData.getDocumentTypeAccessReferenceData().getDocumentCategory()));
            }

            return materialAddedBuilder
                    .withCaseId(caseId)
                    .withCaseType(caseType.name())
                    .withDefendantId(caseDocumentWithReferenceData.getDefendantId())
                    .withDefendantName(getDefendantName(caseDocumentWithReferenceData.getDefendantId()))
                    .withDocumentType(caseDocumentWithReferenceData.getDocumentType())
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withProsecutorDefendantId(prosecutorDefendantId)
                    .withMaterial(material)
                    .withIsCpsCase(isCpsCase)
                    .withCmsDocumentIdentifier(cmsDocumentIdentifier)
                    .withReceivedDateTime(receivedDateTime)
                    .withSectionCode(sectionCode)
                    .build();
        } else {

            return materialRejected()
                    .withCaseId(caseId)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withProsecutorDefendantId(prosecutorDefendantId)
                    .withMaterial(material)
                    .withErrors(rejections)
                    .withIsCpsCase(isCpsCase)
                    .withCmsDocumentId(cmsDocumentId)
                    .withReceivedDateTime(receivedDateTime)
                    .build();
        }
    }

    private Object validateMaterialWithDocumentDetailsV2(AddMaterialCommonV2 addMaterialCommonV2, final ReferenceDataQueryService referenceDataQueryService) {

        CaseDocumentWithReferenceData caseDocumentWithReferenceData;

        caseDocumentWithReferenceData = new CaseDocumentWithReferenceData(referralReasonId, isCaseReferredToCourt(), defendants, addMaterialCommonV2.getMaterialType(), isCaseAssigned(), isCaseEjected(), addMaterialCommonV2.getCourtApplicationSubject(), addMaterialCommonV2.getProsecutionCaseSubject(), addMaterialCommonV2.getMaterialType(), addMaterialCommonV2.getMaterialContentType(), validDefendantIds);

        return ProsecutionCaseFileHelper.validateMaterialWithDocumentDetailsV2(addMaterialCommonV2, referenceDataQueryService, caseDocumentWithReferenceData, caseType, this.defendants);
    }

    private String getDefendantName(final UUID defendantId) {
        if (isNull(defendantId)) {
            return null;
        }

        return defendants.stream()
                .filter(defendant -> defendant.getId().equals(defendantId.toString()))
                .map(defendant -> {
                    final Individual individual = defendant.getIndividual();
                    final String organisationName = defendant.getOrganisationName();
                    String fullName = null;

                    if (nonNull(individual) && nonNull(individual.getPersonalInformation())) {
                        final PersonalInformation personalInformation = individual.getPersonalInformation();
                        fullName = format("%s %s", personalInformation.getFirstName(), personalInformation.getLastName());
                    } else if (nonNull(organisationName) && !organisationName.isEmpty()) {
                        fullName = defendant.getOrganisationName();
                    }
                    return fullName;
                }).findFirst().orElse(null);
    }

    private String getDocumentCategory(final String referenceDataDocumentCategory) {
        if (DocumentCategory.CASE_LEVEL.toString().equalsIgnoreCase(referenceDataDocumentCategory)) {
            return DocumentCategory.CASE_LEVEL.toString();
        } else {
            return DocumentCategory.DEFENDANT_LEVEL.toString();
        }
    }

    private Object validatePendingMaterial(final MaterialPending materialPending, final ReferenceDataQueryService referenceDataQueryService) {
        return validateMaterialWithDocumentDetails(
                materialPending.getCaseId(),
                materialPending.getProsecutingAuthority(),
                materialPending.getProsecutorDefendantId(),
                materialPending.getMaterial(),
                referenceDataQueryService,
                materialPending.getReceivedDateTime(),
                materialPending.getIsCpsCase(),
                materialPending.getCmsDocumentId() != null ?
                        new DocumentDetails(materialPending.getCmsDocumentId(),
                                materialPending.getMaterialType(),
                                materialPending.getSectionCode()) : null);
    }

    private Object validatePendingMaterialV2(final MaterialPendingV2 materialPending, final ReferenceDataQueryService referenceDataQueryService) {

        final AddMaterialCommonV2 addMaterialCommonV2 = AddMaterialCommonV2.addMaterialCommonV2()
                .withCaseId(materialPending.getCaseId())
                .withCaseSubFolderName(materialPending.getCaseSubFolderName())
                .withCaseType(materialPending.getCaseType())
                .withDefendantId(materialPending.getDefendantId())
                .withDefendantName(materialPending.getDefendantName())
                .withDocumentCategory(materialPending.getDocumentCategory())
                .withDocumentType(materialPending.getDocumentType())
                .withDocumentTypeId(materialPending.getDocumentTypeId())
                .withExhibit(materialPending.getExhibit())
                .withFileName(materialPending.getFileName())
                .withIsCpsCase(materialPending.getIsCpsCase())
                .withMaterial(materialPending.getMaterial())
                .withMaterialContentType(materialPending.getMaterialContentType())
                .withMaterialName(materialPending.getMaterialName())
                .withMaterialType(materialPending.getMaterialType())
                .withProsecutionCaseSubject(materialPending.getProsecutionCaseSubject())
                .withReceivedDateTime(materialPending.getReceivedDateTime())
                .withSectionOrderSequence(materialPending.getSectionOrderSequence())
                .withSubmissionId(materialPending.getSubmissionId())
                .withTag(materialPending.getTag())
                .withWitnessStatement(materialPending.getWitnessStatement())
                .build();

        return validateMaterialWithDocumentDetailsV2(addMaterialCommonV2, referenceDataQueryService);
    }

    private Prosecution recreateProsecution(final int index) {
        return Prosecution.prosecution()
                .withCaseDetails(this.caseDetails)
                .withChannel(this.channel)
                .withDefendants(singletonList(defendants.get(index))).build();
    }

    private Object populateIdpcDefendantMatchedEvent(final UUID cmsCaseId, final String urn, final UUID fileServiceId, final String materialType, final Optional<Defendant> associatedDefendant) {
        return idpcDefendantMatched()
                .withCaseId(cmsCaseId)
                .withCaseUrn(urn)
                .withFileServiceId(fileServiceId)
                .withMaterialType(materialType)
                .withDefendantId(getDefendantId(associatedDefendant))
                .build();
    }

    private UUID getExternalIdFromDefendantIds(final List<UUID> defendantIds) {
        if (isNotEmpty(defendantIds)) {
            final Map.Entry<UUID, List<UUID>> listEntry = this.externalIdToDefendantsMap.entrySet().stream().filter(entry -> !disjoint(entry.getValue(), defendantIds)).findFirst().orElse(null);
            return nonNull(listEntry) ? listEntry.getKey() : null;
        } else if (SJP_INITIATION_CODE.equals(this.caseDetails.getInitiationCode())) {
            return this.externalIdToDefendantsMap.keySet().stream().findFirst().orElse(null);
        }
        return null;
    }

    private UUID getExternalIdFromDefendants(final List<Defendant> defendants) {
        final List<UUID> defendantIds = defendants.stream().filter(defendant -> nonNull(defendant.getId())).map(defendant -> fromString(defendant.getId())).collect(toList());
        return getExternalIdFromDefendantIds(defendantIds);
    }

    private void hydrateExternalIdToDefendantsMap(final UUID externalId, final List<Defendant> defendants) {
        if (nonNull(externalId)) {
            this.externalIdToDefendantsMap.put(externalId, defendants.stream().map(Defendant::getId).map(UUID::fromString).collect(toList()));
        }
    }

    private void removeDefendantsFromWithheldList(final List<Defendant> defendantsToRemove) {
        final Iterator<Defendant> iterator = defendantsWithHeldAfterCaseReceived.iterator();
        while (iterator.hasNext()) {
            final Defendant curDefendant = iterator.next();
            final boolean isDefendantPresent = defendantsToRemove.stream().anyMatch(i -> i.getId().equals(curDefendant.getId()));
            if (isDefendantPresent) {
                iterator.remove();
            }
        }
    }

    public boolean isCaseFiltered() {
        return isCaseFiltered;
    }

    public Stream<Object> approveCaseDefendants(final SummonsApplicationApprovedDetails summonsApplicationApprovedDetails, final List<CaseRefDataEnricher> caseRefDataEnrichers, final List<DefendantRefDataEnricher> defendantRefDataEnrichers, final Boolean isCivil) {
        final Builder<Object> builder = builder();
        final List<Defendant> approvedDefendants = getDefendantsForApplication(summonsApplicationApprovedDetails.getApplicationId());
        final List<DefendantProblem> defendantWarningsForApprovedDefendants = getDefendantWarningsForDefendants(approvedDefendants);
        final SummonsApprovedOutcome summonsApprovedOutcome = summonsApplicationApprovedDetails.getSummonsApprovedOutcome();
        final UUID externalId = getExternalIdFromDefendants(approvedDefendants);
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withCaseDetails(this.caseDetails)
                .withDefendants(approvedDefendants)
                .withChannel(this.channel)
                .withIsCivil(isCivil)
                .withIsGroupMaster(false)
                .withIsGroupMember(false)
                .build());
        prosecutionWithReferenceData.setExternalId(externalId);

        caseRefDataEnrichers.forEach(x -> x.enrich(prosecutionWithReferenceData));
        final DefendantsWithReferenceData defendantsWithReferenceData = buildDefendantWithReferenceData(prosecutionWithReferenceData, defendantRefDataEnrichers);

        if (!this.prosecutionReceived) {
            if (SPI.equals(this.channel) || defendantWarningsForApprovedDefendants.isEmpty()) {
                final CcCaseReceived caseReceived = ccCaseReceived()
                        .withProsecutionWithReferenceData(prosecutionWithReferenceData)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome)
                        .withId(randomUUID())
                        .build();
                return apply(builder.add(caseReceived).build());
            }

            final CcCaseReceivedWithWarnings ccCaseReceivedWithWarnings = ccCaseReceivedWithWarnings()
                    .withProsecutionWithReferenceData(prosecutionWithReferenceData)
                    .withDefendantWarnings(defendantWarnings)
                    .withSummonsApprovedOutcome(summonsApprovedOutcome)
                    .withId(randomUUID())
                    .build();
            return apply(builder.add(ccCaseReceivedWithWarnings).build());
        }

        return builder.add(
                        prosecutionDefendantsAdded()
                                .withCaseId(this.caseId)
                                .withExternalId(externalId)
                                .withDefendants(approvedDefendants)
                                .withReferenceDataVO(defendantsWithReferenceData.getReferenceDataVO())
                                .withDefendantWarnings(defendantWarningsForApprovedDefendants)
                                .withChannel(this.channel)
                                .withSummonsApprovedOutcome(summonsApprovedOutcome)
                                .build())
                .build();
    }

    public Stream<Object> addCourtDocument(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId) {
        final Builder<Object> builder = builder();
        pendingMaterialsForCourtDocumentUpload.stream()
                .filter(materialAddedV2 -> materialAddedV2.getMaterial().toString().equals(fileStoreId))
                .findFirst()
                .ifPresent(materialAddedV2 -> builder.add(getCourtDocuments(courtDocument, materialId, fileStoreId, materialAddedV2)));
        pendingMaterialsWithWarningsForCourtDocumentUpload.stream()
                .filter(materialAddedWithWarnings -> materialAddedWithWarnings.getMaterial().toString().equals(fileStoreId))
                .findFirst()
                .ifPresent(materialAddedWithWarnings -> builder.add(getCourtDocuments(courtDocument, materialId, fileStoreId, materialAddedWithWarnings)));
        return apply(builder.build());
    }

    public CourtDocumentAdded getCourtDocuments(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId, MaterialAddedV2 materialAddedV2) {
        final AddMaterialSubmissionV2 addMaterialSubmissionV2 = AddMaterialSubmissionV2.addMaterialSubmissionV2()
                .withCaseId(materialAddedV2.getCaseId())
                .withCaseType(materialAddedV2.getCaseType())
                .withIsCpsCase(materialAddedV2.getIsCpsCase())
                .withExhibit(materialAddedV2.getExhibit())
                .withMaterial(materialId)
                .withWitnessStatement(materialAddedV2.getWitnessStatement())
                .withCourtApplicationSubject(materialAddedV2.getCourtApplicationSubject())
                .withProsecutionCaseSubject(materialAddedV2.getProsecutionCaseSubject())
                .withCaseSubFolderName(materialAddedV2.getCaseSubFolderName())
                .withFileName(materialAddedV2.getFileName())
                .withMaterialName(materialAddedV2.getMaterialName())
                .withMaterialType(materialAddedV2.getMaterialType())
                .withMaterialContentType(materialAddedV2.getMaterialContentType())
                .withSectionOrderSequence(materialAddedV2.getSectionOrderSequence())
                .withTag(materialAddedV2.getTag())
                .withDefendantId(materialAddedV2.getDefendantId())
                .build();

        return CourtDocumentAdded.courtDocumentAdded()
                .withCourtDocument(courtDocument)
                .withFileStoreId(fileStoreId)
                .withIsUnbundledDocument(false)
                .withMaterialId(materialId)
                .withMaterialSubmittedV2(addMaterialSubmissionV2)
                .withCpsFlag(materialAddedV2.getCpsFlag())
                .build();
    }

    public CourtDocumentAdded getCourtDocuments(final CourtDocument courtDocument, final UUID materialId, final String fileStoreId, MaterialAddedWithWarnings materialAddedWithWarnings) {
        final AddMaterialSubmissionV2 addMaterialSubmissionV2 = AddMaterialSubmissionV2.addMaterialSubmissionV2()
                .withCaseId(materialAddedWithWarnings.getCaseId())
                .withCaseType(materialAddedWithWarnings.getCaseType())
                .withMaterial(materialId)
                .withIsCpsCase(materialAddedWithWarnings.getIsCpsCase())
                .withExhibit(materialAddedWithWarnings.getExhibit())
                .withWitnessStatement(materialAddedWithWarnings.getWitnessStatement())
                .withCourtApplicationSubject(materialAddedWithWarnings.getCourtApplicationSubject())
                .withProsecutionCaseSubject(materialAddedWithWarnings.getProsecutionCaseSubject())
                .withCaseSubFolderName(materialAddedWithWarnings.getCaseSubFolderName())
                .withFileName(materialAddedWithWarnings.getFileName())
                .withMaterialName(materialAddedWithWarnings.getMaterialName())
                .withMaterialType(materialAddedWithWarnings.getMaterialType())
                .withMaterialContentType(materialAddedWithWarnings.getMaterialContentType())
                .withSectionOrderSequence(materialAddedWithWarnings.getSectionOrderSequence())
                .withTag(materialAddedWithWarnings.getTag())
                .withDefendantId(materialAddedWithWarnings.getDefendantId())
                .build();

        return CourtDocumentAdded.courtDocumentAdded()
                .withCourtDocument(courtDocument)
                .withFileStoreId(fileStoreId)
                .withIsUnbundledDocument(false)
                .withMaterialId(materialId)
                .withMaterialSubmittedV2(addMaterialSubmissionV2)
                .build();
    }

    public Stream<Object> rejectCaseDefendants(final SummonsApplicationRejectedDetails summonsApplicationRejectedDetails) {
        final List<UUID> defendantIds = this.applicationIdToDefendantIdsMap.get(summonsApplicationRejectedDetails.getApplicationId());
        final Builder<Object> builder = builder();

        builder.add(summonsApplicationRejected()
                .withCaseId(summonsApplicationRejectedDetails.getCaseId())
                .withApplicationId(summonsApplicationRejectedDetails.getApplicationId())
                .withDefendantIds(defendantIds)
                .withSummonsRejectedOutcome(summonsApplicationRejectedDetails.getSummonsRejectedOutcome())
                .build()
        );

        if (MCC.equals(this.channel) || CPPI.equals(this.channel)) {
            final List<Defendant> rejectedDefendants = defendants.stream().filter(defendant -> defendantIds.contains(fromString(defendant.getId()))).collect(toList());
            builder.add(ccProsecutionRejected()
                    .withProsecution(Prosecution.prosecution()
                            .withCaseDetails(this.caseDetails)
                            .withChannel(this.channel)
                            .withDefendants(rejectedDefendants).build())
                    .withCaseErrors(ImmutableList.of(newProblem(SUMMONS_APPLICATION_REJECTED, "rejectionReasons", summonsApplicationRejectedDetails.getSummonsRejectedOutcome().getReasons())))
                    .build()
            );
        }
        return apply(builder.build());
    }

    public Stream<Object> updateDefendant1(final UUID defendantId, LocalDate dateOfBirth, PersonalInformation personalInformation) {

        final Builder<Object> builder = builder();

        builder.accept((CaseDefendantChanged.caseDefendantChanged()
                .withDefendantId(defendantId)
                .withPersonDetails(personalInformation))
                .withDateOfBirth(dateOfBirth)
                .build());
        return apply(builder.build());
    }

    public Stream<Object> updateDefendant(final UUID caseId, final UUID defendantId, LocalDate dateOfBirth, PersonalInformation personalInformation, final String organisationName) {

        final Builder<Object> builder = builder();

        builder.accept((CaseUpdatedWithDefendant.caseUpdatedWithDefendant()
                .withDefendantId(defendantId)
                .withPersonDetails(personalInformation))
                .withDateOfBirth(dateOfBirth)
                .withCaseId(caseId)
                .withOrganisationName(organisationName)
                .build());
        return apply(builder.build());
    }

    private List<Defendant> getDefendantsForApplication(final UUID applicationId) {
        final List<UUID> defendantIds = this.applicationIdToDefendantIdsMap.get(applicationId);
        if (isEmpty(defendantIds)) {
            final List<UUID> rejectedDefendantIds = this.rejectedApplicationIdToDefendantIdsMap.get(applicationId);
            return this.rejectedApplicationDefendants.stream().filter(defendant -> rejectedDefendantIds.contains(fromString(defendant.getId()))).collect(toList());
        } else {
            return this.defendants.stream().filter(defendant -> defendantIds.contains(fromString(defendant.getId()))).collect(toList());
        }
    }

    private List<DefendantProblem> getDefendantWarningsForDefendants(final List<Defendant> defendants) {
        if (isEmpty(this.defendantWarnings)) {
            return EMPTY_LIST;
        }

        final List<String> approvedProsecutorDefendantReferenceList = defendants.stream().map(Defendant::getProsecutorDefendantReference).collect(toList());
        return this.defendantWarnings.stream().filter(defendantWarning -> approvedProsecutorDefendantReferenceList.contains(defendantWarning.getProsecutorDefendantReference())).collect(toList());
    }

    public Stream<Object> recordGroupIdForSummonsApplication(final UUID caseId, final UUID groupId) {
        return apply(builder().add(GroupIdRecordedForSummonsApplication.groupIdRecordedForSummonsApplication()
                        .withCaseId(caseId)
                        .withGroupId(groupId)
                        .build())
                .build());
    }

    public UUID getGroupId() {
        return this.groupId;
    }

    public Stream<Object> rejectProsecution(final List<Problem> problems) {
        final Stream.Builder<Object> builder = builder();
        builder.accept(ccProsecutionRejected()
                .withProsecution(prosecution()
                        .withCaseDetails(CaseDetails.caseDetails()
                                .withCaseId(this.caseId)
                                .build())
                        .withChannel(this.channel)
                        .build())
                .withCaseErrors(problems)
                .build()
        );
        return apply(builder.build());
    }

    public Stream<Object> updateOffenceCode(final String offenceCode, final OffenceReferenceData offenceReferenceData,
                                            final JsonObjectToObjectConverter jsonObjectToObjectConverter, final ObjectToJsonObjectConverter  objectToJsonObjectConverter) {

        if (METLI.equals(this.getProsecutingAuthority()) &&
                this.defendants.stream().flatMap(d -> d.getOffences().stream())
                        .anyMatch(offence -> GM_00001.equals(offence.getOffenceCode()))) {

            final List<Defendant> defendantList = this.defendants
                    .stream()
                    .map((d) -> {
                        final JsonObjectBuilder defendantJsonObjectBuilder = createObjectBuilder(objectToJsonObjectConverter.convert(d));
                        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

                        d.getOffences().forEach((o) -> {
                            final JsonObjectBuilder offenceJsonObjectBuilder = createObjectBuilder(objectToJsonObjectConverter.convert(o));
                            if (o.getOffenceCode().equals(OFFENCE_CODE)) {
                                offenceJsonObjectBuilder.add("offenceCode", offenceCode);
                                offenceJsonObjectBuilder.add("referenceData", objectToJsonObjectConverter.convert(offenceReferenceData));
                                jsonArrayBuilder.add(offenceJsonObjectBuilder.build());
                            } else {
                                jsonArrayBuilder.add(offenceJsonObjectBuilder.build());
                            }

                        });
                        defendantJsonObjectBuilder.add("offences", jsonArrayBuilder.build());
                        return jsonObjectToObjectConverter.convert(defendantJsonObjectBuilder.build(), Defendant.class);
                    }).toList();

            final Stream.Builder<Object> builder = builder();

            builder.accept(SjpProsecutionUpdateOffenceCodeRequestReceived
                    .sjpProsecutionUpdateOffenceCodeRequestReceived()
                    .withCaseId(caseId)
                    .withDefendants(defendantList).build());

            return apply(builder.build());
        }

        return builder().build();
    }
}
