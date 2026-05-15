package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsCaseContact.cpsCaseContact;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantParentGuardian.defendantParentGuardian;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.LocalAuthorityDetailsForYouthDefendants.localAuthorityDetailsForYouthDefendants;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerSubject.policeOfficerSubject;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms.FormConstant.*;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup.applicationsForDirectionsGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.BcmDefendants.bcmDefendants;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Contacts.contacts;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences.cpsDefendantOffences;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails.cpsOffenceDetails;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence.defence;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants.defendants;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup.displayEquipmentYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers.dynamicFormAnswers;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.FormData.formData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup.pendingLinesOfEnquiryYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData.petFormData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup.pointOfLawYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsCaseProgOfficer.prosCaseProgOfficer;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution.prosecution;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject.prosecutionCaseSubject;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup.prosecutionComplianceNoGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup.prosecutionComplianceYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup.prosecutorServeEvidenceYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormData.ptphFormData;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormdefendants.ptphFormdefendants;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphHeader.ptphHeader;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed.receivedCpsServeBcmProcessed;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed.receivedCpsServePetProcessed;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Section2Q1Group.section2Q1Group;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Section2Q2Group.section2Q2Group;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup.slaveryOrExploitationYesGroup;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.EXPIRED;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.PENDING;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus.SUCCESS;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses.witnesses;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.prosecution.casefile.DateUtil;
import uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsCaseContact;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantParentGuardian;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantTrialRepresentative;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.LocalAuthorityDetailsForYouthDefendants;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerSubject;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.DefenceService;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CpsFormValidator;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AddressAndContact;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvAtPthp;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvForTrial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateAtPTPH;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.AdvocateForTrial;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ApplicationsForDirectionsGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BcmDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseProgressionOfficer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Contacts;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendant;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffenceDetails;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffice;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DefenceInformation;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DefendantSubject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DisplayEquipmentYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.DynamicFormAnswers;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.FormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.FormDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.IntermediaryForWitness;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OfficerInCase;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PendingLinesOfEnquiryYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PointOfLawYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsCaseProgOfficer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Prosecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceNoGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionComplianceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionInformation;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorServeEvidenceYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormdefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphHeader;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphWitnesses;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePtphProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsUpdateCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReviewingLawyer;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q10Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q11Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q12Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q13Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q14Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q15Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q16Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q1Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q2Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q3Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q4Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q5Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q6Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q7Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q8Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Section1Q9Group;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SlaveryOrExploitationYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.VariationStandardDirectionsProsecutorYesGroup;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1168", "squid:S1602","squid:S1066","squid:S1188","squid:S2447", "squid:S3776", "squid:S134", "squid:MethodCyclomaticComplexity"})
public class CpsServeMaterialAggregate implements Aggregate {
    /* NOTE: streamId for this aggregate is NOT caseId BUT UUID.nameUUIDFromBytes(caseUrn)
     * */

    private static final long serialVersionUID = 3368153075313169339L;

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsServeMaterialAggregate.class);

    public static final String ANY_OTHER = "anyOther";
    public static final String EVIDENCE_PRE_PTPH1 = "evidencePrePTPH";
    public static final String EVIDENCE_POST_PTPH1 = "evidencePostPTPH";
    public static final String SUBMISSION_STATUS = "REJECTED";
    public static final String FIRST_NAME_OF_INTERMEDIARY_KNOWN_AT_PTPH = "firstNameOfIntermediaryKnownAtPtph";
    public static final String LAST_NAME_OF_INTERMEDIARY_KNOWN_AT_PTPH = "lastNameOfIntermediaryKnownAtPtph";
    public static final String INTERMEDIARY_FOR_WITNESS = "intermediaryForWitness";
    public static final String POLICE_OFFICER_SUBJECT = "policeOfficerSubject";
    public static final String RELEVANT_DISPUTED_ISSUE = "relevantDisputedIssue";
    public static final String WITNESS_FIRST_NAME = "witnessFirstName";
    public static final String WITNESS_LAST_NAME = "witnessLastName";

    private static final String DEFENDANT_SUBJECT = "defendantSubject";
    private static final String TAG = "tag";
    private static final String TRIAL_DATE = "trialDate";
    private static final String LAST_RECORDED_TIME_ESTIMATE = "lastRecordedTimeEstimate";
    private static final String HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED = "hasAllEvidenceToBeReliedOnBeenServed";
    private static final String HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED_DETAILS = "hasAllEvidenceToBeReliedOnBeenServedDetails";
    private static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED = "hasAllDisclosureBeenProvided";
    private static final String HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS = "hasAllDisclosureBeenProvidedDetails";
    private static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH = "haveOtherDirectionsBeenCompliedWith";
    private static final String HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS = "haveOtherDirectionsBeenCompliedWithDetails";
    private static final String PROSECUTION_WITNESSES = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend";
    private static final String PROSECUTION_WITNESSES_DETAILS = "haveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails";
    private static final String WITNESSES_SUMMONSES = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServed";
    private static final String WITNESSES_SUMMONSES_DETAILS = "haveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails";
    private static final String SPECIAL_MEASURES_FOR_WITNESSES = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved";
    private static final String SPECIAL_MEASURES_FOR_WITNESSES_DETAILS = "haveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails";
    private static final String INTERPRETERS_FOR_WITNESSES = "haveInterpretersForWitnessesBeenArranged";
    private static final String INTERPRETERS_FOR_WITNESSES_DETAILS = "haveInterpretersForWitnessesBeenArrangedDetails";
    private static final String INTERVIEWS = "haveEditedAbeInterviewsBeenPreparedAndAgreed";
    private static final String INTERVIEWS_DETAILS = "haveEditedAbeInterviewsBeenPreparedAndAgreedDetails";
    private static final String STATEMENT_OF_POINTS = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement";
    private static final String STATEMENT_OF_POINTS_DETAILS = "haveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails";
    private static final String CASE_READY = "isTheCaseReadyToProceedWithoutDelayBeforeTheJury";
    private static final String CASE_READY_DETAILS = "isTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails";
    private static final String TIME_ESTIMATE = "isTheTimeEstimateCorrect";
    private static final String TIME_ESTIMATE_DETAILS = "isTheTimeEstimateCorrectDetails";
    private static final String FURTHER_INFORMATION = "furtherInformationToAssistTheCourt";
    private static final String PROSECUTOR_TRIAL_READY = "certifyThatTheProsecutionIsTrialReady";
    private static final String PROSECUTOR_TRIAL_READY_DETAILS = "certifyThatTheProsecutionIsTrialReadyDetails";
    private static final String PTR_VACATED = "applyForThePtrToBeVacated";
    private static final String PTR_VACATED_DETAILS = "applyForThePtrToBeVacatedDetails";
    private static final String PROSECUTION_BY_FORM = "formCompletedOnBehalfOfTheProsecutionBy";
    private static final String CERTIFICATION_DATE = "certificationDate";
    public static final String DATE_TEN_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String FORM_COMPLETED_ON_BEHALF_OF_PROSECUTION_BY = "formCompletedOnBehalfOfProsecutionBy";
    public static final String FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION = "furtherProsecutionInformationProvidedAfterCertification";
    public static final String DEFENDANT_ID="defendantId";

    private ReceivedCpsServePetProcessed receivedCpsServePetProcessed;
    private ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed;

    private final Set<PendingType> pendingSet = new HashSet<>();
    private Boolean isPending = FALSE;
    private PendingType formType = PendingType.UNKNOWN;
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String SOW_REF_VALUE = "MoJ";

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(ReceivedCpsServePetProcessed.class).apply(e -> {
                    if (e.getSubmissionStatus().equals(PENDING)) {
                        this.formType = PendingType.PET;
                        this.isPending = TRUE;
                        this.receivedCpsServePetProcessed = e;
                        if (!this.pendingSet.contains(PendingType.PET)) {
                            this.pendingSet.add(PendingType.PET);
                        }
                    } else {
                        this.isPending = FALSE;
                        this.pendingSet.remove(PendingType.PET);
                    }
                }),
                when(ReceivedCpsServeBcmProcessed.class).apply(e -> {
                    if (e.getSubmissionStatus().equals(PENDING)) {
                        this.formType = PendingType.BCM;
                        this.isPending = TRUE;
                        this.receivedCpsServeBcmProcessed = e;
                        if (!this.pendingSet.contains(PendingType.BCM)) {
                            this.pendingSet.add(PendingType.BCM);
                        }
                    } else {
                        this.isPending = FALSE;
                        this.pendingSet.remove(PendingType.BCM);
                    }
                }),
                otherwiseDoNothing());
    }

    public Stream<Object> cpsReceivePet(final JsonObject processReceivedCpsServePetJson,
                                        final String submissionStatus,
                                        final UUID caseId,
                                        final Optional<JsonObject> prosecutionCase,
                                        final CpsFormValidator cpsFormValidator,
                                        final List<String> validOffences,
                                        final JsonArray defendantIds,
                                        final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                        final ProsecutionCaseFile prosecutionCaseFile,
                                        final ProgressionService progressionService,
                                        final DefenceService defenceService) {

        return apply(of(convertToCpsServePetSubmitted(processReceivedCpsServePetJson, submissionStatus, caseId, prosecutionCase, cpsFormValidator, validOffences, defendantIds, jsonObjectToObjectConverter,prosecutionCaseFile, progressionService, defenceService)));
    }


    public Stream<Object> cpsReceivePtph(final JsonObject processReceivedCpsServePtphJson,
                                         final String submissionStatus,
                                         final UUID caseId,
                                         final Optional<JsonObject> prosecutionCase,
                                         final CpsFormValidator cpsFormValidator,
                                         final JsonArray defendantIds,
                                         final Optional<OrganisationUnitReferenceData> ouRefData,
                                         final JsonObjectToObjectConverter jsonObjectToObjectConverter,
                                         final ObjectToJsonObjectConverter objectToJsonObjectConverter
    ) {
        return apply(of(convertToCpsServePtphSubmitted(processReceivedCpsServePtphJson,
                submissionStatus,
                caseId,
                prosecutionCase,
                cpsFormValidator,
                defendantIds, ouRefData,
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter)));
    }


    private PtphFormData convertToPtphFormData(final JsonObject processReceivedCpsServePtphJson,
                                               final Optional<OrganisationUnitReferenceData> ouRefData,
                                               final JsonObject formData,
                                               final JsonObjectToObjectConverter jsonObjectToObjectConverter) {
        final PtphFormData.Builder ptphFormDataBuilder = ptphFormData();
        ptphFormDataBuilder
                .withPtphFormdefendants(convertToPtphFormdefendants(jsonObjectToObjectConverter, formData))
                .withPtphHeader(convertToPtphHeader(processReceivedCpsServePtphJson))
                .withPtphWitnesses(convertToWitnessPtphList(processReceivedCpsServePtphJson))
                .withContacts(convertToContacts(processReceivedCpsServePtphJson, ouRefData))
                .withProsecutionInformation(convertToProsecutionInformation(processReceivedCpsServePtphJson));

        final List<DefenceInformation> defenceInformationList = createDefenceInformationList(formData, processReceivedCpsServePtphJson);

        if (CollectionUtils.isNotEmpty(defenceInformationList)) {
            ptphFormDataBuilder.withDefenceInformation(defenceInformationList);
        }

        final PtphFormData ptphFormData = ptphFormDataBuilder.build();
        LOGGER.info("ptphFormData Urn: {}", ptphFormData.getPtphHeader());

        return ptphFormData;
    }

    private List<DefenceInformation> createDefenceInformationList(final JsonObject formData, final JsonObject processReceivedCpsServePtphJson) {
        final List<DefenceInformation> defenceInformationList = new ArrayList<>();

        final String criminalProceedings = processReceivedCpsServePtphJson.getString(PARTICULARS_OF_ANY_RELATED_CRIMINAL_PROCEEDINGS, null);
        final String familyProceedings = processReceivedCpsServePtphJson.getString(PARTICULARS_OF_ANY_FAMILY, null);
        if (isNotEmpty(criminalProceedings) || isNotEmpty(familyProceedings)) {

            if (nonNull(formData) && nonNull(formData.getJsonArray(PTPH_DEFENDANTS))) {
                formData.getJsonArray(PTPH_DEFENDANTS)
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .forEach(defendant -> {
                            final DefenceInformation.Builder defenceInformationBuilder = DefenceInformation.defenceInformation();
                            defenceInformationBuilder.withId(getDefendantIdFromDefendants(defendant));

                            if (isNotEmpty(criminalProceedings)) {
                                defenceInformationBuilder
                                        .withSection2Q1Group(section2Q1Group()
                                                .withQ1(criminalProceedings)
                                                .build());
                            }

                            if (isNotEmpty(familyProceedings)) {
                                defenceInformationBuilder
                                        .withSection2Q2Group(section2Q2Group()
                                                .withQ2(familyProceedings)
                                                .build());
                            }

                            defenceInformationList.add(defenceInformationBuilder.build());
                        });
            }
        }

        return defenceInformationList;
    }

    private ProsecutionInformation convertToProsecutionInformation(JsonObject processReceivedCpsServePtphJson) {
        return ProsecutionInformation.prosecutionInformation()
                .withSection1Q1Group(convertToQ1(getValueAsString(processReceivedCpsServePtphJson, DRAFT_INDICTMENT),
                        getValueAsString(processReceivedCpsServePtphJson, DRAFT_INDICTMENT + NOTES)))
                .withSection1Q2Group(convertToQ2(getValueAsString(processReceivedCpsServePtphJson, SUMMARY_OF_CIRCUMSTANCES),
                        getValueAsString(processReceivedCpsServePtphJson, SUMMARY_OF_CIRCUMSTANCES + NOTES)))
                .withSection1Q3Group(convertToQ3(getValueAsString(processReceivedCpsServePtphJson, STATEMENT_FOR_PA_AND_ICM),
                        getValueAsString(processReceivedCpsServePtphJson, STATEMENT_FOR_PA_AND_ICM + NOTES)))
                .withSection1Q4Group(convertToQ4(getValueAsString(processReceivedCpsServePtphJson, EXHIBITS_FOR_PA_AND_ICM),
                        getValueAsString(processReceivedCpsServePtphJson, EXHIBITS_FOR_PA_AND_ICM + NOTES)))
                .withSection1Q5Group(convertToQ5(getValueAsString(processReceivedCpsServePtphJson, CCTV),
                        getValueAsString(processReceivedCpsServePtphJson, CCTV + NOTES)))
                .withSection1Q6Group(convertToQ6(getValueAsString(processReceivedCpsServePtphJson, STREAMLINED_FORENSIC_REPORT),
                        getValueAsString(processReceivedCpsServePtphJson, STREAMLINED_FORENSIC_REPORT + NOTES)))
                .withSection1Q7Group(convertToQ7(getValueAsString(processReceivedCpsServePtphJson, MEDICAL_EVIDENCE),
                        getValueAsString(processReceivedCpsServePtphJson, MEDICAL_EVIDENCE + NOTES)))
                .withSection1Q8Group(convertToQ8(getValueAsString(processReceivedCpsServePtphJson, EXPERT_EVIDENCE),
                        getValueAsString(processReceivedCpsServePtphJson, EXPERT_EVIDENCE + NOTES)))
                .withSection1Q9Group(convertToQ9(getValueAsString(processReceivedCpsServePtphJson, BAD_CHARACTER),
                        getValueAsString(processReceivedCpsServePtphJson, BAD_CHARACTER + NOTES)))
                .withSection1Q10Group(convertToQ10(getValueAsString(processReceivedCpsServePtphJson, HEARSAY),
                        getValueAsString(processReceivedCpsServePtphJson, HEARSAY + NOTES)))
                .withSection1Q11Group(convertToQ11(getValueAsString(processReceivedCpsServePtphJson, SPECIAL_MEASURES),
                        getValueAsString(processReceivedCpsServePtphJson, SPECIAL_MEASURES + NOTES)))
                .withSection1Q12Group(convertToQ12(getValueAsString(processReceivedCpsServePtphJson, CRIMINAL_RECORD),
                        getValueAsString(processReceivedCpsServePtphJson, CRIMINAL_RECORD + NOTES)))
                .withSection1Q13Group(convertToQ13(getValueAsString(processReceivedCpsServePtphJson, VICTIM_PERSONAL_STATEMENT),
                        getValueAsString(processReceivedCpsServePtphJson, VICTIM_PERSONAL_STATEMENT + NOTES)))
                .withSection1Q14Group(convertToQ14(getValueAsString(processReceivedCpsServePtphJson, DISCLOSURE_MANAGEMENT_DOC),
                        getValueAsString(processReceivedCpsServePtphJson, DISCLOSURE_MANAGEMENT_DOC + NOTES)))
                .withSection1Q15Group(convertToQ15(getValueAsString(processReceivedCpsServePtphJson, THIRD_PARTY),
                        getValueAsString(processReceivedCpsServePtphJson, THIRD_PARTY + NOTES)))
                .withSection1Q16Group(convertToQ16(getValueAsString(processReceivedCpsServePtphJson, REVIEW_DISCLOSABLE_MATERIAL),
                        getValueAsString(processReceivedCpsServePtphJson, REVIEW_DISCLOSABLE_MATERIAL + NOTES)))
                .build();
    }

    private Section1Q16Group convertToQ16(String question, String notes) {
        return Section1Q16Group
                .section1Q16Group()
                .withQ16(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q15Group convertToQ15(String question, String notes) {
        return Section1Q15Group
                .section1Q15Group()
                .withQ15(question).withParticulars(notes).build();
    }

    private Section1Q14Group convertToQ14(String question, String notes) {
        return Section1Q14Group.section1Q14Group()
                .withQ14(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q13Group convertToQ13(String question, String notes) {
        return Section1Q13Group.section1Q13Group().
                withQ13(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q12Group convertToQ12(String question, String notes) {
        return Section1Q12Group
                .section1Q12Group()
                .withQ12(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q11Group convertToQ11(String question, String notes) {
        return Section1Q11Group.section1Q11Group()
                .withQ11(question).withParticulars(notes)
                .build();

    }

    private Section1Q10Group convertToQ10(String question, String notes) {
        return Section1Q10Group
                .section1Q10Group()
                .withQ10(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q9Group convertToQ9(String question, String notes) {
        return Section1Q9Group
                .section1Q9Group()
                .withQ9(question)
                .withParticulars(notes)
                .build();

    }

    private Section1Q8Group convertToQ8(String question, String notes) {
        return Section1Q8Group
                .section1Q8Group()
                .withQ8(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q7Group convertToQ7(String question, String notes) {
        return Section1Q7Group
                .section1Q7Group()
                .withQ7(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q6Group convertToQ6(String question, String notes) {
        return Section1Q6Group
                .section1Q6Group()
                .withQ6(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q5Group convertToQ5(String question, String notes) {
        return Section1Q5Group
                .section1Q5Group()
                .withQ5(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q4Group convertToQ4(String question, String notes) {
        return Section1Q4Group
                .section1Q4Group()
                .withQ4(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q3Group convertToQ3(String question, String notes) {
        return Section1Q3Group
                .section1Q3Group()
                .withQ3(question)
                .withParticulars(notes)
                .build();
    }

    private Section1Q2Group convertToQ2(String question, String notes) {
        return Section1Q2Group
                .section1Q2Group()
                .withQ2(question)
                .withParticulars(notes).build();
    }

    private Section1Q1Group convertToQ1(String question, String notes) {
        return Section1Q1Group
                .section1Q1Group()
                .withQ1(question)
                .withParticulars(notes)
                .build();
    }

    private Contacts convertToContacts(JsonObject processReceivedCpsServePtphJson, Optional<OrganisationUnitReferenceData> ouRefData) {
        final CpsOffice cpsOffice = convertToCpsOffice(ouRefData);
        return contacts().withAdvAtPthp(convertToAdvAtPtph(processReceivedCpsServePtphJson))
                .withAdvForTrial(convertToAdvForTrial(processReceivedCpsServePtphJson))
                .withCpsOffice(cpsOffice)
                .withOfficerInCase(convertToOfficerInTheCase(processReceivedCpsServePtphJson))
                .withReviewingLawyer(convertToReviewingLawyer(processReceivedCpsServePtphJson))
                .withProsCaseProgOfficer(convertToProgOfficer(processReceivedCpsServePtphJson)).build();

    }

    private List<PtphWitnesses> convertToWitnessPtphList(final JsonObject processReceivedCpsServePtphJson) {
        return convertProsecutionWitnessPtph(processReceivedCpsServePtphJson.getJsonArray(WITNESSES));
    }

    private List<PtphFormdefendants> convertToPtphFormdefendants(final JsonObjectToObjectConverter jsonObjectToObjectConverter, final JsonObject formData) {
        final List<PtphFormdefendants> ptphFormDefendants = new ArrayList<>();
        if (!isNull(formData) && !isNull(formData.getJsonArray(PTPH_DEFENDANTS))) {
            formData.getJsonArray(PTPH_DEFENDANTS)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .forEach(defendant -> ptphFormDefendants.add(constructPtphDefendant(jsonObjectToObjectConverter, defendant)));
        }
        return ptphFormDefendants;
    }

    private PtphFormdefendants constructPtphDefendant(final JsonObjectToObjectConverter jsonObjectToObjectConverter, final JsonObject defendant) {
        return ptphFormdefendants()
                .withAdvocateAtPTPH(convertToAdvocateAtPtph(jsonObjectToObjectConverter, defendant))
                .withAdvocateForTrial(convertToAdvocateForTrial(jsonObjectToObjectConverter, defendant))
                .withCaseProgressionOfficer(convertToProgressionOfficer(jsonObjectToObjectConverter, defendant))
                .withPrincipalCharges(getPrincipalChargesForPtph(defendant))
                .withId(getDefendantIdFromDefendants(defendant))
                .build();
    }

    private String getDefendantIdFromDefendants(final JsonObject defendant) {
        return getValueAsString(defendant, ID);
    }

    private String getPrincipalChargesForPtph(final JsonObject defendant) {
        return getValueAsString(defendant, PRINCIPAL_CHARGES);
    }

    private CaseProgressionOfficer convertToProgressionOfficer(final JsonObjectToObjectConverter jsonObjectToObjectConverter, final JsonObject defendant) {
        final JsonObject progressionOfficer = getValueAsJsonObject(defendant, PROSECUTION_CASE_PROGRESSION_OFFICER);

        if (isNull(progressionOfficer)) {
            return null;
        }

        return jsonObjectToObjectConverter.convert(progressionOfficer, CaseProgressionOfficer.class);
    }

    private AdvocateForTrial convertToAdvocateForTrial(final JsonObjectToObjectConverter jsonObjectToObjectConverter, final JsonObject defendant) {
        final JsonObject contact = getValueAsJsonObject(defendant, TRIAL_ADVOCATE);

        if (isNull(contact)) {
            return null;
        }

        return jsonObjectToObjectConverter.convert(contact, AdvocateForTrial.class);
    }

    private AdvForTrial convertToAdvForTrial(JsonObject processReceivedCpsServePtphJson) {
        final JsonObject contact = getValueAsJsonObject(processReceivedCpsServePtphJson, TRIAL_ADVOCATE);

        if (isNull(contact)) {
            return null;
        }

        final String name = getValueAsString(contact, NAME);
        AdvForTrial advocateForTrial = null;
        if (isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());

                advocateForTrial = AdvForTrial.advForTrial().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                advocateForTrial = AdvForTrial.advForTrial().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }
        }

        return advocateForTrial;
    }

    private PtphHeader convertToPtphHeader(JsonObject processReceivedCpsServePtphJson) {
        return ptphHeader()
                .withUrn(processReceivedCpsServePtphJson.getJsonObject(PROSECUTION_CASE_SUBJECT).getString(URN))
                .build();

    }

    private OfficerInCase convertToOfficerInTheCase(JsonObject processReceivedCpsServePtphJson) {
        final JsonObject contact = getValueAsJsonObject(processReceivedCpsServePtphJson, OFFICER_IN_THE_CASE);

        if (isNull(contact)) {
            return null;
        }
        final String name = getValueAsString(contact, NAME);
        OfficerInCase officerInCase = null;
        if (isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());

                officerInCase = OfficerInCase.officerInCase().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {

                officerInCase = OfficerInCase.officerInCase().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();

            }
        }
        return officerInCase;
    }

    private ReviewingLawyer convertToReviewingLawyer(JsonObject processReceivedCpsServePtphJson) {
        final JsonObject contact = getValueAsJsonObject(processReceivedCpsServePtphJson, REVIEWING_LAWYER);

        if (isNull(contact)) {
            return null;
        }

        final String name = getValueAsString(contact, NAME);
        ReviewingLawyer reviewingLawyer = null;
        if (isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());
                reviewingLawyer = ReviewingLawyer.reviewingLawyer().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                reviewingLawyer = ReviewingLawyer.reviewingLawyer().withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }
        }

        return reviewingLawyer;
    }

    private ProsCaseProgOfficer convertToProgOfficer(JsonObject processReceivedCpsServePtphJson) {
        final JsonObject contact = getValueAsJsonObject(processReceivedCpsServePtphJson, PROSECUTION_CASE_PROGRESSION_OFFICER);

        if (isNull(contact)) {
            return null;
        }
        ProsCaseProgOfficer prosCaseProgOfficer = null;
        final String name = getValueAsString(contact, NAME);
        if (isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {

                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());


                prosCaseProgOfficer = prosCaseProgOfficer()
                        .withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            } else {
                prosCaseProgOfficer = prosCaseProgOfficer()
                        .withEmail(getValueAsString(contact, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(contact, PHONE))
                        .build();
            }
        }

        return prosCaseProgOfficer;
    }

    private AdvocateAtPTPH convertToAdvocateAtPtph(
            final JsonObjectToObjectConverter jsonObjectToObjectConverter, final JsonObject defendant) {
        final JsonObject contact = getValueAsJsonObject(defendant, PTPH_ADVOCATE);

        if (isNull(contact)) {
            return null;
        }

        return jsonObjectToObjectConverter.convert(contact, AdvocateAtPTPH.class);
    }

    private AdvAtPthp convertToAdvAtPtph(JsonObject processReceivedCpsServePtphJson) {
        final JsonObject ptphAdvocate = getValueAsJsonObject(processReceivedCpsServePtphJson, PTPH_ADVOCATE);

        if (isNull(ptphAdvocate)) {
            return null;
        }

        final String name = getValueAsString(ptphAdvocate, NAME);
        AdvAtPthp advocateAtPTPH = null;
        if (isNotEmpty(name)) {
            if (name.contains(StringUtils.SPACE)) {
                final String firstName = name.substring(0, name.indexOf(' '));
                final String lastName = name.substring(name.lastIndexOf(' ') + 1, name.length());
                advocateAtPTPH = AdvAtPthp.advAtPthp().withEmail(getValueAsString(ptphAdvocate, EMAIL))
                        .withFirstName(firstName)
                        .withLastName(lastName)
                        .withPhone(getValueAsString(ptphAdvocate, PHONE))
                        .build();
            } else {
                advocateAtPTPH = AdvAtPthp.advAtPthp().withEmail(getValueAsString(ptphAdvocate, EMAIL))
                        .withFirstName(name)
                        .withPhone(getValueAsString(ptphAdvocate, PHONE))
                        .build();
            }

            return advocateAtPTPH;
        }
        return null;
    }

    private ReceivedCpsServePtphProcessed convertToCpsServePtphSubmitted(
            final JsonObject processReceivedCpsServePtphJson,
            final String submissionStatus,
            final UUID caseId,
            final Optional<JsonObject> prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final JsonArray defendantIds,
            final Optional<OrganisationUnitReferenceData> ouRefData,
            final JsonObjectToObjectConverter jsonObjectToObjectConverter,
            final ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        final ReceivedCpsServePtphProcessed.Builder builder = ReceivedCpsServePtphProcessed.receivedCpsServePtphProcessed()
                .withCaseId(caseId)
                .withSubmissionId(fromString(processReceivedCpsServePtphJson.getString(SUBMISSION_ID)))
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withProsecutionCaseSubject(convertCpsProsecutionCaseSubject(getValueAsJsonObject(processReceivedCpsServePtphJson, PROSECUTION_CASE_SUBJECT)))
                .withCpsDefendant(convertToCpsDefendants(processReceivedCpsServePtphJson.getJsonArray(CPS_DEFENDANTS)));

        if (prosecutionCase.isPresent()) {
            runDefendantMatchingAndRebuildPtphFormData(processReceivedCpsServePtphJson,
                    prosecutionCase.get(),
                    cpsFormValidator, builder,
                    defendantIds,
                    ouRefData,
                    jsonObjectToObjectConverter,
                    objectToJsonObjectConverter);
        } else {
            final List<Problem> errorList = new ArrayList<>();
            errorList.add(Problem.problem()
                    .withCode(ValidationError.CASE_URN_NOT_FOUND.getCode())
                    .withValues(asList(ProblemValue.problemValue()
                            .withKey(URN)
                            .withValue(processReceivedCpsServePtphJson.getJsonObject("prosecutionCaseSubject").getString("urn"))
                            .build()))
                    .build());

            builder.withSubmissionId(fromString(processReceivedCpsServePtphJson.getString(SUBMISSION_ID)))
                    .withSubmissionStatus(SubmissionStatus.REJECTED)
                    .withErrors(errorList);
        }

        return builder.build();
    }

    private void runDefendantMatchingAndRebuildPtphFormData(
            final JsonObject processReceivedCpsServePtphJson,
            final JsonObject prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final ReceivedCpsServePtphProcessed.Builder builder,
            final JsonArray defendantIds,
            final Optional<OrganisationUnitReferenceData> ouRefData,
            final JsonObjectToObjectConverter jsonObjectToObjectConverter,
            final ObjectToJsonObjectConverter objectToJsonObjectConverter) {
        final FormValidationResult formValidationResult = cpsFormValidator
                .validateAndRebuildingFormDataPtph(processReceivedCpsServePtphJson,
                        prosecutionCase,
                        defendantIds,
                        objectToJsonObjectConverter);

        final List<FormDefendants> formDefendantsList = new ArrayList<>();

        if (nonNull(formValidationResult.getFormDefendants())) {
            formValidationResult
                    .getFormDefendants()
                    .getJsonArray(FORM_DEFENDANTS)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .map(formDefendant -> new FormDefendants(formDefendant.getString(CPS_DEFENDANT_ID, null), fromString(formDefendant.getString(DEFENDANT_ID))))
                    .forEach(formDefendantsList::add);
        }

        builder.withFormDefendants(formDefendantsList)
                .withPtphFormData(convertToPtphFormData(processReceivedCpsServePtphJson,
                        ouRefData,
                        formValidationResult.getFormData(),
                        jsonObjectToObjectConverter))
                .withErrors(formValidationResult.getErrorList())
                .withSubmissionStatus(formValidationResult.getSubmissionStatus());

    }

    private CpsOffice convertToCpsOffice(
            final Optional<OrganisationUnitReferenceData> ouRefData) {

        return ouRefData.map(ouData -> CpsOffice.cpsOffice()
                .withAddress1(ouData.getAddress1())
                .withAddress2(ouData.getAddress2())
                .withAddress3(ouData.getAddress3())
                .withAddress4(ouData.getAddress4())
                .withAddress5(ouData.getAddress5())
                .withPostcode(ouData.getPostcode())
                .build())
                .orElse(null);
    }

    public Stream<Object> cpsReceiveBcm(final JsonObject processReceivedCpsServeBcmJson,
                                        final String submissionStatus,
                                        final UUID caseId,
                                        final Optional<JsonObject> prosecutionCase,
                                        final CpsFormValidator cpsFormValidator,
                                        final List<String> validOffences,
                                        final JsonArray defendantIds) {
        return apply(of(convertToCpsServeBcmSubmitted(processReceivedCpsServeBcmJson, submissionStatus, caseId,
                prosecutionCase, cpsFormValidator, validOffences, defendantIds)));
    }


    private ReceivedCpsServePetProcessed convertToCpsServePetSubmitted(
            final JsonObject processReceivedCpsServePetJson,
            final String submissionStatus,
            final UUID caseId,
            final Optional<JsonObject> prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final List<String> validOffences,
            final JsonArray defendantIds,
            final JsonObjectToObjectConverter jsonObjectToObjectConverter,
            final ProsecutionCaseFile prosecutionCaseFile,
            final ProgressionService progressionService,
            final DefenceService defenceService) {


        final ReceivedCpsServePetProcessed.Builder builder = receivedCpsServePetProcessed()
                .withSubmissionId(fromString(processReceivedCpsServePetJson.getString(SUBMISSION_ID)))
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withProsecutionCaseSubject(convertCpsProsecutionCaseSubject(getValueAsJsonObject(processReceivedCpsServePetJson, PROSECUTION_CASE_SUBJECT)))
                .withCpsDefendantOffences(convertCpsDefendantOffences(processReceivedCpsServePetJson.getJsonArray(CPS_DEFENDANT_OFFENCES)))
                .withProsecutionCaseProgressionOfficer(convertCpsCaseContact(getValueAsJsonObject(processReceivedCpsServePetJson, PROSECUTION_CASE_PROGRESSION_OFFICER)))
                .withReviewingLawyer(convertCpsCaseContact(getValueAsJsonObject(processReceivedCpsServePetJson, REVIEWING_LAWYER)))
                .withCaseId(caseId)
                .withIsYouth(processReceivedCpsServePetJson.getBoolean(IS_YOUTH));

        if (prosecutionCase.isPresent()) {
            runDefendantMatchingAndRebuildPetFormData(processReceivedCpsServePetJson, prosecutionCase.get(), cpsFormValidator, builder, jsonObjectToObjectConverter, validOffences, defendantIds,prosecutionCaseFile, progressionService, defenceService);
        } else {
            builder.withPetFormData(convertPetFormData(getValueAsJsonObject(processReceivedCpsServePetJson, PET_FORM_DATA)));
        }

        return builder.build();
    }

    private ReceivedCpsServeBcmProcessed convertToCpsServeBcmSubmitted(
            final JsonObject processReceivedCpsServeBcmJson,
            final String submissionStatus,
            final UUID caseId,
            final Optional<JsonObject> prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final List<String> validOffences,
            final JsonArray defendantIds) {

        final ReceivedCpsServeBcmProcessed.Builder builder = receivedCpsServeBcmProcessed()
                .withCaseId(caseId)
                .withSubmissionId(fromString(processReceivedCpsServeBcmJson.getString(SUBMISSION_ID)))
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withProsecutionCaseSubject(convertCpsProsecutionCaseSubject(getValueAsJsonObject(processReceivedCpsServeBcmJson, PROSECUTION_CASE_SUBJECT)))
                .withCpsDefendantOffences(convertCpsDefendantOffences(processReceivedCpsServeBcmJson.getJsonArray(CPS_DEFENDANT_OFFENCES)));
        if (prosecutionCase.isPresent()) {
            runDefendantMatchingAndRebuildBcmFormData(processReceivedCpsServeBcmJson, prosecutionCase.get(), cpsFormValidator, builder, validOffences, defendantIds);
        } else {
            builder.withFormData(convertBcmFormDataWhenNoProsecutionCase(processReceivedCpsServeBcmJson));
        }
        return builder.build();
    }

    private FormData convertBcmFormDataWhenNoProsecutionCase(
            final JsonObject processReceivedCpsServeBcmJson) {
        final List<BcmDefendants> bcmDefendants = new ArrayList<>();
        final FormData formData = new FormData(bcmDefendants);
        processReceivedCpsServeBcmJson.getJsonArray(CPS_DEFENDANT_OFFENCES).getValuesAs(JsonObject.class)
                .forEach(cpsDefendant -> {
                    final BcmDefendants.Builder builder = bcmDefendants();
                    constructBcmDefendants(processReceivedCpsServeBcmJson, bcmDefendants, cpsDefendant, builder);
                });

        return formData;
    }

    private void constructBcmDefendants(final JsonObject processReceivedCpsServeBcmJson,
                                        final List<BcmDefendants> bcmDefendants, final JsonObject cpsDefendant,
                                        final BcmDefendants.Builder builder) {
        if (cpsDefendant.containsKey(ID)) {
            builder.withId(fromString(getValueAsString(cpsDefendant, ID)));
        }

        builder.withProsecutorOffences(convertCpsOffences(cpsDefendant.getJsonArray(CPS_OFFENCES)));


        if (processReceivedCpsServeBcmJson.containsKey(OTHER_INFORMATION)) {
            builder.withAnyOther(processReceivedCpsServeBcmJson.getString(OTHER_INFORMATION));
        }

        if (processReceivedCpsServeBcmJson.containsKey(EVIDENCE_PRE_PTPH1)) {
            builder.withOtherAreasBeforePtph(processReceivedCpsServeBcmJson.getString(EVIDENCE_PRE_PTPH1));
        }

        if (processReceivedCpsServeBcmJson.containsKey(EVIDENCE_POST_PTPH1)) {
            builder.withOtherAreasAfterPtph(processReceivedCpsServeBcmJson.getString(EVIDENCE_POST_PTPH1));
        }
        bcmDefendants.add(builder.build());
    }

    private CpsCaseContact convertCpsCaseContact(final JsonObject cpsCaseContact) {
        if (isNull(cpsCaseContact)) {
            return null;
        }
        return cpsCaseContact()
                .withEmail(getValueAsString(cpsCaseContact, EMAIL))
                .withName(getValueAsString(cpsCaseContact, NAME))
                .withPhone(getValueAsString(cpsCaseContact, PHONE))
                .build();
    }

    private ProsecutionCaseSubject convertCpsProsecutionCaseSubject(
            final JsonObject prosecutionCaseSubject) {
        if (isNull(prosecutionCaseSubject)) {
            return null;
        }
        return prosecutionCaseSubject()
                .withUrn(getValueAsString(prosecutionCaseSubject, URN))
                .withProsecutingAuthority(getValueAsString(prosecutionCaseSubject, PROSECUTING_AUTHORITY))
                .build();
    }

    private List<DefendantSubject> convertDefendantSubject(final JsonArray defendantSubjects) {
        if (isNull(defendantSubjects)) {
            return null;
        }

        final List<DefendantSubject> subjects = new ArrayList<>();
        defendantSubjects.getValuesAs(JsonObject.class).stream()
                .forEach(defendantSubject -> {
                    final String cpsDefendantId = defendantSubject.getString(CPS_DEFENDANT_ID, null);
                    final String prosecutorDefendantId = defendantSubject.getString(PROSECUTOR_DEFENDANT_ID, null);
                    final String asn = defendantSubject.getString("asn", null);
                    final DefendantSubject.Builder builder = DefendantSubject.defendantSubject();
                    if (nonNull(cpsDefendantId)) {
                        builder.withCpsDefendantId(cpsDefendantId);
                    }
                    if (nonNull(asn)) {
                        builder.withAsn(asn);
                    }
                    if (nonNull(prosecutorDefendantId)) {
                        builder.withProsecutorDefendantId(prosecutorDefendantId);
                    }
                    subjects.add(builder.build());
                });

        return subjects;
    }

    private List<CpsDefendantOffences> convertCpsDefendantOffences(
            final JsonArray cpsDefendantOffences) {
        if (isNull(cpsDefendantOffences)) {
            return emptyList();
        }

        final List<CpsDefendantOffences> returnList = new ArrayList<>();
        cpsDefendantOffences
                .getValuesAs(JsonObject.class)
                .forEach(defendant -> {
                    final CpsDefendantOffences.Builder cpsBuilder = cpsDefendantOffences()
                            .withProsecutorDefendantId(getValueAsString(defendant, PROSECUTOR_DEFENDANT_ID))
                            .withCpsDefendantId(getValueAsString(defendant, CPS_DEFENDANT_ID))
                            .withAsn(getValueAsString(defendant, ASN))
                            .withTitle(getValueAsString(defendant, TITLE))
                            .withForename(getValueAsString(defendant, FORENAME))
                            .withForename2(getValueAsString(defendant, FORENAME2))
                            .withForename3(getValueAsString(defendant, FORENAME3))
                            .withSurname(getValueAsString(defendant, SURNAME))
                            .withOrganisationName(getValueAsString(defendant, ORGANISATION_NAME))
                            .withCpsOffenceDetails(convertCpsOffence(defendant.getJsonArray(CPS_OFFENCE_DETAILS)));
                    if (isNotEmpty(getValueAsString(defendant, DATE_OF_BIRTH))) {
                        cpsBuilder.withDateOfBirth(DateUtil.convertToLocalDate(getValueAsString(defendant, DATE_OF_BIRTH)));
                    }
                    if (isNotEmpty(getValueAsString(defendant, MATCHING_ID))) {
                        cpsBuilder.withMatchingId(fromString(getValueAsString(defendant, MATCHING_ID)));
                    }
                    if(nonNull(defendant.getJsonObject(PARENT_GUARDIAN_FOR_YOUTH_DEFENDANTS))){
                        cpsBuilder.withParentGuardianForYouthDefendants(convertParentGuardianForYouthDefendants(defendant.getJsonObject(PARENT_GUARDIAN_FOR_YOUTH_DEFENDANTS)));
                    }
                    if(nonNull(defendant.getJsonObject(LOCAL_AUTHORITY_DETAILS_FOR_YOUTH_DEFENDANTS))){
                        cpsBuilder.withLocalAuthorityDetailsForYouthDefendants(convertLocalAuthorityDetailsForYouthDefendants(defendant.getJsonObject(LOCAL_AUTHORITY_DETAILS_FOR_YOUTH_DEFENDANTS)));
                    }
                    returnList.add(cpsBuilder.build());
                });
        return returnList;
    }

    private DefendantParentGuardian convertParentGuardianForYouthDefendants(final JsonObject parentGuardianForYouthDefendants) {
        final DefendantParentGuardian.Builder builder = defendantParentGuardian();
        builder.withTitle(getValueAsString(parentGuardianForYouthDefendants, TITLE))
                .withForename(getValueAsString(parentGuardianForYouthDefendants, FORENAME))
                .withForename2(getValueAsString(parentGuardianForYouthDefendants, FORENAME2))
                .withForename3(getValueAsString(parentGuardianForYouthDefendants, FORENAME3))
                .withSurname(getValueAsString(parentGuardianForYouthDefendants, SURNAME))
                .withPhone(getValueAsString(parentGuardianForYouthDefendants, PHONE))
                .withRelationshipToDefendant(getValueAsString(parentGuardianForYouthDefendants, "relationshipToDefendant"))
                .withEmail(getValueAsString(parentGuardianForYouthDefendants, EMAIL))
                .withAddress1(getValueAsString(parentGuardianForYouthDefendants, ADDRESS1))
                .withAddress2(getValueAsString(parentGuardianForYouthDefendants, ADDRESS2))
                .withAddress3(getValueAsString(parentGuardianForYouthDefendants, ADDRESS3))
                .withAddress4(getValueAsString(parentGuardianForYouthDefendants, ADDRESS4))
                .withAddress5(getValueAsString(parentGuardianForYouthDefendants, ADDRESS5))
                .withPostcode(getValueAsString(parentGuardianForYouthDefendants, POSTCODE));
        return builder.build();
    }

    private LocalAuthorityDetailsForYouthDefendants convertLocalAuthorityDetailsForYouthDefendants(final JsonObject localAuthorityDetailsForYouthDefendants) {
        final LocalAuthorityDetailsForYouthDefendants.Builder builder = localAuthorityDetailsForYouthDefendants();
        builder.withTitle(getValueAsString(localAuthorityDetailsForYouthDefendants, TITLE))
                .withForename(getValueAsString(localAuthorityDetailsForYouthDefendants, FORENAME))
                .withIsLookedAfterChild(localAuthorityDetailsForYouthDefendants.getBoolean("isLookedAfterChild"))
                .withAuthority(getValueAsString(localAuthorityDetailsForYouthDefendants, "authority"))
                .withSurname(getValueAsString(localAuthorityDetailsForYouthDefendants, SURNAME))
                .withPhone(getValueAsString(localAuthorityDetailsForYouthDefendants, PHONE))
                .withReference(getValueAsString(localAuthorityDetailsForYouthDefendants, "reference"))
                .withEmail(getValueAsString(localAuthorityDetailsForYouthDefendants, EMAIL))
                .withAddress1(getValueAsString(localAuthorityDetailsForYouthDefendants, "address1"))
                .withAddress2(getValueAsString(localAuthorityDetailsForYouthDefendants, "address2"))
                .withAddress3(getValueAsString(localAuthorityDetailsForYouthDefendants, "address3"))
                .withAddress4(getValueAsString(localAuthorityDetailsForYouthDefendants, "address4"))
                .withAddress5(getValueAsString(localAuthorityDetailsForYouthDefendants, "address5"))
                .withPostcode(getValueAsString(localAuthorityDetailsForYouthDefendants, "postcode"));
        return builder.build();
    }

    private SubmissionStatus convertSubmissionStatus(final String submissionStatus) {
        return SubmissionStatus.valueOf(submissionStatus);
    }

    private void runDefendantMatchingAndRebuildBcmFormData(
            final JsonObject processReceivedCpsServeBcm,
            final JsonObject prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final ReceivedCpsServeBcmProcessed.Builder builder,
            final List<String> validOffences,
            final JsonArray defendantIds) {

        final FormValidationResult formValidationResult = cpsFormValidator
                .validateAndRebuildingFormDataBcm(processReceivedCpsServeBcm, prosecutionCase, validOffences, defendantIds);

        final List<FormDefendants> formDefendantsList = new ArrayList<>();

        if (nonNull(formValidationResult.getFormDefendants())) {
            formValidationResult
                    .getFormDefendants()
                    .getJsonArray(FORM_DEFENDANTS)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .map(formDefendant -> new FormDefendants(formDefendant.getString(CPS_DEFENDANT_ID, null), fromString(formDefendant.getString(DEFENDANT_ID))))
                    .forEach(formDefendantsList::add);
        }

        builder.withFormData(convertBcmFormData(formValidationResult.getFormData()))
                .withFormDefendants(formDefendantsList)
                .withErrors(formValidationResult.getErrorList())
                .withSubmissionStatus(formValidationResult.getSubmissionStatus());
    }

    private void runDefendantMatchingAndRebuildPetFormData(
            final JsonObject processReceivedCpsServePet,
            final JsonObject prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final ReceivedCpsServePetProcessed.Builder builder,
            final JsonObjectToObjectConverter jsonObjectToObjectConverter,
            final List<String> validOffences,
            final JsonArray defendantIds,
            final ProsecutionCaseFile prosecutionCaseFile,
            final ProgressionService progressionService,
            final DefenceService defenceService) {

        final FormValidationResult formValidationResult = cpsFormValidator
                .validateAndRebuildingFormData(processReceivedCpsServePet, prosecutionCase, validOffences, defendantIds);
        PetFormData petFormData = jsonObjectToObjectConverter.convert(formValidationResult.getPetFormData(), PetFormData.class);
        petFormData = updatePetFormDataWitnessesWithId(petFormData,prosecutionCaseFile);

        final List<PetDefendants> petDefendantsList = new ArrayList<>();

        if (nonNull(formValidationResult.getPetDefendants())) {
            formValidationResult
                    .getPetDefendants()
                    .getJsonArray(PET_DEFENDANTS)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .map(petDefendant -> new PetDefendants(petDefendant.getString(CPS_DEFENDANT_ID, null),fromString(petDefendant.getString(DEFENDANT_ID))))
                    .forEach(petDefendantsList::add);
        }

        final SubmissionStatus submissionStatus = formValidationResult.getSubmissionStatus();

        builder.withPetFormData(updateDefendantInformation(petFormData, prosecutionCase, progressionService, defenceService, submissionStatus))
                .withPetDefendants(petDefendantsList)
                .withErrors(formValidationResult.getErrorList())
                .withSubmissionStatus(submissionStatus);
    }

    private PetFormData updateDefendantInformation(final PetFormData petFormData,
                                                   final JsonObject prosecutionCase,
                                                   final ProgressionService progressionService,
                                                   final DefenceService defenceService,
                                                   final SubmissionStatus submissionStatus) {
        final PetFormData.Builder petFormDataBuilder = petFormData()
                .withProsecution(prosecution()
                        .withValuesFrom(petFormData.getProsecution()).build());

        if (nonNull(petFormData.getDefence())) {
            final List<Defendants> defendantsList = petFormData.getDefence().getDefendants().stream().map(defendants -> {
                final Defendants.Builder builder = defendants().withValuesFrom(defendants);

                if(!SUBMISSION_STATUS.equals(submissionStatus.toString())){
                    updateWithDefendantDetails(prosecutionCase, progressionService, defendants, builder);
                }

                if(nonNull(defendants.getId())) {
                    builder.withTrialRepresentative(getTrialRepresentative(defendants.getId(), defenceService));
                }

                return builder.build();
            }).collect(Collectors.toList());

            petFormDataBuilder
                    .withDefence(defence()
                            .withDefendants(defendantsList)
                            .build());
        }

        return petFormDataBuilder.build();
    }

    private void updateWithDefendantDetails(final JsonObject prosecutionCase,
                                            final ProgressionService progressionService,
                                            final Defendants defendant,
                                            final Defendants.Builder builder) {
        prosecutionCase.getJsonArray("defendants").getValuesAs(JsonObject.class).forEach(pcDefendant -> {
            final String caseDefendant = nonNull(pcDefendant.getJsonString(DEFENDANT_ID))?pcDefendant.getJsonString(DEFENDANT_ID).getString():null;
            final String petFormDefendant = nonNull(defendant.getId())?defendant.getId().toString():null;
            if (nonNull(caseDefendant) && nonNull(petFormDefendant) && caseDefendant.equals(petFormDefendant)) {
                LOGGER.info("updateDefendantInformation - pcDefendant {}", pcDefendant.getJsonString(DEFENDANT_ID));

                final Optional<JsonObject> personalInformation = getJsonObject(pcDefendant, "personalInformation");

                if (personalInformation.isPresent()) {
                    final Optional<JsonObject> optionalAddress = getJsonObject(personalInformation.get(), "address");
                    final Optional<JsonObject> optionalContactDetails = getJsonObject(personalInformation.get(), "contactDetails");

                    final AddressAndContact.Builder addressBuilder = AddressAndContact.addressAndContact();
                    optionalAddress.ifPresent(address -> addressBuilder.withAddress(buildAddress(address)));

                    if (optionalContactDetails.isPresent()) {
                        final JsonObject contactDetails = optionalContactDetails.get();
                        buildDefendantAddressAndContact(addressBuilder, contactDetails);
                    }

                    builder.withAddressAndContact(addressBuilder.build());
                }
                else {
                    final JsonObject prosecutionCaseAsJsonObject = progressionService.getProsecutionCase(UUID.fromString(prosecutionCase.getString("caseId")));
                    LOGGER.info("updateDefendantInformation - prosecutionCaseId {}", prosecutionCase.getString("caseId"));

                    for (final JsonValue def : prosecutionCaseAsJsonObject.getJsonObject(PROSECUTION_CASE).getJsonArray(DEFENDANTS)) {
                        final JsonObject defendantAsJsonObject = (JsonObject) def;
                        if (pcDefendant.getJsonString(DEFENDANT_ID).getString().equals(defendantAsJsonObject.getJsonString("id").getString())) {
                            updateOrgInformation(defendantAsJsonObject, builder);
                        }
                    }
                }
            }
        });
    }

    private DefendantTrialRepresentative getTrialRepresentative(final UUID defendantId, final DefenceService defenceService) {
        final DefendantTrialRepresentative.Builder builder = DefendantTrialRepresentative.defendantTrialRepresentative();
        final JsonObject associatedOrganisation = defenceService.getAssociatedOrganisation(defendantId);

        ofNullable(associatedOrganisation).ifPresent( trialRepresentative -> {
            final JsonObject association = trialRepresentative.getJsonObject(ASSOCIATION);

            getAddress(association).ifPresent(builder::withRepresentativeAddress);
            getString(association, ORGANISATION_NAME).ifPresent(builder::withRepresentative);
            getString(association, EMAIL).ifPresent(builder::withEmail);
            getString(association, PHONE_NUMBER).ifPresent(builder::withPhone);
        });

        return builder.build();
    }

    private static Optional<String> getAddress(JsonObject association) {
        return getJsonObject(association, ADDRESS).map(address -> address.keySet()
                .stream()
                .sorted()
                .map(address::getString)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(", ")));
    }

    private void updateOrgInformation(final JsonObject defendantAsJsonObject, final Defendants.Builder builder){
        final JsonObject legalEntityDefendant = defendantAsJsonObject.getJsonObject("legalEntityDefendant");
        if (nonNull(legalEntityDefendant)) {
            final JsonObject organisation = legalEntityDefendant.getJsonObject("organisation");
            if (nonNull(organisation)) {
                final JsonObject address = organisation.getJsonObject("address");
                final JsonObject contact = organisation.getJsonObject("contact");

                final AddressAndContact.Builder addressBuilder = AddressAndContact.addressAndContact();
                final String fullAddress = nonNull(address)? buildAddress(address):null;

                if (nonNull(address)) {
                    addressBuilder.withAddress(fullAddress);
                }

                if (nonNull(contact)) {
                    buildDefendantAddressAndContact(addressBuilder, contact);
                }

                builder.withAddressAndContact(addressBuilder.build());
            }
        }
    }

    private void buildDefendantAddressAndContact(final AddressAndContact.Builder addressBuilder, final JsonObject contactDetails){
        if(nonNull(contactDetails.getString("home", null))){
            addressBuilder.withHome(contactDetails.getString("home"));
        }

        if(nonNull(contactDetails.getString("mobile", null))){
            addressBuilder.withMobile(contactDetails.getString("mobile"));
        }

        if(nonNull(contactDetails.getString("primaryEmail", null))){
            addressBuilder.withPrimaryEmail(contactDetails.getString("primaryEmail"));
        }

        if(nonNull(contactDetails.getString("secondaryEmail", null))){
            addressBuilder.withSecondaryEmail(contactDetails.getString("secondaryEmail"));
        }
    }

    private String buildAddress(final JsonObject address){
        String fullAddress = StringUtils.EMPTY;

        if(nonNull(address.getString(ADDRESS1, null))) {
            fullAddress = address.getString(ADDRESS1);
        }

        if(nonNull(address.getString(ADDRESS2,null))) {
            fullAddress = fullAddress.length()>0?fullAddress.concat(COMMA):StringUtils.EMPTY;
            fullAddress = fullAddress.concat(address.getString(ADDRESS2));
        }

        if(nonNull(address.getString(ADDRESS3, null))) {
            fullAddress = fullAddress.length()>0?fullAddress.concat(COMMA):StringUtils.EMPTY;
            fullAddress = fullAddress.concat(address.getString(ADDRESS3));
        }

        if(nonNull(address.getString(ADDRESS4, null))) {
            fullAddress = fullAddress.length()>0?fullAddress.concat(COMMA):StringUtils.EMPTY;
            fullAddress = fullAddress.concat(address.getString(ADDRESS4));
        }

        if(nonNull(address.getString(ADDRESS5, null))) {
            fullAddress = fullAddress.length()>0?fullAddress.concat(COMMA):StringUtils.EMPTY;
            fullAddress = fullAddress.concat(address.getString(ADDRESS5));
        }

        if(nonNull(address.getString(POSTCODE, null))) {
            fullAddress = fullAddress.length()>0?fullAddress.concat(COMMA):StringUtils.EMPTY;
            fullAddress = fullAddress.concat(address.getString(POSTCODE));
        }

        return fullAddress;
    }

    private PetFormData updatePetFormDataWitnessesWithId(final PetFormData petFormData,final ProsecutionCaseFile prosecutionCaseFile) {
        final List<Witnesses> petWitnesses = new ArrayList<>();

        final PetFormData.Builder petFormDataBuilder = PetFormData.petFormData();

        if(nonNull(petFormData.getDefence())){
            final List<Defendants>  defendantWithDefaultWitness = petFormData.getDefence().getDefendants().stream().map(v-> addDefaultWitness(v,prosecutionCaseFile)).collect(Collectors.toList());
            petFormDataBuilder.withDefence(defence().withValuesFrom(petFormData.getDefence()).withDefendants(defendantWithDefaultWitness).build());
        }
        if (nonNull(petFormData.getProsecution())) {
            petFormData.getProsecution().getWitnesses().forEach(witness -> {
                final Witnesses.Builder builder = witnesses().withValuesFrom(witness);
                builder.withId(randomUUID());
                if (FORM_VALUE_Y.equalsIgnoreCase(witness.getSpecialOtherMeasuresRequired())) {
                    builder.withMeasuresRequired(singletonList(OTHER));
                    if (CollectionUtils.isNotEmpty(witness.getMeasuresRequired())) {
                        builder.withOtherText(witness.getMeasuresRequired().get(0));
                    }
                }

                if (FORM_VALUE_N.equalsIgnoreCase(witness.getSpecialOtherMeasuresRequired())) {
                    builder.withMeasuresRequired(null);
                }

                petWitnesses.add(builder.build());
            });

            petFormDataBuilder
                    .withProsecution(prosecution()
                            .withDynamicFormAnswers(petFormData.getProsecution().getDynamicFormAnswers())
                            .withWitnesses(petWitnesses)
                            .build());
        }

        return petFormDataBuilder.build();
    }

    private Defendants addDefaultWitness(final Defendants defendants, final ProsecutionCaseFile prosecutionCaseFile) {
        if(nonNull(prosecutionCaseFile.getDefendants())){
            final Optional<Defendant> defendant = prosecutionCaseFile.getDefendants().stream()
                    .filter(v-> v.getId().equalsIgnoreCase(ofNullable(defendants.getId()).map(UUID::toString).orElse("")))
                    .findFirst();
            if(defendant.isPresent()){
                final Defendant savedDefendant = defendant.get();
                return Defendants.defendants().withValuesFrom(defendants).withWitnesses(
                        singletonList(witnesses()
                                .withId(randomUUID())
                                .withSelected(true)
                                .withDefendantId(savedDefendant.getId())
                                .withIsDefendantWitness(true)
                                .withFirstName(getFirstName(savedDefendant))
                                .withLastName(getLastName(savedDefendant))
                                .withDateOfBirth(getDataOfBirth(savedDefendant))
                                .build())
                ).build();
            }
        }

        return defendants;
    }

    private String getDataOfBirth(final Defendant savedDefendant) {
        return ofNullable(savedDefendant.getIndividual())
                .map(v-> ofNullable(v.getSelfDefinedInformation())
                        .map(sdi-> ofNullable(sdi)
                                .map(dob-> ofNullable(dob.getDateOfBirth())
                                        .map(LocalDate::toString)
                                        .orElse(null))
                                .orElse(null))
                        .orElse(null))
                .orElse(null);
    }

    private String getLastName(final Defendant savedDefendant) {
        return ofNullable(savedDefendant.getIndividual()).map(v->v.getPersonalInformation().getLastName()).orElse(savedDefendant.getOrganisationName());
    }

    private String getFirstName(final Defendant savedDefendant) {
        return ofNullable(savedDefendant.getIndividual()).map(v->v.getPersonalInformation().getFirstName()).orElse(savedDefendant.getOrganisationName());
    }

    private PetFormData convertPetFormData(final JsonObject petFormData) {
        if (isNull(petFormData)) {
            return null;
        }

        return petFormData()
                .withDefence(convertDefence(getValueAsJsonObject(petFormData, DEFENCE)))
                .withProsecution(convertProsecution(getValueAsJsonObject(petFormData, PROSECUTION)))
                .build();
    }

    private FormData convertBcmFormData(final JsonObject bcmFormData) {

        if (isNull(bcmFormData)) {
            return null;
        }

        return formData()
                .withBcmDefendants(convertDefendantsBcm(bcmFormData))
                .build();
    }

    private Defence convertDefence(final JsonObject defence) {
        if (isNull(defence)) {
            return null;
        }

        return defence()
                .withDefendants(convertDefendants(defence.getJsonArray(DEFENDANTS)))
                .build();
    }

    private List<BcmDefendants> convertDefendantsBcm(final JsonObject bcmFormData) {
        final List<BcmDefendants> bcmDefendantList = new ArrayList<>();
        bcmFormData.getJsonArray(BCM_DEFENDANTS)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach(defendant -> bcmDefendantList.add(convertToBcmDefendant(defendant)));
        return bcmDefendantList;
    }


    private BcmDefendants convertToBcmDefendant(final JsonObject formDefendant) {
        final BcmDefendants.Builder builder = bcmDefendants();
        if (formDefendant.containsKey(ID)) {
            builder.withId(fromString(formDefendant.getString(ID)));
        }
        if (formDefendant.containsKey(ANY_OTHER)) {
            builder.withAnyOther(formDefendant.getString(ANY_OTHER));
        }
        if (formDefendant.containsKey(OTHER_AREAS_AFTER_PTPH)) {
            builder.withOtherAreasAfterPtph(formDefendant.getString(OTHER_AREAS_AFTER_PTPH));
        }
        if (formDefendant.containsKey(OTHER_AREAS_BEFORE_PTPH)) {
            builder.withOtherAreasBeforePtph(formDefendant.getString(OTHER_AREAS_BEFORE_PTPH));
        }
        if (formDefendant.containsKey(PROSECUTOR_OFFENCES)) {
            builder.withProsecutorOffences(buildProsecutorOffencesList(formDefendant));
        }
        return builder.build();
    }

    private List<CpsDefendant> convertToCpsDefendants(final JsonArray defendants) {
        if (isNull(defendants)) {
            return emptyList();
        }
        final List<CpsDefendant> returnList = new ArrayList<>();
        defendants
                .getValuesAs(JsonObject.class)
                .forEach(defendant -> {
                    final CpsDefendant.Builder cpsDefendant = getPtphDefendant(defendant);

                    returnList.add(cpsDefendant.build());
                });

        return returnList;
    }

    private CpsDefendant.Builder getPtphDefendant(final JsonObject defendant) {
        final CpsDefendant.Builder cpsDefendant = CpsDefendant.cpsDefendant()
                .withCpsDefendantId(getValueAsString(defendant, CPS_DEFENDANT_ID))
                .withAsn(getValueAsString(defendant, ASN))
                .withForename(getValueAsString(defendant, FORENAME))
                .withForename2(getValueAsString(defendant, FORENAME2))
                .withForename3(getValueAsString(defendant, FORENAME3))
                .withSurname(getValueAsString(defendant, SURNAME))
                .withProsecutorDefendantId(getValueAsString(defendant, PROSECUTOR_DEFENDANT_ID))
                .withTitle(getValueAsString(defendant, TITLE));
        if (getValueAsString(defendant, DATE_OF_BIRTH) != null) {
            cpsDefendant.
                    withDateOfBirth(DateUtil.convertToLocalDate(getValueAsString(defendant, DATE_OF_BIRTH)));
        }
        if (getValueAsString(defendant, MATCHING_ID) != null) {
            cpsDefendant.withMatchingId(fromString(getValueAsString(defendant, MATCHING_ID)));
        }
        cpsDefendant.withOrganisationName(getValueAsString(defendant, ORGANISATION_NAME))
                .withPrincipalCharges(getValueAsString(defendant, PRINCIPAL_CHARGES));
        return cpsDefendant;
    }

    private List<ProsecutorOffences> buildProsecutorOffencesList(final JsonObject defendant) {
        return defendant.getJsonArray(PROSECUTOR_OFFENCES)
                .getValuesAs(JsonObject.class)
                .stream()
                .map(defendantOffence -> ProsecutorOffences.prosecutorOffences()
                        .withOffenceCode(defendantOffence.getString(OFFENCE_CODE))
                        .withWording(defendantOffence.getString(WORDING))
                        .withDate(DateUtil.convertToLocalDate(defendantOffence.getString(DATE)))
                        .build()
                ).collect(Collectors.toList());
    }


    private List<Defendants> convertDefendants(final JsonArray defendants) {
        if (isNull(defendants)) {
            return emptyList();
        }

        final List<Defendants> returnList = new ArrayList<>();
        defendants
                .getValuesAs(JsonObject.class)
                .forEach(defendant -> {
                    final Defendants.Builder defendantBuilder = defendants();
                    if (defendant.containsKey(ID)) {
                        defendantBuilder.withId(fromString(getValueAsString(defendant, ID)));
                    }
                    if (defendant.containsKey(PROSECUTOR_DEFENDANT_ID)) {
                        defendantBuilder.withProsecutorDefendantId(getValueAsString(defendant, PROSECUTOR_DEFENDANT_ID));
                    }
                    if (defendant.containsKey(CPS_DEFENDANT_ID)) {
                        defendantBuilder.withCpsDefendantId(getValueAsString(defendant, CPS_DEFENDANT_ID));
                    }
                    defendantBuilder.withCpsOffences(convertCpsOffencesForPet(defendant.getJsonArray(CPS_OFFENCES)));
                    returnList.add(defendantBuilder.build());
                });
        return returnList;
    }

    private Prosecution convertProsecution(final JsonObject prosecution) {
        if (isNull(prosecution)) {
            return null;
        }

        return prosecution()
                .withWitnesses(convertProsecutionWitness(prosecution.getJsonArray(WITNESSES)))
                .withDynamicFormAnswers(convertDynamicAnswers(prosecution.getJsonObject(DYNAMIC_FORM_ANSWERS)))
                .build();
    }

    private List<ProsecutorOffences> convertCpsOffences(final JsonArray cpsOffences) {
        if (isNull(cpsOffences)) {
            return emptyList();
        }

        final List<ProsecutorOffences> returnList = new ArrayList<>();
        cpsOffences
                .getValuesAs(JsonObject.class)
                .forEach(offence ->
                        returnList.add(ProsecutorOffences
                                .prosecutorOffences()
                                .withOffenceCode(getValueAsString(offence, OFFENCE_CODE))
                                .withDate(DateUtil.convertToLocalDate(getValueAsString(offence, DATE)))
                                .withWording(getValueAsString(offence, WORDING))
                                .build()));
        return returnList;
    }

    private List<CpsOffences> convertCpsOffencesForPet(final JsonArray cpsOffences) {
        if (isNull(cpsOffences)) {
            return emptyList();
        }

        final List<CpsOffences> returnList = new ArrayList<>();
        cpsOffences
                .getValuesAs(JsonObject.class)
                .forEach(offence ->
                        returnList.add(CpsOffences
                                .cpsOffences()
                                .withOffenceCode(getValueAsString(offence, OFFENCE_CODE))
                                .withDate(DateUtil.convertToLocalDate(getValueAsString(offence, DATE)))
                                .withWording(getValueAsString(offence, WORDING))
                                .build()));
        return returnList;
    }

    private List<Witnesses> convertProsecutionWitness(final JsonArray witnesses) {
        if (isNull(witnesses)) {
            return emptyList();
        }

        final List<Witnesses> returnList = new ArrayList<>();
        witnesses
                .getValuesAs(JsonObject.class)
                .forEach(witness ->
                        returnList.add(witnesses()
                                .withId(randomUUID())
                                .withFirstName(getValueAsString(witness, FIRST_NAME))
                                .withLastName(getValueAsString(witness, LAST_NAME))
                                .withAge(getValueAsInt(witness,(AGE)))
                                .withInterpreterRequired(getValueAsString(witness, INTERPRETER_REQUIRED))
                                .withLanguageAndDialect(getValueAsString(witness, LANGUAGE_AND_DIALECT))
                                .withSpecialOtherMeasuresRequired(getValueAsString(witness, SPECIAL_OTHER_MEASURE_REQUIRED))
                                .withMeasuresRequired(buildMeasuresRequired(witness))
                                .withSelected(true)
                                .withProsecutionProposesWitnessAttendInPerson(getValueAsString(witness, PROSECUTION_PROPOSES_WITNESSES_ATTEND_IN_PERSON))
                                .build())
                );
        return returnList;
    }

    private List<String> buildMeasuresRequired(final JsonObject measuresRequired) {
        if (isNull(measuresRequired)) {
            return emptyList();
        }

        final List<String> values = new ArrayList<>();
        if (nonNull(measuresRequired.getJsonArray(MEASURE_REQUIRED))) {
            measuresRequired.getJsonArray(MEASURE_REQUIRED)
                    .getValuesAs(JsonString.class)
                    .stream().forEach(value -> {
                        values.add(value.getString());
                    });
        }

        return values;
    }

    private List<CpsOffenceDetails> convertCpsOffence(final JsonArray offences) {
        if (isNull(offences)) {
            return emptyList();
        }

        final List<CpsOffenceDetails> returnList = new ArrayList<>();
        offences
                .getValuesAs(JsonObject.class)
                .forEach(offence ->
                        returnList.add(cpsOffenceDetails()
                                .withCjsOffenceCode(getValueAsString(offence, CJS_OFFENCE_CODE))
                                .withOffenceDate(DateUtil.convertToLocalDate(getValueAsString(offence, OFFENCE_DATE)))
                                .withOffenceWording(getValueAsString(offence, OFFENCE_WORDING))
                                .build())
                );
        return returnList;
    }

    private DynamicFormAnswers convertDynamicAnswers(final JsonObject dynamicAnswers) {
        if (isNull(dynamicAnswers)) {
            return null;
        }
        return dynamicFormAnswers()
                .withProsecutorGroup(convertProsecutorGroup(dynamicAnswers.getJsonObject(PROSECUTOR_GROUP)))
                .withApplicationsForDirectionsGroup(convertAppDirectGroup(dynamicAnswers.getJsonObject(APPLICATION_DIRECTIONS_GROUP)))
                .build();
    }

    private ProsecutorGroup convertProsecutorGroup(final JsonObject prosecutorGroup) {
        if (isNull(prosecutorGroup)) {
            return null;
        }
        return ProsecutorGroup
                .prosecutorGroup()
                .withDisplayEquipment(getValueAsString(prosecutorGroup, DISPLAY_EQUIPMENT))
                .withDisplayEquipmentYesGroup(convertDisplayEquipmentYes(getValueAsJsonObject(prosecutorGroup, DISPLAY_EQUIPMENT_YES_GROUP)))
                .withPendingLinesOfEnquiry(getValueAsString(prosecutorGroup, PENDING_LINES_OF_ENQUIRY))
                .withPendingLinesOfEnquiryYesGroup(convertPendingLinesOfEnquiryYes(getValueAsJsonObject(prosecutorGroup, PENDING_LINES_OF_ENQUIRY_YES_GROUP)))
                .withPointOfLaw(getValueAsString(prosecutorGroup, POINT_OF_LAW))
                .withPointOfLawYesGroup(convertPointofLawYes(getValueAsJsonObject(prosecutorGroup, POINT_OF_LAW_YES_GROUP)))
                .withProsecutionCompliance(getValueAsString(prosecutorGroup, PROSECUTION_COMPLIANCE))
                .withProsecutionComplianceNoGroup(convertProsecutionComplianceNo(getValueAsJsonObject(prosecutorGroup, PROSECUTION_COMPLIANCE_NO_GROUP)))
                .withProsecutionComplianceYesGroup(convertProsecutionComplianceYes(getValueAsJsonObject(prosecutorGroup, PROSECUTION_COMPLIANCE_YES_GROUP)))
                .withProsecutorServeEvidence(getValueAsString(prosecutorGroup, PROSECUTOR_SERVE_EVIDENCE))
                .withProsecutorServeEvidenceYesGroup(convertProsecutorServeEvidence(getValueAsJsonObject(prosecutorGroup, PROSECUTOR_SERVE_EVIDENCE_YES_GROUP)))
                .withRelyOn(buildRelyOn(prosecutorGroup))
                .withSlaveryOrExploitation(getValueAsString(prosecutorGroup, SLAVERY_OR_EXPLOITATION))
                .withSlaveryOrExploitationYesGroup(convertSlaveryOrExploitationYes(getValueAsJsonObject(prosecutorGroup, SLAVERY_OR_EXPLOITATION_YES_GROUP)))
                .build();
    }

    private List<String> buildRelyOn(final JsonObject prosecutorGroup) {
        if (isNull(prosecutorGroup.getJsonArray(RELY_ON))) {
            return emptyList();
        }

        final List<String> values = new ArrayList<>();
        prosecutorGroup.getJsonArray(RELY_ON)
                .getValuesAs(JsonString.class)
                .stream().forEach(value -> {
                    values.add(value.getString());
        });

        return values;
    }

    private ApplicationsForDirectionsGroup convertAppDirectGroup(
            final JsonObject appForDirectionsGroup) {
        if (isNull(appForDirectionsGroup)) {
            return null;
        }

        return applicationsForDirectionsGroup()
                .withGroundRulesQuestioning(getValueAsString(appForDirectionsGroup, GROUND_RULES_QUESTIONING))
                .withVariationStandardDirectionsProsecutor(getValueAsString(appForDirectionsGroup, VARIATION_STANDARD_DIRECTIONS_PROSECUTOR))
                .withVariationStandardDirectionsProsecutorYesGroup((convertVariationStdProsecutionYes(appForDirectionsGroup.getJsonObject(VARIATION_STANDARD_DIRECTIONS_PROSECUTOR_YES_GROUP))))
                .build();
    }

    private VariationStandardDirectionsProsecutorYesGroup convertVariationStdProsecutionYes
            (final JsonObject variationStandardsDirections) {
        if (isNull(variationStandardsDirections)) {
            return null;
        }

        return VariationStandardDirectionsProsecutorYesGroup
                .variationStandardDirectionsProsecutorYesGroup()
                .withVariationStandardDirectionsProsecutorYesGroupDetails(getValueAsString(variationStandardsDirections, VARIATION_STANDARD_DIRECTIONS_PROSECUTOR_YES_GROUP_DETAILS))
                .build();
    }

    private DisplayEquipmentYesGroup convertDisplayEquipmentYes(
            final JsonObject displayEquipmentYesGroup) {
        if (isNull(displayEquipmentYesGroup)) {
            return null;
        }

        return displayEquipmentYesGroup()
                .withDisplayEquipmentDetails(getValueAsString(displayEquipmentYesGroup, DISPLAY_EQUIPMENT_DETAILS))
                .build();
    }

    private PendingLinesOfEnquiryYesGroup convertPendingLinesOfEnquiryYes(
            final JsonObject pendingLinesOfEnquiry) {
        if (isNull(pendingLinesOfEnquiry)) {
            return null;
        }

        return pendingLinesOfEnquiryYesGroup()
                .withPendingLinesOfEnquiryYesGroup(getValueAsString(pendingLinesOfEnquiry, PENDING_LINES_OF_ENQUIRY_YES_GROUP))
                .build();
    }

    private PointOfLawYesGroup convertPointofLawYes(final JsonObject pointOfLawYesGroup) {
        if (isNull(pointOfLawYesGroup)) {
            return null;
        }

        return pointOfLawYesGroup()
                .withPointOfLawDetails(getValueAsString(pointOfLawYesGroup, POINT_OF_LAW_DETAILS))
                .build();
    }

    private ProsecutionComplianceNoGroup convertProsecutionComplianceNo(
            final JsonObject prosecutionCompliance) {
        if (isNull(prosecutionCompliance)) {
            return null;
        }

        return prosecutionComplianceNoGroup()
                .withProsecutionComplianceDetailsNo(getValueAsString(prosecutionCompliance, PROSECUTION_COMPLIANCE_DETAILS_NO))
                .build();
    }

    private ProsecutionComplianceYesGroup convertProsecutionComplianceYes(
            final JsonObject prosecutionCompliance) {
        if (isNull(prosecutionCompliance)) {
            return null;
        }

        return prosecutionComplianceYesGroup()
                .withProsecutionComplianceDetailsYes(getValueAsString(prosecutionCompliance, PROSECUTION_COMPLIANCE_DETAILS_YES))
                .build();
    }

    private ProsecutorServeEvidenceYesGroup convertProsecutorServeEvidence(
            final JsonObject prosecutorServeEvidence) {
        if (isNull(prosecutorServeEvidence)) {
            return null;
        }

        return prosecutorServeEvidenceYesGroup()
                .withProsecutorServeEvidenceDetails(getValueAsString(prosecutorServeEvidence, PROSECUTOR_SERVE_EVIDENCE_DETAILS))
                .build();
    }

    private SlaveryOrExploitationYesGroup convertSlaveryOrExploitationYes(
            final JsonObject slaveryOrExploitation) {
        if (isNull(slaveryOrExploitation)) {
            return null;
        }

        return slaveryOrExploitationYesGroup()
                .withSlaveryOrExploitationDetails(getValueAsString(slaveryOrExploitation, SLAVERY_OR_EXPLOITATION_DETAILS))
                .build();
    }

    private String getValueAsString(final JsonObject jsonObject, final String key) {
        if (isNull(jsonObject)) {
            return null;
        }
        return jsonObject.containsKey(key) ? jsonObject.getString(key) : null;
    }

    private Integer getValueAsInt(final JsonObject jsonObject, final String key) {
        if (isNull(jsonObject)) {
            return null;
        }
        return jsonObject.containsKey(key) ? jsonObject.getInt(key) : null;
    }

    private Boolean getValueAsBoolean(final JsonObject jsonObject, final String key) {
        if (isNull(jsonObject)) {
            return null;
        }
        return jsonObject.containsKey(key) ? jsonObject.getBoolean(key) : null;
    }

    private JsonObject getValueAsJsonObject(final JsonObject jsonObject, final String key) {
        if (isNull(jsonObject)) {
            return null;
        }

        return jsonObject.containsKey(key) ? jsonObject.getJsonObject(key) : null;
    }

    public Stream<Object> cpsRejectPetForTimerExpire() {
        if (this.isPending) {
            final Stream.Builder builder = Stream.builder();
            if (this.pendingSet.contains(PendingType.PET)) {
                return apply(Stream.of(receivedCpsServePetProcessed()
                        .withValuesFrom(this.receivedCpsServePetProcessed)
                        .withSubmissionStatus(EXPIRED)
                        .build()));
            }
            return apply(of(builder.build()));
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> acceptCasePet(final UUID caseId, final ProsecutionCaseFile prosecutionCaseFile,
                                        final Optional<JsonObject> prosecutionCase, final CpsFormValidator cpsFormValidator,
                                        final ReferenceDataQueryService referenceDataQueryService, final ProgressionService progressionService,
                                        final JsonObjectToObjectConverter jsonObjectToObjectConverter, final ObjectToJsonObjectConverter objectToJsonObjectConverter,
                                        final ListToJsonArrayConverter listToJsonArrayConverter, final DefenceService defenceService) {
            if (this.pendingSet.contains(PendingType.PET))  {

                Optional<String> sowRef = getSowRef(prosecutionCase);
                final List<Defendants> defendants = this.receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants();
                final List<OffenceReferenceData> validOffences = retrieveOffencesFromReferenceData(defendants, referenceDataQueryService, sowRef);
                final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId, progressionService);
                final JsonObject validationData = buildValidationData(validOffences, cpsDefendantIds);

                final List<String> validOffencesArray = new ArrayList<>();
                validOffences.forEach(offence -> validOffencesArray.add(offence.getCjsOffenceCode()));

                final PetFormData petFormData = this.receivedCpsServePetProcessed.getPetFormData();
                final JsonObject petFormDataObj = objectToJsonObjectConverter.convert(petFormData);

                final JsonObjectBuilder commandBuilder = createObjectBuilder()
                        .add(SUBMISSION_ID, this.receivedCpsServePetProcessed.getSubmissionId().toString())
                        .add(SUBMISSION_STATUS, SUCCESS.toString())
                        .add(PROSECUTION_CASE_SUBJECT, objectToJsonObjectConverter.convert(this.receivedCpsServePetProcessed.getProsecutionCaseSubject()))
                        .add(CPS_DEFENDANT_OFFENCES,  listToJsonArrayConverter.convert(this.receivedCpsServePetProcessed.getCpsDefendantOffences()))
                        .add(PET_FORM_DATA, petFormDataObj)
                        .add(VALIDATION_DATA, validationData)
                        .add(REVIEWING_LAWYER, objectToJsonObjectConverter.convert(this.receivedCpsServePetProcessed.getReviewingLawyer()))
                        .add(IS_YOUTH, this.receivedCpsServePetProcessed.getIsYouth());

                return cpsReceivePet(commandBuilder.build(),SUCCESS.toString(), caseId, prosecutionCase,cpsFormValidator, validOffencesArray,cpsDefendantIds, jsonObjectToObjectConverter, prosecutionCaseFile, progressionService, defenceService);
            }
            return Stream.empty();
    }

    public Stream<Object> acceptCaseBcm(final UUID caseId, final Optional<JsonObject> prosecutionCase, final CpsFormValidator cpsFormValidator,
                                        final ReferenceDataQueryService referenceDataQueryService, final ProgressionService progressionService,
                                        final ObjectToJsonObjectConverter objectToJsonObjectConverter, final ListToJsonArrayConverter listToJsonArrayConverter) {
            if (this.pendingSet.contains(PendingType.BCM)) {
                final Optional<String> sowRef = getSowRef(prosecutionCase);
                final List<OffenceReferenceData> validOffences = retrieveOffencesFromReferenceDataForBcm(this.receivedCpsServeBcmProcessed.getCpsDefendantOffences(), referenceDataQueryService, sowRef);
                final JsonArray cpsDefendantIds = retrieveAndBuildCpsDefendantIdsList(caseId, progressionService);

                final List<String> validOffencesArray = new ArrayList<>();
                validOffences.forEach(offence -> validOffencesArray.add(offence.getCjsOffenceCode()));

                final JsonObjectBuilder commandBuilder = createObjectBuilder()
                        .add(SUBMISSION_ID, this.receivedCpsServeBcmProcessed.getSubmissionId().toString())
                        .add(SUBMISSION_STATUS, SUCCESS.toString())
                        .add(PROSECUTION_CASE_SUBJECT, objectToJsonObjectConverter.convert(this.receivedCpsServeBcmProcessed.getProsecutionCaseSubject()))
                        .add(CPS_DEFENDANT_OFFENCES, listToJsonArrayConverter.convert(this.receivedCpsServeBcmProcessed.getCpsDefendantOffences()))
                        .add(VALIDATION_DATA, buildValidationData(validOffences, cpsDefendantIds));

                final BcmDefendants bcmDefendant = this.receivedCpsServeBcmProcessed.getFormData().getBcmDefendants().get(0);
                if(nonNull(bcmDefendant.getAnyOther())){
                    commandBuilder.add(OTHER_INFORMATION,bcmDefendant.getAnyOther());
                }

                if(nonNull(bcmDefendant.getOtherAreasBeforePtph())){
                    commandBuilder.add(EVIDENCE_PRE_PTPH,bcmDefendant.getOtherAreasBeforePtph());
                }

                if(nonNull(bcmDefendant.getOtherAreasAfterPtph())){
                    commandBuilder.add(EVIDENCE_POST_PTPH,bcmDefendant.getOtherAreasAfterPtph());
                }

                return cpsReceiveBcm(commandBuilder.build(),SUCCESS.toString(), caseId, prosecutionCase,cpsFormValidator, validOffencesArray,cpsDefendantIds);
            }
            return Stream.empty();
    }

    private List<OffenceReferenceData> retrieveOffencesFromReferenceData(final List<Defendants> defendants, final ReferenceDataQueryService referenceDataQueryService, Optional<String> sowRef) {
        final List<String> offenceCodeList = new ArrayList<>();
        defendants.forEach(defendants1 -> {
            defendants1.getCpsOffences().forEach(cpsOffences -> {
                offenceCodeList.add(cpsOffences.getOffenceCode());
            });
        });

        return referenceDataQueryService.retrieveOffenceDataList(offenceCodeList, sowRef);
    }

    private List<OffenceReferenceData> retrieveOffencesFromReferenceDataForBcm(final List<CpsDefendantOffences> cpsDefendantOffences, final ReferenceDataQueryService referenceDataQueryService, final Optional<String> sowRef) {
        final List<String> offenceCodeList = new ArrayList<>();
        cpsDefendantOffences.forEach(defendants1 -> {
            defendants1.getCpsOffenceDetails().forEach(cpsOffences -> {
                offenceCodeList.add(cpsOffences.getCjsOffenceCode());
            });
        });

        return referenceDataQueryService.retrieveOffenceDataList(offenceCodeList, sowRef);
    }

    private JsonArray retrieveAndBuildCpsDefendantIdsList(final UUID caseId, final ProgressionService progressionService) {
        final JsonArrayBuilder cpsDefendantsBuilder = createArrayBuilder();

        if (caseId != null) {
            final JsonObject prosecutionCaseAsJsonObject = progressionService.getProsecutionCase(caseId);

            final JsonArray defendants = prosecutionCaseAsJsonObject.getJsonObject(PROSECUTION_CASE).getJsonArray(DEFENDANTS);
            for (final JsonValue defendant : defendants) {
                final JsonObject defendantAsJsonObject = (JsonObject) defendant;
                final String cpsDefendantId = defendantAsJsonObject.getString(CPS_DEFENDANT_ID, null);
                if (cpsDefendantId != null) {
                    final String defendantId = defendantAsJsonObject.getString(ID);
                    cpsDefendantsBuilder.add(createObjectBuilder()
                            .add(CPS_DEFENDANT_ID, cpsDefendantId)
                            .add(DEFENDANT_ID, defendantId)
                            .build());
                }
            }
        }
        return cpsDefendantsBuilder.build();
    }

    private JsonObject buildValidationData(final List<OffenceReferenceData> cjsOffences, final JsonArray defendantIds) {
        final JsonArrayBuilder validOffencesArray = createArrayBuilder();

        cjsOffences.forEach(offence -> validOffencesArray.add(offence.getCjsOffenceCode()));

        final JsonObjectBuilder validationDataBuilder = createObjectBuilder()
                .add("validOffences", validOffencesArray.build());

        if (!defendantIds.isEmpty()) {
            validationDataBuilder.add(DEFENDANT_IDS, defendantIds);
        }

        return validationDataBuilder.build();
    }

    public Boolean isPet() {
        return this.formType.equals(PendingType.PET);
    }

    public Boolean isBcm() {
        return this.formType.equals(PendingType.BCM);
    }

    private List<PtphWitnesses> convertProsecutionWitnessPtph(final JsonArray witnessesPtph) {
        if (isNull(witnessesPtph)) {
            return emptyList();
        }

        final List<PtphWitnesses> returnList = new ArrayList<>();
        witnessesPtph
                .getValuesAs(JsonObject.class)
                .forEach(witnessPtph -> {
                            final PtphWitnesses.Builder ptpBuilder = PtphWitnesses.ptphWitnesses()
                                    .withId(randomUUID())
                                    .withFirstNameOfIntermediaryKnownAtPtph(getValueAsString(witnessPtph, FIRST_NAME_OF_INTERMEDIARY_KNOWN_AT_PTPH))
                                    .withLastNameOfIntermediaryKnownAtPtph(getValueAsString(witnessPtph, LAST_NAME_OF_INTERMEDIARY_KNOWN_AT_PTPH))
                                    .withIntermediaryForWitness(getValueAsString(witnessPtph, INTERMEDIARY_FOR_WITNESS) != null ?
                                            IntermediaryForWitness.valueFor(getValueAsString(witnessPtph, INTERMEDIARY_FOR_WITNESS)).orElse(null) : null)
                                    .withRelevantDisputedIssue(getValueAsString(witnessPtph, RELEVANT_DISPUTED_ISSUE))
                                    .withWitnessFirstName(getValueAsString(witnessPtph, WITNESS_FIRST_NAME))
                                    .withWitnessLastName(getValueAsString(witnessPtph, WITNESS_LAST_NAME));

                            if (witnessPtph.containsKey(POLICE_OFFICER_SUBJECT)) {
                                ptpBuilder.withPoliceOfficerSubject(convertToPoliceOfficerSubject(getValueAsJsonObject(witnessPtph, POLICE_OFFICER_SUBJECT)));
                            }
                            returnList.add(ptpBuilder.build());
                        }
                );

        return returnList;
    }

    private PoliceOfficerSubject convertToPoliceOfficerSubject(
            final JsonObject policeOfficerSubject) {
        return policeOfficerSubject().withOfficerInCharge(getValueAsBoolean(policeOfficerSubject, "officerInCharge"))
                .withOfficerRank(getValueAsString(policeOfficerSubject, "officerRank"))
                .withOfficeCollarNumber(getValueAsString(policeOfficerSubject, "officeCollarNumber"))
                .build();
    }

    public Stream<Object> cpsRejectBcmForTimerExpire() {
        if (this.isPending) {
            final Stream.Builder builder = Stream.builder();
            if (this.pendingSet.contains(PendingType.BCM)) {
                return apply(Stream.of(receivedCpsServeBcmProcessed()
                        .withValuesFrom(this.receivedCpsServeBcmProcessed)
                        .withSubmissionStatus(EXPIRED)
                        .build()));
            }
            return apply(of(builder.build()));
        } else {
            return Stream.empty();
        }
    }

    public Stream<Object> cpsUpdateCotr(final JsonObject processReceivedCpsUpdateCotrJson,
                                        final String submissionStatus,
                                        final UUID caseId) {
        final ReceivedCpsUpdateCotrProcessed.Builder builder = ReceivedCpsUpdateCotrProcessed.receivedCpsUpdateCotrProcessed()
                .withCaseId(caseId)
                .withSubmissionId(fromString(processReceivedCpsUpdateCotrJson.getString(SUBMISSION_ID)))
                .withCotrId(fromString(processReceivedCpsUpdateCotrJson.getString(COTR_ID)))
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withProsecutionCaseSubject(convertCpsProsecutionCaseSubject(getValueAsJsonObject(processReceivedCpsUpdateCotrJson, PROSECUTION_CASE_SUBJECT)))
                .withDefendantSubject(convertDefendantSubject(processReceivedCpsUpdateCotrJson.getJsonArray(DEFENDANT_SUBJECT)))
                .withCertifyThatTheProsecutionIsTrialReady(processReceivedCpsUpdateCotrJson.getString(PROSECUTOR_TRIAL_READY))
                .withFormCompletedOnBehalfOfProsecutionBy(processReceivedCpsUpdateCotrJson.getString(FORM_COMPLETED_ON_BEHALF_OF_PROSECUTION_BY))
                .withFurtherProsecutionInformationProvidedAfterCertification(processReceivedCpsUpdateCotrJson.getString(FURTHER_PROSECUTION_INFORMATION_PROVIDED_AFTER_CERTIFICATION))
                .withCertificationDate(LocalDate.parse(processReceivedCpsUpdateCotrJson.getString(CERTIFICATION_DATE), DateTimeFormatter.ofPattern(DATE_TEN_FORMAT_YYYY_MM_DD)));

        if (nonNull(fetchString(processReceivedCpsUpdateCotrJson, TAG))) {
            builder.withTag(processReceivedCpsUpdateCotrJson.getString(TAG));
        }

        return apply(of(builder.build()));

    }

    public Stream<Object> cpsReceiveCotr(final JsonObject processReceivedCpsServeCotrJson,
                                         final String submissionStatus,
                                         final UUID caseId,
                                         final Optional<JsonObject> prosecutionCase,
                                         final CpsFormValidator cpsFormValidator,
                                         final JsonArray defendantIds) {
        final ReceivedCpsServeCotrProcessed.Builder builder = ReceivedCpsServeCotrProcessed.receivedCpsServeCotrProcessed()
                .withCaseId(caseId)
                .withSubmissionId(fromString(processReceivedCpsServeCotrJson.getString(SUBMISSION_ID)))
                .withSubmissionStatus(convertSubmissionStatus(submissionStatus))
                .withProsecutionCaseSubject(convertCpsProsecutionCaseSubject(getValueAsJsonObject(processReceivedCpsServeCotrJson, PROSECUTION_CASE_SUBJECT)))
                .withDefendantSubject(convertDefendantSubject(processReceivedCpsServeCotrJson.getJsonArray(DEFENDANT_SUBJECT)))
                .withTag(fetchString(processReceivedCpsServeCotrJson, TAG))
                .withHasAllEvidenceToBeReliedOnBeenServed(fetchString(processReceivedCpsServeCotrJson, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED))
                .withHasAllEvidenceToBeReliedOnBeenServedDetails(fetchString(processReceivedCpsServeCotrJson, HAS_ALL_EVIDENCE_TOBE_RELIED_ON_BEEN_SERVED_DETAILS))
                .withHasAllDisclosureBeenProvided(fetchString(processReceivedCpsServeCotrJson, HAS_ALL_DISCLOSURE_BEEN_PROVIDED))
                .withHasAllDisclosureBeenProvidedDetails(fetchString(processReceivedCpsServeCotrJson, HAS_ALL_DISCLOSURE_BEEN_PROVIDED_DETAILS))
                .withHaveOtherDirectionsBeenCompliedWith(fetchString(processReceivedCpsServeCotrJson, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH))
                .withHaveOtherDirectionsBeenCompliedWithDetails(fetchString(processReceivedCpsServeCotrJson, HAVE_OTHER_DIRECTIONS_BEEN_COMPLIED_WITH_DETAILS))
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttend(fetchString(processReceivedCpsServeCotrJson, PROSECUTION_WITNESSES))
                .withHaveTheProsecutionWitnessesRequiredToAttendAcknowledgedThatTheyWillAttendDetails(fetchString(processReceivedCpsServeCotrJson, PROSECUTION_WITNESSES_DETAILS))
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServed(fetchString(processReceivedCpsServeCotrJson, WITNESSES_SUMMONSES))
                .withHaveAnyWitnessSummonsesRequiredBeenReceivedAndServedDetails(fetchString(processReceivedCpsServeCotrJson, WITNESSES_SUMMONSES_DETAILS))
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolved(fetchString(processReceivedCpsServeCotrJson, SPECIAL_MEASURES_FOR_WITNESSES))
                .withHaveSpecialMeasuresOrRemoteAttendanceIssuesForWitnessesBeenResolvedDetails(fetchString(processReceivedCpsServeCotrJson, SPECIAL_MEASURES_FOR_WITNESSES_DETAILS))
                .withHaveInterpretersForWitnessesBeenArranged(fetchString(processReceivedCpsServeCotrJson, INTERPRETERS_FOR_WITNESSES))
                .withHaveInterpretersForWitnessesBeenArrangedDetails(fetchString(processReceivedCpsServeCotrJson, INTERPRETERS_FOR_WITNESSES_DETAILS))
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreed(fetchString(processReceivedCpsServeCotrJson, INTERVIEWS))
                .withHaveEditedAbeInterviewsBeenPreparedAndAgreedDetails(fetchString(processReceivedCpsServeCotrJson, INTERVIEWS_DETAILS))
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreement(fetchString(processReceivedCpsServeCotrJson, STATEMENT_OF_POINTS))
                .withHaveArrangementsBeenMadeForStatementOfPointsOfAgreementAndDisagreementDetails(fetchString(processReceivedCpsServeCotrJson, STATEMENT_OF_POINTS_DETAILS))
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJury(fetchString(processReceivedCpsServeCotrJson, CASE_READY))
                .withIsTheCaseReadyToProceedWithoutDelayBeforeTheJuryDetails(fetchString(processReceivedCpsServeCotrJson, CASE_READY_DETAILS))
                .withIsTheTimeEstimateCorrect(fetchString(processReceivedCpsServeCotrJson, TIME_ESTIMATE))
                .withIsTheTimeEstimateCorrectDetails(fetchString(processReceivedCpsServeCotrJson, TIME_ESTIMATE_DETAILS))
                .withFurtherInformationToAssistTheCourt(fetchString(processReceivedCpsServeCotrJson, FURTHER_INFORMATION))
                .withCertifyThatTheProsecutionIsTrialReady(fetchString(processReceivedCpsServeCotrJson, PROSECUTOR_TRIAL_READY))
                .withCertifyThatTheProsecutionIsTrialReadyDetails(fetchString(processReceivedCpsServeCotrJson, PROSECUTOR_TRIAL_READY_DETAILS))
                .withApplyForThePtrToBeVacated(fetchString(processReceivedCpsServeCotrJson, PTR_VACATED))
                .withApplyForThePtrToBeVacatedDetails(fetchString(processReceivedCpsServeCotrJson, PTR_VACATED_DETAILS))
                .withFormCompletedOnBehalfOfTheProsecutionBy(fetchString(processReceivedCpsServeCotrJson, PROSECUTION_BY_FORM));

        if (nonNull(fetchString(processReceivedCpsServeCotrJson, TRIAL_DATE))) {
            builder.withTrialDate(LocalDate.parse(processReceivedCpsServeCotrJson.getString(TRIAL_DATE), DateTimeFormatter.ofPattern(DATE_TEN_FORMAT_YYYY_MM_DD)));
        }

        if (processReceivedCpsServeCotrJson.containsKey(LAST_RECORDED_TIME_ESTIMATE)) {
            builder.withLastRecordedTimeEstimate(processReceivedCpsServeCotrJson.getInt(LAST_RECORDED_TIME_ESTIMATE));
        }

        if (nonNull(fetchString(processReceivedCpsServeCotrJson, CERTIFICATION_DATE))) {
            builder.withCertificationDate(LocalDate.parse(processReceivedCpsServeCotrJson.getString(CERTIFICATION_DATE), DateTimeFormatter.ofPattern(DATE_TEN_FORMAT_YYYY_MM_DD)));
        }

        if (prosecutionCase.isPresent()) {
            LOGGER.info("ProsecutionCase present");
            final FormValidationResult formValidationResult = runDefendantMatchingForCotr(processReceivedCpsServeCotrJson, prosecutionCase.get(), cpsFormValidator, defendantIds);
            final List<FormDefendants> formDefendantsList = extractDefendants(formValidationResult);
            builder.withFormDefendants(formDefendantsList)
                    .withErrors(formValidationResult.getErrorList())
                    .withSubmissionStatus(formValidationResult.getSubmissionStatus());
        }


        return apply(of(builder.build()));
    }

    private static Optional<String> getSowRef(final Optional<JsonObject> prosecutionCase) {
        Optional<String> sowRef = Optional.empty();
        if(prosecutionCase.isPresent()) {
            final boolean isCivil = prosecutionCase.get().getBoolean("isCivil", false);
            if(isCivil){
                sowRef = Optional.of(SOW_REF_VALUE);
            }
        }
        return sowRef;
    }

    private String fetchString(final JsonObject processReceivedCpsServeCotrJson, final String key) {
        if (processReceivedCpsServeCotrJson.containsKey(key)) {
            return processReceivedCpsServeCotrJson.getString(key);
        }
        return null;
    }

    private FormValidationResult runDefendantMatchingForCotr(
            final JsonObject processReceivedCpsServeCotr,
            final JsonObject prosecutionCase,
            final CpsFormValidator cpsFormValidator,
            final JsonArray defendantIds) {
        LOGGER.info("runDefendantMatchingForCotr with Defendant Ids: {}", defendantIds);

        return cpsFormValidator
                .validateCotr(processReceivedCpsServeCotr, prosecutionCase, defendantIds);
    }

    private List<FormDefendants> extractDefendants(final FormValidationResult formValidationResult) {

        final List<FormDefendants> formDefendantsList = new ArrayList<>();

        if (nonNull(formValidationResult.getFormDefendants())) {
            LOGGER.info("formValidationResult.getFormDefendants() {}", formValidationResult.getFormDefendants());
            formValidationResult
                    .getFormDefendants()
                    .getJsonArray(FORM_DEFENDANTS)
                    .getValuesAs(JsonObject.class)
                    .stream()
                    .map(formDefendant -> new FormDefendants(formDefendant.getString(CPS_DEFENDANT_ID, null), fromString(formDefendant.getString(DEFENDANT_ID))))
                    .forEach(formDefendantsList::add);
        }

        return formDefendantsList;
    }


    public enum PendingType {
        UNKNOWN("UNKNOWN"),
        PET("PET"),
        BCM("BCM");

        private final String value;

        PendingType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}